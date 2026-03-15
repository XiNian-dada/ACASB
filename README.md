# ACASB

ACASB 是一个面向中国古建筑图像分析、数据入库与大屏展示的双服务项目：

- Java Spring Boot 负责 API、数据集查询、仪表盘统计、图片访问和鉴权
- Python FastAPI 负责本地 19 维图像特征提取
- 云端 OpenAI 兼容模型负责建筑语义解析

当前主链路是：

- 本地返回 19 维传统图像特征
- 云端返回 `ai_analysis` 结构化建筑解析
- 同时保留 `ai_analyze` 原始文本/标准化 JSON 串
- 本地训练的 MLP 代码仍保留，但默认不作为主流程输出

## 文档入口

- API 总文档：[API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
- 前端联调清单：[FRONTEND_API_CURRENT.md](./FRONTEND_API_CURRENT.md)
- 开发维护文档：[DEVELOPMENT.md](./DEVELOPMENT.md)

如果你是前端同学，优先看 [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)。  
这份文档已经把大屏接口、互动体验接口、数据集接口放在最前面，并提供完整响应 JSON 示例。

## 主要能力

- 图像分析：返回颜色占比、HSV 统计、边缘密度、熵值、GLCM 纹理等 19 维特征
- 云端 AI 建筑解析：返回省份、朝代、建筑等级、风格、颜色分布等结构化字段
- 数据集导入：支持目录导入、manifest 导入、单图加 JSON 导入
- 数据集查询：支持按朝代、省份、等级、分组、关键词筛选
- 大屏接口：提供 `/api/dashboard/*`
- 互动体验接口：提供 `/api/experience/*`
- 图片访问：提供 `/media/dataset/**`，前端可直接使用 `imageUrl`
- JWT 鉴权：默认支持生产环境 Bearer Token 鉴权

## 项目结构

```text
ACASB/
├── src/main/java/com/leeinx/acasb/
│   ├── controller/              # API 控制器
│   ├── service/                 # 业务服务、AI 编排、数据集处理
│   ├── entity/                  # MyBatis-Plus 实体
│   ├── mapper/                  # 数据访问层
│   ├── dto/                     # 接口 DTO
│   ├── config/                  # 配置绑定、Web 配置、JWT 初始化
│   └── jwt/                     # JWT 鉴权逻辑
├── src/main/resources/
│   └── application.properties   # Spring Boot 默认配置
├── acasb-analysis/
│   ├── api_server.py            # Python 服务入口
│   ├── ancient_arch_extractor.py
│   ├── mlp_inference.py
│   └── requirements.txt
├── acasb-frontend/
│   └── api-test.html            # 简单接口测试页
├── API_DOCUMENTATION.md
├── FRONTEND_API_CURRENT.md
├── DEVELOPMENT.md
├── install_linux.sh
├── start_java.sh
├── start_python.sh
└── build_package.py
```

## 运行环境

- Java 17+
- Maven 3.6+，或使用 `./mvnw`
- Python 3.11+
- MySQL 8.x 或 OceanBase MySQL 兼容模式

## 快速开始

### 1. 配置

开发模式默认读取 `src/main/resources/application.properties`。  
发布包模式默认读取 `config.properties`。

最少需要配置：

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/acasb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456

python.service.host=localhost
python.service.port=5000

ai.analysis.enabled=true
ai.analysis.base-url=https://api.openai.com
ai.analysis.api-interface=responses
ai.analysis.responses-path=/v1/responses
ai.analysis.chat-completions-path=/v1/chat/completions
ai.analysis.api-key=sk-xxx
ai.analysis.model=gpt-4.1-mini

auth.jwt.enabled=true
auth.jwt.secret=replace-with-a-stable-secret
auth.jwt.expires-hours=720
```

### 2. 启动 Python 服务

```bash
cd acasb-analysis
pip install -r requirements.txt
python api_server.py
```

### 3. 启动 Java 服务

```bash
./mvnw spring-boot:run
```

### 4. 健康检查

```bash
curl http://localhost:8080/api/health
curl http://localhost:5000/health
curl http://localhost:8080/testPython
```

## 发布包启动方式

项目已经提供跨平台启动脚本：

- Windows：`start_java.bat`、`start_python.bat`
- macOS：`start_java.command`、`start_python.command`
- Linux：`install_linux.sh`、`start_java.sh`、`start_python.sh`

Linux 推荐顺序：

```bash
chmod +x install_linux.sh start_java.sh start_python.sh
./install_linux.sh
./start_python.sh
./start_java.sh
```

说明：

- Linux 安装脚本会使用 `opencv-python-headless`
- 安装完成后会验证 `cv2` 是否可导入
- JWT 开启时会要求存在稳定的 `auth.jwt.secret`

## 接口说明

完整接口请看 [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)。

当前前端主要会用到：

- `/api/dashboard/*`
- `/api/experience/*`
- `/api/dataset/*`
- `/api/analyze`
- `/media/dataset/**`

其中：

- `/api/analyze` 返回 19 维特征，并可追加云端 AI 解析
- `/api/dataset/*` 返回结构化比赛数据
- `/media/dataset/**` 用于图片直链访问
- 数据集记录中的 `imageUrl` 才是前端应使用的图片地址

## 数据集导入

支持三种模式：

1. 导入服务端本机目录：`POST /api/dataset/import-folder`
2. 导入单个区域 manifest：`POST /api/dataset/upload-manifest`
3. 上传单张图片和单条 JSON：`POST /api/dataset/upload-record`

比赛数据导入后：

- `relativePath` / `groupRelativePath` 为 ASCII 规范路径
- `originalRelativePath` / `originalGroupRelativePath` 保留原始中文路径
- `imageUrl` 为前端应直接使用的图片地址

## AI 解析说明

`/api/analyze` 的云端 AI 解析基于 OpenAI 兼容接口，支持：

- `responses`
- `chat/completions`
- `auto`

接口返回中：

- `ai_analysis` 是结构化对象
- `ai_analyze` 是标准化后的文本结果
- `building_color_distribution` 会携带 `hex`

这意味着前端不需要自己再做颜色名到 HEX 的映射。

## 鉴权说明

默认情况下：

- `/api/**` 需要 JWT
- `/data/**` 需要 JWT

请求头格式：

```http
Authorization: Bearer <token>
```

生产环境建议：

- 固定 `auth.jwt.secret`
- 不要关闭 `auth.jwt.enabled`
- 通过发布包启动日志获取 token，或自己用同一 secret 生成 token

## API 测试页

项目自带简单测试页：

- [acasb-frontend/api-test.html](./acasb-frontend/api-test.html)

它可以直接测试：

- 健康检查
- 数据集查询
- 大屏接口
- 互动体验接口
- 图片上传分析

## 说明

- 本地 19 维特征分析始终保留
- 云端 AI 解析失败不会阻断本地特征返回
- 建筑风格、等级、朝代属于模型推断，不应视为最终史学结论
- 若部署环境是 Linux 服务器，前端展示图片请使用 `imageUrl`，不要直接使用数据库里的绝对路径字段
