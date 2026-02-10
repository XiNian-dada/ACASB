import requests
import json
import os
import time

base_uri = "http://localhost:8080"

def print_separator(char="=", length=80):
    print(char * length)

def print_header(text):
    print(f"\n{text}")
    print_separator()

def print_test_result(test_name, success, message, response_data=None):
    if success:
        print(f"  ✓ {test_name}")
        if response_data:
            print(f"    响应: {json.dumps(response_data, ensure_ascii=False, indent=2)}")
    else:
        print(f"  ✗ {test_name}")
        print(f"    错误: {message}")
    print_separator("-")

def test_image_prediction():
    print_header("测试 1: 图像预测接口")
    
    test_file = "E:\\Code\\ACASB\\1.jpg"
    
    if not os.path.exists(test_file):
        print_test_result("图像预测", False, f"测试文件不存在: {test_file}")
        return
    
    try:
        response = requests.post(f"{base_uri}/api/predict", json={
            "image_path": test_file
        })
        result = response.json()
        
        if result.get('success'):
            print_test_result("图像预测", True, "成功", result)
        else:
            print_test_result("图像预测", False, result.get('message', '未知错误'))
    except Exception as e:
        print_test_result("图像预测", False, str(e))

def test_image_analysis():
    print_header("测试 2: 图像分析接口")
    
    test_file = "E:\\Code\\ACASB\\1.jpg"
    
    if not os.path.exists(test_file):
        print_test_result("图像分析", False, f"测试文件不存在: {test_file}")
        return
    
    try:
        with open(test_file, 'rb') as f:
            files = {'file': ('1.jpg', f, 'image/jpeg')}
            response = requests.post(f"{base_uri}/api/analyze", files=files)
        result = response.json()
        
        if result.get('success'):
            print_test_result("图像分析", True, "成功", result)
        else:
            print_test_result("图像分析", False, result.get('message', '未知错误'))
    except Exception as e:
        print_test_result("图像分析", False, str(e))

def test_data_upload():
    print_header("测试 3: 数据上传接口")
    
    test_file = "E:\\Code\\ACASB\\1.jpg"
    
    if not os.path.exists(test_file):
        print_test_result("数据上传", False, f"测试文件不存在: {test_file}")
        return
    
    try:
        with open(test_file, 'rb') as f:
            files = {'file': ('1.jpg', f, 'image/jpeg')}
            response = requests.post(f"{base_uri}/data/add", files=files)
        result = response.json()
        
        if result.get('success'):
            print_test_result("数据上传", True, "成功", result)
        else:
            print_test_result("数据上传", False, result.get('message', '未知错误'))
    except Exception as e:
        print_test_result("数据上传", False, str(e))

def test_batch_upload():
    print_header("测试 4: 批量上传接口")
    
    test_files = [
        "E:\\Code\\ACASB\\1.jpg",
        "E:\\Code\\ACASB\\2.jpg",
        "E:\\Code\\ACASB\\3.jpg"
    ]
    
    valid_files = [f for f in test_files if os.path.exists(f)]
    
    if not valid_files:
        print_test_result("批量上传", False, "所有测试文件都不存在")
        return
    
    try:
        files_data = []
        for file_path in valid_files:
            filename = os.path.basename(file_path)
            with open(file_path, 'rb') as f:
                file_content = f.read()
                files_data.append(('files', (filename, file_content, 'image/jpeg')))
        
        response = requests.post(f"{base_uri}/data/batch", files=files_data)
        result = response.json()
        
        if result.get('success'):
            print_test_result("批量上传", True, "成功", {
                'totalCount': result.get('totalCount'),
                'successCount': result.get('successCount'),
                'failureCount': result.get('failureCount')
            })
        else:
            print_test_result("批量上传", False, result.get('message', '未知错误'))
    except Exception as e:
        print_test_result("批量上传", False, str(e))

def test_query_analysis():
    print_header("测试 5: 查询分析信息接口")
    
    test_id = 1
    
    try:
        response = requests.get(f"{base_uri}/data/analysis/{test_id}")
        result = response.json()
        
        if result.get('success'):
            print_test_result("查询分析信息", True, "成功", result.get('data'))
        else:
            print_test_result("查询分析信息", False, result.get('message', '未知错误'))
    except Exception as e:
        print_test_result("查询分析信息", False, str(e))

def test_query_type():
    print_header("测试 6: 查询建筑类型接口")
    
    test_id = 1
    
    try:
        response = requests.get(f"{base_uri}/data/type/{test_id}")
        result = response.json()
        
        if result.get('success'):
            print_test_result("查询建筑类型", True, "成功", result.get('data'))
        else:
            print_test_result("查询建筑类型", False, result.get('message', '未知错误'))
    except Exception as e:
        print_test_result("查询建筑类型", False, str(e))

def test_sort_query():
    print_header("测试 7: 数据排序查询接口")
    
    try:
        params = {
            'field': 'royalRatio',
            'order': 'desc',
            'limit': 5
        }
        response = requests.get(f"{base_uri}/data/list", params=params)
        result = response.json()
        
        if result.get('success'):
            print_test_result("数据排序查询", True, "成功", {
                'count': result.get('count'),
                'data': result.get('data', [])[:2]
            })
        else:
            print_test_result("数据排序查询", False, result.get('message', '未知错误'))
    except Exception as e:
        print_test_result("数据排序查询", False, str(e))

def test_sort_with_prediction():
    print_header("测试 8: 数据排序查询接口（带类型筛选）")
    
    try:
        params = {
            'field': 'entropy',
            'order': 'asc',
            'limit': 10,
            'prediction': 'royal'
        }
        response = requests.get(f"{base_uri}/data/list", params=params)
        result = response.json()
        
        if result.get('success'):
            print_test_result("数据排序查询（带类型筛选）", True, "成功", {
                'count': result.get('count'),
                'data': result.get('data', [])[:2]
            })
        else:
            print_test_result("数据排序查询（带类型筛选）", False, result.get('message', '未知错误'))
    except Exception as e:
        print_test_result("数据排序查询（带类型筛选）", False, str(e))

def main():
    print_header("ACASB API 接口可用性测试")
    print(f"测试时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"服务地址: {base_uri}")
    print_separator()
    
    print("\n开始测试所有接口...")
    print_separator("-")
    
    time.sleep(2)
    
    test_image_prediction()
    time.sleep(1)
    
    test_image_analysis()
    time.sleep(1)
    
    test_data_upload()
    time.sleep(1)
    
    test_batch_upload()
    time.sleep(1)
    
    test_query_analysis()
    time.sleep(1)
    
    test_query_type()
    time.sleep(1)
    
    test_sort_query()
    time.sleep(1)
    
    test_sort_with_prediction()
    
    print_header("✅ 测试完成")
    print("\n所有接口测试已完成！")
    print("请查看上方结果以了解每个接口的可用性。")
    print_separator()

if __name__ == "__main__":
    main()
