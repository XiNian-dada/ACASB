import json
import sys
sys.path.append('E:/Code/ACASB/acasb-analysis')

from ancient_arch_extractor import AncientArchExtractor

def test_feature_extraction(image_path):
    print("=" * 70)
    print(f"Testing feature extraction: {image_path}")
    print("=" * 70)
    
    extractor = AncientArchExtractor()
    
    try:
        feature_dict, feature_vector = extractor.extract_features(image_path)
        
        print(f"✅ Feature extraction successful!")
        print(f"\nFeature Dictionary (JSON format):")
        print(json.dumps(feature_dict, ensure_ascii=False, indent=2))
        
        print(f"\nFeature Vector (NumPy array):")
        print(f"Shape: {feature_vector.shape}")
        print(f"Values: {feature_vector}")
        print(f"Data type: {feature_vector.dtype}")
        
        print(f"\nFeature Keys: {list(feature_dict.keys())}")
        print(f"Total features: {len(feature_vector)}")
        print()
        
        return feature_dict, feature_vector
        
    except Exception as e:
        print(f"❌ Feature extraction failed: {e}")
        print()
        return None, None

if __name__ == "__main__":
    try:
        print("=" * 70)
        print("Ancient Architecture Feature Extractor - Test Suite")
        print("=" * 70)
        print()
        
        results = []
        
        for i in range(1, 4):
            image_path = f"E:/Code/ACASB/{i}.jpg"
            feature_dict, feature_vector = test_feature_extraction(image_path)
            
            if feature_dict is not None and feature_vector is not None:
                results.append({
                    "image": f"{i}.jpg",
                    "features": feature_dict,
                    "vector": feature_vector.tolist()
                })
        
        print("=" * 70)
        print("Summary")
        print("=" * 70)
        
        for result in results:
            print(f"\nImage: {result['image']}")
            print(f"Classification: {'Royal' if result['features']['ratio_yellow'] > 0.15 or result['features']['ratio_red_1'] > 0.15 else 'Civilian'}")
            print(f"Key features:")
            print(f"  - Yellow ratio: {result['features']['ratio_yellow']:.4f}")
            print(f"  - Red ratio: {result['features']['ratio_red_1'] + result['features']['ratio_red_2']:.4f}")
            print(f"  - Edge density: {result['features']['edge_density']:.4f}")
            print(f"  - Entropy: {result['features']['entropy']:.4f}")
            print(f"  - Contrast: {result['features']['contrast']:.4f}")
        
        print("\n" + "=" * 70)
        print("✅ All feature extraction tests completed!")
        print("=" * 70)
        
        print("\nFeature vector format for MLP training:")
        print("-" * 70)
        print("Each image produces a 19-dimensional feature vector:")
        print("[ratio_yellow, ratio_red_1, ratio_red_2, ratio_blue, ratio_green,")
        print(" ratio_gray_white, ratio_black, h_mean, h_std, s_mean, s_std,")
        print(" v_mean, v_std, edge_density, entropy, contrast,")
        print(" dissimilarity, homogeneity, asm]")
        print("-" * 70)
        
    except Exception as e:
        print(f"❌ Test suite failed: {e}")