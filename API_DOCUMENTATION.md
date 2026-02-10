# ACASB API 接口文档

## 概述

ACASB（Ancient Chinese Architecture Style Binary）系统提供了一套完整的 RESTful API 接口，用于古代建筑风格识别与分析。本文档详细介绍了所有可用的接口、请求参数、响应格式以及使用方法。

## 基础信息

- **Base URL**: `http://localhost:8080`
- **Content-Type**: 
  - `application/json`（JSON 请求）
  - `multipart/form-data`（文件上传）
- **字符编码**: UTF-8
- **认证方式**: 暂无认证（开发阶段）

---

## 目录

1. [图像预测接口](#1-图像预测接口)
2. [图像分析接口](#2-图像分析接口)
3. [数据上传接口](#3-数据上传接口)
4. [批量上传接口](#4-批量上传接口)
5. [数据查询接口](#5-数据查询接口)
6. [数据排序查询接口](#6-数据排序查询接口)
7. [健康检查接口](#7-健康检查接口)
8. [前端界面使用](#8-前端界面使用)
9. [错误处理](#9-错误处理)
10. [常见问题](#10-常见问题)

---

## 1. 图像预测接口

### 接口信息

- **端点**: `POST /api/predict`
- **描述**: 对上传的图片进行建筑风格预测（皇室建筑 vs 平民建筑）
- **认证**: 无需认证

### 请求参数

**请求格式**: `application/json`

```json
{
  "image_path": "图片文件的绝对路径"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|--------|------|
| image_path | string | 是 | 图片文件的绝对路径（支持 JPG、PNG 等常见格式） |

### 响应格式

**成功响应**:

```json
{
  "success": true,
  "message": "Prediction completed",
  "prediction": "royal",
  "confidence": 0.8567,
  "royal_ratio": 0.4523,
  "entropy_score": 0.8234,
  "edge_density": 0.3456,
  "texture_complexity": 2.1234
}
```

**响应字段说明**:

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | boolean | 请求是否成功 |
| message | string | 操作结果消息 |
| prediction | string | 预测结果：`royal`（皇室建筑）或 `civilian`（平民建筑） |
| confidence | number | 预测置信度，范围 0-1，值越大表示预测越可信 |
| royal_ratio | number | 皇家色彩占比（黄色+红色），范围 0-1 |
| entropy_score | number | 图像熵值，反映纹理复杂度 |
| edge_density | number | 边缘密度，反映结构复杂度 |
| texture_complexity | number | 纹理对比度 |

### 请求示例

**Python**:
```python
import requests

response = requests.post(
    'http://localhost:8080/api/predict',
    json={
        'image_path': 'E:\\Code\\ACASB\\test.jpg'
    }
)
result = response.json()
print(f"预测结果: {result['prediction']}")
print(f"置信度: {result['confidence']:.2%}")
```

**curl**:
```bash
curl -X POST http://localhost:8080/api/predict \
  -H "Content-Type: application/json" \
  -d '{"image_path": "E:\\Code\\ACASB\\test.jpg"}'
```

**JavaScript (Fetch)**:
```javascript
fetch('http://localhost:8080/api/predict', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        image_path: 'E:\\Code\\ACASB\\test.jpg'
    })
})
.then(response => response.json())
.then(data => {
    console.log('预测结果:', data.prediction);
    console.log('置信度:', data.confidence);
});
```

---

## 2. 图像分析接口

### 接口信息

- **端点**: `POST /api/analyze`
- **描述**: 提取图像的 19 维特征，不进行预测。支持文件上传和图片路径两种方式。
- **认证**: 无需认证

### 请求参数

**方式一：文件上传（推荐）**

**请求格式**: `multipart/form-data`

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|--------|------|
| file | file | 是 | 图片文件 |

**方式二：图片路径**

**请求格式**: `application/json`

```json
{
  "image_path": "图片文件的绝对路径"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|--------|------|
| image_path | string | 是 | 图片文件的绝对路径 |

### 响应格式

**成功响应**:

```json
{
  "success": true,
  "message": "图像分析完成",
  "prediction": null,
  "confidence": null,
  "vmean": 0.7506,
  "vstd": 0.272,
  "smean": 0.3527,
  "sstd": 0.3184,
  "hmean": 0.0702,
  "hstd": 0.1356,
  "ratio_yellow": 0.0508,
  "ratio_red_1": 0.5097,
  "ratio_red_2": 0.011,
  "ratio_blue": 0.0069,
  "ratio_green": 0.0083,
  "ratio_gray_white": 0.3702,
  "ratio_black": 0.034,
  "h_mean": 0.0702,
  "h_std": 0.1356,
  "s_mean": 0.3527,
  "s_std": 0.3184,
  "v_mean": 0.7506,
  "v_std": 0.272,
  "edge_density": 0.2024,
  "entropy": 0.8382,
  "contrast": 2.3383,
  "dissimilarity": 0.0488,
  "homogeneity": 0.4276,
  "asm": 0.055,
  "royal_ratio": 0.5714
}
```

**响应字段说明**:

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | boolean | 请求是否成功 |
| message | string | 操作结果消息 |
| prediction | null | 此接口不进行预测，始终为 null |
| confidence | null | 此接口不进行预测，始终为 null |

**色彩特征**:

| 字段名 | 说明 |
|--------|------|
| ratio_yellow | 黄色像素占比（0-1） |
| ratio_red_1 | 红色1像素占比（0-1） |
| ratio_red_2 | 红色2像素占比（0-1） |
| ratio_blue | 蓝色像素占比（0-1） |
| ratio_green | 绿色像素占比（0-1） |
| ratio_gray_white | 灰白色像素占比（0-1） |
| ratio_black | 黑色像素占比（0-1） |
| royal_ratio | 皇家色彩占比（黄色+红色）（0-1） |

**HSV 特征**:

| 字段名 | 说明 |
|--------|------|
| h_mean | 色相均值（0-1） |
| h_std | 色相标准差（0-1） |
| s_mean | 饱和度均值（0-1） |
| s_std | 饱和度标准差（0-1） |
| v_mean | 明度均值（0-1） |
| v_std | 明度标准差（0-1） |

**纹理特征**:

| 字段名 | 说明 |
|--------|------|
| edge_density | 边缘密度（0-1），反映结构复杂度 |
| entropy | 熵值，反映纹理复杂度 |
| contrast | 对比度 |
| dissimilarity | 不相似度 |
| homogeneity | 同质性 |
| asm | 角二阶矩 |

### 请求示例

**Python（文件上传）**:
```python
import requests

with open('E:\\Code\\ACASB\\test.jpg', 'rb') as f:
    files = {'file': ('test.jpg', f, 'image/jpeg')}
    response = requests.post('http://localhost:8080/api/analyze', files=files)
    result = response.json()
    print(f"皇家比例: {result['royal_ratio']:.2%}")
    print(f"边缘密度: {result['edge_density']:.4f}")
```

**Python（图片路径）**:
```python
import requests

response = requests.post(
    'http://localhost:8080/api/analyze',
    json={
        'image_path': 'E:\\Code\\ACASB\\test.jpg'
    }
)
result = response.json()
print(f"皇家比例: {result['royal_ratio']:.2%}")
```

**curl（文件上传）**:
```bash
curl -X POST http://localhost:8080/api/analyze \
  -F "file=@E:\\Code\\ACASB\\test.jpg"
```

**JavaScript（文件上传）**:
```javascript
const formData = new FormData();
const fileInput = document.getElementById('fileInput');
formData.append('file', fileInput.files[0]);

fetch('http://localhost:8080/api/analyze', {
    method: 'POST',
    body: formData
})
.then(response => response.json())
.then(data => {
    console.log('皇家比例:', data.royal_ratio);
    console.log('边缘密度:', data.edge_density);
});
```

---

## 3. 数据上传接口

### 接口信息

- **端点**: `POST /data/add`
- **描述**: 上传图片并存储分析信息和预测结果到数据库
- **认证**: 无需认证

### 请求参数

**请求格式**: `multipart/form-data`

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|--------|------|
| file | file | 是 | 图片文件（支持 JPG、PNG 等常见格式） |

### 响应格式

**成功响应**:

```json
{
  "success": true,
  "message": "数据添加成功",
  "analysisId": 1,
  "typeId": 1
}
```

**响应字段说明**:

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | boolean | 请求是否成功 |
| message | string | 操作结果消息 |
| analysisId | number | 分析信息记录 ID（可用于查询分析详情） |
| typeId | number | 建筑类型记录 ID（可用于查询预测结果） |

### 工作流程

1. 接收上传的图片文件
2. 将图片保存到临时目录
3. 调用 Python 服务进行特征提取和预测
4. 将分析信息存储到 `building_analysis` 表
5. 将预测结果存储到 `building_type` 表
6. 自动删除临时文件
7. 返回生成的记录 ID

### 请求示例

**Python**:
```python
import requests

with open('E:\\Code\\ACASB\\test.jpg', 'rb') as f:
    files = {'file': ('test.jpg', f, 'image/jpeg')}
    response = requests.post('http://localhost:8080/data/add', files=files)
    result = response.json()
    if result['success']:
        print(f"上传成功！分析ID: {result['analysisId']}, 类型ID: {result['typeId']}")
    else:
        print(f"上传失败: {result['message']}")
```

**curl**:
```bash
curl -X POST http://localhost:8080/data/add \
  -F "file=@E:\\Code\\ACASB\\test.jpg"
```

**JavaScript**:
```javascript
const formData = new FormData();
const fileInput = document.getElementById('fileInput');
formData.append('file', fileInput.files[0]);

fetch('http://localhost:8080/data/add', {
    method: 'POST',
    body: formData
})
.then(response => response.json())
.then(data => {
    if (data.success) {
        console.log('上传成功！');
        console.log('分析ID:', data.analysisId);
        console.log('类型ID:', data.typeId);
    } else {
        console.error('上传失败:', data.message);
    }
});
```

---

## 4. 批量上传接口

### 接口信息

- **端点**: `POST /data/batch`
- **描述**: 批量上传多张图片并存储分析信息和预测结果到数据库
- **认证**: 无需认证

### 请求参数

**请求格式**: `multipart/form-data`

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|--------|------|
| files | file[] | 是 | 多个图片文件（数组） |

### 响应格式

**成功响应**:

```json
{
  "totalCount": 3,
  "successCount": 3,
  "failureCount": 0,
  "items": [
    {
      "fileName": "1.jpg",
      "analysisId": 1,
      "typeId": 1,
      "message": "上传成功",
      "success": true
    },
    {
      "fileName": "2.jpg",
      "analysisId": 2,
      "typeId": 2,
      "message": "上传成功",
      "success": true
    },
    {
      "fileName": "3.jpg",
      "analysisId": 3,
      "typeId": 3,
      "message": "上传成功",
      "success": true
    }
  ]
}
```

**响应字段说明**:

| 字段名 | 类型 | 说明 |
|--------|------|------|
| totalCount | number | 总文件数 |
| successCount | number | 成功上传的文件数 |
| failureCount | number | 失败的文件数 |
| items | array | 每个文件的处理结果数组 |
| items[].fileName | string | 文件名 |
| items[].analysisId | number | 分析信息记录 ID |
| items[].typeId | number | 建筑类型记录 ID |
| items[].message | string | 处理结果消息 |
| items[].success | boolean | 是否成功 |

### 工作流程

1. 接收多个图片文件
2. 逐个调用 Python 服务进行特征提取和预测
3. 将所有分析信息存储到 `building_analysis` 表
4. 将所有预测结果存储到 `building_type` 表
5. 自动删除临时文件
6. 返回详细的批量处理结果

### 请求示例

**Python**:
```python
import requests

files = [
    ('files', open('1.jpg', 'rb')),
    ('files', open('2.jpg', 'rb')),
    ('files', open('3.jpg', 'rb'))
]

response = requests.post('http://localhost:8080/data/batch', files=files)
result = response.json()

print(f"总数: {result['totalCount']}")
print(f"成功: {result['successCount']}")
print(f"失败: {result['failureCount']}")

for item in result['items']:
    status = "✓" if item['success'] else "✗"
    print(f"{status} {item['fileName']}: {item['message']}")
```

**curl**:
```bash
curl -X POST http://localhost:8080/data/batch \
  -F "files=@1.jpg" \
  -F "files=@2.jpg" \
  -F "files=@3.jpg"
```

**JavaScript**:
```javascript
const formData = new FormData();
const fileInput = document.getElementById('fileInput');

for (let i = 0; i < fileInput.files.length; i++) {
    formData.append('files', fileInput.files[i]);
}

fetch('http://localhost:8080/data/batch', {
    method: 'POST',
    body: formData
})
.then(response => response.json())
.then(data => {
    console.log(`总数: ${data.totalCount}`);
    console.log(`成功: ${data.successCount}`);
    console.log(`失败: ${data.failureCount}`);
    
    data.items.forEach(item => {
        const status = item.success ? '✓' : '✗';
        console.log(`${status} ${item.fileName}: ${item.message}`);
    });
});
```

---

## 5. 数据查询接口

### 5.1 查询分析信息

#### 接口信息

- **端点**: `GET /data/analysis/{id}`
- **描述**: 根据记录 ID 查询建筑分析信息
- **认证**: 无需认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|--------|------|
| id | number | 是 | 分析信息记录 ID（URL 路径参数） |

#### 响应格式

**成功响应**:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "imagePath": "E:\\Code\\ACASB\\temp\\xxx.jpg",
    "ratioYellow": 0.0537,
    "ratioRed1": 0.1669,
    "ratioRed2": 0.0488,
    "ratioBlue": 0.2628,
    "ratioGreen": 0.0502,
    "ratioGrayWhite": 0.1234,
    "ratioBlack": 0.2942,
    "hMean": 0.1234,
    "hStd": 0.0567,
    "sMean": 0.4567,
    "sStd": 0.2345,
    "vMean": 0.6789,
    "vStd": 0.1234,
    "edgeDensity": 0.3456,
    "entropy": 7.8901,
    "contrast": 0.2345,
    "dissimilarity": 0.1234,
    "homogeneity": 0.8901,
    "asm": 0.0123,
    "royalRatio": 0.2694,
    "createTime": "2026-02-06T18:29:42",
    "updateTime": "2026-02-06T18:29:42"
  }
}
```

#### 响应字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | boolean | 请求是否成功 |
| data | object | 分析信息对象 |
| data.id | number | 记录 ID |
| data.imagePath | string | 图片路径 |
| data.ratioYellow | number | 黄色比例 |
| data.ratioRed1 | number | 红色1比例 |
| data.ratioRed2 | number | 红色2比例 |
| data.ratioBlue | number | 蓝色比例 |
| data.ratioGreen | number | 绿色比例 |
| data.ratioGrayWhite | number | 灰白色比例 |
| data.ratioBlack | number | 黑色比例 |
| data.hMean | number | 色相均值 |
| data.hStd | number | 色相标准差 |
| data.sMean | number | 饱和度均值 |
| data.sStd | number | 饱和度标准差 |
| data.vMean | number | 明度均值 |
| data.vStd | number | 明度标准差 |
| data.edgeDensity | number | 边缘密度 |
| data.entropy | number | 熵值 |
| data.contrast | number | 对比度 |
| data.dissimilarity | number | 不相似度 |
| data.homogeneity | number | 同质性 |
| data.asm | number | 角二阶矩 |
| data.royalRatio | number | 皇家比例 |
| data.createTime | string | 创建时间（ISO 8601 格式） |
| data.updateTime | string | 更新时间（ISO 8601 格式） |

#### 请求示例

**curl**:
```bash
curl "http://localhost:8080/data/analysis/1"
```

**Python**:
```python
import requests

response = requests.get('http://localhost:8080/data/analysis/1')
result = response.json()

if result['success']:
    data = result['data']
    print(f"图片路径: {data['imagePath']}")
    print(f"皇家比例: {data['royalRatio']:.2%}")
    print(f"边缘密度: {data['edgeDensity']:.4f}")
else:
    print("查询失败")
```

**JavaScript**:
```javascript
fetch('http://localhost:8080/data/analysis/1')
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            const data = result.data;
            console.log('图片路径:', data.imagePath);
            console.log('皇家比例:', (data.royalRatio * 100).toFixed(2) + '%');
            console.log('边缘密度:', data.edgeDensity.toFixed(4));
        } else {
            console.error('查询失败');
        }
    });
```

### 5.2 查询建筑类型

#### 接口信息

- **端点**: `GET /data/type/{id}`
- **描述**: 根据记录 ID 查询建筑类型预测结果
- **认证**: 无需认证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|--------|------|
| id | number | 是 | 建筑类型记录 ID（URL 路径参数） |

#### 响应格式

**成功响应**:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "imagePath": "E:\\Code\\ACASB\\temp\\xxx.jpg",
    "prediction": "royal",
    "confidence": 0.8567,
    "analysisId": 1,
    "createTime": "2026-02-06T18:29:42",
    "updateTime": "2026-02-06T18:29:42"
  }
}
```

#### 响应字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | boolean | 请求是否成功 |
| data | object | 建筑类型对象 |
| data.id | number | 记录 ID |
| data.imagePath | string | 图片路径 |
| data.prediction | string | 预测结果：`royal`（皇室建筑）或 `civilian`（平民建筑） |
| data.confidence | number | 预测置信度（0-1） |
| data.analysisId | number | 关联的分析信息记录 ID |
| data.createTime | string | 创建时间（ISO 8601 格式） |
| data.updateTime | string | 更新时间（ISO 8601 格式） |

#### 请求示例

**curl**:
```bash
curl "http://localhost:8080/data/type/1"
```

**Python**:
```python
import requests

response = requests.get('http://localhost:8080/data/type/1')
result = response.json()

if result['success']:
    data = result['data']
    prediction_text = "皇室建筑" if data['prediction'] == 'royal' else "平民建筑"
    print(f"预测结果: {prediction_text}")
    print(f"置信度: {data['confidence']:.2%}")
else:
    print("查询失败")
```

---

## 6. 数据排序查询接口

### 接口信息

- **端点**: `GET /data/list`
- **描述**: 按指定字段排序查询建筑分析信息，支持筛选和分页
- **认证**: 无需认证

### 请求参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| field | string | 否 | royalRatio | 排序字段 |
| order | string | 否 | desc | 排序方式：`asc`（升序）或 `desc`（降序） |
| limit | number | 否 | 无 | 返回记录数限制 |
| prediction | string | 否 | 无 | 建筑类型筛选：`royal`（只查询皇室建筑）或 `civilian`（只查询平民建筑） |

**可选排序字段**:

| 字段名 | 说明 |
|--------|------|
| royalRatio | 皇家比例 |
| entropy | 熵值 |
| edgeDensity | 边缘密度 |
| contrast | 对比度 |
| dissimilarity | 不相似度 |
| homogeneity | 同质性 |
| asm | 角二阶矩 |
| hMean | 色相均值 |
| hStd | 色相标准差 |
| sMean | 饱和度均值 |
| sStd | 饱和度标准差 |
| vMean | 明度均值 |
| vStd | 明度标准差 |
| ratioYellow | 黄色比例 |
| ratioRed1 | 红色1比例 |
| ratioRed2 | 红色2比例 |
| ratioBlue | 蓝色比例 |
| ratioGreen | 绿色比例 |
| ratioGrayWhite | 灰白色比例 |
| ratioBlack | 黑色比例 |

### 响应格式

**成功响应**:

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "imagePath": "E:\\Code\\ACASB\\temp\\xxx.jpg",
      "royalRatio": 0.4523,
      "entropy": 0.8234,
      "edgeDensity": 0.3456,
      "ratioYellow": 0.0537,
      "ratioRed1": 0.1669,
      "ratioRed2": 0.0488,
      "ratioBlue": 0.2628,
      "ratioGreen": 0.0502,
      "ratioGrayWhite": 0.1234,
      "ratioBlack": 0.2942,
      "hMean": 0.1234,
      "hStd": 0.0567,
      "sMean": 0.4567,
      "sStd": 0.2345,
      "vMean": 0.6789,
      "vStd": 0.1234,
      "contrast": 0.2345,
      "dissimilarity": 0.1234,
      "homogeneity": 0.8901,
      "asm": 0.0123,
      "createTime": "2026-02-06T18:29:42",
      "updateTime": "2026-02-06T18:29:42"
    }
  ],
  "count": 1,
  "message": "查询成功"
}
```

**响应字段说明**:

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | boolean | 请求是否成功 |
| data | array | 分析信息数组 |
| data[].id | number | 记录 ID |
| data[].imagePath | string | 图片路径 |
| data[].所有特征字段 | number | 19 维特征值 |
| data[].createTime | string | 创建时间 |
| data[].updateTime | string | 更新时间 |
| count | number | 返回的记录数 |
| message | string | 操作结果消息 |

### 请求示例

**curl**:
```bash
# 按皇家比例降序排列，返回前5条
curl "http://localhost:8080/data/list?field=royalRatio&order=desc&limit=5"

# 按熵值升序排列，返回前10条
curl "http://localhost:8080/data/list?field=entropy&order=asc&limit=10"

# 查询所有皇室建筑并按皇家比例降序
curl "http://localhost:8080/data/list?field=royalRatio&order=desc&prediction=royal&limit=10"

# 查询所有平民建筑并按熵值升序
curl "http://localhost:8080/data/list?field=entropy&order=asc&prediction=civilian&limit=10"
```

**Python**:
```python
import requests

# 按皇家比例降序排列
params = {
    'field': 'royalRatio',
    'order': 'desc',
    'limit': 5
}
response = requests.get('http://localhost:8080/data/list', params=params)
result = response.json()

if result['success']:
    print(f"查询到 {result['count']} 条记录")
    for item in result['data']:
        prediction = "皇室" if item.get('prediction') == 'royal' else "平民"
        print(f"ID: {item['id']}, 皇家比例: {item['royalRatio']:.2%}")
```

**JavaScript**:
```javascript
// 按皇家比例降序排列
const params = new URLSearchParams({
    field: 'royalRatio',
    order: 'desc',
    limit: 5
});

fetch(`http://localhost:8080/data/list?${params}`)
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            console.log(`查询到 ${result.count} 条记录`);
            result.data.forEach(item => {
                console.log(`ID: ${item.id}, 皇家比例: ${(item.royalRatio * 100).toFixed(2)}%`);
            });
        }
    });
```

---

## 7. 健康检查接口

### 7.1 Java 后端健康检查

#### 接口信息

- **端点**: `GET /api/health`
- **描述**: 检查 Java 后端服务是否正常运行
- **认证**: 无需认证

#### 响应格式

**成功响应**:
```text
Java Backend is running!
```

#### 请求示例

```bash
curl http://localhost:8080/api/health
```

### 7.2 Python 服务健康检查

#### 接口信息

- **端点**: `GET http://localhost:5000/health`
- **描述**: 检查 Python 分析服务是否正常运行
- **认证**: 无需认证

#### 响应格式

**成功响应**:
```json
{
  "status": "healthy",
  "message": "API is ready"
}
```

#### 请求示例

```bash
curl http://localhost:5000/health
```

---

## 8. 前端界面使用

### 访问前端

前端界面已集成到 Spring Boot 应用中，可以直接通过浏览器访问：

```
http://localhost:8080
```

### 功能介绍

前端界面提供以下功能：

1. **批量上传图片**
   - 支持拖拽上传
   - 支持点击选择文件
   - 显示上传进度和结果

2. **数据查询与排序**
   - 按任意特征字段排序
   - 按建筑类型筛选
   - 限制返回记录数

3. **API 接口测试**
   - 图像预测接口测试
   - 图像分析接口测试
   - 数据上传接口测试
   - 批量上传接口测试
   - 数据查询接口测试
   - 一键测试所有接口

4. **实时统计**
   - 总记录数
   - 皇室建筑数量
   - 平民建筑数量
   - 预测准确率

### 使用方法

1. **启动服务**
   ```bash
   # 启动 Python 服务
   start_python.bat
   
   # 启动 Java 服务
   start_java.bat
   ```

2. **访问前端**
   - 在浏览器中打开 `http://localhost:8080`

3. **上传图片**
   - 点击或拖拽图片到上传区域
   - 点击"开始上传"按钮
   - 查看上传结果

4. **测试 API**
   - 在 API 测试区域选择测试接口
   - 填写参数或选择文件
   - 点击"测试"按钮
   - 查看测试结果

5. **查询数据**
   - 选择排序字段和排序方式
   - 选择建筑类型筛选（可选）
   - 设置返回数量
   - 点击"查询"按钮
   - 查看查询结果表格

### 前端技术栈

- **框架**: 原生 JavaScript（无依赖）
- **样式**: CSS3
- **HTTP 请求**: XMLHttpRequest / Fetch API
- **兼容性**: 现代浏览器（Chrome、Firefox、Edge、Safari）

---

## 9. 错误处理

### 错误响应格式

所有接口在失败时会返回以下格式的错误响应：

```json
{
  "success": false,
  "message": "错误描述信息"
}
```

### 常见错误

| 错误类型 | 可能原因 | 解决方法 |
|---------|---------|---------|
| 图片文件不存在 | 指定的图片路径不正确 | 检查图片路径是否正确 |
| 图片格式不支持 | 上传了不支持的图片格式 | 使用 JPG、PNG 等常见格式 |
| 数据库连接失败 | 数据库服务未启动或配置错误 | 检查数据库服务状态和配置 |
| 特征提取失败 | 图片损坏或无法读取 | 检查图片文件是否完整 |
| 预测失败 | Python 服务未启动或模型加载失败 | 检查 Python 服务状态和模型文件 |
| HTTP 500 | 服务器内部错误 | 查看服务器日志获取详细错误信息 |

### 错误处理示例

**Python**:
```python
import requests

try:
    response = requests.post('http://localhost:8080/api/predict', json={
        'image_path': 'E:\\Code\\ACASB\\test.jpg'
    })
    result = response.json()
    
    if result['success']:
        print(f"预测结果: {result['prediction']}")
    else:
        print(f"预测失败: {result['message']}")
        
except requests.exceptions.ConnectionError:
    print("连接失败，请检查服务是否启动")
except requests.exceptions.Timeout:
    print("请求超时，请稍后重试")
except Exception as e:
    print(f"发生错误: {e}")
```

**JavaScript**:
```javascript
fetch('http://localhost:8080/api/predict', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        image_path: 'E:\\Code\\ACASB\\test.jpg'
    })
})
.then(response => {
    if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    return response.json();
})
.then(data => {
    if (data.success) {
        console.log('预测结果:', data.prediction);
    } else {
        console.error('预测失败:', data.message);
    }
})
.catch(error => {
    console.error('发生错误:', error.message);
});
```

---

## 10. 常见问题

### Q1: 如何启动服务？

**A**: 按照以下步骤启动服务：

1. 启动 Python 服务：
   ```bash
   cd acasb-analysis
   python api_server.py
   ```

2. 启动 Java 服务：
   ```bash
   # 设置 JAVA_HOME
   set JAVA_HOME=D:\Zulu17
   set PATH=%JAVA_HOME%\bin;%PATH%
   
   # 编译并运行
   .\mvnw.cmd spring-boot:run
   ```

或使用启动脚本：
```bash
start_python.bat
start_java.bat
```

### Q2: 如何验证服务是否启动成功？

**A**: 使用以下命令检查服务状态：

```bash
# 检查 Java 服务
curl http://localhost:8080/api/health

# 检查 Python 服务
curl http://localhost:5000/health
```

### Q3: 文件上传接口支持哪些图片格式？

**A**: 支持常见的图片格式，包括：
- JPG / JPEG
- PNG
- BMP
- TIFF

### Q4: 批量上传有限制吗？

**A**: 建议单次批量上传不超过 50 张图片。如果需要上传更多图片，建议分批处理。

### Q5: 如何配置数据库？

**A**: 编辑 `src/main/resources/application.properties` 文件：

```properties
spring.datasource.url=jdbc:mysql://192.168.1.199:2881/test?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root@test
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### Q6: 预测结果的置信度是什么意思？

**A**: 置信度表示模型对预测结果的确定程度，范围是 0-1：
- 接近 1：模型非常确定
- 接近 0.5：模型不太确定
- 接近 0：模型认为预测结果不太可能

一般来说，置信度大于 0.7 的预测结果是可靠的。

### Q7: 如何提高预测准确率？

**A**: 可以通过以下方式提高预测准确率：
1. 使用清晰、高质量的图片
2. 确保图片中建筑主体清晰可见
3. 避免使用模糊、过暗或过曝的图片
4. 训练更多样化的数据集

### Q8: 如何查看服务器日志？

**A**: 
- Java 服务日志：在控制台输出
- Python 服务日志：在控制台输出

如果需要将日志保存到文件，可以修改启动脚本重定向输出。

### Q9: 前端界面打不开怎么办？

**A**: 检查以下几点：
1. 确保 Java 服务已启动（端口 8080）
2. 确保浏览器地址正确：`http://localhost:8080`
3. 检查防火墙设置
4. 尝试清除浏览器缓存

### Q10: 如何部署到生产环境？

**A**: 使用打包脚本生成部署包：

```bash
python build_package.py
```

然后：
1. 解压生成的 ZIP 文件到目标服务器
2. 修改配置文件（数据库连接等）
3. 启动 Python 服务
4. 启动 Java 服务

---

## 技术栈

- **后端框架**: Spring Boot 3.5.10
- **数据库**: OceanBase（MySQL 兼容模式）
- **ORM**: MyBatis-Plus
- **特征提取**: Python (OpenCV + NumPy)
- **机器学习**: MLP 分类器
- **前端**: 原生 JavaScript + CSS3

---

## 联系方式

如有问题或建议，请联系开发团队。

---

**"建筑是凝固的音乐，色彩是无声的语言" —— 让我们用代码解读历史的密码。**
