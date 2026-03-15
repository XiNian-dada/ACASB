from __future__ import annotations

import logging
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

import joblib
import numpy as np
from sklearn.decomposition import PCA
from sklearn.model_selection import StratifiedKFold
from sklearn.neural_network import MLPClassifier
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.svm import SVC

logger = logging.getLogger(__name__)

IMAGENET_MEAN = [0.485, 0.456, 0.406]
IMAGENET_STD = [0.229, 0.224, 0.225]


def _load_torch_stack():
    try:
        import torch
        import torch.nn as nn
        from PIL import Image
        from torchvision import models, transforms
    except ImportError as exc:
        raise RuntimeError(
            "Experimental ResNet18 pipeline requires optional dependencies. "
            "Install acasb-analysis/requirements-experimental.txt first."
        ) from exc
    return torch, nn, Image, models, transforms


class ResNet18FeatureExtractor:
    """
    Experimental feature extractor based on the article workflow:
    pretrained ResNet18 -> frozen backbone -> remove FC -> 512-d vector.

    This pipeline is intentionally kept separate from the current production
    MLP flow and is not imported by the main api_server.
    """

    def __init__(self, device: str = "cpu", image_size: int = 224):
        torch, nn, image_cls, models, transforms = _load_torch_stack()
        self._torch = torch
        self._nn = nn
        self._image_cls = image_cls
        self._models = models
        self._transforms = transforms
        self.device = torch.device(device)
        self.image_size = image_size
        self.model = self._load_model()
        self.base_transform = self._build_base_transform()
        self.augment_transform = self._build_augment_transform()

    def _load_model(self):
        model = self._models.resnet18(weights=self._models.ResNet18_Weights.DEFAULT)
        for param in model.parameters():
            param.requires_grad = False
        backbone = self._nn.Sequential(*list(model.children())[:-1])
        backbone.eval()
        backbone.to(self.device)
        return backbone

    def _build_base_transform(self):
        return self._transforms.Compose([
            self._transforms.Resize(256),
            self._transforms.CenterCrop(self.image_size),
            self._transforms.ToTensor(),
            self._transforms.Normalize(mean=IMAGENET_MEAN, std=IMAGENET_STD),
        ])

    def _build_augment_transform(self):
        return self._transforms.Compose([
            self._transforms.RandomResizedCrop(self.image_size, scale=(0.8, 1.0)),
            self._transforms.RandomHorizontalFlip(p=0.5),
            self._transforms.RandomRotation(degrees=15),
            self._transforms.ColorJitter(
                brightness=0.2,
                contrast=0.2,
                saturation=0.2,
                hue=0.1,
            ),
            self._transforms.ToTensor(),
            self._transforms.Normalize(mean=IMAGENET_MEAN, std=IMAGENET_STD),
        ])

    def extract_features(self, image_path: str | Path, augmented: bool = False) -> np.ndarray:
        transform = self.augment_transform if augmented else self.base_transform
        with self._image_cls.open(image_path) as image:
            image = image.convert("RGB")
            tensor = transform(image).unsqueeze(0).to(self.device)

        with self._torch.no_grad():
            features = self.model(tensor)

        return features.squeeze().cpu().numpy().astype(np.float32)


@dataclass
class HybridConfig:
    pca_components: int | float = 40
    svm_kernel: str = "rbf"
    svm_c: float = 1.0
    svm_gamma: str = "scale"
    mlp_hidden_layers: tuple[int, ...] = (64, 32)
    mlp_alpha: float = 0.05
    mlp_batch_size: int = 16
    mlp_max_iter: int = 2000
    mlp_validation_fraction: float = 0.2
    mlp_n_iter_no_change: int = 30
    random_state: int = 42


class HybridClassifier:
    """
    Experimental classifier mirroring the article:
    ResNet features -> StandardScaler -> PCA -> SVM + MLP -> soft voting.
    """

    def __init__(self, config: HybridConfig | None = None):
        self.config = config or HybridConfig()
        self.scaler = StandardScaler()
        self.pca = PCA(
            n_components=self.config.pca_components,
            random_state=self.config.random_state,
        )
        self.svm = SVC(
            kernel=self.config.svm_kernel,
            C=self.config.svm_c,
            gamma=self.config.svm_gamma,
            probability=True,
            random_state=self.config.random_state,
        )
        self.mlp = MLPClassifier(
            hidden_layer_sizes=self.config.mlp_hidden_layers,
            activation="relu",
            solver="adam",
            alpha=self.config.mlp_alpha,
            batch_size=self.config.mlp_batch_size,
            learning_rate="adaptive",
            learning_rate_init=0.001,
            max_iter=self.config.mlp_max_iter,
            random_state=self.config.random_state,
            early_stopping=True,
            validation_fraction=self.config.mlp_validation_fraction,
            n_iter_no_change=self.config.mlp_n_iter_no_change,
            verbose=False,
        )
        self.label_encoder = LabelEncoder()
        self._is_fitted = False

    def fit(self, X: np.ndarray, y: Iterable[str]) -> "HybridClassifier":
        encoded = self.label_encoder.fit_transform(list(y))
        X_scaled = self.scaler.fit_transform(X)
        X_pca = self.pca.fit_transform(X_scaled)
        self.svm.fit(X_pca, encoded)
        self.mlp.fit(X_pca, encoded)
        self._is_fitted = True
        return self

    def predict_proba(self, X: np.ndarray) -> np.ndarray:
        self._ensure_fitted()
        X_scaled = self.scaler.transform(X)
        X_pca = self.pca.transform(X_scaled)
        svm_proba = self.svm.predict_proba(X_pca)
        mlp_proba = self.mlp.predict_proba(X_pca)
        return (svm_proba + mlp_proba) / 2.0

    def predict(self, X: np.ndarray) -> np.ndarray:
        ensemble_proba = self.predict_proba(X)
        indices = np.argmax(ensemble_proba, axis=1)
        return self.label_encoder.inverse_transform(indices)

    def predict_single(self, feature_vector: np.ndarray) -> dict[str, Any]:
        if feature_vector.ndim != 1:
            raise ValueError("feature_vector must be a 1D vector")
        probabilities = self.predict_proba(feature_vector.reshape(1, -1))[0]
        best_index = int(np.argmax(probabilities))
        class_names = self.label_encoder.classes_
        return {
            "prediction": str(class_names[best_index]),
            "confidence": float(probabilities[best_index]),
            "probabilities": {
                str(class_name): float(probabilities[idx])
                for idx, class_name in enumerate(class_names)
            },
        }

    def cross_validate(self, X: np.ndarray, y: Iterable[str], n_splits: int = 5) -> dict[str, Any]:
        labels = list(y)
        encoded = self.label_encoder.fit_transform(labels)
        skf = StratifiedKFold(n_splits=n_splits, shuffle=True, random_state=self.config.random_state)

        fold_scores: list[float] = []
        for train_idx, test_idx in skf.split(X, encoded):
            fold_model = HybridClassifier(config=self.config)
            y_train = [labels[index] for index in train_idx]
            y_test = [labels[index] for index in test_idx]
            fold_model.fit(X[train_idx], y_train)
            fold_pred = fold_model.predict(X[test_idx])
            accuracy = float(np.mean(fold_pred == np.array(y_test)))
            fold_scores.append(accuracy)

        return {
            "fold_scores": [round(score, 4) for score in fold_scores],
            "mean_accuracy": round(float(np.mean(fold_scores)), 4),
            "std_accuracy": round(float(np.std(fold_scores)), 4),
        }

    def save(self, output_path: str | Path) -> None:
        self._ensure_fitted()
        payload = {
            "config": self.config,
            "scaler": self.scaler,
            "pca": self.pca,
            "svm": self.svm,
            "mlp": self.mlp,
            "label_encoder": self.label_encoder,
        }
        joblib.dump(payload, output_path)

    @classmethod
    def load(cls, model_path: str | Path) -> "HybridClassifier":
        payload = joblib.load(model_path)
        model = cls(config=payload["config"])
        model.scaler = payload["scaler"]
        model.pca = payload["pca"]
        model.svm = payload["svm"]
        model.mlp = payload["mlp"]
        model.label_encoder = payload["label_encoder"]
        model._is_fitted = True
        return model

    def _ensure_fitted(self) -> None:
        if not self._is_fitted:
            raise RuntimeError("HybridClassifier is not fitted")
