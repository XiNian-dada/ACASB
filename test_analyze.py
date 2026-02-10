#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import requests
import os
import time
import json

API_BASE_URL = "http://localhost:8080"
TEST_IMAGE = r"E:\Code\ACASB\1.jpg"

def test_analyze():
    print("=" * 60)
    print("测试 analyze 接口")
    print("=" * 60)
    
    if not os.path.exists(TEST_IMAGE):
        print(f"错误: 测试图片不存在: {TEST_IMAGE}")
        return False
    
    print(f"测试图片: {TEST_IMAGE}")
    print(f"API地址: {API_BASE_URL}/api/analyze")
    print()
    
    url = f"{API_BASE_URL}/api/analyze"
    
    try:
        print("正在发送请求...")
        with open(TEST_IMAGE, 'rb') as f:
            files = {'file': ('1.jpg', f, 'image/jpeg')}
            response = requests.post(url, files=files, timeout=30)
        
        print(f"HTTP状态码: {response.status_code}")
        print(f"响应头: {dict(response.headers)}")
        print()
        
        try:
            result = response.json()
            print("响应内容:")
            print(json.dumps(result, indent=2, ensure_ascii=False))
            print()
            
            if response.status_code == 200:
                if result.get('success'):
                    print("✓ 测试成功!")
                    print(f"消息: {result.get('message', 'N/A')}")
                    return True
                else:
                    print("✗ 测试失败: API返回success=false")
                    print(f"错误消息: {result.get('message', 'N/A')}")
                    return False
            else:
                print("✗ 测试失败: HTTP状态码不是200")
                return False
                
        except json.JSONDecodeError as e:
            print(f"✗ 测试失败: 无法解析JSON响应")
            print(f"原始响应: {response.text}")
            return False
            
    except requests.exceptions.Timeout:
        print("✗ 测试失败: 请求超时")
        return False
    except requests.exceptions.ConnectionError as e:
        print(f"✗ 测试失败: 连接错误 - {e}")
        print("请确保后端服务已启动 (端口8080)")
        return False
    except Exception as e:
        print(f"✗ 测试失败: {type(e).__name__} - {e}")
        return False

def main():
    attempt = 1
    max_attempts = 10
    
    while attempt <= max_attempts:
        print(f"\n尝试 #{attempt}")
        print("-" * 60)
        
        success = test_analyze()
        
        if success:
            print("\n" + "=" * 60)
            print("测试通过!")
            print("=" * 60)
            return 0
        
        attempt += 1
        
        if attempt <= max_attempts:
            print(f"\n等待3秒后重试...")
            time.sleep(3)
    
    print("\n" + "=" * 60)
    print(f"测试失败: 已尝试 {max_attempts} 次")
    print("=" * 60)
    return 1

if __name__ == "__main__":
    exit(main())
