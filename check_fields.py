import requests

base_uri = "http://localhost:8080"

response = requests.get(f"{base_uri}/data/analysis/3")
data = response.json()

if data.get('success'):
    print("数据库中的字段:")
    for key in data['data'].keys():
        print(f"  {key}: {data['data'][key]}")
else:
    print(f"查询失败: {data.get('message')}")
