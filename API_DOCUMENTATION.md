# ACASB API 文档

Base URL: `http://localhost:8080`

Python 分析服务默认地址：`http://localhost:5000`

说明：

- Java 服务对外提供统一接口。
- `/api/analyze` 在传统图像特征基础上，支持额外输出 `ai_analysis` 和 `ai_analyze`。
- 鉴权逻辑已预留，但当前默认关闭。

## 1. 接口总览

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/health` | Java 健康检查 |
| `GET` | `/api/test` | Java 图像接口测试 |
| `POST` | `/api/predict` | 建筑二分类预测，默认关闭 |
| `POST` | `/api/analyze` | 图像特征分析，保留 19 维特征并可附加云端 AI 解析 |
| `POST` | `/data/add` | 单张图片入库 |
| `POST` | `/data/batch` | 批量图片入库 |
| `GET` | `/data/analysis/{id}` | 查询分析详情 |
| `GET` | `/data/type/{id}` | 查询预测详情 |
| `GET` | `/data/list` | 排序查询分析记录 |
| `GET` | `/testPython` | 透传 Python 健康检查 |
| `POST` | `/api/dataset/import-folder` | 导入本机目录中的高质量标注数据 |
| `POST` | `/api/dataset/import-manifest` | 导入指定 manifest 文件 |
| `POST` | `/api/dataset/upload-manifest` | 上传一个 manifest 和对应图片数组 |
| `POST` | `/api/dataset/upload-record` | 上传单张图片和对应 JSON |
| `GET` | `/api/dataset/records` | 查询高质量标注记录 |
| `GET` | `/api/dataset/records/{id}` | 查询单条高质量标注记录 |
| `GET` | `/api/dataset/stats/overview` | 汇总统计 |
| `GET` | `/api/dataset/stats/regions` | 省级区域统计 |
| `GET` | `/api/dataset/stats/colors` | 核心色彩聚合统计 |
| `GET` | `/api/dataset/stats/ranks` | 建筑等级统计 |
| `GET` | `/api/dataset/stats/styles` | 建筑风格统计 |

## 2. 通用说明

### 2.1 `enable_ai`

支持 `enable_ai` 的接口：

- `POST /api/analyze`
- `POST /data/add`
- `POST /data/batch`

行为：

- 如果传 `enable_ai=true`，本次请求强制启用云端 AI 解析。
- 如果不传，按 `ai.analysis.enabled` 的全局配置决定。
- AI 解析失败不会让传统图像特征提取失败，失败原因会追加到返回的 `message`。

### 2.2 `ai_analysis` 与 `ai_analyze`

可能出现于：

- `/api/analyze` 返回体
- `/data/add` 返回体
- `/data/analysis/{id}`
- `/data/list`

字段含义：

```json
{
  "ai_analysis": {
    "province_level_region": "北京市",
    "dynasty_guess": "清",
    "building_rank": "皇家",
    "analysis_status": "success"
  },
  "ai_analyze": "{\"province_level_region\":\"北京市\",...}"
}
```

- `ai_analysis` 是解析后的结构化建筑字段。
- `ai_analyze` 是云端模型返回的原始文本，通常是 JSON 字符串。

## 3. `POST /api/predict`

使用 Python MLP 模型判断图片更接近 `royal` 还是 `civilian`。

默认情况下 `local.model.prediction-enabled=false`，此接口会直接返回“本地训练模型预测已禁用”。

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

分析图片并返回 19 维图像特征；可额外返回云端 AI 的结构化解析结果和原始文本。

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
    "province_level_region": "北京市",
    "province_confidence": 0.82,
    "dynasty_guess": "清",
    "building_rank": "皇家",
    "scene_type": "建筑外观",
    "building_present": true,
    "building_primary_colors": ["红色", "黄色", "灰色"],
    "building_color_distribution": [
      { "color": "红色", "ratio": 0.52 },
      { "color": "黄色", "ratio": 0.28 },
      { "color": "灰色", "ratio": 0.2 }
    ],
    "architecture_style": ["皇家官式古建"],
    "scene_description": "画面主体为建筑外观中的近代以前传统建筑，建筑主体色彩分布为红色约占52%，黄色约占28%，灰色约占20%，朝代判断归入清，建筑等级归入皇家。",
    "reasoning": [
      "建筑主体颜色分布清楚，红色约占52%，黄色约占28%，灰色约占20%。",
      "屋顶形制与礼制色彩组合明确，整体属于近代以前的皇家官式古建体系。"
    ],
    "needs_manual_review": false,
    "file_name": "demo.jpg",
    "relative_path": "demo.jpg",
    "analysis_status": "success",
    "error_message": ""
  },
  "ai_analyze": "{\"province_level_region\":\"北京市\",\"province_confidence\":0.82,...}"
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
| `ai_analysis` | 云端 AI 结构化建筑解析结果 |
| `ai_analyze` | 云端 AI 原始输出文本 |

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
  "typeId": null,
  "storedImagePath": "/abs/path/uploads/7c...a.jpg",
  "ai_analyze": "推测为官式建筑，主体颜色以红色与黄色为主。"
}
```

说明：

- 当 `local.model.prediction-enabled=false` 时，`typeId` 会是 `null`。

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

查询分析详情。除了传统特征外，还会返回入库时保存的 `ai_analyze` 原始文本。

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
    "ai_analyze": "推测为官式建筑，风格偏明清官式，年代判断大致在明清时期。"
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
      "ai_analyze": "推测为官式建筑，风格偏明清官式。"
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

## 11. 高质量标注数据接口

这批真实数据的字段核心是：

- `province_level_region`
- `dynasty_guess`
- `building_rank`
- `scene_type`
- `building_primary_colors`
- `building_color_distribution`
- `architecture_style`
- `scene_description`
- `reasoning`
- `needs_manual_review`

### `POST /api/dataset/import-folder`

导入服务端本机已有的数据目录。默认会导入当前工作区下的 `计算机设计大赛`。

请求体：

```json
{
  "dataset_path": "./计算机设计大赛",
  "dataset_name": "competition-dataset",
  "copy_images": true
}
```

说明：

- `copy_images=true` 时，图片会复制到 `app.dataset-storage-folder`
- 服务端会自动把中文目录转换成 ASCII 规范路径

### `POST /api/dataset/upload-manifest`

`Content-Type: multipart/form-data`

参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `manifest` | file | 是 | 区域目录中的 `analysis.json` |
| `images` | file[] | 是 | manifest 中引用的图片文件 |
| `dataset_name` | string | 否 | 数据集名称 |

### `POST /api/dataset/upload-record`

`Content-Type: multipart/form-data`

参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | file | 是 | 图片文件 |
| `metadata` | string | 否 | 单图 JSON 字符串 |
| `metadata_file` | file | 否 | 单图 JSON 文件 |
| `dataset_name` | string | 否 | 数据集名称 |
| `group_name` | string | 否 | 组中文名称 |
| `group_relative_path` | string | 否 | 原始组路径 |

说明：`metadata` 和 `metadata_file` 二选一。

### `GET /api/dataset/records`

查询参数：

| 参数 | 说明 |
|---|---|
| `datasetName` | 数据集名称 |
| `groupName` | 区域组名称，如 `中南地区` |
| `dynasty` | 朝代，如 `清` |
| `province` | 省级区域，如 `湖北省` |
| `rank` | 建筑等级，如 `皇家`、`官员`、`平民` |
| `sceneType` | 场景类型，如 `建筑外观`、`建筑群` |
| `manualReview` | 是否需人工复核 |
| `analysisStatus` | 分析状态 |
| `keyword` | 描述/风格/路径模糊搜索 |
| `limit` | 返回数量，默认 20 |
| `offset` | 偏移量，默认 0 |

返回中的路径字段说明：

- `relativePath` / `groupRelativePath`：ASCII 规范路径
- `originalRelativePath` / `originalGroupRelativePath`：原始中文路径

### `GET /api/dataset/stats/overview`

返回当前筛选条件下的：

- 总记录数
- 成功记录数
- 人工复核数
- 朝代分布
- 等级分布
- 场景分布
- 省级区域分布
- 区域组分布

### `GET /api/dataset/stats/regions`

返回按 `province_level_region` 聚合后的：

- 记录数
- 人工复核数
- 平均区域置信度

### `GET /api/dataset/stats/colors`

基于 `building_color_distribution` 聚合颜色统计，返回：

- `name`
- `imageCount`
- `totalRatio`
- `averageRatio`

### `GET /api/dataset/stats/ranks`

返回实际数据中的建筑等级统计，目前基于真实数据只会出现：

- `皇家`
- `官员`
- `平民`

### `GET /api/dataset/stats/styles`

基于 `architecture_style` 聚合出现频次，便于后续做风格标签云和风格分布图。

## 12. 常见错误

### 图片路径不存在

通常来自 Python 服务，典型表现：

```json
{
  "detail": "Image file not found: ..."
}
```

### AI 解析未配置

如果 `enable_ai=true`，但未配置 `ai.analysis.api-key`，接口仍会成功返回本地特征，但 `message` 会附带 AI 失败原因。

### Python 模型不存在

`/api/predict` 依赖 `acasb-analysis/models/` 下的模型文件。如果未训练或文件缺失，Python 会返回模型加载失败。
