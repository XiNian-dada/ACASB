# ACASB: Ancient Chinese Architecture in SpringBoot

> **“权力的色谱” —— 1911 年前中国官方等级的建筑量化边界研究系统**
> *An Interdisciplinary Digital Humanities Project for Ancient Chinese Architecture.*

---

## 🏛️ 项目愿景 (Vision)

在中国古代，建筑色彩并非单纯的美学选择，而是被法律（如《大清会典》）严格定义的“权力资产”。**ACASB** 旨在通过数字化手段，将晦涩的礼制条文转化为可量化的算法逻辑，揭示砖瓦之间流淌的社会秩序。

---

## 🛠️ 异构多语言系统架构 (Architecture)

本项目采用前后端分离及跨语言协作架构，充分发挥不同技术栈的优势：

* **前台展示 (Frontend)**: **Vue 3 + 待定**
* 实现像素化映射的可视化展示与交互式饼图。


* **业务大脑 (Main Backend)**: **Java 17 (Zulu JDK) + Spring Boot 3.5.x**
* 负责核心业务逻辑、安全校验、事务管理及国产数据库 OceanBase 等对接。


* **分析引擎 (AI & CV Module)**: **Python 3.10+ (FastAPI)**
* 集成 **OpenCV** 处理图像计算、优化等


* **数据持久层 (Database)**: **OceanBase (社区版) / PostgreSQL**
* 支撑“常读不常写、单个数据量大”的高并发读取场景，确保礼制数据的一致性。



---

## ✨ 核心预期功能 (Core Features)

### 1. 像素级色彩提取 (Pixel Mapping & Extraction)

* **智能降噪**：自动识别并剔除天空、树木等环境背景对建筑色彩占比的干扰。
* **多模态适配**：针对历史黑白照片，自动从“色彩分析”切换为“结构特征（开间、屋顶）分析”。
* **量化输出**：计算建筑立面的色彩占比  与色彩熵 。

### 2. 礼制规则引擎 (Regulation Engine)

* **参数化转译**：将《大清会典》中的定性描述（如“凡民间房舍，不许用黄瓦”）转译为数据库中的约束参数。
* **逾制指数计算**：分析色彩占有率相对于职官等级的离散导数 ，自动判定建筑是否存在“僭越”行为。

### 3. “僭越警戒”可视化 (Visualized Compliance)

* 当检测到非法色彩（如民居使用琉璃金 #D9A441）时，触发系统 **Neon Cyan (#00FFFF)** 警戒提示。

---

## 📂 项目目录结构 (Project Structure)

```text
ACASB/
├── acasb-backend/ (Java SpringBoot)
│   ├── src/main/java/com/leeinx/acasb/
│   │   ├── controller/    # RESTful API 接口
│   │   ├── service/       # 礼制校验逻辑
│   │   ├── mapper/        # MyBatis-Plus 数据库映射
│   │   └── entity/        # 规制数据模型
│   └── src/main/resources/application.yml
├── acasb-analysis/ (Python FastAPI)
│   ├── main.py            # 分析引擎接口
│   ├── scripts/           # OpenCV 色彩提取算法
│   └── models/            # 结构识别预训练模型
└── acasb-frontend/ (Vue 3)

```

---

## 🚀 开发者说明 (For Developers)

* **数据库初始化**：使用 MySQL 协议连接 **OceanBase** 实例，执行 `src/main/resources/db/init.sql`。
* **API 文档**：启动项目后访问 `/swagger-ui/index.html` 查看标准接口定义。
* **跨域处理**：后端已集成 `@CrossOrigin` 注解，支持前端本地开发联调。

---

## 📜 学术参考

* 《大清会典》
* 《工部工程做法则例》
