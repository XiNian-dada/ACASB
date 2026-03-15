# ACASB Frontend API Current

本文档用于给前端说明当前项目里已经可用、或已按真实数据近似实现的接口。

说明：

- 本文档以当前代码实现为准，不再沿用最早那版纯设计稿。
- 大屏接口统一挂在 `/api/dashboard/*`。
- 互动体验接口统一挂在 `/api/experience/*`。
- 高质量图片数据接口统一挂在 `/api/dataset/*`。
- 其中部分接口为“近似实现”，即功能接近前端需求，但字段含义会明确说明。

## 1. 状态总览

| 模块 | 路径 | 状态 | 说明 |
|---|---|---|---|
| 数据集记录查询 | `/api/dataset/records` | 已实现 | 列表分页查询 |
| 数据集单条查询 | `/api/dataset/records/{id}` | 已实现 | 按数据库主键查 |
| 数据集精确查询 | `/api/dataset/record-by-index` | 新增实现 | 按 `groupName + imageIndex` 查 |
| 数据集精确查询 | `/api/dataset/record-by-file` | 新增实现 | 按 `groupName + fileName` 查 |
| 数据集概览统计 | `/api/dataset/stats/overview` | 已实现 | 总量、朝代、等级、区域等 |
| 数据集区域统计 | `/api/dataset/stats/regions` | 已实现 | 省级分布 |
| 数据集颜色统计 | `/api/dataset/stats/colors` | 已实现 | 颜色词聚合，已补 `hex` |
| 数据集等级统计 | `/api/dataset/stats/ranks` | 已实现 | 建筑等级聚合 |
| 数据集风格统计 | `/api/dataset/stats/styles` | 已实现 | 风格词聚合 |
| 大屏建筑色彩等级 | `/api/dashboard/color-levels` | 新增实现 | 基于真实等级数据 |
| 大屏朝代基础面板 | `/api/dashboard/dynasty-stats` | 新增实现 | 基于真实朝代和颜色词 |
| 大屏地图分布 | `/api/dashboard/map-distribution` | 新增实现 | 省级数据映射到大区 |
| 大屏核心色彩 | `/api/dashboard/core-colors` | 新增实现 | 基于朝代颜色词聚合 |
| 大屏色彩雷达 | `/api/dashboard/color-analysis` | 新增实现 | 饱和度/明度为启发式估算 |
| 大屏等级柱状图 | `/api/dashboard/level-stats` | 新增实现 | 按朝代统计等级 |
| 大屏多朝代对比 | `/api/dashboard/dynasty-comparison` | 新增实现 | 唐宋元明清等级对比 |
| 大屏历史趋势 | `/api/dashboard/history-trend` | 新增实现 | 朝代锚点趋势，近似实现 |
| 大屏区域等级分布 | `/api/dashboard/region-rank-dist` | 新增实现 | 当前样本最多省份的混合图 |
| 大屏材料成本 | `/api/dashboard/material-analysis` | 新增实现 | 启发式指数，不直接来自数据表 |
| 互动等级配色规则 | `/api/experience/rank-rules` | 新增实现 | 规则化接口 |
| 互动配色合法性校验 | `/api/experience/validate` | 新增实现 | 规则校验，可选 AI 润色 |

## 2. 数据集查询接口

### 2.1 `GET /api/dataset/records`

用途：

- 列表页
- 后台管理
- 分组翻页浏览

常用参数：

- `datasetName`
- `groupName`
- `dynasty`
- `province`
- `rank`
- `sceneType`
- `manualReview`
- `analysisStatus`
- `keyword`
- `limit`
- `offset`

说明：

- 返回项里同时保留 `relativePath` 和 `originalRelativePath`
- `buildingColorDistribution[].hex` 和 `rawMetadata.building_color_distribution[].hex` 已可直接用于前端渲染
- `groupName=华东地区` 这类中文参数在实际请求里建议 URL encode

### 2.2 `GET /api/dataset/records/{id}`

用途：

- 按数据库主键查单条

### 2.3 `GET /api/dataset/record-by-index`

用途：

- 按“分组内序号”精确查单条

参数：

- `datasetName`
- `groupName`
- `imageIndex`

示例：

```bash
curl -G "http://localhost:8080/api/dataset/record-by-index" \
  --data-urlencode "datasetName=competition-dataset" \
  --data-urlencode "groupName=中南地区" \
  --data-urlencode "imageIndex=54"
```

说明：

- `imageIndex` 是分组内排序序号
- 它不一定等于文件名数字，例如 `fileName=47.jpg` 可能对应 `imageIndex=54`

### 2.4 `GET /api/dataset/record-by-file`

用途：

- 按“分组 + 文件名”精确查单条

参数：

- `datasetName`
- `groupName`
- `fileName`

示例：

```bash
curl -G "http://localhost:8080/api/dataset/record-by-file" \
  --data-urlencode "datasetName=competition-dataset" \
  --data-urlencode "groupName=中南地区" \
  --data-urlencode "fileName=47.jpg"
```

## 3. 大屏接口

### 3.1 `GET /api/dashboard/color-levels`

对应需求：

- 建筑色彩等级数据

当前实现：

- 基于 `building_rank`
- 固定返回 `皇家 / 王公 / 官员 / 富户 / 平民`
- 没有样本的等级返回 `0`
- `percent` 当前定义为“相对最大等级数量的百分比”

可选参数：

- `datasetName`

### 3.2 `GET /api/dashboard/dynasty-stats`

对应需求：

- 朝代基础数据面板

参数：

- `dynasty`，支持 `tang/song/yuan/ming/qing` 或 `唐/宋/元/明/清`
- `datasetName` 可选

返回字段：

- `dynastyName`
- `totalBuildings`
- `yellowCount`
- `redCount`
- `greenCount`

说明：

- `yellowCount / redCount / greenCount` 当前按“该颜色家族在该图中占比达到一定阈值”计数

### 3.3 `GET /api/dashboard/map-distribution`

对应需求：

- 中国建筑色彩地理分布数据

参数：

- `dynasty`
- `datasetName` 可选

当前实现：

- 真实数据是省级区域
- 接口内部把省级区域归并到 `华北 / 东北 / 华东 / 华中 / 华南 / 西南 / 西北`

返回字段：

- `maxCount`
- `regions[].name`
- `regions[].value`
- `regions[].provinces`

### 3.4 `GET /api/dashboard/core-colors`

对应需求：

- 朝代核心色彩与文化标签

参数：

- `dynasty`
- `datasetName` 可选

当前实现：

- 颜色来自 `building_color_distribution`
- 每个颜色词都会补一个稳定的 `hex`
- `colors` 取该朝代最显著的前 4 个颜色词
- `hex` 由颜色词映射为近似 UI 色值
- `cultureTags` 优先使用朝代固定审美标签，缺失时退化为风格词拆分

### 3.5 `GET /api/dashboard/color-analysis`

对应需求：

- 朝代色彩使用分析（雷达图）

参数：

- `dynasty`
- `datasetName` 可选

固定返回 6 个维度：

- `黄色使用`
- `饱和度`
- `明度`
- `青色`
- `绿色`
- `红色`

说明：

- 顺序固定，前端可以直接绑定雷达图
- `饱和度`、`明度` 当前不是直接图像 HSV，而是基于颜色词到色彩档案的启发式映射估算

### 3.6 `GET /api/dashboard/level-stats`

对应需求：

- 社会等级建筑数量统计

参数：

- `dynasty`
- `datasetName` 可选

当前实现：

- 固定返回 `皇家 / 王公 / 官员 / 富户 / 平民`
- `ratio = 当前值 / 当前朝代下最大等级值`

### 3.7 `GET /api/dashboard/dynasty-comparison`

对应需求：

- 多朝代建筑等级分布对比

参数：

- `datasetName` 可选

当前实现：

- 固定时间轴：`唐 / 宋 / 元 / 明 / 清`
- 固定等级：`皇家 / 王公 / 官员 / 富户 / 平民`
- 若某等级无样本则返回 `0`

### 3.8 `GET /api/dashboard/history-trend`

对应需求：

- 历代色彩趋势变化与核心色值

参数：

- `datasetName` 可选

当前实现：

- 使用 `唐=618 / 宋=960 / 元=1271 / 明=1368 / 清=1644` 作为朝代锚点
- 材料名称为前端友好的代理名称：
  - `黄色琉璃瓦`
  - `红色墙体`
  - `绿色琉璃瓦`
  - `青色瓦片`
- 数值来源是颜色家族比例的总量近似换算

说明：

- 这是“近似实现”
- 更适合做趋势感展示，不适合作为严格历史计量依据

### 3.9 `GET /api/dashboard/region-rank-dist`

对应需求：

- 主要区域建筑等级分布（混合图表）

参数：

- `datasetName` 可选

当前实现：

- 自动选择当前数据集中样本最多的 5 个省级区域
- 返回 4 个系列：
  - `皇家` bar
  - `官员` bar
  - `平民` line
  - `总量` line dashed

说明：

- 这和最早设计稿里的固定五省略有不同
- 但展示功能是对应的，而且更贴近真实数据

### 3.10 `GET /api/dashboard/material-analysis`

对应需求：

- 建筑材料成本与工艺分析

当前实现：

- 采用启发式材料指数，不依赖 `dataset_image_record` 直接聚合
- 返回：
  - `dimensions`
  - `materials[].name`
  - `materials[].values`
  - `materials[].colors`

说明：

- 适合大屏对比展示
- 当前不是历史文献定量数据库结果

## 4. 互动体验接口

### 4.1 `GET /api/experience/rank-rules`

对应需求：

- 社会等级配色方案查询

参数：

- `rankId`

支持值：

- `emperor`
- `noble`
- `official`
- `wealthy`
- `civilian`
- 也兼容中文：`皇家 / 王公 / 官员 / 富户 / 平民`

返回字段：

- `rankId`
- `rankName`
- `maxColors`
- `availableColors`

### 4.2 `POST /api/experience/validate`

对应需求：

- 配色合法性校验

请求体：

```json
{
  "rankId": "civilian",
  "selections": [
    { "part": "屋顶", "colorHex": "#ffbb00" },
    { "part": "墙体", "colorHex": "#888888" }
  ]
}
```

可选 query：

- `enable_ai=true`

当前实现：

- 基础判断为规则校验
- 若 `enable_ai=true` 或全局开启 `ai.analysis.enabled=true`，会尝试调用云端兼容大模型对反馈文案做润色
- AI 失败时自动回退到本地规则文案

返回字段：

- `isValid`
- `resultTitle`
- `resultLevel`
- `feedback`
- `knowledgePoint`
- `rankName`
- `selectionCount`
- `violations`

## 5. 推荐对接方式

### 首页大屏

- 左上等级卡片：`/api/dashboard/color-levels`
- 左下朝代面板：`/api/dashboard/dynasty-stats`
- 中央地图：`/api/dashboard/map-distribution`
- 右侧核心色彩：`/api/dashboard/core-colors`
- 右上雷达图：`/api/dashboard/color-analysis`
- 左侧等级柱状图：`/api/dashboard/level-stats`
- 底部朝代对比：`/api/dashboard/dynasty-comparison`
- 底部趋势图：`/api/dashboard/history-trend`
- 底部区域混合图：`/api/dashboard/region-rank-dist`
- 材料成本图：`/api/dashboard/material-analysis`

### 互动页

- 身份初始化：`/api/experience/rank-rules`
- 提交校验：`/api/experience/validate`

### 单图详情页 / 后台

- 列表：`/api/dataset/records`
- 单条主键：`/api/dataset/records/{id}`
- 按序号精确查：`/api/dataset/record-by-index`
- 按文件名精确查：`/api/dataset/record-by-file`

## 6. 当前数据约束

- 当前真实数据集中，`building_rank` 主要是 `皇家 / 官员 / 平民`
- `王公 / 富户` 暂时样本很少或没有，所以相关图表会出现 `0`
- `imageIndex` 是分组内顺序号，不等于文件名数字
- `relativePath` 是 ASCII 规范路径，`originalRelativePath` 保留原始中文路径
- 大屏部分视觉指标是由颜色词做启发式推断，不是原图像素级重算
