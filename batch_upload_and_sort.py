import requests
import os

base_uri = "http://localhost:8080"

def print_separator(char="=", length=80):
    print(char * length)

def print_header(text):
    print(f"\n{text}")
    print_separator()

def batch_upload_images():
    print_header("📤 批量上传图片")
    
    files = []
    for i in range(1, 11):
        file_path = f"E:\\Code\\ACASB\\{i}.jpg"
        if os.path.exists(file_path):
            files.append(file_path)
            print(f"  找到: {i}.jpg")
        else:
            print(f"  ✗ 未找到: {i}.jpg")
    
    if not files:
        print("  没有找到任何图片文件！")
        return
    
    print(f"\n准备上传 {len(files)} 张图片...")
    print_separator("-")
    
    success_count = 0
    failure_count = 0
    
    for file_path in files:
        filename = os.path.basename(file_path)
        print(f"正在上传: {filename}")
        
        try:
            with open(file_path, 'rb') as f:
                files_param = {'files': (filename, f, 'image/jpeg')}
                response = requests.post(f"{base_uri}/data/batch", files=files_param)
                result = response.json()
                
                if result.get('success', False):
                    items = result.get('items', [])
                    for item in items:
                        if item.get('success'):
                            success_count += 1
                            print(f"  ✓ {filename}: 分析ID={item.get('analysisId')}, 类型ID={item.get('typeId')}")
                        else:
                            failure_count += 1
                            print(f"  ✗ {filename}: {item.get('message')}")
                else:
                    failure_count += 1
                    print(f"  ✗ {filename}: 上传失败")
        except Exception as e:
            failure_count += 1
            print(f"  ✗ {filename}: 处理失败 - {str(e)}")
    
    print_separator()
    print(f"上传完成！")
    print(f"  成功: {success_count}")
    print(f"  失败: {failure_count}")
    print(f"  总计: {success_count + failure_count}")
    print_separator()

def test_sort_query():
    print_header("🔍 测试排序查询")
    
    tests = [
        ("按皇家比例降序排列（默认）", "royalRatio", "desc", 5, None),
        ("按皇家比例升序排列", "royalRatio", "asc", 5, None),
        ("按熵值降序排列", "entropy", "desc", 5, None),
        ("按边缘密度降序排列", "edgeDensity", "desc", 5, None),
        ("查询所有皇室建筑并按皇家比例降序", "royalRatio", "desc", 10, "royal"),
        ("查询所有平民建筑并按熵值升序", "entropy", "asc", 10, "civilian"),
    ]
    
    for test_name, field, order, limit, prediction in tests:
        print(f"\n{test_name}")
        print(f"  字段: {field}, 排序: {order}, 限制: {limit}, 预测: {prediction}")
        
        params = {}
        if field:
            params['field'] = field
        if order:
            params['order'] = order
        if limit:
            params['limit'] = limit
        if prediction:
            params['prediction'] = prediction
        
        try:
            response = requests.get(f"{base_uri}/data/list", params=params)
            result = response.json()
            
            if result.get('success'):
                print(f"  ✓ 查询成功，返回 {result.get('count')} 条记录")
                data = result.get('data', [])
                if data:
                    print(f"  前 {min(5, len(data))} 条记录:")
                    for idx, item in enumerate(data[:5], 1):
                        print(f"    {idx}. ID: {item.get('id')}, 皇家比例: {item.get('royalRatio', 'N/A'):.4f}, 熵值: {item.get('entropy', 'N/A'):.4f}")
            else:
                print(f"  ✗ 查询失败: {result.get('message')}")
        except Exception as e:
            print(f"  ✗ 请求失败: {str(e)}")
    
    print_separator()

def main():
    print_header("🚀 ACASB 批量上传与排序查询测试")
    
    batch_upload_images()
    
    print("\n等待 2 秒后测试排序查询...")
    import time
    time.sleep(2)
    
    test_sort_query()
    
    print_header("✅ 测试完成")

if __name__ == "__main__":
    main()
