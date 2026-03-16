import os
import sys
from typing import Tuple
from pathlib import Path

current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, current_dir)

from ancient_arch_extractor import AncientArchExtractor
from resnet_hybrid_pipeline import HybridClassifier, HybridConfig, HybridFeatureExtractor
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

IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
WRAPPER_DIRS = {"dataset_fixed", "dataset", "images", "imgs"}
DEFAULT_DATASET_DIR = str((Path(current_dir).parent / "datasets").resolve())
DEFAULT_MODEL_DIR = str((Path(current_dir) / "models").resolve())

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


class HybridTrainer:
    def __init__(self, device: str = "cpu"):
        self.device = device
        self.extractor = HybridFeatureExtractor(device=device)

    def _infer_label(self, image_path: Path, base_dir: Path) -> str:
        parent = image_path.parent
        if parent.name in WRAPPER_DIRS and parent.parent != base_dir:
            return parent.parent.name
        return parent.name

    def collect_samples(self, base_dir: str) -> list[tuple[Path, str]]:
        root = Path(base_dir).resolve()
        samples: list[tuple[Path, str]] = []
        for path in sorted(root.rglob("*")):
            if not path.is_file() or path.suffix.lower() not in IMAGE_SUFFIXES:
                continue
            label = self._infer_label(path, root)
            if label.startswith("."):
                continue
            samples.append((path, label))
        return samples

    def build_feature_matrix(self, samples: list[tuple[Path, str]], augment_factor: int) -> tuple[np.ndarray, np.ndarray]:
        features: list[np.ndarray] = []
        labels: list[str] = []

        for image_path, label in samples:
            features.append(self.extractor.extract_features(image_path, augmented=False))
            labels.append(label)
            for _ in range(max(augment_factor, 0)):
                features.append(self.extractor.extract_features(image_path, augmented=True))
                labels.append(label)

        return np.vstack(features), np.array(labels)

    def run(
        self,
        base_dir: str,
        save_dir: str,
        augment_factor: int = 2,
        pca_components: int | float = 50,
        svm_kernel: str = "rbf",
        svm_c: float = 1.0,
    ) -> bool:
        try:
            print("=" * 70)
            print("🚀 Starting ResNet18 + SVM/MLP Hybrid Training Pipeline")
            print("=" * 70)
            print(f"Dataset directory: {base_dir}")
            print(f"Model save directory: {save_dir}")
            print("=" * 70)
            print()

            samples = self.collect_samples(base_dir)
            if not samples:
                logger.error("No valid samples found in dataset!")
                return False

            X, y = self.build_feature_matrix(samples, augment_factor)
            logger.info("Collected %s raw samples, expanded to %s feature rows", len(samples), len(y))
            logger.info("Feature matrix shape: %s", X.shape)

            config = HybridConfig(
                pca_components=pca_components,
                svm_kernel=svm_kernel,
                svm_c=svm_c,
            )
            classifier = HybridClassifier(config=config)
            cv_result = classifier.cross_validate(X, y, n_splits=5)
            logger.info("Cross validation mean=%s std=%s folds=%s",
                        cv_result["mean_accuracy"], cv_result["std_accuracy"], cv_result["fold_scores"])

            X_train, X_test, y_train, y_test = train_test_split(
                X, y, test_size=0.2, random_state=config.random_state, stratify=y
            )
            classifier.fit(X_train, y_train)
            y_pred = classifier.predict(X_test)
            accuracy = accuracy_score(y_test, y_pred)

            print("\n" + "=" * 70)
            print("Classification Report:")
            print("=" * 70)
            print(classification_report(y_test, y_pred))
            print("Confusion Matrix:")
            print(confusion_matrix(y_test, y_pred))

            os.makedirs(save_dir, exist_ok=True)
            model_path = os.path.join(save_dir, 'resnet_hybrid_bundle.pkl')
            classifier.save(model_path)

            print("\n" + "=" * 70)
            print("✅ Hybrid training completed successfully!")
            print("=" * 70)
            print(f"\nModel saved to: {model_path}")
            print(f"Raw samples processed: {len(samples)}")
            print(f"Expanded samples: {len(y)}")
            print(f"Cross-validation mean: {cv_result['mean_accuracy']}")
            print(f"Cross-validation std: {cv_result['std_accuracy']}")
            print(f"Holdout accuracy: {round(float(accuracy), 4)}")
            print("=" * 70)
            return True
        except Exception as e:
            logger.error(f"Hybrid training pipeline failed: {e}")
            return False

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="ACASB local model trainer")
    parser.add_argument("--base-dir", default=DEFAULT_DATASET_DIR)
    parser.add_argument("--save-dir", default=DEFAULT_MODEL_DIR)
    parser.add_argument("--model-type", default="mlp", choices=["mlp", "hybrid"])
    parser.add_argument("--augment-factor", type=int, default=2)
    parser.add_argument("--pca-components", default="50")
    parser.add_argument("--svm-kernel", default="rbf", choices=["linear", "rbf", "poly", "sigmoid"])
    parser.add_argument("--svm-c", type=float, default=1.0)
    parser.add_argument("--device", default="cpu")
    args = parser.parse_args()

    try:
        if args.model_type == "hybrid":
            trainer = HybridTrainer(device=args.device)
            pca_components = float(args.pca_components) if "." in args.pca_components else int(args.pca_components)
            success = trainer.run(
                args.base_dir,
                args.save_dir,
                augment_factor=args.augment_factor,
                pca_components=pca_components,
                svm_kernel=args.svm_kernel,
                svm_c=args.svm_c,
            )
        else:
            trainer = MLPTrainer()
            success = trainer.run(args.base_dir, args.save_dir)
        
        if not success:
            print("\n❌ Training failed!")
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n\n⚠️  Training interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Training failed: {e}")
        sys.exit(1)
