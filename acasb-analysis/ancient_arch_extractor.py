import cv2
import numpy as np
from skimage import feature, filters
from typing import Dict, Tuple
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class AncientArchExtractor:
    def __init__(self):
        self.target_size = (400, 400)
        
        self.hue_ranges = {
            "yellow": (20, 35),
            "red_1": (0, 10),
            "red_2": (170, 180),
            "blue": (100, 125),
            "green": (35, 85)
        }
    
    def preprocess_image(self, image: np.ndarray) -> np.ndarray:
        try:
            lab = cv2.cvtColor(image, cv2.COLOR_BGR2LAB)
            l, a, b = cv2.split(lab)
            
            clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
            l_enhanced = clahe.apply(l)
            
            enhanced_lab = cv2.merge([l_enhanced, a, b])
            enhanced_image = cv2.cvtColor(enhanced_lab, cv2.COLOR_LAB2BGR)
            
            resized = cv2.resize(enhanced_image, self.target_size, interpolation=cv2.INTER_AREA)
            
            logger.info(f"Applied CLAHE and resized to {self.target_size}")
            return resized
        except Exception as e:
            logger.error(f"Image preprocessing failed: {e}")
            raise
    
    def extract_hue_ratios(self, hsv_image: np.ndarray) -> Dict[str, float]:
        try:
            h, s, v = cv2.split(hsv_image)
            
            hue_ratios = {}
            
            for color_name, (h_min, h_max) in self.hue_ranges.items():
                if h_min <= h_max:
                    mask = (h >= h_min) & (h <= h_max)
                else:
                    mask = (h >= h_min) | (h <= h_max)
                
                ratio = np.sum(mask) / (h.shape[0] * h.shape[1])
                hue_ratios[f"ratio_{color_name}"] = float(ratio)
            
            return hue_ratios
        except Exception as e:
            logger.error(f"Hue ratio extraction failed: {e}")
            raise
    
    def extract_achromatic_ratios(self, hsv_image: np.ndarray) -> Dict[str, float]:
        try:
            h, s, v = cv2.split(hsv_image)
            
            gray_white_mask = (s < 30) & (v > 200)
            black_mask = v < 50
            
            ratio_gray_white = np.sum(gray_white_mask) / (h.shape[0] * h.shape[1])
            ratio_black = np.sum(black_mask) / (h.shape[0] * h.shape[1])
            
            return {
                "ratio_gray_white": float(ratio_gray_white),
                "ratio_black": float(ratio_black)
            }
        except Exception as e:
            logger.error(f"Achromatic ratio extraction failed: {e}")
            raise
    
    def extract_hsv_statistics(self, hsv_image: np.ndarray) -> Dict[str, float]:
        try:
            h, s, v = cv2.split(hsv_image)
            
            h_mean = np.mean(h) / 180.0
            h_std = np.std(h) / 180.0
            s_mean = np.mean(s) / 255.0
            s_std = np.std(s) / 255.0
            v_mean = np.mean(v) / 255.0
            v_std = np.std(v) / 255.0
            
            return {
                "h_mean": float(h_mean),
                "h_std": float(h_std),
                "s_mean": float(s_mean),
                "s_std": float(s_std),
                "v_mean": float(v_mean),
                "v_std": float(v_std)
            }
        except Exception as e:
            logger.error(f"HSV statistics extraction failed: {e}")
            raise
    
    def extract_edge_density(self, image: np.ndarray) -> float:
        try:
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            
            edges = cv2.Canny(gray, 50, 150)
            
            edge_density = np.sum(edges > 0) / (edges.shape[0] * edges.shape[1])
            
            return float(edge_density)
        except Exception as e:
            logger.error(f"Edge density extraction failed: {e}")
            raise
    
    def extract_shannon_entropy(self, image: np.ndarray) -> float:
        try:
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            
            hist = cv2.calcHist([gray], [0], None, [256], [0, 256])
            hist = hist.flatten()
            hist = hist / (hist.sum() + 1e-10)
            
            entropy = -np.sum(hist * np.log2(hist + 1e-10))
            
            normalized_entropy = entropy / 8.0
            
            return float(normalized_entropy)
        except Exception as e:
            logger.error(f"Shannon entropy extraction failed: {e}")
            raise
    
    def extract_glcm_features(self, image: np.ndarray) -> Dict[str, float]:
        try:
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            
            glcm = feature.graycomatrix(gray, distances=[1], angles=[0], symmetric=True, normed=True)
            
            contrast = feature.graycoprops(glcm, 'contrast')[0]
            dissimilarity = feature.graycoprops(glcm, 'dissimilarity')[0]
            homogeneity = feature.graycoprops(glcm, 'homogeneity')[0]
            asm = feature.graycoprops(glcm, 'ASM')[0]
            
            return {
                "contrast": float(contrast / 255.0),
                "dissimilarity": float(dissimilarity / 255.0),
                "homogeneity": float(homogeneity),
                "asm": float(asm)
            }
        except Exception as e:
            logger.error(f"GLCM feature extraction failed: {e}")
            raise
    
    def extract_features(self, image_path: str) -> Tuple[Dict[str, float], np.ndarray]:
        try:
            logger.info(f"Extracting features from: {image_path}")
            
            image = cv2.imread(image_path)
            if image is None:
                raise ValueError(f"Cannot read image: {image_path}")
            
            preprocessed = self.preprocess_image(image)
            
            hsv_image = cv2.cvtColor(preprocessed, cv2.COLOR_BGR2HSV)
            
            hue_ratios = self.extract_hue_ratios(hsv_image)
            
            achromatic_ratios = self.extract_achromatic_ratios(hsv_image)
            
            hsv_stats = self.extract_hsv_statistics(hsv_image)
            
            edge_density = self.extract_edge_density(preprocessed)
            
            shannon_entropy = self.extract_shannon_entropy(preprocessed)
            
            glcm_features = self.extract_glcm_features(preprocessed)
            
            feature_dict = {
                **hue_ratios,
                **achromatic_ratios,
                **hsv_stats,
                "edge_density": edge_density,
                "entropy": shannon_entropy,
                **glcm_features
            }
            
            feature_keys = [
                "ratio_yellow", "ratio_red_1", "ratio_red_2", "ratio_blue", "ratio_green",
                "ratio_gray_white", "ratio_black",
                "h_mean", "h_std", "s_mean", "s_std", "v_mean", "v_std",
                "edge_density", "entropy", "contrast", "dissimilarity", "homogeneity", "asm"
            ]
            
            feature_vector = np.array([feature_dict.get(key, 0.0) for key in feature_keys], dtype=np.float32)
            
            logger.info(f"Extracted {len(feature_vector)} features from {image_path}")
            
            return feature_dict, feature_vector
            
        except Exception as e:
            logger.error(f"Feature extraction failed: {image_path}, error: {e}")
            
            feature_keys = [
                "ratio_yellow", "ratio_red_1", "ratio_red_2", "ratio_blue", "ratio_green",
                "ratio_gray_white", "ratio_black",
                "h_mean", "h_std", "s_mean", "s_std", "v_mean", "v_std",
                "edge_density", "entropy", "contrast", "dissimilarity", "homogeneity", "asm"
            ]
            
            zero_dict = {key: 0.0 for key in feature_keys}
            zero_vector = np.zeros(len(feature_keys), dtype=np.float32)
            
            return zero_dict, zero_vector