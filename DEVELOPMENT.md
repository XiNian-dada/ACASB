# ACASB 开发文档

本文档面向维护者和二次开发者，说明项目结构、核心流程、配置项、数据库模型、扩展点和常见排障方法。

## 1. 技术栈

### Java 服务

- Spring Boot 3.5.x
- MyBatis-Plus
- MySQL Connector/J
- Jackson

职责：

- 暴露统一 REST API
- 处理文件上传与持久化
- 调用 Python 分析服务
- 调用 OpenAI 兼容视觉模型
- 保存分析结果与分类结果

### Python 服务

- FastAPI
- OpenCV
- scikit-image
- scikit-learn
- NumPy / Pandas

职责：

- 提取 19 维图像特征
- 加载训练好的 MLP 模型
- 输出二分类结果

## 2. 代码结构

### Java 侧

- `src/main/java/com/leeinx/acasb/controller/ImageController.java`
  - `/api/predict`
  - `/api/analyze`
- `src/main/java/com/leeinx/acasb/controller/dataController.java`
  - `/data/add`
  - `/data/batch`
  - `/data/analysis/{id}`
  - `/data/type/{id}`
  - `/data/list`
- `src/main/java/com/leeinx/acasb/service/PythonAnalysisClient.java`
  - 对 Python `/predict`、`/analyze` 做统一调用
- `src/main/java/com/leeinx/acasb/service/OpenAiCompatibleBuildingAnalysisService.java`
  - 调用 OpenAI 兼容 `chat/completions`
  - 组装图片为 base64 data URI
  - 解析结构化 JSON 响应
- `src/main/java/com/leeinx/acasb/config/DatabaseInitializer.java`
  - 启动时建表
  - 为旧库补充 AI 相关列
- `src/main/java/com/leeinx/acasb/config/PythonServiceProperties.java`
  - Python 服务地址绑定
- `src/main/java/com/leeinx/acasb/config/AiAnalysisProperties.java`
  - AI 解析配置绑定

### Python 侧

- `acasb-analysis/api_server.py`
  - FastAPI 入口
  - `/analyze` 只做传统特征分析
  - `/predict` 做 MLP 推理
- `acasb-analysis/ancient_arch_extractor.py`
  - 图像预处理、颜色统计、边缘与纹理特征提取
- `acasb-analysis/mlp_inference.py`
  - 模型加载与预测
- `acasb-analysis/mlp_trainer.py`
  - 训练脚本

## 3. 请求流程

### 3.1 `/api/analyze`

1. Java 接收文件上传或图片路径。
2. 如果是上传文件，先写入 `app.temp-folder`。
3. Java 调用 Python `/analyze` 提取传统图像特征。
4. 若本次请求启用了 `enable_ai`，Java 再调用 OpenAI 兼容视觉接口。
5. Java 合并结果，返回传统特征 + `ai_analysis`。
6. 临时上传文件会在请求结束后删除。

### 3.2 `/data/add` 与 `/data/batch`

1. Java 把上传原图保存到 `app.storage-folder`。
2. 调用 Python `/analyze`。
3. 如果启用 AI 解析，再调用 OpenAI 兼容视觉接口。
4. 保存 `building_analysis` 记录。
5. 再调用 Python `/predict` 保存 `building_type` 记录。
6. 返回分析 ID / 类型 ID / AI 解析结果摘要。

和 `/api/analyze` 的区别：

- `/api/analyze` 是一次性分析，不持久化原图。
- `/data/*` 是数据入库流程，会保存原图与数据库记录。

## 4. 配置项

主配置文件：`src/main/resources/application.properties`

### 4.1 存储与上传

| 配置项 | 说明 | 默认值 |
|---|---|---|
| `app.temp-folder` | `/api/analyze` 临时上传目录 | `./temp` |
| `app.storage-folder` | `/data/*` 原图持久化目录 | `./uploads` |

### 4.2 Python 服务

| 配置项 | 说明 | 默认值 |
|---|---|---|
| `python.service.scheme` | 协议 | `http` |
| `python.service.host` | 主机 | `localhost` |
| `python.service.port` | 端口 | `5000` |

### 4.3 AI 建筑解析

| 配置项 | 说明 | 默认值 |
|---|---|---|
| `ai.analysis.enabled` | 是否默认启用 AI 解析 | `false` |
| `ai.analysis.base-url` | OpenAI 兼容服务地址 | `https://api.openai.com` |
| `ai.analysis.chat-completions-path` | Chat Completions 路径 | `/v1/chat/completions` |
| `ai.analysis.api-key` | API Key | 空 |
| `ai.analysis.model` | 模型名 | `gpt-4.1-mini` |
| `ai.analysis.temperature` | 采样温度 | `0.2` |
| `ai.analysis.max-tokens` | 最大输出 token | `900` |

启用方式：

- 全局打开 `ai.analysis.enabled=true`
- 或在单次请求中传 `enable_ai=true`

## 5. AI 解析输出结构

`ImageFeatures` 新增：

```json
{
  "ai_analysis": {
    "enabled": true,
    "success": true,
    "provider": "openai-compatible",
    "model": "gpt-4.1-mini",
    "building_type": "宫殿式官式建筑",
    "building_type_confidence": 0.86,
    "style": "明清官式风格",
    "style_confidence": 0.8,
    "estimated_era": "明清",
    "estimated_era_reasoning": "红墙黄瓦、重檐屋顶与礼制色彩明显",
    "roof_type": "歇山顶",
    "main_materials": ["木构", "琉璃瓦"],
    "dominant_colors": [
      {"name": "red", "ratio": 0.45, "description": "柱墙与立面"},
      {"name": "yellow", "ratio": 0.3, "description": "屋顶瓦面"}
    ],
    "key_features": ["中轴对称", "高台基", "彩画构件"],
    "summary": "整体接近明清官式建筑特征。"
  }
}
```

失败时不会抛弃整次分析，只会返回：

```json
{
  "ai_analysis": {
    "enabled": true,
    "success": false,
    "provider": "openai-compatible",
    "model": "gpt-4.1-mini",
    "error": "AI analysis is enabled but ai.analysis.api-key is empty"
  }
}
```

## 6. 数据库设计

### 6.1 `building_analysis`

核心字段：

- 传统特征字段：`ratio_*`、`h_*`、`s_*`、`v_*`、`edge_density`、`entropy`、`contrast`、`dissimilarity`、`homogeneity`、`asm`、`royal_ratio`
- AI 字段：
  - `ai_building_type`
  - `ai_style`
  - `ai_estimated_era`
  - `ai_summary`
  - `ai_analysis_json`

说明：

- `ai_analysis_json` 存完整结构化结果，便于后续重算或前端直接展示。
- 其余四个字段是从结构化 JSON 中提炼出的高频字段，便于列表页与 SQL 查询。

### 6.2 `building_type`

- `prediction`
- `confidence`
- `analysis_id`

`analysis_id` 指向 `building_analysis.id`。

## 7. 扩展建议

### 7.1 如果要扩展 AI 输出字段

需要同步修改以下位置：

1. `src/main/java/com/leeinx/acasb/dto/AiBuildingAnalysis.java`
2. `src/main/java/com/leeinx/acasb/service/OpenAiCompatibleBuildingAnalysisService.java`
3. `src/main/java/com/leeinx/acasb/controller/dataController.java`
4. `src/main/java/com/leeinx/acasb/entity/BuildingAnalysis.java`
5. `src/main/java/com/leeinx/acasb/config/DatabaseInitializer.java`

### 7.2 如果要替换 OpenAI 兼容网关

通常只要改以下配置，不需要改代码：

```properties
ai.analysis.base-url=https://your-gateway.example.com
ai.analysis.chat-completions-path=/v1/chat/completions
ai.analysis.api-key=your-key
ai.analysis.model=your-vision-model
```

前提是网关支持：

- `chat/completions`
- 多模态消息格式
- `image_url.url = data:image/...;base64,...`

### 7.3 如果要新增排序字段

需要同时修改：

1. `BuildingAnalysis` 实体
2. `DatabaseInitializer`
3. `BuildingAnalysisService.ALLOWED_SORT_FIELDS`
4. 前端排序字段选项

## 8. 已修正的项目行为

本轮整理包含以下修正：

- 把 Python 服务地址从硬编码 `http://localhost:5000` 改为配置驱动。
- `/api/analyze` 新增 JSON 请求方式，并支持 `enable_ai`。
- `/data/*` 不再把即将删除的临时文件路径写入数据库，而是保存到持久化目录。
- `/data/list` 的 `prediction` 过滤已接入实际逻辑。
- 启动时会自动为已有 `building_analysis` 表补齐 AI 相关字段。

## 9. 常见问题

### 9.1 Java 服务能启动，但 `/api/analyze` 报 Python 调用失败

检查：

- `acasb-analysis/api_server.py` 是否已经启动
- `python.service.host` / `python.service.port` 是否正确
- Java 与 Python 是否运行在同一台机器或网络可达

### 9.2 AI 解析字段一直返回失败

常见原因：

- `ai.analysis.api-key` 为空
- 兼容网关不支持多模态图片输入
- 兼容网关路径不是 `/v1/chat/completions`
- 模型本身不是视觉模型

### 9.3 数据库没有新增 AI 字段

排查顺序：

1. 查看 Java 启动日志是否执行了 `DatabaseInitializer`
2. 确认数据库用户是否有 `ALTER TABLE` 权限
3. 手动检查 `information_schema.columns`

## 10. 调试建议

### Java 侧

- 查看控制器返回的 `message`
- 查看 Spring Boot 控制台异常堆栈
- 重点排查上传目录权限和数据库连通性

### Python 侧

- 直接请求 `http://localhost:5000/health`
- 用固定图片路径请求 `/analyze` 和 `/predict`
- 检查模型文件是否存在于 `acasb-analysis/models/`

## 11. 测试与验证

建议最少执行：

```bash
./mvnw -q -DskipTests compile
python -m py_compile acasb-analysis/api_server.py
```

如果接入了真实 AI 服务，再手动验证一次：

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"image_path":"./uploads/demo.jpg","enable_ai":true}'
```
