import os
import sys

current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, current_dir)

from ancient_arch_extractor import AncientArchExtractor
import numpy as np
import joblib
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class MLPInference:
    def __init__(self):
        self.extractor = AncientArchExtractor()
        self.model = None
        self.scaler = None
        self.model_path = os.path.join(current_dir, "models", "mlp_model.pkl")
        self.scaler_path = os.path.join(current_dir, "models", "scaler.pkl")
    
    def load_model(self):
        try:
            if os.path.exists(self.model_path) and os.path.exists(self.scaler_path):
                self.model = joblib.load(self.model_path)
                self.scaler = joblib.load(self.scaler_path)
                logger.info(f"Model loaded from: {self.model_path}")
                logger.info(f"Scaler loaded from: {self.scaler_path}")
                return True
            else:
                logger.error("Model files not found!")
                return False
        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            return False
    
    def predict(self, image_path: str):
        try:
            if self.model is None or self.scaler is None:
                logger.error("Model not loaded!")
                return {
                    "success": False,
                    "message": "Model not loaded",
                    "prediction": "unknown",
                    "confidence": 0.0,
                    "royal_ratio": 0.0,
                    "entropy_score": 0.0,
                    "edge_density": 0.0,
                    "texture_complexity": 0.0
                }
            
            logger.info(f"Processing image: {image_path}")
            
            feature_dict, feature_vector = self.extractor.extract_features(image_path)
            
            if len(feature_vector) == 0:
                logger.error("Failed to extract features!")
                return {
                    "success": False,
                    "message": "Failed to extract features",
                    "prediction": "unknown",
                    "confidence": 0.0,
                    "royal_ratio": 0.0,
                    "entropy_score": 0.0,
                    "edge_density": 0.0,
                    "texture_complexity": 0.0
                }
            
            feature_vector_reshaped = feature_vector.reshape(1, -1)
            
            feature_vector_scaled = self.scaler.transform(feature_vector_reshaped)
            
            prediction = self.model.predict(feature_vector_scaled)[0]
            prediction_proba = self.model.predict_proba(feature_vector_scaled)[0]
            
            prediction_label = "royal" if prediction == 1 else "civilian"
            confidence = float(prediction_proba[prediction])
            
            royal_ratio = feature_dict.get('ratio_yellow', 0) + feature_dict.get('ratio_red_1', 0) + feature_dict.get('ratio_red_2', 0)
            entropy_score = feature_dict.get('entropy', 0)
            edge_density = feature_dict.get('edge_density', 0)
            texture_complexity = feature_dict.get('contrast', 0)
            
            result = {
                "success": True,
                "message": "Prediction completed",
                "prediction": prediction_label,
                "confidence": round(confidence, 4),
                "royal_ratio": round(royal_ratio, 4),
                "entropy_score": round(entropy_score, 4),
                "edge_density": round(edge_density, 4),
                "texture_complexity": round(texture_complexity, 4)
            }
            
            logger.info(f"Prediction: {prediction_label} (confidence: {confidence:.4f})")
            
            return result
            
        except Exception as e:
            logger.error(f"Prediction failed: {e}")
            return {
                "success": False,
                "message": f"Prediction failed: {str(e)}",
                "prediction": "unknown",
                "confidence": 0.0,
                "royal_ratio": 0.0,
                "entropy_score": 0.0,
                "edge_density": 0.0,
                "texture_complexity": 0.0
            }

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description='MLP Inference for Ancient Building Classification')
    parser.add_argument('--image', type=str, help='Path to image file')
    parser.add_argument('--load-model', action='store_true', help='Load model before prediction')
    
    args = parser.parse_args()
    
    inference = MLPInference()
    
    if args.load_model:
        print("=" * 70)
        print("🔄 Loading Model...")
        print("=" * 70)
        
        if not inference.load_model():
            print("\n❌ Failed to load model!")
            sys.exit(1)
        
        print("\n✅ Model loaded successfully!")
    
    if args.image:
        print("=" * 70)
        print("🔮 Running Inference...")
        print("=" * 70)
        print(f"Image: {args.image}")
        print("=" * 70)
        
        result = inference.predict(args.image)
        
        print("\n" + "=" * 70)
        print("📊 Prediction Result")
        print("=" * 70)
        print(f"Success: {result['success']}")
        print(f"Message: {result['message']}")
        print(f"Prediction: {result['prediction']}")
        print(f"Confidence: {result['confidence']}")
        print(f"Royal Ratio: {result['royal_ratio']}")
        print(f"Entropy Score: {result['entropy_score']}")
        print(f"Edge Density: {result['edge_density']}")
        print(f"Texture Complexity: {result['texture_complexity']}")
        print("=" * 70)
    else:
        print("\nUsage:")
        print("  python mlp_inference.py --image <image_path>")
        print("  python mlp_inference.py --load-model")
        print("  python mlp_inference.py --image <image_path> --load-model")
        sys.exit(0)