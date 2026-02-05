# ACASB: Ancient Chinese Architecture in Spring Boot

> **"权力的色谱" —— 1911 年前中国官方等级的建筑量化边界研究系统**
> *An Interdisciplinary Digital Humanities Project for Ancient Chinese Architecture.*

---

## 🏛️ 项目愿景 (Vision)

在中国古代，建筑色彩并非单纯的美学选择，而是被法律（如《大清会典》）严格定义的"权力资产"。**ACASB** 旨在通过数字化手段，将晦涩的礼制条文转化为可量化的算法逻辑，揭示砖瓦之间流淌的社会秩序。

---

## 🛠️ 系统架构 (Architecture)

本项目采用前后端分离及跨语言协作架构，充分发挥不同技术栈的优势：

### 技术栈

* **业务大脑 (Main Backend)**: **Java 17 (Zulu JDK) + Spring Boot 3.5.x**
  * 负责核心业务逻辑、安全校验、事务管理
  * 提供 RESTful API 接口
  * 作为 API 网关协调各服务

* **分析引擎 (AI & CV Module)**: **Python 3.11+ (FastAPI)**
  * 集成 **OpenCV** 处理图像计算、优化
  * MLP 分类器进行建筑类型识别（皇家/民间）
  * 特征提取：色彩分析、纹理特征、边缘密度等

* **数据持久层 (Database)**: **OceanBase (社区版) / PostgreSQL**
  * 支撑"常读不常写、单个数据量大"的高并发读取场景
  * 确保礼制数据的一致性

### 服务架构

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────┐
│  Java Backend (Port 8080)  │
│  - API Gateway             │
│  - Business Logic          │
│  - Data Management         │
└──────┬────────────────────┘
       │ RestTemplate
       ▼
┌─────────────────────────────┐
│ Python API (Port 5000)     │
│  - Image Processing        │
│  - Feature Extraction      │
│  - ML Prediction          │
└─────────────────────────────┘
```

---

## ✨ 核心功能 (Core Features)

### 1. 建筑类型识别 (Building Classification)

* **MLP 分类器**：基于 19 维特征向量的深度学习模型
* **特征提取**：
  * 色彩特征：黄色、红色占比
  * 纹理特征：熵值、对比度
  * 结构特征：边缘密度
* **分类结果**：皇家建筑 (Royal) / 民间建筑 (Civilian)
* **置信度评估**：提供预测可信度评分

### 2. 像素级色彩分析 (Pixel Mapping & Extraction)

* **智能降噪**：自动识别并剔除天空、树木等环境背景对建筑色彩占比的干扰
* **多模态适配**：针对历史黑白照片，自动从"色彩分析"切换为"结构特征（开间、屋顶）分析"
* **量化输出**：计算建筑立面的色彩占比与色彩熵

### 3. 礼制规则引擎 (Regulation Engine)

* **参数化转译**：将《大清会典》中的定性描述（如"凡民间房舍，不许用黄瓦"）转译为数据库中的约束参数
* **逾制指数计算**：分析色彩占有率相对于职官等级的离散导数，自动判定建筑是否存在"僭越"行为

---

## 📂 项目目录结构 (Project Structure)

```text
ACASB/
├── src/
│   └── main/
│       ├── java/com/leeinx/acasb/
│       │   ├── AcasbApplication.java      # Spring Boot 主类
│       │   ├── TestController.java         # 测试控制器
│       │   ├── PredictionController.java    # 预测 API 控制器
│       │   ├── PredictionRequest.java      # 预测请求 DTO
│       │   └── dto/                       # 数据传输对象
│       └── resources/
│           └── application.properties      # 应用配置
├── acasb-analysis/                         # Python 分析引擎
│   ├── api_server.py                      # FastAPI 服务入口
│   ├── mlp_inference.py                  # MLP 推理模块
│   ├── mlp_trainer.py                    # MLP 训练模块
│   ├── ancient_arch_extractor.py          # 特征提取器
│   ├── models/                           # 训练好的模型
│   │   ├── mlp_model.pkl
│   │   └── scaler.pkl
│   └── requirements.txt                   # Python 依赖
├── datasets/                             # 数据集
│   ├── royal/                            # 皇家建筑图片
│   └── civilian/                         # 民间建筑图片
├── start_java.bat                        # Java 启动脚本
├── start_python.bat                      # Python 启动脚本
├── build_package.py                      # 一键打包脚本
├── pom.xml                              # Maven 配置
└── README.md                            # 项目文档
```

---

## 🚀 快速开始 (Quick Start)

### 前置要求

* **Java 17** (推荐使用 Zulu JDK: `D:\Zulu17`)
* **Python 3.11+**
* **Maven 3.6+** (项目已包含 Maven Wrapper)

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd ACASB
   ```

2. **安装 Python 依赖**
   ```bash
   cd acasb-analysis
   pip install -r requirements.txt
   ```

3. **启动服务**

   **方式一：使用启动脚本（推荐）**
   ```bash
   # 先启动 Python 服务
   start_python.bat
   
   # 再启动 Java 服务
   start_java.bat
   ```

   **方式二：手动启动**
   ```bash
   # 终端 1：启动 Python API
   cd acasb-analysis
   python api_server.py
   
   # 终端 2：启动 Java Backend
   # 设置 JAVA_HOME
   set JAVA_HOME=D:\Zulu17
   set PATH=%JAVA_HOME%\bin;%PATH%
   
   # 编译并运行
   .\mvnw.cmd spring-boot:run
   ```

### 验证安装

```bash
# 检查 Java 服务
curl http://localhost:8080/api/health

# 检查 Python 服务
curl http://localhost:5000/health
```

---

## 📖 API 使用 (API Usage)

### 预测接口

**端点**: `POST /api/predict`

**请求格式**:
```json
{
  "image_path": "图片文件的绝对路径"
}
```

**PowerShell 示例**:
```powershell
$body = @{
    image_path = "$PWD\test.jpg"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/predict" -Method POST -ContentType "application/json" -Body $body
```

**响应格式**:
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
- `success`: 请求是否成功
- `prediction`: 预测结果（"royal" 或 "civilian"）
- `confidence`: 预测置信度（0-1）
- `royal_ratio`: 皇家色彩占比（黄色+红色）
- `entropy_score`: 图像熵值（纹理复杂度）
- `edge_density`: 边缘密度（结构复杂度）
- `texture_complexity`: 纹理对比度

### 健康检查接口

**Java Backend**: `GET /api/health`
```bash
curl http://localhost:8080/api/health
# 返回: "Java Backend is running!"
```

**Python API**: `GET /health`
```bash
curl http://localhost:5000/health
# 返回: {"status":"healthy","message":"API is ready"}
```

---

## 📦 打包部署 (Package & Deployment)

### 一键打包

项目提供自动化打包脚本，将 Java JAR、Python 代码和启动脚本打包为 ZIP 文件：

```bash
python build_package.py
```

打包完成后会生成：`ACASB_Package_YYYYMMDD_HHMMSS.zip`

### ZIP 包内容

```
ACASB_Package_YYYYMMDD_HHMMSS.zip
├── ACASB-0.0.1-SNAPSHOT.jar      # Java 应用
├── acasb-analysis/               # Python 服务
│   ├── api_server.py
│   ├── mlp_inference.py
│   ├── models/
│   └── requirements.txt
├── start_java.bat                # Java 启动脚本
├── start_python.bat              # Python 启动脚本
└── README.txt                    # 部署说明
```

### 部署步骤

1. 解压 ZIP 文件到目标服务器
2. 确保目标环境已安装 Java 17 和 Python 3.11+
3. 修改 `start_java.bat` 中的 JAVA_HOME 路径
4. 先运行 `start_python.bat` 启动 Python 服务
5. 再运行 `start_java.bat` 启动 Java 服务

---

## 🔧 开发者指南 (Developer Guide)

### 数据库配置

编辑 `src/main/resources/application.properties`:

```properties
# OceanBase / MySQL 配置
spring.datasource.url=jdbc:mysql://localhost:2881/acasb?useSSL=false
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### 训练自定义模型

```bash
cd acasb-analysis

# 准备数据集
# 将图片按类别放入 datasets/royal/ 和 datasets/civilian/

# 运行训练脚本
python mlp_trainer.py

# 模型将保存在 models/ 目录下
```

### 添加新的特征

编辑 `ancient_arch_extractor.py`，在 `extract_features` 方法中添加新的特征提取逻辑。

---

## 🔄 CI/CD 自动化 (CI/CD Automation)

项目配置了 GitHub Actions，每次代码提交都会自动：

1. **构建 Java 项目**：使用 Maven 编译并打包
2. **运行 Python 打包脚本**：生成部署 ZIP 包
3. **上传构建产物**：将 ZIP 包作为 GitHub Release 附件

查看 CI/CD 配置：`.github/workflows/build.yml`

---

## 📜 学术参考 (Academic References)

* 《大清会典》
* 《工部工程做法则例》
* 《中国古代建筑史》

---

## 🤝 贡献指南 (Contributing)

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 许可证 (License)

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

---

## 👥 作者 (Authors)

* **LeeInx** - 项目负责人

---

## 🙏 致谢 (Acknowledgments)

* 感谢所有为数字人文研究做出贡献的学者和开发者
* 感谢开源社区提供的优秀工具和框架

---

## 📞 联系方式 (Contact)

如有问题或建议，请通过以下方式联系：

* 提交 [Issue](../../issues)
* 发送邮件至: [your-email@example.com]

---

**"建筑是凝固的音乐，色彩是无声的语言" —— 让我们用代码解读历史的密码。**
