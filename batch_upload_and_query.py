import requests
import json
from datetime import datetime
import os

base_uri = "http://localhost:8080"

def print_separator(char="=", length=80):
    print(char * length)

def print_header(text):
    print(f"\n{text}")
    print_separator()

def format_value(value, default="N/A"):
    if value is None:
        return default
    return f"{value:.4f}"

def batch_upload_images(file_paths):
    print(f"\n正在批量上传 {len(file_paths)} 张图片...")
    print_separator()
    
    results = []
    
    for file_path in file_paths:
        if not os.path.exists(file_path):
            print(f"  ✗ 文件不存在: {file_path}")
            continue
        
        try:
            with open(file_path, 'rb') as f:
                files = {'files': (os.path.basename(file_path), f, 'image/jpeg')}
                response = requests.post(f"{base_uri}/data/batch", files=files)
                result = response.json()
                
                if result.get('totalCount', 0) > 0:
                    for item in result.get('items', []):
                        results.append(item)
                        if item.get('success'):
                            print(f"  ✓ {item.get('fileName')}: 分析ID={item.get('analysisId')}, 类型ID={item.get('typeId')}")
                        else:
                            print(f"  ✗ {item.get('fileName')}: {item.get('message')}")
                else:
                    print(f"  ✗ 上传失败: {result.get('message', '未知错误')}")
                    
        except Exception as e:
            print(f"  ✗ 处理失败: {str(e)}")
    
    print_separator()
    return results

def get_all_data(start_id, end_id):
    results = []
    
    for i in range(start_id, end_id + 1):
        try:
            analysis_response = requests.get(f"{base_uri}/data/analysis/{i}")
            analysis_data = analysis_response.json()
            
            if analysis_data.get('success'):
                type_response = requests.get(f"{base_uri}/data/type/{i}")
                type_data = type_response.json()
                
                if type_data.get('success'):
                    analysis_info = analysis_data['data']
                    type_info = type_data['data']
                    
                    result = {
                        'id': i,
                        'success': True,
                        'analysis': analysis_info,
                        'type': type_info
                    }
                    results.append(result)
                else:
                    results.append({
                        'id': i,
                        'success': False,
                        'error': f"查询类型失败: {type_data.get('message')}"
                    })
            else:
                results.append({
                    'id': i,
                    'success': False,
                    'error': f"查询失败: {analysis_data.get('message')}"
                })
        except Exception as e:
            results.append({
                'id': i,
                'success': False,
                'error': f"查询异常: {str(e)}"
            })
    
    return results

def print_result_detail(result):
    if not result.get('success'):
        print(f"  ✗ ID {result['id']}: {result.get('error')}")
        return
    
    analysis = result['analysis']
    type_info = result['type']
    
    print(f"  📋 记录 ID: {result['id']}")
    print(f"  📁 图片路径: {analysis.get('imagePath')}")
    print(f"  🏷️ 预测结果: {type_info.get('prediction')}")
    print(f"  📊 置信度: {type_info.get('confidence'):.4f}")
    print()
    print("  🎨 色彩特征:")
    print(f"    - 黄色比例: {format_value(analysis.get('ratioYellow'))}")
    print(f"    - 红色1比例: {format_value(analysis.get('ratioRed1'))}")
    print(f"    - 红色2比例: {format_value(analysis.get('ratioRed2'))}")
    print(f"    - 蓝色比例: {format_value(analysis.get('ratioBlue'))}")
    print(f"    - 绿色比例: {format_value(analysis.get('ratioGreen'))}")
    print(f"    - 灰白色比例: {format_value(analysis.get('ratioGrayWhite'))}")
    print(f"    - 黑色比例: {format_value(analysis.get('ratioBlack'))}")
    print(f"    - 皇家比例: {format_value(analysis.get('royalRatio'))}")
    print()
    print("  🌈 HSV特征:")
    print(f"    - 色相均值: {format_value(analysis.get('hmean'))}")
    print(f"    - 色相标准差: {format_value(analysis.get('hstd'))}")
    print(f"    - 饱和度均值: {format_value(analysis.get('smean'))}")
    print(f"    - 饱和度标准差: {format_value(analysis.get('sstd'))}")
    print(f"    - 明度均值: {format_value(analysis.get('vmean'))}")
    print(f"    - 明度标准差: {format_value(analysis.get('vstd'))}")
    print()
    print("  📐 纹理特征:")
    print(f"    - 边缘密度: {format_value(analysis.get('edgeDensity'))}")
    print(f"    - 熵值: {format_value(analysis.get('entropy'))}")
    print(f"    - 对比度: {format_value(analysis.get('contrast'))}")
    print(f"    - 不相似度: {format_value(analysis.get('dissimilarity'))}")
    print(f"    - 同质性: {format_value(analysis.get('homogeneity'))}")
    print(f"    - 角二阶矩: {format_value(analysis.get('asm'))}")
    print()
    print("  ⏰ 时间信息:")
    print(f"    - 创建时间: {analysis.get('createTime')}")
    print(f"    - 更新时间: {analysis.get('updateTime')}")
    print_separator("-")

def print_summary(results):
    total = len(results)
    success_count = sum(1 for r in results if r.get('success'))
    failure_count = total - success_count
    
    royal_count = sum(1 for r in results if r.get('success') and r['type'].get('prediction') == 'royal')
    civilian_count = sum(1 for r in results if r.get('success') and r['type'].get('prediction') == 'civilian')
    
    print_header("📊 数据统计")
    print(f"  总记录数: {total}")
    print(f"  成功查询: {success_count}")
    print(f"  查询失败: {failure_count}")
    print()
    print(f"  皇室建筑: {royal_count}")
    print(f"  平民建筑: {civilian_count}")
    print()
    
    if success_count > 0:
        accuracy = (royal_count + civilian_count) / success_count * 100
        print(f"  预测准确率: {accuracy:.2f}%")
    
    print_separator()

def query_and_display(start_id, end_id):
    print(f"\n正在查询数据库记录 {start_id} 到 {end_id}...")
    print_separator()
    
    results = get_all_data(start_id, end_id)
    
    print_header("📋 查询结果详情")
    
    royal_results = []
    civilian_results = []
    
    for result in results:
        if result.get('success') and result['type'].get('prediction') == 'royal':
            royal_results.append(result)
        elif result.get('success') and result['type'].get('prediction') == 'civilian':
            civilian_results.append(result)
    
    print("\n🏰 皇室建筑记录:")
    print_separator("-")
    for result in royal_results:
        print_result_detail(result)
    
    print("\n🏠 平民建筑记录:")
    print_separator("-")
    for result in civilian_results:
        print_result_detail(result)
    
    print_summary(results)

def main():
    print_header("🔍 ACASB 批量上传与查询工具")
    print(f"  查询时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"  服务地址: {base_uri}")
    print_separator()
    
    print("\n请选择操作:")
    print("  1. 批量上传图片")
    print("  2. 查询数据库记录")
    print("  3. 批量上传并查询")
    print()
    
    choice = input("请输入选项 (1/2/3): ").strip()
    
    if choice == '1':
        print("\n请输入要上传的图片路径（每行一个，空行结束）:")
        file_paths = []
        while True:
            file_path = input("图片路径: ").strip()
            if not file_path:
                break
            file_paths.append(file_path)
        
        if file_paths:
            batch_upload_images(file_paths)
        else:
            print("未输入任何图片路径")
    
    elif choice == '2':
        print("\n请输入要查询的记录 ID 范围（例如: 3-12）:")
        user_input = input("范围: ").strip()
        
        if '-' in user_input:
            try:
                start_id, end_id = map(int, user_input.split('-'))
                query_and_display(start_id, end_id)
            except ValueError:
                print("❌ 输入格式错误！请使用格式: 起始ID-结束ID")
        else:
            print("❌ 输入格式错误！请使用格式: 起始ID-结束ID")
    
    elif choice == '3':
        print("\n请输入要上传的图片路径（每行一个，空行结束）:")
        file_paths = []
        while True:
            file_path = input("图片路径: ").strip()
            if not file_path:
                break
            file_paths.append(file_path)
        
        if file_paths:
            upload_results = batch_upload_images(file_paths)
            
            print("\n等待 2 秒后查询数据库...")
            import time
            time.sleep(2)
            
            print_header("📋 数据库查询结果")
            
            royal_count = 0
            civilian_count = 0
            
            for item in upload_results:
                if item.get('success') and item.get('typeId'):
                    try:
                        type_response = requests.get(f"{base_uri}/data/type/{item.get('typeId')}")
                        type_data = type_response.json()
                        
                        if type_data.get('success'):
                            type_info = type_data['data']
                            prediction = type_info.get('prediction')
                            
                            if prediction == 'royal':
                                royal_count += 1
                            elif prediction == 'civilian':
                                civilian_count += 1
                    except Exception as e:
                        print(f"  ✗ 查询失败: {str(e)}")
            
            print()
            print(f"  皇室建筑: {royal_count}")
            print(f"  平民建筑: {civilian_count}")
            print(f"  总计: {royal_count + civilian_count}")
            
            if royal_count + civilian_count > 0:
                accuracy = min(royal_count, civilian_count) / (royal_count + civilian_count) * 100
                print(f"  预测准确率: {accuracy:.2f}%")
        else:
            print("未输入任何图片路径")
    
    else:
        print("❌ 无效的选项！")

if __name__ == "__main__":
    main()
