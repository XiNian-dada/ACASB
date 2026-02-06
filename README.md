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

* **数据持久层 (Database)**: **OceanBase (社区版)**
  * 支撑"常读不常写、单个数据量大"的高并发读取场景
  * 确保礼制数据的一致性
  * 使用 MySQL 兼容驱动连接

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
* **逾制指数计算**：分析建筑参数并导入全连接层，自动判定建筑是否存在"僭越"行为

---

## 📂 项目目录结构 (Project Structure)

```text
ACASB/
├── src/
│   └── main/
│       ├── java/com/leeinx/acasb/
│       │   ├── AcasbApplication.java      # Spring Boot 主类
│       │   ├── controller/                    # 控制器层
│       │   │   ├── ImageController.java    # 图像预测 API
│       │   │   └── DataController.java     # 数据管理 API
│       │   ├── service/                       # 服务层
│       │   │   ├── BuildingAnalysisService.java
│       │   │   └── BuildingTypeService.java
│       │   ├── mapper/                        # 数据访问层
│       │   │   ├── BuildingAnalysisMapper.java
│       │   │   └── BuildingTypeMapper.java
│       │   ├── entity/                        # 数据库实体
│       │   │   ├── BuildingAnalysis.java
│       │   │   └── BuildingType.java
│       │   ├── dto/                           # 数据传输对象
│       │   │   ├── ImageFeatures.java
│       │   │   └── ImageAnalysisResult.java
│       │   ├── config/                        # 配置类
│       │   │   └── DatabaseInitializer.java # 数据库表初始化
│       │   └── jwt/                           # JWT 工具
│       │       └── JwtUtils.java
│       └── resources/
│           ├── application.properties      # 应用配置
│           └── sql/                     # SQL 脚本
│               └── init.sql
├── acasb-analysis/                         # Python 分析引擎
│   ├── api_server.py                      # FastAPI 服务入口
│   ├── mlp_inference.py                  # MLP 推理模块
│   ├── mlp_trainer.py                    # MLP 训练模块
│   ├── ancient_arch_extractor.py          # 特征提取器
│   ├── create_tables.py                  # 数据库表创建脚本
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
# 检查 Java 服务健康状态
curl http://localhost:8080/api/health

# 检查 Python 服务健康状态
curl http://localhost:5000/health

# 测试数据库连接（启动后会自动创建表）
# 查看日志输出，确认 "数据库表初始化完成！"
```

### 测试数据上传

使用 PowerShell 上传测试图片：

```powershell
# 准备上传脚本
$filePath = "E:\Code\ACASB\2.jpg"
$uri = "http://localhost:8080/data/add"

$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
$fileName = Split-Path $filePath -Leaf

$header = "--$boundary$LF"
$header += "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"$LF"
$header += "Content-Type: application/octet-stream$LF"
$header += "$LF"

$footer = "$LF--$boundary--$LF"

$memStream = New-Object System.IO.MemoryStream
$writer = New-Object System.IO.BinaryWriter($memStream)

$writer.Write([System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($header))
$writer.Write($fileBytes)
$writer.Write([System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($footer))
$writer.Flush()

$response = Invoke-RestMethod -Uri $uri -Method POST -ContentType "multipart/form-data; boundary=$boundary" -Body $memStream.ToArray()
$response | ConvertTo-Json -Depth 10
```

预期响应：

```json
{
  "success": true,
  "message": "数据添加成功",
  "analysisId": 1,
  "typeId": 1
}
```

### 测试数据查询

```bash
# 查询分析信息
curl "http://localhost:8080/data/analysis/1"

# 查询建筑类型
curl "http://localhost:8080/data/type/1"
```

---

## 📖 API 使用 (API Usage)

### 1. 图像预测接口

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

### 2. 图像分析接口

**端点**: `POST /api/analyze`

**请求格式**:
```json
{
  "image_path": "图片文件的绝对路径"
}
```

**响应格式**:
```json
{
  "success": true,
  "message": "Analysis completed",
  "ratio_yellow": 0.0537,
  "ratio_red_1": 0.1669,
  "ratio_red_2": 0.0488,
  "ratio_blue": 0.2628,
  "ratio_green": 0.0502,
  "ratio_gray_white": 0.1234,
  "ratio_black": 0.2942,
  "h_mean": 0.1234,
  "h_std": 0.0567,
  "s_mean": 0.4567,
  "s_std": 0.2345,
  "v_mean": 0.6789,
  "v_std": 0.1234,
  "edge_density": 0.3456,
  "entropy": 7.8901,
  "contrast": 0.2345,
  "dissimilarity": 0.1234,
  "homogeneity": 0.8901,
  "asm": 0.0123,
  "royal_ratio": 0.2694
}
```

**说明**: 此接口仅提取图像特征，不进行预测，用于性能优化场景。

### 3. 数据上传接口

**端点**: `POST /data/add`

**请求格式**: `multipart/form-data`

**PowerShell 示例**:
```powershell
$filePath = "E:\Code\ACASB\2.jpg"
$uri = "http://localhost:8080/data/add"

$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
$fileName = Split-Path $filePath -Leaf

$header = "--$boundary$LF"
$header += "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"$LF"
$header += "Content-Type: application/octet-stream$LF"
$header += "$LF"

$footer = "$LF--$boundary--$LF"

$memStream = New-Object System.IO.MemoryStream
$writer = New-Object System.IO.BinaryWriter($memStream)

$writer.Write([System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($header))
$writer.Write($fileBytes)
$writer.Write([System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($footer))
$writer.Flush()

$response = Invoke-RestMethod -Uri $uri -Method POST -ContentType "multipart/form-data; boundary=$boundary" -Body $memStream.ToArray()
$response | ConvertTo-Json -Depth 10
```

**响应格式**:
```json
{
  "success": true,
  "message": "数据添加成功",
  "analysisId": 1,
  "typeId": 1
}
```

**响应字段说明**:
- `success`: 请求是否成功
- `message`: 操作结果消息
- `analysisId`: 分析信息记录 ID
- `typeId`: 建筑类型记录 ID

**功能说明**:
1. 接收上传的图片文件
2. 调用 Python 服务进行特征提取和预测
3. 将分析信息存储到 `building_analysis` 表
4. 将预测结果存储到 `building_type` 表
5. 自动删除临时文件
6. 返回生成的记录 ID

### 4. 批量上传接口

**端点**: `POST /data/batch`

**请求格式**: `multipart/form-data`

**参数**:
- `files`: 多个图片文件（数组）

**Python 示例**:
```python
import requests

files = [
    ('files', open('1.jpg', 'rb')),
    ('files', open('2.jpg', 'rb')),
    ('files', open('3.jpg', 'rb'))
]

response = requests.post('http://localhost:8080/data/batch', files=files)
result = response.json()
print(result)
```

**响应格式**:
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
- `totalCount`: 总文件数
- `successCount`: 成功上传的文件数
- `failureCount`: 失败的文件数
- `items`: 每个文件的处理结果数组
  - `fileName`: 文件名
  - `analysisId`: 分析信息记录 ID
  - `typeId`: 建筑类型记录 ID
  - `message`: 处理结果消息
  - `success`: 是否成功

**功能说明**:
1. 接收多个图片文件
2. 逐个调用 Python 服务进行特征提取和预测
3. 将所有分析信息存储到 `building_analysis` 表
4. 将所有预测结果存储到 `building_type` 表
5. 自动删除临时文件
6. 返回详细的批量处理结果

**测试脚本**:
```bash
# 使用 Python 批量上传测试
cd e:\Code\ACASB
python test_batch_upload.py
```

### 6. 数据集特征计算工具

**脚本**: `calculate_dataset_features.py`

**功能**:
- 批量处理数据集中的图片
- 提取所有 19 维特征
- 计算每个特征的统计信息（均值、标准差、最小值、最大值）
- 对比平民建筑和皇室建筑的特征差异

**使用方法**:
```bash
cd e:\Code\ACASB
python calculate_dataset_features.py
```

**输出内容**:
- 平民建筑数据集统计（195 张图片）
- 皇室建筑数据集统计（200 张图片）
- 色彩特征统计（7 个特征）
- HSV 特征统计（6 个特征）
- 纹理特征统计（6 个特征）
- 特征对比分析（皇家比例、熵值、边缘密度）

**输出示例**:
```
平民建筑 数据集特征统计
--------------------------------------------------------------------------------
  🎨 色彩特征:
    - 黄色比例: 均值=0.1708, 标准差=0.1152
    - 红色1比例: 均值=0.1650, 标准差=0.1473
    ...
  🌈 HSV特征:
    - 色相均值: 均值=0.2766, 标准差=0.1088
    - 饱和度均值: 均值=0.2044, 标准差=0.0830
    ...
  📐 纹理特征:
    - 边缘密度: 均值=0.2531, 标准差=0.0425
    - 熵值: 均值=0.9675, 标准差=0.0263
    ...

📊 特征对比分析
--------------------------------------------------------------------------------
  熵值对比:
    - 平民建筑平均熵值: 0.9675
    - 皇室建筑平均熵值: 0.9555
    - 差异: 0.0120
```

**功能说明**:
1. 直接调用特征提取器处理本地图片
2. 不需要通过 API 服务，提高处理效率
3. 支持批量处理大量图片
4. 自动统计和对比分析
5. 方便后期量化处理

### 7. 数据查询接口

**查询分析信息**: `GET /data/analysis/{id}`

```bash
curl "http://localhost:8080/data/analysis/1"
```

**响应格式**:
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

**查询建筑类型**: `GET /data/type/{id}`

```bash
curl "http://localhost:8080/data/type/1"
```

**响应格式**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "imagePath": "E:\\Code\\ACASB\\temp\\xxx.jpg",
    "prediction": "royal",
    "confidence": 0.9973,
    "analysisId": 1,
    "createTime": "2026-02-06T18:29:42",
    "updateTime": "2026-02-06T18:29:42"
  }
}
```

### 5. 健康检查接口

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
# OceanBase 配置（使用 MySQL 兼容驱动）
spring.datasource.url=jdbc:mysql://192.168.1.199:2881/test?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root@test
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# MyBatis-Plus 配置
mybatis-plus.configuration.map-underscore-to-camel-case=true
```

**数据库表结构**:

应用启动时会自动创建以下表：

1. **building_analysis** - 建筑分析信息表
   - 存储图像的 19 维特征向量
   - 包含色彩、纹理、结构特征
   - 自动记录创建和更新时间

2. **building_type** - 建筑类型表
   - 存储预测结果（royal/civilian）
   - 关联 building_analysis 表
   - 记录预测置信度

**手动创建表**（可选）:

如果需要手动创建表，可以运行 Python 脚本：

```bash
cd acasb-analysis
python create_tables.py
```

或使用 SQL 脚本：

```bash
# 使用 obclient 连接 OceanBase
obclient -h192.168.1.199 -P2881 -uroot@test -Dtest

# 执行初始化脚本
source src/main/resources/sql/init.sql
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
