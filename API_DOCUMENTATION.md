# ACASB API 文档

Base URL: `http://localhost:8080`

本文档按“前端优先”顺序编写，先列首页大屏和互动体验接口，再列数据集查询、图像分析、旧版查询和导入接口。

说明：

- 文档以当前代码实现和实际测试结果为准。
- 前端优先接口全部放在最前面。
- 每个接口都提供完整响应 JSON 示例，不只列字段名。
- 部分大屏接口是“近似实现”，即功能满足前端展示，但底层统计口径是根据现有数据结构推导出来的，文中会明确标注。

---

## 0. 通用说明

### 0.1 鉴权

部署包启动 Java 服务后，控制台会打印当前实例的 Bearer Token。

当前默认开启 JWT 鉴权，拦截范围：

- `/api/**`
- `/data/**`

可配置项：

```properties
auth.jwt.enabled=true
auth.jwt.secret=请配置稳定长密钥
auth.jwt.expires-hours=720
```

请求头格式：

```http
Authorization: Bearer <token>
```

### 0.2 返回格式

当前项目存在两类返回格式。

新接口统一返回：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

旧接口保留历史返回格式：

```json
{
  "success": true,
  "message": "查询成功",
  "data": {}
}
```

### 0.3 朝代参数

`/api/dashboard/*` 里需要传 `dynasty` 的接口，当前支持：

- `tang`
- `song`
- `yuan`
- `ming`
- `qing`
- `唐`
- `宋`
- `元`
- `明`
- `清`

### 0.4 AI 开关

以下接口支持 `enable_ai`：

- `POST /api/analyze`
- `POST /data/add`
- `POST /data/batch`
- `POST /api/experience/validate`

行为：

- 传 `enable_ai=true` 时，本次请求强制启用云端 AI。
- 不传时按 `ai.analysis.enabled` 的全局配置决定。
- AI 失败不会让本地特征分析失败，接口仍会返回基础结果。

---

## 1. 大屏统计：建筑色彩等级数据

**功能描述**：
用于首页大屏左上角“建筑色彩等级”卡片。统计当前数据集中，各个社会等级对应的建筑数量，并返回前端可直接使用的颜色与比例。

- **请求地址**: `GET /api/dashboard/color-levels`
- **请求方式**: `GET`
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `datasetName` | `String` | 否 | 数据集名称，默认 `competition-dataset` | `competition-dataset` |

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "datasetName": "competition-dataset",
    "colorLevels": [
      {
        "name": "皇家",
        "value": 57,
        "percent": "38.5%",
        "color": "#f3b746"
      },
      {
        "name": "王公",
        "value": 0,
        "percent": "0.0%",
        "color": "#5e9c45"
      },
      {
        "name": "官员",
        "value": 148,
        "percent": "100.0%",
        "color": "#3c8dbc"
      },
      {
        "name": "富户",
        "value": 0,
        "percent": "0.0%",
        "color": "#666666"
      },
      {
        "name": "平民",
        "value": 107,
        "percent": "72.3%",
        "color": "#333333"
      }
    ]
  }
}
```

### 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `data.datasetName` | `String` | 当前统计所使用的数据集名称 |
| `data.colorLevels` | `Array` | 等级统计列表 |
| `data.colorLevels[].name` | `String` | 等级名称，固定为 `皇家/王公/官员/富户/平民` |
| `data.colorLevels[].value` | `Integer` | 当前等级建筑数量 |
| `data.colorLevels[].percent` | `String` | 相对于最大等级值的百分比 |
| `data.colorLevels[].color` | `String` | 该等级的固定 UI 色值 |

### 实现说明

- 真实统计字段来自 `building_rank`。
- 当前数据中若 `王公` 或 `富户` 无样本，仍会返回，只是数量为 `0`。
- `percent` 当前不是全量占比，而是 `当前等级数量 / 最大等级数量`。

---

## 2. 大屏统计：朝代基础数据面板

**功能描述**：
用于首页大屏左下角“朝代数据”卡片。根据用户切换的朝代，返回该朝代的建筑总数以及黄色、红色、绿色三类核心色的使用情况。

- **请求地址**: `GET /api/dashboard/dynasty-stats`
- **请求方式**: `GET`
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `dynasty` | `String` | 是 | 朝代标识符 | `qing` |
| `datasetName` | `String` | 否 | 数据集名称 | `competition-dataset` |

### 完整返回示例

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

### 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `data.dynastyName` | `String` | 朝代中文名 |
| `data.totalBuildings` | `Integer` | 当前朝代建筑总数 |
| `data.yellowCount` | `Integer` | 黄色家族计数 |
| `data.redCount` | `Integer` | 红色家族计数 |
| `data.greenCount` | `Integer` | 绿色家族计数 |

### 实现说明

- 当前按颜色词家族统计，不是逐像素聚类。
- 这一口径适合大屏概览，不建议解释为严格材料计数。

---

## 3. 大屏统计：中国建筑色彩地理分布数据

**功能描述**：
用于首页大屏中央中国地图。根据当前朝代，统计各大宏观地理区域的建筑总数，并返回该区域对应的省份列表。

- **请求地址**: `GET /api/dashboard/map-distribution`
- **请求方式**: `GET`
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `dynasty` | `String` | 是 | 朝代标识符 | `qing` |
| `datasetName` | `String` | 否 | 数据集名称 | `competition-dataset` |

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "maxCount": 95,
    "regions": [
      {
        "name": "华东地区",
        "value": 95,
        "provinces": ["上海市", "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省"]
      },
      {
        "name": "华中地区",
        "value": 82,
        "provinces": ["河南省", "湖北省", "湖南省"]
      },
      {
        "name": "西南地区",
        "value": 50,
        "provinces": ["重庆市", "四川省", "贵州省", "云南省", "西藏自治区"]
      },
      {
        "name": "华北地区",
        "value": 15,
        "provinces": ["北京市", "天津市", "河北省", "山西省", "内蒙古自治区"]
      },
      {
        "name": "东北地区",
        "value": 12,
        "provinces": ["辽宁省", "吉林省", "黑龙江省"]
      },
      {
        "name": "华南地区",
        "value": 10,
        "provinces": ["广东省", "广西壮族自治区", "海南省"]
      },
      {
        "name": "西北地区",
        "value": 0,
        "provinces": ["陕西省", "甘肃省", "青海省", "宁夏回族自治区", "新疆维吾尔自治区"]
      }
    ]
  }
}
```

### 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `data.maxCount` | `Integer` | 当前所有区域中的最大建筑数 |
| `data.regions` | `Array` | 区域数据列表 |
| `data.regions[].name` | `String` | 区域名称 |
| `data.regions[].value` | `Integer` | 该区域建筑总数 |
| `data.regions[].provinces` | `Array` | 区域包含的省份名称列表 |

### 实现说明

- 真实数据存的是省级区域。
- 当前接口内部将省级区域归并为 `华北/东北/华东/华中/华南/西南/西北` 七个区域。

---

## 4. 大屏统计：朝代核心色彩与文化标签

**功能描述**：
用于首页右侧“核心色彩”区域。根据朝代返回最显著的颜色及文化标签。

- **请求地址**: `GET /api/dashboard/core-colors`
- **请求方式**: `GET`
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `dynasty` | `String` | 是 | 朝代标识符 | `song` |
| `datasetName` | `String` | 否 | 数据集名称 | `competition-dataset` |

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dynastyName": "宋朝",
    "colors": [
      { "name": "青", "hex": "#7fb2c5", "count": 2 },
      { "name": "白", "hex": "#f2eada", "count": 2 },
      { "name": "灰", "hex": "#888888", "count": 1 },
      { "name": "褐", "hex": "#704d30", "count": 1 }
    ],
    "cultureTags": ["素雅含蓄", "青白淡彩", "园林诗意", "文人审美"]
  }
}
```

### 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `data.dynastyName` | `String` | 朝代名称 |
| `data.colors` | `Array` | 核心色彩列表，最多 4 个 |
| `data.colors[].name` | `String` | 简化后的颜色名 |
| `data.colors[].hex` | `String` | 颜色对应的近似 UI 色值 |
| `data.colors[].count` | `Integer` | 该颜色出现的图片数 |
| `data.cultureTags` | `Array` | 文化标签列表 |

### 实现说明

- `colors` 来自 `building_color_distribution` 聚合。
- `cultureTags` 优先使用朝代固定标签，缺失时退化为风格词。

---

## 5. 大屏统计：朝代色彩使用分析（雷达图）

**功能描述**：
用于首页右上角“色彩使用分析”雷达图。返回固定六个维度的标准化值。

- **请求地址**: `GET /api/dashboard/color-analysis`
- **请求方式**: `GET`
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `dynasty` | `String` | 是 | 朝代标识符 | `qing` |
| `datasetName` | `String` | 否 | 数据集名称 | `competition-dataset` |

### 完整返回示例

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

### 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `data.dynastyName` | `String` | 当前分析朝代 |
| `data.indicators` | `Array` | 固定 6 个维度 |
| `data.indicators[].name` | `String` | 维度名称 |
| `data.indicators[].value` | `Integer` | 0-100 的得分 |
| `data.indicators[].max` | `Integer` | 固定为 100 |
| `data.note` | `String` | 当前统计口径说明 |

### 前端说明

- 六个维度顺序固定，可直接绑定雷达图。
- `饱和度/明度` 是近似值，不是图片逐像素 HSV 的真实统计。

---

## 6. 大屏统计：社会等级建筑数量统计

**功能描述**：
用于首页左侧“等级数据统计”横向柱状图。展示某个朝代下的等级分布。

- **请求地址**: `GET /api/dashboard/level-stats`
- **请求方式**: `GET`
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `dynasty` | `String` | 是 | 朝代标识符 | `qing` |
| `datasetName` | `String` | 否 | 数据集名称 | `competition-dataset` |

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dynastyName": "清朝",
    "levels": [
      { "label": "皇家建筑", "value": 54, "ratio": 0.3776 },
      { "label": "王公建筑", "value": 0, "ratio": 0.0 },
      { "label": "官员建筑", "value": 143, "ratio": 1.0 },
      { "label": "富户建筑", "value": 0, "ratio": 0.0 },
      { "label": "平民建筑", "value": 66, "ratio": 0.4615 }
    ]
  }
}
```

### 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `data.dynastyName` | `String` | 当前朝代名称 |
| `data.levels` | `Array` | 等级列表 |
| `data.levels[].label` | `String` | 等级标签 |
| `data.levels[].value` | `Integer` | 等级建筑数 |
| `data.levels[].ratio` | `Float` | 相对于当前朝代最大等级值的比例 |

---

## 7. 大屏统计：多朝代建筑等级分布对比（堆叠柱状图）

**功能描述**：
用于底部“各朝代建筑等级分布对比”图表。

- **请求地址**: `GET /api/dashboard/dynasty-comparison`
- **请求方式**: `GET`
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `datasetName` | `String` | 否 | 数据集名称 |

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "timeline": ["唐", "宋", "元", "明", "清"],
    "series": [
      {
        "rankName": "皇家",
        "data": [1, 0, 0, 2, 54],
        "color": "#f3b746"
      },
      {
        "rankName": "王公",
        "data": [0, 0, 0, 0, 0],
        "color": "#5e9c45"
      },
      {
        "rankName": "官员",
        "data": [0, 1, 1, 3, 143],
        "color": "#3c8dbc"
      },
      {
        "rankName": "富户",
        "data": [0, 0, 0, 0, 0],
        "color": "#666666"
      },
      {
        "rankName": "平民",
        "data": [0, 1, 0, 40, 66],
        "color": "#333333"
      }
    ],
    "description": "基于当前入库数据，对唐、宋、元、明、清五个朝代的建筑等级分布进行对比。王公、富户若无样本则返回 0。"
  }
}
```

### 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `data.timeline` | `Array` | 朝代时间轴 |
| `data.series` | `Array` | 系列数组 |
| `data.series[].rankName` | `String` | 等级名称 |
| `data.series[].data` | `Array` | 与时间轴一一对应的数值数组 |
| `data.series[].color` | `String` | 固定 UI 色值 |
| `data.description` | `String` | 图表说明 |

---

## 8. 大屏统计：历代色彩趋势变化与核心色值

**功能描述**：
用于底部“历史趋势”面积图。

- **请求地址**: `GET /api/dashboard/history-trend`
- **请求方式**: `GET`
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `datasetName` | `String` | 否 | 数据集名称 |

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "years": [618, 960, 1271, 1368, 1644],
    "series": [
      {
        "materialName": "黄色琉璃瓦",
        "data": [0, 0, 0, 6, 349],
        "color": "#f3b746"
      },
      {
        "materialName": "红色墙体",
        "data": [6, 36, 0, 92, 644],
        "color": "#e65d25"
      },
      {
        "materialName": "绿色琉璃瓦",
        "data": [0, 0, 0, 8, 142],
        "color": "#5e9c45"
      },
      {
        "materialName": "青色瓦片",
        "data": [0, 19, 0, 30, 251],
        "color": "#5a7fb4"
      }
    ],
    "cultureTags": ["礼制色彩", "地域风格", "材质层次", "时代演变"],
    "description": "使用唐、宋、元、明、清五个朝代锚点，按颜色材质代理词统计历史色彩趋势。"
  }
}
```

### 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `data.years` | `Array` | 朝代锚点年份 |
| `data.series` | `Array` | 趋势序列 |
| `data.series[].materialName` | `String` | 材料/色彩名称 |
| `data.series[].data` | `Array` | 趋势值数组 |
| `data.series[].color` | `String` | 图表颜色 |
| `data.cultureTags` | `Array` | 胶囊标签 |
| `data.description` | `String` | 底部说明 |

### 实现说明

- 当前不是逐年连续统计。
- 这是按朝代锚点做的近似趋势表达。

---

## 9. 大屏统计：主要区域建筑等级分布（混合图表）

**功能描述**：
用于底部“主要区域建筑等级分布”混合图。

- **请求地址**: `GET /api/dashboard/region-rank-dist`
- **请求方式**: `GET`
- **请求参数**: 无

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "provinces": ["江苏省", "福建省", "湖北省", "云南省", "湖南省"],
    "series": [
      {
        "name": "皇家",
        "type": "bar",
        "data": [4, 1, 1, 8, 4],
        "color": "#f3b746"
      },
      {
        "name": "官员",
        "type": "bar",
        "data": [31, 20, 14, 12, 11],
        "color": "#3c8dbc"
      },
      {
        "name": "平民",
        "type": "line",
        "data": [24, 16, 21, 11, 9],
        "color": "#666666"
      },
      {
        "name": "总量",
        "type": "line",
        "data": [59, 37, 36, 31, 24],
        "color": "#111111",
        "lineStyle": "dashed"
      }
    ],
    "description": "基于当前数据集中样本数最多的 5 个省级区域，展示主要建筑等级分布与总量趋势。"
  }
}
```

### 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `data.provinces` | `Array` | 当前选出的 5 个重点省级区域 |
| `data.series` | `Array` | 混合图序列列表 |
| `data.series[].name` | `String` | 系列名称 |
| `data.series[].type` | `String` | `bar` 或 `line` |
| `data.series[].data` | `Array` | 数值数组 |
| `data.series[].color` | `String` | 系列颜色 |
| `data.series[].lineStyle` | `String` | 仅折线可能出现，当前可能为 `dashed` |
| `data.description` | `String` | 图表说明 |

### 实现说明

- 当前不是固定的“北京/陕西/江苏/浙江/山西”。
- 实际返回的是当前样本最多的前 5 个省级区域。

---

## 10. 大屏统计：建筑材料成本与工艺分析（分组柱状图）

**功能描述**：
用于底部“材料成本”图表，返回规则化启发式指数。

- **请求地址**: `GET /api/dashboard/material-analysis`
- **请求方式**: `GET`
- **请求参数**: 无

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "dimensions": ["工艺难度", "成本指数", "稀有度"],
    "materials": [
      {
        "name": "黄色琉璃瓦",
        "values": [95, 98, 92],
        "colors": ["#f3b746", "#e65d25", "#3c8dbc"]
      },
      {
        "name": "绿色琉璃瓦",
        "values": [78, 65, 55],
        "colors": ["#f3b746", "#e65d25", "#3c8dbc"]
      },
      {
        "name": "朱红漆料",
        "values": [88, 85, 82],
        "colors": ["#f3b746", "#e65d25", "#3c8dbc"]
      },
      {
        "name": "青色瓦片",
        "values": [45, 45, 30],
        "colors": ["#f3b746", "#e65d25", "#3c8dbc"]
      },
      {
        "name": "灰黑瓦片",
        "values": [25, 15, 10],
        "colors": ["#f3b746", "#e65d25", "#3c8dbc"]
      }
    ],
    "description": "材料成本分析当前为规则化启发式指数，用于大屏材料成本对比展示。"
  }
}
```

### 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `data.dimensions` | `Array` | 三个指标维度 |
| `data.materials` | `Array` | 材料列表 |
| `data.materials[].name` | `String` | 材料名称 |
| `data.materials[].values` | `Array` | 对应三个维度的数值 |
| `data.materials[].colors` | `Array` | 每个维度的配色 |
| `data.description` | `String` | 图表说明 |

---

## 11. 互动体验：社会等级配色方案查询

**功能描述**：
用于“用户体验”涂色页面初始化，根据身份返回可用色彩池。

- **请求地址**: `GET /api/experience/rank-rules`
- **请求方式**: `GET`
- **请求参数**:

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `rankId` | `String` | 是 | 身份标识符 | `civilian` |

支持值：

- `emperor`
- `noble`
- `official`
- `wealthy`
- `civilian`
- 也兼容中文值 `皇家/王公/官员/富户/平民`

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "rankId": "civilian",
    "rankName": "平民",
    "maxColors": 3,
    "availableColors": [
      {
        "name": "灰色",
        "hex": "#888888",
        "description": "布瓦本色"
      },
      {
        "name": "青色",
        "hex": "#2c5a7d",
        "description": "常用建筑用色"
      },
      {
        "name": "黑色",
        "hex": "#333333",
        "description": "普通木材本色"
      }
    ],
    "note": "当前规则为礼制体验启发式色彩池，适合前端互动页初始化。"
  }
}
```

### 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `data.rankId` | `String` | 标准化后的身份标识 |
| `data.rankName` | `String` | 身份中文名 |
| `data.maxColors` | `Integer` | 推荐最大用色数量 |
| `data.availableColors` | `Array` | 可用颜色池 |
| `data.availableColors[].name` | `String` | 颜色名称 |
| `data.availableColors[].hex` | `String` | 颜色值 |
| `data.availableColors[].description` | `String` | 颜色说明 |
| `data.note` | `String` | 规则说明 |

---

## 12. 互动体验：配色合法性校验（核心逻辑）

**功能描述**：
用于体验页“提交验证”。判断用户配色是否越制，并返回反馈文案与知识点。

- **请求地址**: `POST /api/experience/validate`
- **请求方式**: `POST`
- **请求参数**:

Query 参数：

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| `enable_ai` | `Boolean` | 否 | 是否启用云端 AI 润色反馈文案 | `true` |

JSON Body：

```json
{
  "rankId": "civilian",
  "selections": [
    { "part": "屋顶", "colorHex": "#ffbb00" },
    { "part": "墙体", "colorHex": "#888888" }
  ]
}
```

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "isValid": false,
    "resultTitle": "僭越警告！",
    "resultLevel": "danger",
    "feedback": "作为平民，您的屋顶使用了黄色，该颜色更接近皇家礼制配色。建议优先回到灰色、青色、黑色这类低阶或常用色系。",
    "knowledgePoint": "历史知识：黄色在明清礼制中高度象征皇权，常与皇家屋顶和核心礼制空间绑定。",
    "rankName": "平民",
    "selectionCount": 2,
    "violations": [
      {
        "part": "屋顶",
        "colorHex": "#ffbb00",
        "reservedFor": "皇家",
        "colorName": "黄色"
      }
    ]
  }
}
```

### 字段说明

| 字段名 | 类型 | 说明 |
|---|---|---|
| `data.isValid` | `Boolean` | 是否合规 |
| `data.resultTitle` | `String` | 前端可直接展示的结果标题 |
| `data.resultLevel` | `String` | `success/warning/danger` |
| `data.feedback` | `String` | 反馈文案 |
| `data.knowledgePoint` | `String` | 历史知识点 |
| `data.rankName` | `String` | 当前身份中文名 |
| `data.selectionCount` | `Integer` | 本次提交的颜色数量 |
| `data.violations` | `Array` | 违规明细 |

### 实现说明

- 核心合法性判断由本地规则完成。
- `enable_ai=true` 时，云端 AI 仅润色 `feedback` 和 `knowledgePoint`。

---

## 13. 高质量数据集查询接口

这一组接口用于列表页、详情页、后台管理和大屏数据源。

### 13.1 查询数据集记录列表

**请求地址**: `GET /api/dataset/records`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `datasetName` | `String` | 否 | 数据集名称 |
| `groupName` | `String` | 否 | 分组名称 |
| `dynasty` | `String` | 否 | 朝代 |
| `province` | `String` | 否 | 省级区域 |
| `rank` | `String` | 否 | 建筑等级 |
| `sceneType` | `String` | 否 | 场景类型 |
| `manualReview` | `Boolean` | 否 | 是否人工复核 |
| `analysisStatus` | `String` | 否 | 分析状态 |
| `keyword` | `String` | 否 | 关键字搜索 |
| `limit` | `Integer` | 否 | 默认 20，最大 200 |
| `offset` | `Integer` | 否 | 偏移量 |

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 312,
    "limit": 2,
    "offset": 0,
    "note": "relativePath/groupRelativePath 为 ASCII 规范路径，originalRelativePath/originalGroupRelativePath 保留原始中文路径",
    "items": [
      {
        "id": 1,
        "datasetName": "competition-dataset",
        "groupName": "东北地区",
        "groupRelativePath": "northeast",
        "originalGroupRelativePath": "东北地区",
        "schemaVersion": "image_region_analysis_v2",
        "promptVersion": "v4",
        "generatedAt": "2026-03-14T06:20:21.863565+00:00",
        "apiInterface": "responses",
        "model": "gpt-5.2",
        "provinceLevelRegion": "辽宁省",
        "provinceConfidence": 0.48,
        "dynastyGuess": "清",
        "buildingRank": "官员",
        "sceneType": "建筑外观",
        "buildingPresent": true,
        "buildingPrimaryColors": ["米白色", "黄色", "灰色"],
        "buildingColorDistribution": [
          { "color": "米白色", "ratio": 0.55, "hex": "#efe5cf" },
          { "color": "黄色", "ratio": 0.25, "hex": "#f3b746" },
          { "color": "灰色", "ratio": 0.2, "hex": "#888888" }
        ],
        "architectureStyle": ["近代中西合璧官式建筑", "民国公共建筑"],
        "sceneDescription": "画面主体为建筑外观中的近代以前传统建筑，建筑主体色彩分布为米白色约占55%，黄色约占25%，灰色约占20%，屋顶、立柱、墙面与台基层次清楚，整体呈现近代中西合璧官式建筑、民国公共建筑特征，材质以木构、灰瓦与石质构件组合为主，朝代判断归入清，建筑等级归入官员，整体审美与辽宁省传统建筑风格相符。",
        "reasoning": [
          "建筑主体颜色分布清楚，米白色约占55%，黄色约占25%，灰色约占20%，色彩主要集中在屋顶、立柱、墙面与台基等核心构件。",
          "屋顶形制、檐口层次、门窗比例与装饰构件明确，整体属于近代以前的清近代中西合璧官式建筑、民国公共建筑体系。",
          "主体构图、构件比例与色彩组织共同指向辽宁省范围内的官员等级传统建筑风格表达。"
        ],
        "needsManualReview": true,
        "fileName": "1.jpg",
        "relativePath": "northeast/1.jpg",
        "originalRelativePath": "东北地区/1.jpg",
        "storedImagePath": "/Users/bernard/Downloads/ACASB_Package_20260314_065734/dataset-storage/competition-dataset/东北地区/1.jpg",
        "sourceImagePath": "/Users/bernard/Code/ACASB/计算机设计大赛/东北地区/1.jpg",
        "analysisStatus": "success",
        "errorMessage": "",
        "imageIndex": 1,
        "rawMetadata": {
          "province_level_region": "辽宁省",
          "province_confidence": 0.48,
          "dynasty_guess": "清",
          "building_rank": "官员",
          "scene_type": "建筑外观",
          "building_present": true,
          "building_primary_colors": ["米白色", "黄色", "灰色"],
          "building_color_distribution": [
            { "color": "米白色", "ratio": 0.55, "hex": "#efe5cf" },
            { "color": "黄色", "ratio": 0.25, "hex": "#f3b746" },
            { "color": "灰色", "ratio": 0.2, "hex": "#888888" }
          ],
          "architecture_style": ["近代中西合璧官式建筑", "民国公共建筑"],
          "scene_description": "画面主体为建筑外观中的近代以前传统建筑，建筑主体色彩分布为米白色约占55%，黄色约占25%，灰色约占20%，屋顶、立柱、墙面与台基层次清楚，整体呈现近代中西合璧官式建筑、民国公共建筑特征，材质以木构、灰瓦与石质构件组合为主，朝代判断归入清，建筑等级归入官员，整体审美与辽宁省传统建筑风格相符。",
          "reasoning": [
            "建筑主体颜色分布清楚，米白色约占55%，黄色约占25%，灰色约占20%，色彩主要集中在屋顶、立柱、墙面与台基等核心构件。",
            "屋顶形制、檐口层次、门窗比例与装饰构件明确，整体属于近代以前的清近代中西合璧官式建筑、民国公共建筑体系。",
            "主体构图、构件比例与色彩组织共同指向辽宁省范围内的官员等级传统建筑风格表达。"
          ],
          "needs_manual_review": true,
          "file_name": "1.jpg",
          "relative_path": "东北地区/1.jpg",
          "analysis_status": "success",
          "error_message": "",
          "image_index": 1
        },
        "createTime": "2026-03-14T16:28:32",
        "updateTime": "2026-03-14T16:28:32"
      }
    ]
  }
}
```

### 13.2 查询单条数据集记录

**请求地址**: `GET /api/dataset/records/{id}`

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "datasetName": "competition-dataset",
    "groupName": "东北地区",
    "groupRelativePath": "northeast",
    "originalGroupRelativePath": "东北地区",
    "schemaVersion": "image_region_analysis_v2",
    "promptVersion": "v4",
    "generatedAt": "2026-03-14T06:20:21.863565+00:00",
    "apiInterface": "responses",
    "model": "gpt-5.2",
    "provinceLevelRegion": "辽宁省",
    "provinceConfidence": 0.48,
    "dynastyGuess": "清",
    "buildingRank": "官员",
    "sceneType": "建筑外观",
    "buildingPresent": true,
    "buildingPrimaryColors": ["米白色", "黄色", "灰色"],
    "buildingColorDistribution": [
      { "color": "米白色", "ratio": 0.55, "hex": "#efe5cf" },
      { "color": "黄色", "ratio": 0.25, "hex": "#f3b746" },
      { "color": "灰色", "ratio": 0.2, "hex": "#888888" }
    ],
    "architectureStyle": ["近代中西合璧官式建筑", "民国公共建筑"],
    "sceneDescription": "画面主体为建筑外观中的近代以前传统建筑，建筑主体色彩分布为米白色约占55%，黄色约占25%，灰色约占20%，屋顶、立柱、墙面与台基层次清楚，整体呈现近代中西合璧官式建筑、民国公共建筑特征，材质以木构、灰瓦与石质构件组合为主，朝代判断归入清，建筑等级归入官员，整体审美与辽宁省传统建筑风格相符。",
    "reasoning": [
      "建筑主体颜色分布清楚，米白色约占55%，黄色约占25%，灰色约占20%，色彩主要集中在屋顶、立柱、墙面与台基等核心构件。",
      "屋顶形制、檐口层次、门窗比例与装饰构件明确，整体属于近代以前的清近代中西合璧官式建筑、民国公共建筑体系。",
      "主体构图、构件比例与色彩组织共同指向辽宁省范围内的官员等级传统建筑风格表达。"
    ],
    "needsManualReview": true,
    "fileName": "1.jpg",
    "relativePath": "northeast/1.jpg",
    "originalRelativePath": "东北地区/1.jpg",
    "storedImagePath": "/Users/bernard/Downloads/ACASB_Package_20260314_065734/dataset-storage/competition-dataset/东北地区/1.jpg",
    "sourceImagePath": "/Users/bernard/Code/ACASB/计算机设计大赛/东北地区/1.jpg",
    "analysisStatus": "success",
    "errorMessage": "",
    "imageIndex": 1,
    "rawMetadata": {},
    "createTime": "2026-03-14T16:28:32",
    "updateTime": "2026-03-14T16:28:32"
  }
}
```

### 13.3 按分组序号查询

**请求地址**: `GET /api/dataset/record-by-index`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `datasetName` | `String` | 是 | 数据集名称 |
| `groupName` | `String` | 是 | 分组名称 |
| `imageIndex` | `Integer` | 是 | 分组内序号 |

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 55,
    "datasetName": "competition-dataset",
    "groupName": "中南地区",
    "imageIndex": 54,
    "fileName": "47.jpg",
    "originalRelativePath": "中南地区/47.jpg",
    "provinceLevelRegion": "西藏自治区",
    "dynastyGuess": "清",
    "buildingRank": "皇家",
    "sceneType": "建筑群"
  }
}
```

### 13.4 按分组和文件名查询

**请求地址**: `GET /api/dataset/record-by-file`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `datasetName` | `String` | 是 | 数据集名称 |
| `groupName` | `String` | 是 | 分组名称 |
| `fileName` | `String` | 是 | 文件名，例如 `47.jpg` |

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 55,
    "datasetName": "competition-dataset",
    "groupName": "中南地区",
    "imageIndex": 54,
    "fileName": "47.jpg",
    "originalRelativePath": "中南地区/47.jpg",
    "provinceLevelRegion": "西藏自治区",
    "dynastyGuess": "清",
    "buildingRank": "皇家",
    "sceneType": "建筑群"
  }
}
```

### 13.5 数据集统计概览

**请求地址**: `GET /api/dataset/stats/overview`

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "totalRecords": 312,
    "successCount": 312,
    "manualReviewCount": 78,
    "dynasties": [
      { "name": "清", "count": 263 },
      { "name": "明", "count": 45 },
      { "name": "宋", "count": 2 },
      { "name": "元", "count": 1 },
      { "name": "唐", "count": 1 }
    ],
    "ranks": [
      { "name": "官员", "count": 148 },
      { "name": "平民", "count": 107 },
      { "name": "皇家", "count": 57 }
    ],
    "sceneTypes": [
      { "name": "建筑外观", "count": 116 },
      { "name": "建筑群", "count": 112 },
      { "name": "园林", "count": 68 },
      { "name": "街景", "count": 16 }
    ],
    "provinces": [
      { "name": "江苏省", "count": 59 },
      { "name": "福建省", "count": 37 },
      { "name": "湖北省", "count": 36 }
    ],
    "groups": [
      { "name": "华东地区", "count": 118 },
      { "name": "中南地区", "count": 111 },
      { "name": "西南地区", "count": 52 },
      { "name": "东北地区", "count": 17 },
      { "name": "华北地区", "count": 14 }
    ]
  }
}
```

### 13.6 区域统计

**请求地址**: `GET /api/dataset/stats/regions`

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 19,
    "regions": [
      { "name": "江苏省", "count": 59, "percent": 18.91 },
      { "name": "福建省", "count": 37, "percent": 11.86 },
      { "name": "湖北省", "count": 36, "percent": 11.54 }
    ]
  }
}
```

### 13.7 色彩统计

**请求地址**: `GET /api/dataset/stats/colors`

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "totalRecords": 312,
    "colors": [
      { "name": "灰色", "hex": "#888888", "imageCount": 219, "totalRatio": 80.48, "averageRatio": 0.3675 },
      { "name": "白色", "hex": "#f2eada", "imageCount": 158, "totalRatio": 55.22, "averageRatio": 0.3495 },
      { "name": "红色", "hex": "#c4473a", "imageCount": 163, "totalRatio": 42.64, "averageRatio": 0.2616 },
      { "name": "灰黑色", "hex": "#4a4a4a", "imageCount": 47, "totalRatio": 19.48, "averageRatio": 0.4145 },
      { "name": "土黄色", "hex": "#c49a44", "imageCount": 24, "totalRatio": 11.76, "averageRatio": 0.49 }
    ]
  }
}
```

说明：

- `hex` 是后端为颜色词补齐的近似 UI 色值，用于前端直接渲染图例、色块和标签。
- 当前即使存在 40 多种颜色词，也都会稳定返回对应的 `hex`。

### 13.8 等级统计

**请求地址**: `GET /api/dataset/stats/ranks`

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 3,
    "ranks": [
      { "name": "官员", "count": 148, "percent": 47.44 },
      { "name": "平民", "count": 107, "percent": 34.29 },
      { "name": "皇家", "count": 57, "percent": 18.27 }
    ]
  }
}
```

### 13.9 风格统计

**请求地址**: `GET /api/dataset/stats/styles`

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 5,
    "styles": [
      { "name": "徽派古建", "count": 29, "percent": 9.29 },
      { "name": "藏式古建", "count": 18, "percent": 5.77 },
      { "name": "闽南红砖古建", "count": 16, "percent": 5.13 },
      { "name": "皇家官式古建", "count": 15, "percent": 4.81 },
      { "name": "岭南祠庙古建", "count": 12, "percent": 3.85 }
    ]
  }
}
```

---

## 14. 图像分析与基础服务接口

### 14.1 Java 健康检查

**请求地址**: `GET /api/health`

### 完整返回示例

```text
Java Backend is running!
```

### 14.2 Java 图像接口测试

**请求地址**: `GET /api/test`

### 完整返回示例

```text
Image API is working!
```

### 14.3 Python 健康透传

**请求地址**: `GET /testPython`

### 完整返回示例

```json
{
  "status": "healthy",
  "message": "API is ready"
}
```

### 14.4 建筑二分类预测

**请求地址**: `POST /api/predict`

**请求体**：

```json
{
  "image_path": "./uploads/demo.jpg"
}
```

### 完整返回示例

```json
{
  "success": false,
  "message": "本地训练模型预测已禁用，请使用云端 AI 解析字段",
  "prediction": null,
  "confidence": 0.0
}
```

### 14.5 图像 19 维特征分析与 AI 解析

**请求地址**: `POST /api/analyze`

支持：

- `multipart/form-data`
- `application/json`

JSON 请求体支持两种字段写法：

```json
{
  "image_path": "./uploads/demo.jpg",
  "enable_ai": true
}
```

或：

```json
{
  "imagePath": "./uploads/demo.jpg",
  "enableAi": true
}
```

### 完整返回示例

```json
{
  "success": true,
  "message": "Analysis completed",
  "prediction": null,
  "confidence": null,
  "ai_analyze": "{\"architecture_style\":[\"皇家官式古建\",\"北方宫殿式建筑\"],\"building_color_distribution\":[{\"color\":\"灰色\",\"ratio\":0.62},{\"color\":\"红色\",\"ratio\":0.23},{\"color\":\"蓝色\",\"ratio\":0.1},{\"color\":\"金色\",\"ratio\":0.05}],\"building_present\":true,\"building_primary_colors\":[\"灰色\",\"红色\",\"蓝色\",\"金色\"],\"building_rank\":\"皇家\",\"dynasty_guess\":\"清\",\"needs_manual_review\":true,\"province_confidence\":0.53,\"province_level_region\":\"云南省\",\"reasoning\":[\"建筑为多重歇山顶与灰色筒瓦屋面，檐口上翘，屋脊设宝顶，呈官式做法\",\"柱子与门窗以红色为主，檐下斗拱与彩画以蓝绿为主，具典型宫殿式彩绘特征\"],\"scene_description\":\"画面为山地城市上方俯瞰的一组官式楼阁建筑，整体为清代官式皇家等级风格的楼阁建筑。\",\"scene_type\":\"建筑群\"}",
  "ai_analysis": {
    "province_level_region": "云南省",
    "province_confidence": 0.53,
    "dynasty_guess": "清",
    "building_rank": "皇家",
    "scene_type": "建筑群",
    "building_present": true,
    "building_primary_colors": ["灰色", "红色", "蓝色", "金色"],
    "building_color_distribution": [
      { "color": "灰色", "ratio": 0.62, "hex": "#888888" },
      { "color": "红色", "ratio": 0.23, "hex": "#c4473a" },
      { "color": "蓝色", "ratio": 0.1, "hex": "#4d79a7" },
      { "color": "金色", "ratio": 0.05, "hex": "#c9a227" }
    ],
    "architecture_style": ["皇家官式古建", "北方宫殿式建筑"],
    "scene_description": "画面主体为建筑群中的近代以前传统建筑，建筑主体色彩分布为灰色约占62%，红色约占23%，蓝色约占10%，金色约占5%，屋顶、立柱、墙面与台基层次清楚，整体呈现皇家官式古建、北方宫殿式建筑特征，材质以木构、灰瓦与石质构件组合为主，朝代判断归入清，建筑等级归入皇家，整体审美与云南省传统建筑风格相符。",
    "reasoning": [
      "建筑主体颜色分布清楚，灰色约占62%，红色约占23%，蓝色约占10%，金色约占5%，色彩主要集中在屋顶、立柱、墙面与台基等核心构件。",
      "屋顶形制、檐口层次、门窗比例与装饰构件明确，整体属于近代以前的清皇家官式古建、北方宫殿式建筑体系。",
      "主体构图、构件比例与色彩组织共同指向云南省范围内的皇家等级传统建筑风格表达。"
    ],
    "needs_manual_review": true,
    "file_name": "demo.jpg",
    "relative_path": "demo.jpg",
    "analysis_status": "success",
    "error_message": "",
    "image_index": null
  },
  "ratio_yellow": 0.2088,
  "ratio_red_1": 0.0932,
  "ratio_red_2": 0.0409,
  "ratio_blue": 0.238,
  "ratio_green": 0.1154,
  "ratio_gray_white": 0.0391,
  "ratio_black": 0.0496,
  "h_mean": 0.3674,
  "h_std": 0.2654,
  "s_mean": 0.3771,
  "s_std": 0.2556,
  "v_mean": 0.5943,
  "v_std": 0.2322,
  "edge_density": 0.3068,
  "entropy": 0.9709,
  "contrast": 6.7755,
  "dissimilarity": 0.1087,
  "homogeneity": 0.1155,
  "asm": 0.0001,
  "royal_ratio": 0.3429
}
```

补充说明：

- `ai_analyze` 现在会被后端标准化为 JSON 字符串，`building_color_distribution` 中同样会带 `hex`。
- `ai_analysis.building_color_distribution[].hex` 与 `ai_analyze` 中的 `hex` 口径一致，前端优先读取 `ai_analysis` 更方便。

---

## 15. 旧版分析查询接口

### 15.1 查询分析详情

**请求地址**: `GET /data/analysis/{id}`

### 完整返回示例：存在记录

```json
{
  "success": true,
  "data": {
    "id": 12,
    "imagePath": "./uploads/demo.jpg",
    "ratioYellow": 0.2088,
    "ratioRed1": 0.0932,
    "ratioRed2": 0.0409,
    "ratioBlue": 0.238,
    "ratioGreen": 0.1154,
    "ratioGrayWhite": 0.0391,
    "ratioBlack": 0.0496,
    "hMean": 0.3674,
    "hStd": 0.2654,
    "sMean": 0.3771,
    "sStd": 0.2556,
    "vMean": 0.5943,
    "vStd": 0.2322,
    "edgeDensity": 0.3068,
    "entropy": 0.9709,
    "contrast": 6.7755,
    "dissimilarity": 0.1087,
    "homogeneity": 0.1155,
    "asm": 0.0001,
    "royalRatio": 0.3429,
    "aiAnalyze": "{\"province_level_region\":\"云南省\",...}",
    "createTime": "2026-03-14T17:38:30",
    "updateTime": "2026-03-14T17:38:30"
  }
}
```

### 完整返回示例：记录不存在

```json
{
  "success": false,
  "message": "未找到编号为 1 的分析信息"
}
```

### 15.2 查询预测详情

**请求地址**: `GET /data/type/{id}`

### 完整返回示例：存在记录

```json
{
  "success": true,
  "data": {
    "id": 12,
    "imagePath": "./uploads/demo.jpg",
    "prediction": "royal",
    "confidence": 0.8567,
    "analysisId": 12,
    "createTime": "2026-03-14T17:38:30",
    "updateTime": "2026-03-14T17:38:30"
  }
}
```

### 完整返回示例：记录不存在

```json
{
  "success": false,
  "message": "未找到编号为 1 的建筑类型信息"
}
```

### 15.3 分析记录列表

**请求地址**: `GET /data/list`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `field` | `String` | 否 | 排序字段 |
| `order` | `String` | 否 | `asc` 或 `desc` |
| `limit` | `Integer` | 否 | 默认 10，最大 200 |
| `prediction` | `String` | 否 | 按预测值过滤 |

### 完整返回示例

```json
{
  "success": true,
  "data": [],
  "count": 0,
  "message": "查询成功"
}
```

### 说明

- 当 `field` 不传时，当前默认按 `royal_ratio desc`。
- 如果没有通过 `/data/add` 或 `/data/batch` 往旧表里写入过数据，这里会返回空数组。

---

## 16. 上传与导入接口

这一组接口主要面向后台、运维、数据整理流程。

### 16.1 单张图片入库

**请求地址**: `POST /data/add`

**请求方式**: `multipart/form-data`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | `File` | 是 | 上传图片 |
| `enable_ai` | `Boolean` | 否 | 是否启用云端 AI |

### 完整返回示例

```json
{
  "success": true,
  "message": "数据添加成功",
  "analysisId": 101,
  "typeId": null,
  "storedImagePath": "./uploads/2df8d7f6-1b4c-4f5a-bf2f-fec504ba5a7c.jpg",
  "ai_analyze": "{\"province_level_region\":\"云南省\",\"dynasty_guess\":\"清\",\"building_rank\":\"皇家\",...}"
}
```

### 16.2 批量图片入库

**请求地址**: `POST /data/batch`

**请求方式**: `multipart/form-data`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `files` | `File[]` | 是 | 图片数组 |
| `enable_ai` | `Boolean` | 否 | 是否启用云端 AI |

### 完整返回示例

```json
{
  "totalCount": 2,
  "successCount": 2,
  "failureCount": 0,
  "items": [
    {
      "fileName": "1.jpg",
      "analysisId": 101,
      "typeId": null,
      "message": "上传成功",
      "success": true
    },
    {
      "fileName": "2.jpg",
      "analysisId": 102,
      "typeId": null,
      "message": "上传成功",
      "success": true
    }
  ]
}
```

### 16.3 导入服务端目录

**请求地址**: `POST /api/dataset/import-folder`

**请求体**：

```json
{
  "datasetPath": "/data/competition-dataset",
  "datasetName": "competition-dataset",
  "copyImages": true
}
```

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "datasetName": "competition-dataset",
    "manifestsDiscovered": 5,
    "recordsProcessed": 312,
    "importedCount": 312,
    "updatedCount": 0,
    "failedCount": 0,
    "copiedImageCount": 312,
    "errors": []
  }
}
```

### 16.4 导入指定 manifest

**请求地址**: `POST /api/dataset/import-manifest`

**请求体**：

```json
{
  "manifestPath": "/data/competition-dataset/中南地区/analysis.json",
  "datasetName": "competition-dataset",
  "copyImages": true
}
```

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "datasetName": "competition-dataset",
    "manifestsDiscovered": 1,
    "recordsProcessed": 111,
    "importedCount": 111,
    "updatedCount": 0,
    "failedCount": 0,
    "copiedImageCount": 111,
    "errors": []
  }
}
```

### 16.5 上传 manifest 与图片

**请求地址**: `POST /api/dataset/upload-manifest`

**请求方式**: `multipart/form-data`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `manifest` | `File` | 是 | `analysis.json` 文件 |
| `images` | `File[]` | 是 | 对应图片数组 |
| `dataset_name` | `String` | 否 | 数据集名称 |

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "datasetName": "competition-dataset",
    "manifestsDiscovered": 1,
    "recordsProcessed": 52,
    "importedCount": 52,
    "updatedCount": 0,
    "failedCount": 0,
    "copiedImageCount": 52,
    "errors": []
  }
}
```

### 16.6 上传单条高质量记录

**请求地址**: `POST /api/dataset/upload-record`

**请求方式**: `multipart/form-data`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | `File` | 是 | 图片文件 |
| `metadata` | `String` | 否 | JSON 字符串 |
| `metadata_file` | `File` | 否 | JSON 文件 |
| `dataset_name` | `String` | 否 | 数据集名称 |
| `group_name` | `String` | 否 | 分组名 |
| `group_relative_path` | `String` | 否 | 分组相对路径 |
| `schema_version` | `String` | 否 | schema 版本 |
| `prompt_version` | `String` | 否 | prompt 版本 |
| `generated_at` | `String` | 否 | 生成时间 |
| `api_interface` | `String` | 否 | 使用的 AI 接口 |
| `model` | `String` | 否 | 模型名称 |

### 完整返回示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "success": true,
    "datasetName": "competition-dataset",
    "recordId": 313,
    "groupName": "中南地区",
    "fileName": "999.jpg",
    "storedImagePath": "./dataset-storage/competition-dataset/zhongnan/999.jpg",
    "originalRelativePath": "中南地区/999.jpg",
    "relativePath": "south-central/999.jpg",
    "message": "单条记录上传成功"
  }
}
```

---

## 17. 前端推荐接入顺序

如果当前主要是首页大屏与互动体验，建议优先对接：

- `GET /api/dashboard/color-levels`
- `GET /api/dashboard/dynasty-stats`
- `GET /api/dashboard/map-distribution`
- `GET /api/dashboard/core-colors`
- `GET /api/dashboard/color-analysis`
- `GET /api/dashboard/level-stats`
- `GET /api/dashboard/dynasty-comparison`
- `GET /api/dashboard/history-trend`
- `GET /api/dashboard/region-rank-dist`
- `GET /api/dashboard/material-analysis`
- `GET /api/experience/rank-rules`
- `POST /api/experience/validate`
- `GET /api/dataset/records`
- `GET /api/dataset/record-by-index`
- `GET /api/dataset/record-by-file`

如果需要图像分析页，再接：

- `POST /api/analyze`
- `POST /api/predict`

如果需要后台入库，再接：

- `POST /data/add`
- `POST /data/batch`
- `POST /api/dataset/import-folder`
- `POST /api/dataset/import-manifest`
- `POST /api/dataset/upload-manifest`
- `POST /api/dataset/upload-record`
