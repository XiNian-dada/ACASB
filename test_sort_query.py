import requests
import json

base_uri = "http://localhost:8080"

def print_separator(char="=", length=80):
    print(char * length)

def print_header(text):
    print(f"\n{text}")
    print_separator()

def test_sort_by_field(field, order="desc", limit=None, prediction=None):
    print(f"\n测试按字段排序: {field}, 排序: {order}")
    
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
            print(f"  ✓ 查询成功")
            print(f"  返回记录数: {result.get('count')}")
            print()
            
            data = result.get('data', [])
            print(f"  前 {min(10, len(data))} 条记录:")
            print_separator("-")
            
            for idx, item in enumerate(data[:10], 1):
                print(f"  {idx}. ID: {item.get('id')}")
                print(f"     图片路径: {item.get('imagePath')}")
                print(f"     皇家比例: {item.get('royalRatio', 'N/A'):.4f}")
                print(f"     熵值: {item.get('entropy', 'N/A'):.4f}")
                print(f"     边缘密度: {item.get('edgeDensity', 'N/A'):.4f}")
                print()
        else:
            print(f"  ✗ 查询失败: {result.get('message')}")
    except Exception as e:
        print(f"  ✗ 请求失败: {str(e)}")
    
    print_separator()

def main():
    print_header("🔍 ACASB 数据排序查询测试")
    
    print("\n测试 1: 按皇家比例降序排列（默认）")
    test_sort_by_field("royalRatio", "desc", 5)
    
    print("\n测试 2: 按皇家比例升序排列")
    test_sort_by_field("royalRatio", "asc", 5)
    
    print("\n测试 3: 按熵值降序排列")
    test_sort_by_field("entropy", "desc", 5)
    
    print("\n测试 4: 按边缘密度降序排列")
    test_sort_by_field("edgeDensity", "desc", 5)
    
    print("\n测试 5: 查询所有皇室建筑并按皇家比例降序")
    test_sort_by_field("royalRatio", "desc", 10, "royal")
    
    print("\n测试 6: 查询所有平民建筑并按熵值升序")
    test_sort_by_field("entropy", "asc", 10, "civilian")
    
    print_header("✅ 测试完成")

if __name__ == "__main__":
    main()
