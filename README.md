# ACASB

ACASB 是一个面向中国古建筑图像分析的双服务项目，使用 Spring Boot 负责 API、数据落库与流程编排，使用 FastAPI + OpenCV + scikit-learn 负责图像特征提取与二分类推理。

当前版本在原有 19 维手工特征分析与皇家/民居预测基础上，新增了可选的 AI 建筑解析能力：`/api/analyze` 可以把图片发送到 OpenAI 兼容视觉接口，补充输出建筑类型、主色占比、风格、年代推断、屋顶形制、材料与摘要等结构化信息。

## 功能概览

- 图像 19 维特征提取：颜色占比、HSV 统计量、边缘密度、熵值、 GLCM 纹理特征。
- 建筑二分类预测：输出 `royal` / `civilian` 以及置信度。
- AI 建筑解析：输出 `ai_analysis` 字段，适合做更高层的语义判断。
- 数据持久化：`/data/add` 和 `/data/batch` 会把原图保存到 `app.storage-folder`，并把分析结果写入数据库。
- 排序与筛选查询：支持按颜色、纹理、皇家比例等字段排序，并支持 `prediction` 过滤。

## 系统架构

```text
Client
  |
  v
Spring Boot (:8080)
  - /api/* 图像分析与预测
  - /data/* 数据上传与查询
  - 持久化与配置管理
  - OpenAI 兼容 AI 解析编排
  |
  +--> FastAPI (:5000)
  |     - OpenCV 特征提取
  |     - MLP 模型推理
  |
  +--> MySQL / OceanBase
        - building_analysis
        - building_type
```

## 目录说明

```text
ACASB/
├── src/main/java/com/leeinx/acasb/
│   ├── controller/              # Java API 控制器
│   ├── service/                 # Python 调用、AI 解析、业务服务
│   ├── entity/                  # MyBatis-Plus 实体
│   ├── mapper/                  # 数据访问层
│   ├── dto/                     # 接口 DTO
│   ├── config/                  # 数据库初始化、配置绑定
│   └── jwt/                     # 预留 JWT 逻辑
├── src/main/resources/
│   └── application.properties   # 主配置文件
├── acasb-analysis/
│   ├── api_server.py            # FastAPI 服务入口
│   ├── ancient_arch_extractor.py
│   ├── mlp_inference.py
│   ├── mlp_trainer.py
│   └── requirements.txt
├── acasb-frontend/              # 静态前端示例
├── datasets/                    # 训练数据集
├── API_DOCUMENTATION.md         # 接口文档
├── DEVELOPMENT.md               # 开发与维护文档
└── README.md
```

## 运行环境

- Java 17
- Maven 3.6+，或直接使用项目内置的 `mvnw` / `mvnw.cmd`
- Python 3.11+
- MySQL 8.x / OceanBase(MySQL 兼容模式)

## 快速开始

### 1. 配置数据库与服务地址

编辑 `src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/acasb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456

python.service.scheme=http
python.service.host=localhost
python.service.port=5000

app.temp-folder=./temp
app.storage-folder=./uploads
```

### 2. 安装 Python 依赖

```bash
cd acasb-analysis
pip install -r requirements.txt
```

### 3. 启动 Python 分析服务

```bash
cd acasb-analysis
python api_server.py
```

### 4. 启动 Java 服务

```bash
./mvnw spring-boot:run
```

Windows 环境也可以直接使用 `start_python.bat` 和 `start_java.bat`。

### 5. 健康检查

```bash
curl http://localhost:8080/api/health
curl http://localhost:5000/health
curl http://localhost:8080/testPython
```

## AI 建筑解析配置

AI 解析默认关闭。只有在以下两种情况之一满足时才会调用 OpenAI 兼容接口：

- 全局打开 `ai.analysis.enabled=true`
- 单次请求显式传入 `enable_ai=true`

配置项如下：

```properties
ai.analysis.enabled=false
ai.analysis.base-url=https://api.openai.com
ai.analysis.chat-completions-path=/v1/chat/completions
ai.analysis.api-key=sk-xxx
ai.analysis.model=gpt-4.1-mini
ai.analysis.temperature=0.2
ai.analysis.max-tokens=900
```

说明：

- 这里使用的是 OpenAI 兼容 `chat/completions` 协议，不依赖官方 SDK。
- 图片会以 `data:image/...;base64,...` 形式随请求发送。
- AI 解析失败不会中断传统特征分析，接口仍会返回原始图像特征；失败信息会写在 `ai_analysis.error`。

## 核心接口

### 1. 预测接口

```bash
curl -X POST http://localhost:8080/api/predict \
  -H "Content-Type: application/json" \
  -d '{"image_path":"./uploads/demo.jpg"}'
```

### 2. 分析接口

文件上传并启用 AI：

```bash
curl -X POST "http://localhost:8080/api/analyze?enable_ai=true" \
  -F "file=@./example.jpg"
```

JSON 方式按路径分析：

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"image_path":"./uploads/demo.jpg","enable_ai":true}'
```

成功响应会同时包含传统特征和可选的 `ai_analysis`：

```json
{
  "success": true,
  "message": "Analysis completed",
  "ratio_yellow": 0.0508,
  "edge_density": 0.2024,
  "royal_ratio": 0.5714,
  "ai_analysis": {
    "enabled": true,
    "success": true,
    "provider": "openai-compatible",
    "model": "gpt-4.1-mini",
    "building_type": "宫殿式官式建筑",
    "style": "明清官式风格",
    "estimated_era": "明清",
    "dominant_colors": [
      {"name": "red", "ratio": 0.45, "description": "柱墙与大面积立面"},
      {"name": "yellow", "ratio": 0.30, "description": "屋顶瓦面"}
    ],
    "summary": "整体呈现典型官式建筑色彩与屋顶形制特征。"
  }
}
```

### 3. 数据入库接口

单张上传：

```bash
curl -X POST "http://localhost:8080/data/add?enable_ai=true" \
  -F "file=@./example.jpg"
```

批量上传：

```bash
curl -X POST "http://localhost:8080/data/batch?enable_ai=true" \
  -F "files=@./a.jpg" \
  -F "files=@./b.jpg"
```

`/data/*` 接口会把原图保存到 `app.storage-folder`，数据库中保存的是持久化路径，不再是会被删除的临时文件路径。

## 数据表

### `building_analysis`

除原有 19 维特征字段外，新增以下 AI 相关字段：

- `ai_building_type`
- `ai_style`
- `ai_estimated_era`
- `ai_summary`
- `ai_analysis_json`

应用启动时会自动执行建表，并尝试为旧表补齐新增列。

### `building_type`

- `prediction`
- `confidence`
- `analysis_id`

## 鉴权说明

项目保留了 JWT 拦截器与启动时打印 Token 的逻辑，但当前 `AuthInterceptor` 中 `enableJwt=false`，默认不会真正拦截请求。文档和接口示例按“默认关闭鉴权”描述。

## 文档入口

- 接口文档：`API_DOCUMENTATION.md`
- 开发文档：`DEVELOPMENT.md`

## 已知行为

- `/api/analyze` 的 AI 解析是增强项，不替代本地特征提取。
- AI 输出依赖具体模型能力与图像质量，建筑年代和风格属于推断值，不应视为权威结论。
- 若使用第三方 OpenAI 兼容网关，请确认其支持多模态 `chat/completions`。
