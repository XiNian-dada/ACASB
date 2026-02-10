import requests
import json

base_uri = "http://localhost:8080"

def print_separator(char="=", length=80):
    print(char * length)

def print_header(text):
    print(f"\n{text}")
    print_separator()

def test_sort_by_field(field, order, limit):
    print_header(f"测试按 {field} {order} 排序（限制 {limit} 条）")
    
    url = f"{base_uri}/data/list?field={field}&order={order}&limit={limit}"
    
    try:
        response = requests.get(url)
        result = response.json()
        
        if result.get('success'):
            data = result.get('data', [])
            print(f"  ✓ 查询成功，返回 {len(data)} 条记录")
            
            if len(data) > 0:
                print(f"\n  前 5 条记录的 {field} 值:")
                for idx, item in enumerate(data[:5], 1):
                    value = item.get(field)
                    if value is not None:
                        print(f"    {idx}. ID {item.get('id')}: {float(value):.4f}")
                    else:
                        print(f"    {idx}. ID {item.get('id')}: N/A")
                
                print(f"\n  检查排序是否正确:")
                values = [item.get(field) for item in data if item.get(field) is not None]
                
                if len(values) > 1:
                    epsilon = 0.0001
                    if order == 'desc':
                        is_sorted = all(values[i] >= values[i+1] - epsilon for i in range(len(values)-1))
                    else:
                        is_sorted = all(values[i] <= values[i+1] + epsilon for i in range(len(values)-1))
                    
                    if is_sorted:
                        print(f"  ✓ 排序正确！")
                    else:
                        print(f"  ✗ 排序错误！")
                        print(f"    期望顺序: {'降序' if order == 'desc' else '升序'}")
                        values_list = []
                        for v in values[:5]:
                            values_list.append(f"{float(v):.4f}")
                        print(f"    实际顺序: {values_list}")
                else:
                    print(f"  ⚠️ 数据不足，无法验证排序")
        else:
            print(f"  ✗ 查询失败: {result.get('message')}")
    except Exception as e:
        print(f"  ✗ 请求失败: {str(e)}")
    
    print_separator()

def main():
    print_header("ACASB 排序功能验证测试")
    print(f"测试时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"服务地址: {base_uri}")
    print_separator()
    
    test_sort_by_field('royalRatio', 'desc', 10)
    print()
    
    test_sort_by_field('royalRatio', 'asc', 10)
    print()
    
    test_sort_by_field('entropy', 'desc', 10)
    print()
    
    test_sort_by_field('entropy', 'asc', 10)
    print()
    
    test_sort_by_field('edgeDensity', 'desc', 10)
    print()
    
    test_sort_by_field('edgeDensity', 'asc', 10)
    print()
    
    print_header("✅ 测试完成")

if __name__ == "__main__":
    import time
    main()
