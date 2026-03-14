# ACASB API 文档

Base URL: `http://localhost:8080`

本文档按“前端优先”顺序整理当前项目接口。

说明：

- 前端直接需要的 `/api/dashboard/*`、`/api/experience/*`、`/api/dataset/*` 放在最前面。
- 文档以当前代码实现和实际返回结构为准。
- 部分大屏接口属于“近似实现”，即功能与设计稿对齐，但底层统计口径会明确说明。
- 老接口 `/data/*` 和图像分析接口 `/api/analyze` 仍然保留，放在后半部分。

## 1. 鉴权与通用约定

### 1.1 鉴权

部署包默认会在 Java 启动日志中打印本次实例的 Bearer Token。

如启用了鉴权，请在请求头中附带：

```http
Authorization: Bearer <token>
```

### 1.2 返回格式

当前项目存在两类返回格式：

1. 新接口统一包装：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

2. 老接口直接返回：

```json
{
  "success": true,
  "message": "查询成功",
  "data": []
}
```

### 1.3 朝代参数

大屏接口中的 `dynasty` 当前支持：

- `tang` / `song` / `yuan` / `ming` / `qing`
- `唐` / `宋` / `元` / `明` / `清`

### 1.4 AI 参数

部分接口支持 `enable_ai`：

- `/api/analyze`
- `/data/add`
- `/data/batch`
- `/api/experience/validate`

行为：

- 传 `enable_ai=true` 时，本次请求强制启用云端 AI。
- 不传时按全局配置 `ai.analysis.enabled` 决定。
- AI 失败不会中断本地特征分析和规则校验，接口仍返回基础结果。

## 2. 前端优先：大屏统计接口

这一组接口优先面向首页大屏。

### 2.1 建筑色彩等级数据

**请求地址**: `GET /api/dashboard/color-levels`

**功能描述**：

用于首页左上角“建筑色彩等级”卡片。统计当前数据集中不同社会等级的建筑数量，并返回固定 UI 色值。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `datasetName` | string | 否 | 数据集名称，默认 `competition-dataset` |

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "datasetName": "competition-dataset",
    "colorLevels": [
      { "name": "皇家", "value": 57, "percent": "38.5%", "color": "#f3b746" },
      { "name": "王公", "value": 0, "percent": "0.0%", "color": "#5e9c45" },
      { "name": "官员", "value": 148, "percent": "100.0%", "color": "#3c8dbc" },
      { "name": "富户", "value": 0, "percent": "0.0%", "color": "#666666" },
      { "name": "平民", "value": 107, "percent": "72.3%", "color": "#333333" }
    ]
  }
}
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|---|---|---|
| `colorLevels[].name` | string | 等级名称，固定为 `皇家/王公/官员/富户/平民` |
| `colorLevels[].value` | integer | 当前等级建筑数量 |
| `colorLevels[].percent` | string | 当前值相对于最大等级值的百分比 |
| `colorLevels[].color` | string | 该等级对应的固定 UI 色值 |

**实现说明**：

- 真实统计字段来自 `building_rank`。
- 当前数据集中如果某等级没有样本，返回 `0`，不会缺项。

### 2.2 朝代基础数据面板

**请求地址**: `GET /api/dashboard/dynasty-stats`

**功能描述**：

用于首页左下角“朝代数据”卡片。返回指定朝代下的建筑总数，以及黄色、红色、绿色三类核心色的使用情况。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `dynasty` | string | 是 | 朝代标识符 | `qing` |
| `datasetName` | string | 否 | 数据集名称 | `competition-dataset` |

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dynastyName": "清朝",
    "totalBuildings": 263,
    "yellowCount": 93,
    "redCount": 149,
    "greenCount": 33
  }
}
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|---|---|---|
| `dynastyName` | string | 朝代中文名 |
| `totalBuildings` | integer | 该朝代建筑总数 |
| `yellowCount` | integer | 黄色家族建筑计数 |
| `redCount` | integer | 红色家族建筑计数 |
| `greenCount` | integer | 绿色家族建筑计数 |

**实现说明**：

- 当前按颜色词家族聚合，不是逐像素 HSV 聚类。
- `yellowCount/redCount/greenCount` 的口径是“该图中出现该颜色家族且占比达到一定阈值”。

### 2.3 中国建筑色彩地理分布数据

**请求地址**: `GET /api/dashboard/map-distribution`

**功能描述**：

用于首页中央中国地图。按朝代统计宏观地理区域的建筑数量，并返回区域对应的省份列表。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `dynasty` | string | 是 | 朝代标识符 | `qing` |
| `datasetName` | string | 否 | 数据集名称 | `competition-dataset` |

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "maxCount": 68,
    "regions": [
      { "name": "华东地区", "value": 68, "provinces": ["上海市", "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省"] },
      { "name": "华中地区", "value": 60, "provinces": ["河南省", "湖北省", "湖南省"] }
    ]
  }
}
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|---|---|---|
| `maxCount` | integer | 全区域最大值，前端可用于 `visualMap.max` |
| `regions` | array | 宏观区域统计列表 |
| `regions[].name` | string | 区域名称 |
| `regions[].value` | integer | 区域建筑数量 |
| `regions[].provinces` | array | 该区域映射的省级行政区列表 |

**实现说明**：

- 底层真实数据是省级字段 `province_level_region`。
- 当前接口内部统一映射到 `华北/东北/华东/华中/华南/西南/西北` 七个区域。

### 2.4 朝代核心色彩与文化标签

**请求地址**: `GET /api/dashboard/core-colors`

**功能描述**：

用于首页右侧“核心色彩”区域。返回指定朝代下最显著的 4 种颜色，以及对应文化标签。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `dynasty` | string | 是 | 朝代标识符 | `song` |
| `datasetName` | string | 否 | 数据集名称 | `competition-dataset` |

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dynastyName": "宋朝",
    "colors": [
      { "name": "青", "hex": "#7fb2c5", "count": 2 },
      { "name": "白", "hex": "#f2eada", "count": 2 }
    ],
    "cultureTags": ["素雅含蓄", "青白淡彩", "园林诗意", "文人审美"]
  }
}
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|---|---|---|
| `dynastyName` | string | 朝代名称 |
| `colors` | array | 核心色彩列表，最多 4 个 |
| `colors[].name` | string | 简化后的颜色名称 |
| `colors[].hex` | string | 颜色近似 UI 色值 |
| `colors[].count` | integer | 该颜色出现的图片数 |
| `cultureTags` | array | 朝代审美标签，通常 4 个 |

**实现说明**：

- 颜色来自 `building_color_distribution` 聚合结果。
- `hex` 由颜色词映射为近似展示色，不是图片原始采样值。

### 2.5 朝代色彩使用分析（雷达图）

**请求地址**: `GET /api/dashboard/color-analysis`

**功能描述**：

用于首页右上角雷达图。按固定六个维度返回 0-100 的得分。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `dynasty` | string | 是 | 朝代标识符 | `qing` |
| `datasetName` | string | 否 | 数据集名称 | `competition-dataset` |

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dynastyName": "清朝",
    "indicators": [
      { "name": "黄色使用", "value": 35, "max": 100 },
      { "name": "饱和度", "value": 53, "max": 100 },
      { "name": "明度", "value": 52, "max": 100 },
      { "name": "青色", "value": 14, "max": 100 },
      { "name": "绿色", "value": 8, "max": 100 },
      { "name": "红色", "value": 24, "max": 100 }
    ],
    "note": "饱和度与明度基于入库颜色词的启发式色彩映射估算"
  }
}
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|---|---|---|
| `dynastyName` | string | 当前朝代名称 |
| `indicators` | array | 固定 6 个维度 |
| `indicators[].name` | string | 维度名称 |
| `indicators[].value` | integer | 0-100 分值 |
| `indicators[].max` | integer | 固定为 `100` |

**前端说明**：

- 六个维度顺序固定，前端可直接绑定雷达图。
- `饱和度` 与 `明度` 当前为启发式估算，不是直接读取原图 HSV。

### 2.6 社会等级建筑数量统计

**请求地址**: `GET /api/dashboard/level-stats`

**功能描述**：

用于左侧横向柱状图。返回指定朝代下不同等级建筑数量与相对比例。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `dynasty` | string | 是 | 朝代标识符 | `qing` |
| `datasetName` | string | 否 | 数据集名称 | `competition-dataset` |

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dynastyName": "清朝",
    "levels": [
      { "label": "皇家建筑", "value": 54, "ratio": 0.365 },
      { "label": "王公建筑", "value": 0, "ratio": 0.0 },
      { "label": "官员建筑", "value": 143, "ratio": 1.0 },
      { "label": "富户建筑", "value": 0, "ratio": 0.0 },
      { "label": "平民建筑", "value": 66, "ratio": 0.462 }
    ]
  }
}
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|---|---|---|
| `levels[].label` | string | 等级标签 |
| `levels[].value` | integer | 当前等级建筑数量 |
| `levels[].ratio` | number | 相对于当前朝代最大等级值的比例 |

### 2.7 多朝代建筑等级分布对比

**请求地址**: `GET /api/dashboard/dynasty-comparison`

**功能描述**：

用于底部堆叠柱状图，对比唐宋元明清五个朝代的等级分布。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `datasetName` | string | 否 | 数据集名称 |

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "timeline": ["唐", "宋", "元", "明", "清"],
    "series": [
      { "rankName": "皇家", "data": [1, 0, 0, 2, 54], "color": "#f3b746" },
      { "rankName": "王公", "data": [0, 0, 0, 0, 0], "color": "#5e9c45" }
    ],
    "description": "基于当前入库数据，对唐、宋、元、明、清五个朝代的建筑等级分布进行对比。"
  }
}
```

**实现说明**：

- 时间轴固定为 `唐/宋/元/明/清`。
- 当前数据缺失的等级会返回 `0`，不会缺项。

### 2.8 历代色彩趋势变化与核心色值

**请求地址**: `GET /api/dashboard/history-trend`

**功能描述**：

用于底部趋势图。返回四类代表性建筑材料/色彩在不同朝代锚点上的趋势。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `datasetName` | string | 否 | 数据集名称 |

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "years": [618, 960, 1271, 1368, 1644],
    "series": [
      { "materialName": "黄色琉璃瓦", "data": [0, 0, 0, 6, 349], "color": "#f3b746" },
      { "materialName": "红色墙体", "data": [6, 36, 0, 92, 644], "color": "#e65d25" }
    ],
    "cultureTags": ["礼制色彩", "地域风格", "材质层次", "时代演变"],
    "description": "使用唐、宋、元、明、清五个朝代锚点，按颜色材质代理词统计历史色彩趋势。"
  }
}
```

**实现说明**：

- 当前不是连续年份统计，而是使用五个朝代锚点近似表达趋势。
- 适合前端面积图展示，但不应当解释为逐年史料曲线。

### 2.9 主要区域建筑等级分布（混合图表）

**请求地址**: `GET /api/dashboard/region-rank-dist`

**功能描述**：

用于底部混合图。比较当前样本最多的 5 个省级区域的等级分布。

**请求参数**：

无。

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "provinces": ["江苏省", "福建省", "湖北省", "云南省", "湖南省"],
    "series": [
      { "name": "皇家", "type": "bar", "data": [4, 1, 1, 8, 4], "color": "#f3b746" },
      { "name": "官员", "type": "bar", "data": [31, 20, 14, 12, 11], "color": "#3c8dbc" },
      { "name": "平民", "type": "line", "data": [24, 16, 21, 11, 9], "color": "#666666" },
      { "name": "总量", "type": "line", "data": [59, 37, 36, 31, 24], "color": "#111111", "lineStyle": "dashed" }
    ],
    "description": "基于当前数据集中样本数最多的 5 个省级区域，展示主要建筑等级分布与总量趋势。"
  }
}
```

**实现说明**：

- 当前不是固定返回“北京/陕西/江苏/浙江/山西”。
- 实际返回的是当前数据集中样本最多的前 5 个省级区域。

### 2.10 建筑材料成本与工艺分析

**请求地址**: `GET /api/dashboard/material-analysis`

**功能描述**：

用于“材料成本”分组柱状图。返回材料在工艺难度、成本指数、稀有度三个维度上的指数化结果。

**请求参数**：

无。

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dimensions": ["工艺难度", "成本指数", "稀有度"],
    "materials": [
      { "name": "黄色琉璃瓦", "values": [95, 98, 92], "colors": ["#f3b746", "#e65d25", "#3c8dbc"] },
      { "name": "青色瓦片", "values": [45, 45, 30], "colors": ["#f3b746", "#e65d25", "#3c8dbc"] }
    ],
    "description": "材料成本分析当前为规则化启发式指数，用于大屏材料成本对比展示。"
  }
}
```

**实现说明**：

- 该接口当前是规则化启发式结果。
- 适合可视化展示，不直接对应数据库原始字段。

## 3. 前端优先：互动体验接口

### 3.1 社会等级配色方案查询

**请求地址**: `GET /api/experience/rank-rules`

**功能描述**：

用于体验页初始化，根据身份等级返回可用色彩池。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `rankId` | string | 是 | 身份标识符 | `civilian` |

**支持值**：

- `emperor`
- `noble`
- `official`
- `wealthy`
- `civilian`

也支持中文别名：

- `皇家`
- `王公`
- `官员`
- `富户`
- `平民`

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "rankId": "civilian",
    "rankName": "平民",
    "maxColors": 3,
    "availableColors": [
      { "name": "灰色", "hex": "#888888", "description": "布瓦本色" },
      { "name": "青色", "hex": "#2c5a7d", "description": "常用建筑用色" },
      { "name": "黑色", "hex": "#333333", "description": "普通木材本色" }
    ],
    "note": "当前规则为礼制体验启发式色彩池，适合前端互动页初始化。"
  }
}
```

### 3.2 配色合法性校验

**请求地址**: `POST /api/experience/validate`

**功能描述**：

提交用户配色方案，返回是否越制、风险等级、反馈文案和文化知识点。

**请求参数**：

- Query: `enable_ai` 可选，开启后会调用云端 AI 对反馈文案做润色。

**请求体**：

```json
{
  "rankId": "civilian",
  "selections": [
    { "part": "屋顶", "colorHex": "#ffbb00" },
    { "part": "墙体", "colorHex": "#888888" }
  ]
}
```

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "isValid": false,
    "resultTitle": "僭越警告！",
    "resultLevel": "danger",
    "feedback": "作为平民，您的屋顶使用了黄色，该颜色更接近皇家礼制配色。",
    "knowledgePoint": "历史知识：黄色在明清礼制中高度象征皇权。",
    "rankName": "平民",
    "selectionCount": 2,
    "violations": [
      { "part": "屋顶", "colorHex": "#ffbb00", "reservedFor": "皇家", "colorName": "黄色" }
    ]
  }
}
```

**实现说明**：

- 核心合法性判断当前为本地规则引擎。
- `enable_ai=true` 时，云端 AI 只负责润色 `feedback` 与 `knowledgePoint`，不会推翻基础判定。

## 4. 前端优先：高质量数据集查询接口

这一组接口用于前端列表页、详情页、后台管理页、大屏数据源。

### 4.1 查询数据集记录列表

**请求地址**: `GET /api/dataset/records`

**功能描述**：

分页查询高质量标注记录。

**常用参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `datasetName` | string | 否 | 数据集名称 |
| `groupName` | string | 否 | 分组名称，如 `华东地区` |
| `dynasty` | string | 否 | 朝代 |
| `province` | string | 否 | 省级区域 |
| `rank` | string | 否 | 建筑等级 |
| `sceneType` | string | 否 | 场景类型 |
| `manualReview` | boolean | 否 | 是否需人工复核 |
| `analysisStatus` | string | 否 | 分析状态 |
| `keyword` | string | 否 | 关键字搜索 |
| `limit` | integer | 否 | 每页数量，默认 20，最大 200 |
| `offset` | integer | 否 | 偏移量 |

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 312,
    "limit": 20,
    "offset": 0,
    "note": "relativePath/groupRelativePath 为 ASCII 规范路径，originalRelativePath/originalGroupRelativePath 保留原始中文路径",
    "items": []
  }
}
```

**说明**：

- `relativePath` / `groupRelativePath` 为 ASCII 规范路径。
- `originalRelativePath` / `originalGroupRelativePath` 保留中文原始路径。

### 4.2 查询单条数据集记录

**请求地址**: `GET /api/dataset/records/{id}`

按数据库主键查询一条记录。

### 4.3 按分组序号查询

**请求地址**: `GET /api/dataset/record-by-index`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `datasetName` | string | 是 | 数据集名称 |
| `groupName` | string | 是 | 分组名称 |
| `imageIndex` | integer | 是 | 分组内序号 |

**说明**：

- `imageIndex` 是分组内排序序号。
- 它不一定等于文件名数字，例如 `47.jpg` 可能对应 `imageIndex=54`。

### 4.4 按分组和文件名查询

**请求地址**: `GET /api/dataset/record-by-file`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `datasetName` | string | 是 | 数据集名称 |
| `groupName` | string | 是 | 分组名称 |
| `fileName` | string | 是 | 文件名，例如 `47.jpg` |

### 4.5 数据集统计概览

**请求地址**: `GET /api/dataset/stats/overview`

返回：

- 总记录数
- 成功记录数
- 人工复核数
- 朝代分布
- 等级分布
- 场景类型分布
- 省级区域分布
- 分组分布

### 4.6 区域统计

**请求地址**: `GET /api/dataset/stats/regions`

返回省级区域统计列表。

### 4.7 色彩统计

**请求地址**: `GET /api/dataset/stats/colors`

返回颜色词聚合结果，可用于大屏颜色排行。

### 4.8 等级统计

**请求地址**: `GET /api/dataset/stats/ranks`

返回建筑等级聚合结果。

### 4.9 风格统计

**请求地址**: `GET /api/dataset/stats/styles`

返回风格词聚合结果。

## 5. 图像分析与基础服务接口

### 5.1 Java 健康检查

**请求地址**: `GET /api/health`

返回示例：

```text
Java Backend is running!
```

### 5.2 Java 图像接口测试

**请求地址**: `GET /api/test`

返回示例：

```text
Image API is working!
```

### 5.3 Python 健康透传

**请求地址**: `GET /testPython`

返回 Python 服务 `/health` 的结果。

### 5.4 建筑二分类预测

**请求地址**: `POST /api/predict`

**功能描述**：

使用本地 MLP 模型做 `royal/civilian` 二分类预测。

**请求体**：

```json
{
  "image_path": "./uploads/demo.jpg"
}
```

**当前默认行为**：

- `local.model.prediction-enabled=false`
- 默认返回“本地训练模型预测已禁用”

### 5.5 图像 19 维特征分析与 AI 解析

**请求地址**: `POST /api/analyze`

**功能描述**：

返回本地 19 维图像特征，并可附加云端 AI 结构化建筑解析。

**支持两种请求方式**：

1. `multipart/form-data`
2. `application/json`

**JSON 请求体**：

```json
{
  "image_path": "./uploads/demo.jpg",
  "enable_ai": true
}
```

也兼容：

```json
{
  "imagePath": "./uploads/demo.jpg",
  "enableAi": true
}
```

**返回关键字段**：

| 字段名 | 类型 | 说明 |
|---|---|---|
| `ratio_yellow` ~ `royal_ratio` | number | 本地 19 维特征 |
| `ai_analysis` | object | 云端 AI 结构化解析 |
| `ai_analyze` | string | 云端 AI 原始输出文本 |

**说明**：

- 即使 AI 失败，本地特征也会返回。
- `ai_analysis.analysis_status` 可用于前端判断 AI 是否成功。

## 6. 旧版入库查询接口

### 6.1 查询分析详情

**请求地址**: `GET /data/analysis/{id}`

按 `building_analysis.id` 查询单条分析记录。

### 6.2 查询预测详情

**请求地址**: `GET /data/type/{id}`

按 `building_type.id` 查询单条预测记录。

### 6.3 分析记录列表

**请求地址**: `GET /data/list`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `field` | string | 否 | 排序字段 |
| `order` | string | 否 | `asc` / `desc` |
| `limit` | integer | 否 | 数量上限 |
| `prediction` | string | 否 | 预测值过滤 |

**说明**：

- 当 `field` 为空时，当前默认按 `royal_ratio desc` 返回。
- 如果库里没有通过 `/data/add` 或 `/data/batch` 入库过记录，列表会返回空数组。

## 7. 上传与导入接口

这一组接口保留在文档末尾，供后台或运维使用。

### 7.1 单张图片入库

**请求地址**: `POST /data/add`

上传单张图片，完成：

- 19 维特征分析
- 可选 AI 解析
- 持久化图片
- 写入 `building_analysis`
- 若本地预测开启，再写入 `building_type`

### 7.2 批量图片入库

**请求地址**: `POST /data/batch`

批量处理多张图片并返回逐项结果。

### 7.3 导入服务端目录

**请求地址**: `POST /api/dataset/import-folder`

从服务端本机目录导入高质量数据集。

### 7.4 导入指定 manifest

**请求地址**: `POST /api/dataset/import-manifest`

导入一个已有 `analysis.json` 文件。

### 7.5 上传 manifest 与图片

**请求地址**: `POST /api/dataset/upload-manifest`

上传一个区域目录的 `analysis.json` 和对应图片。

### 7.6 上传单条高质量记录

**请求地址**: `POST /api/dataset/upload-record`

上传单张图片和单条高质量 JSON 元数据。

## 8. 前端对接建议

如果你当前主要在做首页大屏和互动页，推荐只优先接以下接口：

- 左上等级卡片：`GET /api/dashboard/color-levels`
- 左下朝代面板：`GET /api/dashboard/dynasty-stats`
- 中央地图：`GET /api/dashboard/map-distribution`
- 右侧核心色彩：`GET /api/dashboard/core-colors`
- 右上雷达图：`GET /api/dashboard/color-analysis`
- 左侧等级柱状图：`GET /api/dashboard/level-stats`
- 底部朝代对比：`GET /api/dashboard/dynasty-comparison`
- 底部趋势图：`GET /api/dashboard/history-trend`
- 底部区域混合图：`GET /api/dashboard/region-rank-dist`
- 材料成本图：`GET /api/dashboard/material-analysis`
- 身份初始化：`GET /api/experience/rank-rules`
- 提交校验：`POST /api/experience/validate`
- 列表页数据源：`GET /api/dataset/records`
- 详情页精确查询：`GET /api/dataset/record-by-index`
- 文件名精确查询：`GET /api/dataset/record-by-file`
