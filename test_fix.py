import requests
import json

base_uri = "http://localhost:8080"

def print_separator(char="=", length=80):
    print(char * length)

def print_header(text):
    print(f"\n{text}")
    print_separator()

def test_sort_query():
    print_header("测试修复后的排序查询接口")
    
    tests = [
        ("按皇家比例降序排列", "royalRatio", "desc", 10, None),
        ("按熵值升序排列", "entropy", "asc", 10, None),
        ("查询所有平民建筑并按皇家比例降序", "royalRatio", "desc", 10, "civilian"),
        ("查询所有皇室建筑并按熵值升序", "entropy", "asc", 10, "royal"),
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
                    print(f"  前 3 条记录:")
                    for idx, item in enumerate(data[:3], 1):
                        confidence = item.get('confidence')
                        prediction = item.get('prediction')
                        print(f"    {idx}. ID: {item.get('id')}, 预测: {prediction}, 置信度: {confidence}")
                        
                        if confidence is not None:
                            print(f"       ✓ 置信度正常: {confidence:.4f}")
                        else:
                            print(f"       ✗ 置信度为 None")
            else:
                print(f"  ✗ 查询失败: {result.get('message')}")
        except Exception as e:
            print(f"  ✗ 请求失败: {str(e)}")
    
    print_separator()

def main():
    print_header("ACASB 修复验证测试")
    print(f"测试时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"服务地址: {base_uri}")
    print_separator()
    
    test_sort_query()
    
    print_header("✅ 测试完成")
    print("\n修复说明:")
    print("1. 修改了 GET /data/list 接口")
    print("2. 关联查询 BuildingAnalysis 和 BuildingType 表")
    print("3. 返回完整的 prediction 和 confidence 字段")
    print("4. 解决了置信度显示 NaN 的问题")
    print_separator()

if __name__ == "__main__":
    import time
    main()
