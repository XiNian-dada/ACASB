from fastapi import FastAPI, HTTPException, UploadFile, File
from pydantic import BaseModel
import uvicorn
import logging
import os
import tempfile
from typing import Dict
from ancient_arch_extractor import AncientArchExtractor
from mlp_inference import MLPInference
import joblib
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.neural_network import MLPClassifier
from sklearn.metrics import classification_report, accuracy_score

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Order-Decoder API",
    description="Ancient building classification using ML",
    version="3.0.0"
)

extractor = AncientArchExtractor()
inference = MLPInference()

class TrainRequest(BaseModel):
    base_dir: str = r"E:\Code\ACASB\datasets"
    save_dir: str = r"E:\Code\ACASB\acasb-analysis\models"

class PredictRequest(BaseModel):
    image_path: str

class TrainResponse(BaseModel):
    success: bool
    message: str
    samples_processed: int = None
    royal_samples: int = None
    civilian_samples: int = None
    accuracy: float = None
    model_path: str = None

class PredictResponse(BaseModel):
    success: bool
    message: str
    prediction: str = None
    confidence: float = None
    royal_ratio: float = None
    entropy_score: float = None
    edge_density: float = None
    texture_complexity: float = None

@app.get("/health")
async def health_check() -> Dict[str, str]:
    logger.info("Health check received")
    return {"status": "healthy", "message": "API is ready"}

@app.post("/train", response_model=TrainResponse)
async def train_model(request: TrainRequest) -> Dict:
    try:
        logger.info("Training MLP model...")
        
        base_dir = request.base_dir
        save_dir = request.save_dir
        
        logger.info(f"Scanning dataset in: {base_dir}")
        
        data = []
        
        for category in ["royal", "civilian"]:
            category_dir = os.path.join(base_dir, category)
            
            if not os.path.exists(category_dir):
                logger.warning(f"Category directory not found: {category_dir}")
                continue
            
            for filename in os.listdir(category_dir):
                if filename.endswith(('.jpg', '.jpeg', '.png')):
                    image_path = os.path.join(category_dir, filename)
                    
                    try:
                        feature_dict, feature_vector = extractor.extract_features(image_path)
                        
                        label = 1 if category == "royal" else 0
                        
                        data.append({
                            'image_path': image_path,
                            'category': category,
                            'label': label,
                            **feature_dict
                        })
                        
                        logger.info(f"Processed: {filename}")
                        
                    except Exception as e:
                        logger.error(f"Failed to process {filename}: {e}")
        
        df = pd.DataFrame(data)
        
        logger.info(f"Total samples: {len(df)}")
        logger.info(f"Royal samples: {len(df[df['label'] == 1])}")
        logger.info(f"Civilian samples: {len(df[df['label'] == 0])}")
        
        if len(df) == 0:
            raise HTTPException(status_code=400, detail="No valid samples found in dataset")
        
        feature_keys = [
            "ratio_yellow", "ratio_red_1", "ratio_red_2", "ratio_blue", "ratio_green",
            "ratio_gray_white", "ratio_black",
            "h_mean", "h_std", "s_mean", "s_std", "v_mean", "v_std",
            "edge_density", "entropy", "contrast", "dissimilarity", "homogeneity", "asm"
        ]
        
        X = df[feature_keys].values
        y = df['label'].values
        
        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X)
        
        logger.info(f"Feature matrix shape: {X_scaled.shape}")
        logger.info(f"Labels shape: {y.shape}")
        
        X_train, X_test, y_train, y_test = train_test_split(
            X_scaled, y, test_size=0.2, random_state=42, stratify=y
        )
        
        logger.info(f"Training set size: {X_train.shape[0]}")
        logger.info(f"Test set size: {X_test.shape[0]}")
        
        mlp = MLPClassifier(
            hidden_layer_sizes=(64, 32, 16),
            activation='relu',
            solver='adam',
            alpha=0.0001,
            batch_size=32,
            learning_rate='adaptive',
            learning_rate_init=0.001,
            max_iter=500,
            random_state=42,
            early_stopping=True,
            validation_fraction=0.1,
            n_iter_no_change=10,
            verbose=True
        )
        
        mlp.fit(X_train, y_train)
        
        y_pred = mlp.predict(X_test)
        accuracy = accuracy_score(y_test, y_pred)
        
        logger.info(f"Training accuracy: {accuracy:.4f}")
        logger.info(f"Model trained successfully!")
        
        os.makedirs(save_dir, exist_ok=True)
        
        model_path = os.path.join(save_dir, 'mlp_model.pkl')
        scaler_path = os.path.join(save_dir, 'scaler.pkl')
        
        import joblib
        joblib.dump(mlp, model_path)
        joblib.dump(scaler, scaler_path)
        
        logger.info(f"Model saved to: {model_path}")
        logger.info(f"Scaler saved to: {scaler_path}")
        
        result = {
            "success": True,
            "message": "Training completed",
            "samples_processed": len(df),
            "royal_samples": len(df[df['label'] == 1]),
            "civilian_samples": len(df[df['label'] == 0]),
            "accuracy": round(accuracy, 4),
            "model_path": model_path
        }
        
        logger.info(f"Training completed: {len(df)} samples processed")
        
        return result
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Training failed: {e}")
        raise HTTPException(status_code=500, detail=f"Training failed: {str(e)}")

@app.post("/predict", response_model=PredictResponse)
async def predict_image(request: PredictRequest) -> Dict:
    try:
        logger.info(f"Prediction request received: {request.image_path}")
        
        if not os.path.exists(request.image_path):
            raise HTTPException(status_code=404, detail=f"Image file not found: {request.image_path}")
        
        if not inference.load_model():
            logger.warning("Model not loaded, attempting to load...")
            if not inference.load_model():
                raise HTTPException(status_code=500, detail="Model files not found. Please train the model first.")
        
        result = inference.predict(request.image_path)
        
        logger.info(f"Prediction completed: {result['prediction']}")
        
        return result
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Prediction failed: {e}")
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")

@app.get("/")
async def root():
    return {
        "message": "Order-Decoder API",
        "description": "Ancient building classification using ML",
        "version": "3.0.0",
        "endpoints": {
            "GET /health": "Health check",
            "POST /train": "Train MLP model",
            "POST /predict": "Predict building type",
            "GET /": "API information"
        }
    }

if __name__ == "__main__":
    logger.info("Starting Order-Decoder API service...")
    uvicorn.run(
        app,
        host="127.0.0.1",
        port=5000,
        log_level="info"
    )