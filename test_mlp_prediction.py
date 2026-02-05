import requests
import json

API_URL = "http://127.0.0.1:5000"

def test_predict(image_path):
    print("=" * 70)
    print(f"Testing prediction: {image_path}")
    print("=" * 70)
    
    payload = {
        "image_path": image_path
    }
    
    response = requests.post(f"{API_URL}/predict", json=payload)
    print(f"Status Code: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), ensure_ascii=False, indent=2)}")
    print()

if __name__ == "__main__":
    try:
        test_predict("E:/Code/ACASB/1.jpg")
        test_predict("E:/Code/ACASB/2.jpg")
        test_predict("E:/Code/ACASB/3.jpg")
        
        print("=" * 70)
        print("✅ All prediction tests completed!")
        print("=" * 70)
        
    except Exception as e:
        print(f"❌ Test failed: {e}")