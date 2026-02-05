import os
import sys
from typing import Tuple
sys.path.append('E:/Code/ACASB/acasb-analysis')

from ancient_arch_extractor import AncientArchExtractor
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.neural_network import MLPClassifier
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
import joblib
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class MLPTrainer:
    def __init__(self):
        self.extractor = AncientArchExtractor()
        self.feature_keys = [
            "ratio_yellow", "ratio_red_1", "ratio_red_2", "ratio_blue", "ratio_green",
            "ratio_gray_white", "ratio_black",
            "h_mean", "h_std", "s_mean", "s_std", "v_mean", "v_std",
            "edge_density", "entropy", "contrast", "dissimilarity", "homogeneity", "asm"
        ]
    
    def scan_dataset(self, base_dir: str) -> pd.DataFrame:
        try:
            logger.info(f"Scanning dataset in: {base_dir}")
            
            data = []
            
            for category in ["royal", "civilian"]:
                category_dir = os.path.join(base_dir, category, "dataset_fixed")
                
                if not os.path.exists(category_dir):
                    logger.warning(f"Category directory not found: {category_dir}")
                    continue
                
                for filename in os.listdir(category_dir):
                    if filename.endswith(('.jpg', '.jpeg', '.png')):
                        image_path = os.path.join(category_dir, filename)
                        
                        try:
                            feature_dict, feature_vector = self.extractor.extract_features(image_path)
                            
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
            
            return df
            
        except Exception as e:
            logger.error(f"Failed to scan dataset: {e}")
            return pd.DataFrame()
    
    def prepare_training_data(self, df: pd.DataFrame) -> Tuple[np.ndarray, np.ndarray, StandardScaler]:
        try:
            X = df[self.feature_keys].values
            y = df['label'].values
            
            scaler = StandardScaler()
            X_scaled = scaler.fit_transform(X)
            
            logger.info(f"Feature matrix shape: {X_scaled.shape}")
            logger.info(f"Labels shape: {y.shape}")
            
            return X_scaled, y, scaler
            
        except Exception as e:
            logger.error(f"Failed to prepare training data: {e}")
            return np.array([]), np.array([]), None
    
    def train_model(self, X: np.ndarray, y: np.ndarray) -> MLPClassifier:
        try:
            logger.info("Training MLP classifier...")
            
            X_train, X_test, y_train, y_test = train_test_split(
                X, y, test_size=0.2, random_state=42, stratify=y
            )
            
            mlp = MLPClassifier(
                hidden_layer_sizes=(128, 64, 32),
                activation='relu',
                solver='adam',
                alpha=0.0001,
                batch_size=32,
                learning_rate='adaptive',
                learning_rate_init=0.001,
                max_iter=2000,
                random_state=42,
                early_stopping=True,
                validation_fraction=0.1,
                n_iter_no_change=50,
                verbose=True
            )
            
            mlp.fit(X_train, y_train)
            
            y_pred = mlp.predict(X_test)
            accuracy = accuracy_score(y_test, y_pred)
            
            logger.info(f"Training accuracy: {accuracy:.4f}")
            logger.info(f"Model trained successfully!")
            
            print("\n" + "=" * 70)
            print("Classification Report:")
            print("=" * 70)
            print(classification_report(y_test, y_pred, target_names=['Civilian', 'Royal']))
            
            return mlp
            
        except Exception as e:
            logger.error(f"Failed to train model: {e}")
            return None
    
    def save_model(self, model: MLPClassifier, scaler: StandardScaler, save_dir: str):
        try:
            os.makedirs(save_dir, exist_ok=True)
            
            model_path = os.path.join(save_dir, 'mlp_model.pkl')
            scaler_path = os.path.join(save_dir, 'scaler.pkl')
            
            joblib.dump(model, model_path)
            joblib.dump(scaler, scaler_path)
            
            logger.info(f"Model saved to: {model_path}")
            logger.info(f"Scaler saved to: {scaler_path}")
            
        except Exception as e:
            logger.error(f"Failed to save model: {e}")
    
    def run(self, base_dir: str, save_dir: str):
        try:
            print("=" * 70)
            print("🚀 Starting MLP Training Pipeline")
            print("=" * 70)
            print(f"Dataset directory: {base_dir}")
            print(f"Model save directory: {save_dir}")
            print("=" * 70)
            print()
            
            df = self.scan_dataset(base_dir)
            
            if len(df) == 0:
                logger.error("No valid samples found in dataset!")
                return False
            
            X, y, scaler = self.prepare_training_data(df)
            
            model = self.train_model(X, y)
            
            if model is None:
                logger.error("Failed to train model!")
                return False
            
            self.save_model(model, scaler, save_dir)
            
            print("\n" + "=" * 70)
            print("✅ Training completed successfully!")
            print("=" * 70)
            print(f"\nModel saved to: {save_dir}")
            print(f"Total samples processed: {len(df)}")
            print("\nNext steps:")
            print("1. Use mlp_inference.py for prediction")
            print("2. Start api_server.py for serving predictions")
            print("=" * 70)
            
            return True
            
        except Exception as e:
            logger.error(f"Training pipeline failed: {e}")
            return False

if __name__ == "__main__":
    base_dir = r"E:\Code\ACASB\datasets"
    save_dir = r"E:\Code\ACASB\acasb-analysis\models"
    
    trainer = MLPTrainer()
    
    try:
        success = trainer.run(base_dir, save_dir)
        
        if not success:
            print("\n❌ Training failed!")
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n\n⚠️  Training interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Training failed: {e}")
        sys.exit(1)