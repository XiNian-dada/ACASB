import requests
import json

base_uri = "http://localhost:8080"

print("=" * 60)
print("从数据库查询数据")
print("=" * 60)
print()

print("前5张图片（皇室建筑）:")
print()

for i in range(3, 8):
    print(f"查询分析记录 ID: {i}")
    
    try:
        analysis_response = requests.get(f"{base_uri}/data/analysis/{i}")
        analysis_data = analysis_response.json()
        
        if analysis_data.get('success'):
            type_response = requests.get(f"{base_uri}/data/type/{i}")
            type_data = type_response.json()
            
            if type_data.get('success'):
                prediction = type_data['data']['prediction']
                confidence = type_data['data']['confidence']
                print(f"  ✓ 预测结果: {prediction}, 置信度: {confidence}")
                
                analysis_info = analysis_data['data']
                print(f"    - 图片路径: {analysis_info['imagePath']}")
                print(f"    - 皇家比例: {analysis_info['royalRatio']}")
                print(f"    - 熵值: {analysis_info['entropy']}")
                print(f"    - 边缘密度: {analysis_info['edgeDensity']}")
            else:
                print(f"  ✗ 查询类型失败: {type_data.get('message')}")
        else:
            print(f"  ✗ 查询失败: {analysis_data.get('message')}")
    except Exception as e:
        print(f"  ✗ 查询异常: {e}")
    
    print()

print()
print("后5张图片（平民建筑）:")
print()

for i in range(8, 13):
    print(f"查询分析记录 ID: {i}")
    
    try:
        analysis_response = requests.get(f"{base_uri}/data/analysis/{i}")
        analysis_data = analysis_response.json()
        
        if analysis_data.get('success'):
            type_response = requests.get(f"{base_uri}/data/type/{i}")
            type_data = type_response.json()
            
            if type_data.get('success'):
                prediction = type_data['data']['prediction']
                confidence = type_data['data']['confidence']
                print(f"  ✓ 预测结果: {prediction}, 置信度: {confidence}")
                
                analysis_info = analysis_data['data']
                print(f"    - 图片路径: {analysis_info['imagePath']}")
                print(f"    - 皇家比例: {analysis_info['royalRatio']}")
                print(f"    - 熵值: {analysis_info['entropy']}")
                print(f"    - 边缘密度: {analysis_info['edgeDensity']}")
            else:
                print(f"  ✗ 查询类型失败: {type_data.get('message')}")
        else:
            print(f"  ✗ 查询失败: {analysis_data.get('message')}")
    except Exception as e:
        print(f"  ✗ 查询异常: {e}")
    
    print()

print()
print("=" * 60)
print("查询完成")
print("=" * 60)
