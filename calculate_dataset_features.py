import sys
import os
import numpy as np
from PIL import Image
import cv2
from datetime import datetime

def print_separator(char="=", length=80):
    print(char * length)

def print_header(text):
    print(f"\n{text}")
    print_separator()

def format_value(value, decimals=4):
    if value is None:
        return "N/A"
    format_str = "{:." + str(decimals) + "f}"
    return format_str.format(value)

def extract_features(image_path):
    sys.path.append(os.path.join(os.path.dirname(__file__), 'acasb-analysis'))
    from ancient_arch_extractor import AncientArchExtractor
    
    extractor = AncientArchExtractor()
    features, _ = extractor.extract_features(image_path)
    return features

def process_dataset(dataset_path, label):
    print(f"\n正在处理数据集: {dataset_path}")
    print(f"标签: {label}")
    print_separator("-")
    
    if not os.path.exists(dataset_path):
        print(f"  ✗ 目录不存在: {dataset_path}")
        return None
    
    image_files = [f for f in os.listdir(dataset_path) if f.lower().endswith(('.jpg', '.jpeg', '.png'))]
    total_files = len(image_files)
    
    if total_files == 0:
        print(f"  ✗ 目录中没有图片文件")
        return None
    
    print(f"  找到 {total_files} 张图片")
    
    all_features = []
    failed_count = 0
    
    for idx, filename in enumerate(image_files, 1):
        image_path = os.path.join(dataset_path, filename)
        
        try:
            features = extract_features(image_path)
            
            if features:
                all_features.append(features)
                if idx % 20 == 0:
                    print(f"  进度: {idx}/{total_files} ({idx*100//total_files}%)")
            else:
                print(f"  ✗ 处理失败: {filename}")
                failed_count += 1
        except Exception as e:
            print(f"  ✗ 处理失败: {filename} - {str(e)}")
            failed_count += 1
    
    print(f"\n成功处理: {len(all_features)} 张图片")
    print(f"失败: {failed_count} 张")
    
    if len(all_features) == 0:
        return None
    
    return all_features

def calculate_average_features(features_list):
    if not features_list or len(features_list) == 0:
        return None
    
    feature_keys = [
        'ratio_yellow', 'ratio_red_1', 'ratio_red_2', 'ratio_blue', 'ratio_green',
        'ratio_gray_white', 'ratio_black', 'royal_ratio',
        'h_mean', 'h_std', 's_mean', 's_std', 'v_mean', 'v_std',
        'edge_density', 'entropy', 'contrast', 'dissimilarity', 'homogeneity', 'asm'
    ]
    
    averages = {}
    
    for key in feature_keys:
        values = []
        for f in features_list:
            value = f.get(key)
            if value is not None:
                values.append(value)
        
        if values:
            averages[key] = {
                'mean': float(np.mean(values)),
                'std': float(np.std(values)),
                'min': float(np.min(values)),
                'max': float(np.max(values)),
                'count': len(values)
            }
        else:
            averages[key] = None
    
    return averages

def print_feature_statistics(averages, label):
    print(f"\n{label} 数据集特征统计")
    print_separator("-")
    
    if not averages:
        print("  无数据")
        return
    
    print("  🎨 色彩特征:")
    royal_ratio = averages.get('royal_ratio')
    if royal_ratio is not None:
        print(f"    - 皇家比例: 均值={format_value(royal_ratio.get('mean'))}, 标准差={format_value(royal_ratio.get('std'))}")
    else:
        print("    - 皇家比例: N/A")
    
    ratio_yellow = averages.get('ratio_yellow')
    if ratio_yellow is not None:
        print(f"    - 黄色比例: 均值={format_value(ratio_yellow.get('mean'))}, 标准差={format_value(ratio_yellow.get('std'))}")
    else:
        print("    - 黄色比例: N/A")
    
    ratio_red_1 = averages.get('ratio_red_1')
    if ratio_red_1 is not None:
        print(f"    - 红色1比例: 均值={format_value(ratio_red_1.get('mean'))}, 标准差={format_value(ratio_red_1.get('std'))}")
    else:
        print("    - 红色1比例: N/A")
    
    ratio_red_2 = averages.get('ratio_red_2')
    if ratio_red_2 is not None:
        print(f"    - 红色2比例: 均值={format_value(ratio_red_2.get('mean'))}, 标准差={format_value(ratio_red_2.get('std'))}")
    else:
        print("    - 红色2比例: N/A")
    
    ratio_blue = averages.get('ratio_blue')
    if ratio_blue is not None:
        print(f"    - 蓝色比例: 均值={format_value(ratio_blue.get('mean'))}, 标准差={format_value(ratio_blue.get('std'))}")
    else:
        print("    - 蓝色比例: N/A")
    
    ratio_green = averages.get('ratio_green')
    if ratio_green is not None:
        print(f"    - 绿色比例: 均值={format_value(ratio_green.get('mean'))}, 标准差={format_value(ratio_green.get('std'))}")
    else:
        print("    - 绿色比例: N/A")
    
    ratio_gray_white = averages.get('ratio_gray_white')
    if ratio_gray_white is not None:
        print(f"    - 灰白色比例: 均值={format_value(ratio_gray_white.get('mean'))}, 标准差={format_value(ratio_gray_white.get('std'))}")
    else:
        print("    - 灰白色比例: N/A")
    
    ratio_black = averages.get('ratio_black')
    if ratio_black is not None:
        print(f"    - 黑色比例: 均值={format_value(ratio_black.get('mean'))}, 标准差={format_value(ratio_black.get('std'))}")
    else:
        print("    - 黑色比例: N/A")
    
    print()
    print("  🌈 HSV特征:")
    h_mean = averages.get('h_mean')
    if h_mean is not None:
        print(f"    - 色相均值: 均值={format_value(h_mean.get('mean'))}, 标准差={format_value(h_mean.get('std'))}")
    else:
        print("    - 色相均值: N/A")
    
    h_std = averages.get('h_std')
    if h_std is not None:
        print(f"    - 色相标准差: 均值={format_value(h_std.get('mean'))}, 标准差={format_value(h_std.get('std'))}")
    else:
        print("    - 色相标准差: N/A")
    
    s_mean = averages.get('s_mean')
    if s_mean is not None:
        print(f"    - 饱和度均值: 均值={format_value(s_mean.get('mean'))}, 标准差={format_value(s_mean.get('std'))}")
    else:
        print("    - 饱和度均值: N/A")
    
    s_std = averages.get('s_std')
    if s_std is not None:
        print(f"    - 饱和度标准差: 均值={format_value(s_std.get('mean'))}, 标准差={format_value(s_std.get('std'))}")
    else:
        print("    - 饱和度标准差: N/A")
    
    v_mean = averages.get('v_mean')
    if v_mean is not None:
        print(f"    - 明度均值: 均值={format_value(v_mean.get('mean'))}, 标准差={format_value(v_mean.get('std'))}")
    else:
        print("    - 明度均值: N/A")
    
    v_std = averages.get('v_std')
    if v_std is not None:
        print(f"    - 明度标准差: 均值={format_value(v_std.get('mean'))}, 标准差={format_value(v_std.get('std'))}")
    else:
        print("    - 明度标准差: N/A")
    
    print()
    print("  📐 纹理特征:")
    edge_density = averages.get('edge_density')
    if edge_density is not None:
        print(f"    - 边缘密度: 均值={format_value(edge_density.get('mean'))}, 标准差={format_value(edge_density.get('std'))}")
    else:
        print("    - 边缘密度: N/A")
    
    entropy = averages.get('entropy')
    if entropy is not None:
        print(f"    - 熵值: 均值={format_value(entropy.get('mean'))}, 标准差={format_value(entropy.get('std'))}")
    else:
        print("    - 熵值: N/A")
    
    contrast = averages.get('contrast')
    if contrast is not None:
        print(f"    - 对比度: 均值={format_value(contrast.get('mean'))}, 标准差={format_value(contrast.get('std'))}")
    else:
        print("    - 对比度: N/A")
    
    dissimilarity = averages.get('dissimilarity')
    if dissimilarity is not None:
        print(f"    - 不相似度: 均值={format_value(dissimilarity.get('mean'))}, 标准差={format_value(dissimilarity.get('std'))}")
    else:
        print("    - 不相似度: N/A")
    
    homogeneity = averages.get('homogeneity')
    if homogeneity is not None:
        print(f"    - 同质性: 均值={format_value(homogeneity.get('mean'))}, 标准差={format_value(homogeneity.get('std'))}")
    else:
        print("    - 同质性: N/A")
    
    asm = averages.get('asm')
    if asm is not None:
        print(f"    - 角二阶矩: 均值={format_value(asm.get('mean'))}, 标准差={format_value(asm.get('std'))}")
    else:
        print("    - 角二阶矩: N/A")
    
    print_separator()

def compare_features(civilian_averages, royal_averages):
    print("\n📊 特征对比分析")
    print_separator("-")
    
    if not civilian_averages or not royal_averages:
        print("  无数据可对比")
        return
    
    print("  皇家比例对比:")
    civilian_royal = None
    royal_royal = None
    
    if civilian_averages.get('royal_ratio') is not None:
        civilian_royal = civilian_averages.get('royal_ratio').get('mean')
    
    if royal_averages.get('royal_ratio') is not None:
        royal_royal = royal_averages.get('royal_ratio').get('mean')
    
    if civilian_royal is not None and royal_royal is not None:
        print(f"    - 平民建筑平均皇家比例: {civilian_royal:.4f}")
        print(f"    - 皇室建筑平均皇家比例: {royal_royal:.4f}")
        print(f"    - 差异: {abs(royal_royal - civilian_royal):.4f}")
        print(f"    - 比率: {civilian_royal/royal_royal:.4f}x" if royal_royal > 0 else "N/A")
    
    print()
    print("  熵值对比:")
    civilian_entropy = None
    royal_entropy = None
    
    if civilian_averages.get('entropy') is not None:
        civilian_entropy = civilian_averages.get('entropy').get('mean')
    
    if royal_averages.get('entropy') is not None:
        royal_entropy = royal_averages.get('entropy').get('mean')
    
    if civilian_entropy is not None and royal_entropy is not None:
        print(f"    - 平民建筑平均熵值: {civilian_entropy:.4f}")
        print(f"    - 皇室建筑平均熵值: {royal_entropy:.4f}")
        print(f"    - 差异: {abs(royal_entropy - civilian_entropy):.4f}")
    
    print()
    print("  边缘密度对比:")
    civilian_edge = None
    royal_edge = None
    
    if civilian_averages.get('edge_density') is not None:
        civilian_edge = civilian_averages.get('edge_density').get('mean')
    
    if royal_averages.get('edge_density') is not None:
        royal_edge = royal_averages.get('edge_density').get('mean')
    
    if civilian_edge is not None and royal_edge is not None:
        print(f"    - 平民建筑平均边缘密度: {civilian_edge:.4f}")
        print(f"    - 皇室建筑平均边缘密度: {royal_edge:.4f}")
        print(f"    - 差异: {abs(royal_edge - civilian_edge):.4f}")
    
    print_separator()

def main():
    print_header("🔍 ACASB 数据集特征计算工具")
    print(f"  计算时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print_separator()
    
    base_dir = os.path.dirname(os.path.abspath(__file__))
    civilian_dataset = os.path.join(base_dir, 'datasets', 'civilian', 'dataset_fixed')
    royal_dataset = os.path.join(base_dir, 'datasets', 'royal', 'dataset_fixed')
    
    print("\n开始处理数据集...")
    print(f"平民建筑数据集: {civilian_dataset}")
    print(f"皇室建筑数据集: {royal_dataset}")
    print_separator()
    
    civilian_features = process_dataset(civilian_dataset, "平民建筑")
    royal_features = process_dataset(royal_dataset, "皇室建筑")
    
    if civilian_features is None or royal_features is None:
        print("\n❌ 处理失败，无法计算特征")
        return
    
    print_separator()
    
    civilian_averages = calculate_average_features(civilian_features)
    royal_averages = calculate_average_features(royal_features)
    
    print_feature_statistics(civilian_averages, "平民建筑")
    print_feature_statistics(royal_averages, "皇室建筑")
    
    compare_features(civilian_averages, royal_averages)
    
    print_header("✅ 计算完成")
    print(f"  平民建筑样本数: {len(civilian_features)}")
    print(f"  皇室建筑样本数: {len(royal_features)}")
    print_separator()

if __name__ == "__main__":
    main()
