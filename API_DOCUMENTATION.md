# ACASB API 文档

Base URL: `http://localhost:8080`

Python 分析服务默认地址：`http://localhost:5000`

说明：

- Java 服务对外提供统一接口。
- `/api/analyze` 在传统图像特征基础上，支持额外输出 `ai_analysis`。
- 鉴权逻辑已预留，但当前默认关闭。

## 1. 接口总览

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/health` | Java 健康检查 |
| `GET` | `/api/test` | Java 图像接口测试 |
| `POST` | `/api/predict` | 建筑二分类预测 |
| `POST` | `/api/analyze` | 图像特征分析，可选 AI 解析 |
| `POST` | `/data/add` | 单张图片入库 |
| `POST` | `/data/batch` | 批量图片入库 |
| `GET` | `/data/analysis/{id}` | 查询分析详情 |
| `GET` | `/data/type/{id}` | 查询预测详情 |
| `GET` | `/data/list` | 排序查询分析记录 |
| `GET` | `/testPython` | 透传 Python 健康检查 |

## 2. 通用说明

### 2.1 `enable_ai`

支持 `enable_ai` 的接口：

- `POST /api/analyze`
- `POST /data/add`
- `POST /data/batch`

行为：

- 如果传 `enable_ai=true`，本次请求强制启用 AI 解析。
- 如果不传，按 `ai.analysis.enabled` 的全局配置决定。
- AI 解析失败不会让传统图像特征提取失败，错误会落在 `ai_analysis.error`。

### 2.2 `ai_analysis` 字段

可能出现于：

- `/api/analyze` 返回体
- `/data/add` 返回体
- `/data/analysis/{id}`
- `/data/list`

结构示例：

```json
{
  "enabled": true,
  "success": true,
  "provider": "openai-compatible",
  "model": "gpt-4.1-mini",
  "building_type": "宫殿式官式建筑",
  "building_type_confidence": 0.86,
  "style": "明清官式风格",
  "style_confidence": 0.8,
  "estimated_era": "明清",
  "estimated_era_reasoning": "红墙黄瓦与礼制化屋顶明显",
  "roof_type": "歇山顶",
  "main_materials": ["木构", "琉璃瓦"],
  "dominant_colors": [
    {"name": "red", "ratio": 0.45, "description": "墙柱立面"},
    {"name": "yellow", "ratio": 0.3, "description": "屋顶瓦面"}
  ],
  "key_features": ["红墙黄瓦", "高台基", "中轴对称"],
  "summary": "整体接近明清官式建筑。"
}
```

失败示例：

```json
{
  "enabled": true,
  "success": false,
  "provider": "openai-compatible",
  "model": "gpt-4.1-mini",
  "error": "AI analysis is enabled but ai.analysis.api-key is empty"
}
```

## 3. `POST /api/predict`

使用 Python MLP 模型判断图片更接近 `royal` 还是 `civilian`。

### 请求

`Content-Type: application/json`

```json
{
  "image_path": "./uploads/demo.jpg"
}
```

### 成功响应

```json
{
  "success": true,
  "message": "Prediction completed",
  "prediction": "royal",
  "confidence": 0.8567
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `success` | boolean | 是否成功 |
| `message` | string | 结果消息 |
| `prediction` | string | `royal` 或 `civilian` |
| `confidence` | number | 预测置信度 |

### 示例

```bash
curl -X POST http://localhost:8080/api/predict \
  -H "Content-Type: application/json" \
  -d '{"image_path":"./uploads/demo.jpg"}'
```

## 4. `POST /api/analyze`

分析图片并返回 19 维图像特征；可额外返回 AI 建筑解析结果。

支持两种请求方式。

### 4.1 方式一：文件上传

`Content-Type: multipart/form-data`

参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | file | 否 | 上传图片 |
| `image_path` | string | 否 | 表单中的本地图片路径 |
| `enable_ai` | boolean | 否 | 是否启用 AI 解析 |

说明：`file` 与 `image_path` 二选一。

### 4.2 方式二：JSON 请求

`Content-Type: application/json`

```json
{
  "image_path": "./uploads/demo.jpg",
  "enable_ai": true
}
```

### 成功响应

```json
{
  "success": true,
  "message": "Analysis completed",
  "prediction": null,
  "confidence": null,
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
  "royal_ratio": 0.5714,
  "ai_analysis": {
    "enabled": true,
    "success": true,
    "building_type": "宫殿式官式建筑",
    "style": "明清官式风格",
    "estimated_era": "明清"
  }
}
```

### 传统特征字段

| 字段 | 说明 |
|---|---|
| `ratio_yellow` | 黄色占比 |
| `ratio_red_1` / `ratio_red_2` | 红色占比 |
| `ratio_blue` | 蓝色占比 |
| `ratio_green` | 绿色占比 |
| `ratio_gray_white` | 灰白占比 |
| `ratio_black` | 黑色占比 |
| `h_mean` / `h_std` | 色相均值 / 标准差 |
| `s_mean` / `s_std` | 饱和度均值 / 标准差 |
| `v_mean` / `v_std` | 明度均值 / 标准差 |
| `edge_density` | 边缘密度 |
| `entropy` | 熵值 |
| `contrast` | GLCM 对比度 |
| `dissimilarity` | GLCM 不相似度 |
| `homogeneity` | GLCM 同质性 |
| `asm` | GLCM 角二阶矩 |
| `royal_ratio` | 皇家色彩占比，等于黄色 + 红色占比 |

### 示例

文件上传：

```bash
curl -X POST "http://localhost:8080/api/analyze?enable_ai=true" \
  -F "file=@./example.jpg"
```

JSON 请求：

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"image_path":"./uploads/demo.jpg","enable_ai":true}'
```

## 5. `POST /data/add`

上传单张图片，完成分析、预测、入库，并返回持久化结果。

### 请求

`Content-Type: multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | file | 是 | 上传图片 |
| `enable_ai` | boolean | 否 | 是否启用 AI 解析 |

### 成功响应

```json
{
  "success": true,
  "message": "数据添加成功",
  "analysisId": 12,
  "typeId": 12,
  "storedImagePath": "/abs/path/uploads/7c...a.jpg",
  "aiAnalysis": {
    "enabled": true,
    "success": true,
    "building_type": "官式建筑"
  }
}
```

### 示例

```bash
curl -X POST "http://localhost:8080/data/add?enable_ai=true" \
  -F "file=@./example.jpg"
```

## 6. `POST /data/batch`

批量上传并入库。

### 请求

`Content-Type: multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `files` | file[] | 是 | 多张图片 |
| `enable_ai` | boolean | 否 | 是否启用 AI 解析 |

### 成功响应

```json
{
  "totalCount": 2,
  "successCount": 2,
  "failureCount": 0,
  "items": [
    {
      "fileName": "a.jpg",
      "analysisId": 21,
      "typeId": 21,
      "success": true,
      "message": "上传成功"
    }
  ]
}
```

### 示例

```bash
curl -X POST "http://localhost:8080/data/batch?enable_ai=true" \
  -F "files=@./a.jpg" \
  -F "files=@./b.jpg"
```

## 7. `GET /data/analysis/{id}`

查询分析详情。除了传统特征外，还会返回入库时保存的 AI 解析摘要与 `aiAnalysis`。

### 示例

```bash
curl http://localhost:8080/data/analysis/12
```

### 响应示例

```json
{
  "success": true,
  "data": {
    "id": 12,
    "imagePath": "/abs/path/uploads/7c...a.jpg",
    "royalRatio": 0.57,
    "aiBuildingType": "官式建筑",
    "aiStyle": "明清官式风格",
    "aiEstimatedEra": "明清",
    "aiSummary": "整体接近明清官式建筑。",
    "aiAnalysis": {
      "enabled": true,
      "success": true
    }
  }
}
```

## 8. `GET /data/type/{id}`

查询预测记录。

### 示例

```bash
curl http://localhost:8080/data/type/12
```

### 响应示例

```json
{
  "success": true,
  "data": {
    "id": 12,
    "imagePath": "/abs/path/uploads/7c...a.jpg",
    "prediction": "royal",
    "confidence": 0.91,
    "analysisId": 12
  }
}
```

## 9. `GET /data/list`

按指定字段排序返回分析结果，支持 `prediction` 过滤。

### 查询参数

| 参数 | 必填 | 默认值 | 说明 |
|---|---|---|---|
| `field` | 否 | `royalRatio` | 排序字段 |
| `order` | 否 | `desc` | `asc` 或 `desc` |
| `limit` | 否 | `10` | 返回数量，最大 `200` |
| `prediction` | 否 | 无 | `royal` 或 `civilian` |

### 可排序字段

- `royalRatio`
- `edgeDensity`
- `entropy`
- `contrast`
- `dissimilarity`
- `homogeneity`
- `asm`
- `ratioYellow`
- `ratioRed1`
- `ratioRed2`
- `ratioBlue`
- `ratioGreen`
- `ratioGrayWhite`
- `ratioBlack`
- `hMean`
- `hStd`
- `sMean`
- `sStd`
- `vMean`
- `vStd`
- `createTime`
- `updateTime`

### 示例

查询皇家比例最高的 5 条：

```bash
curl "http://localhost:8080/data/list?field=royalRatio&order=desc&limit=5"
```

查询预测为 `royal` 的记录：

```bash
curl "http://localhost:8080/data/list?field=royalRatio&order=desc&prediction=royal&limit=10"
```

### 响应示例

```json
{
  "success": true,
  "count": 2,
  "message": "查询成功",
  "data": [
    {
      "id": 12,
      "imagePath": "/abs/path/uploads/7c...a.jpg",
      "prediction": "royal",
      "confidence": 0.91,
      "royalRatio": 0.57,
      "aiBuildingType": "官式建筑",
      "aiSummary": "整体接近明清官式建筑。",
      "aiAnalysis": {
        "enabled": true,
        "success": true
      }
    }
  ]
}
```

## 10. 健康检查接口

### `GET /api/health`

```bash
curl http://localhost:8080/api/health
```

### `GET /testPython`

```bash
curl http://localhost:8080/testPython
```

## 11. 常见错误

### 图片路径不存在

通常来自 Python 服务，典型表现：

```json
{
  "detail": "Image file not found: ..."
}
```

### AI 解析未配置

如果 `enable_ai=true`，但未配置 `ai.analysis.api-key`，会在 `ai_analysis.error` 中得到错误信息。

### Python 模型不存在

`/api/predict` 依赖 `acasb-analysis/models/` 下的模型文件。如果未训练或文件缺失，Python 会返回模型加载失败。
