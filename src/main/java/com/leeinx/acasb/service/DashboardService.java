package com.leeinx.acasb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leeinx.acasb.dto.DatasetColorDistributionItem;
import com.leeinx.acasb.entity.DatasetImageRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
public class DashboardService {
    private static final String DEFAULT_DATASET_NAME = "competition-dataset";
    private static final List<String> DYNASTY_ORDER = List.of("唐", "宋", "元", "明", "清");
    private static final Map<String, Integer> DYNASTY_YEAR = Map.of(
            "唐", 618,
            "宋", 960,
            "元", 1271,
            "明", 1368,
            "清", 1644
    );
    private static final List<String> LEVEL_ORDER = List.of("皇家", "王公", "官员", "富户", "平民");
    private static final Map<String, String> LEVEL_COLORS = Map.of(
            "皇家", "#f3b746",
            "王公", "#5e9c45",
            "官员", "#3c8dbc",
            "富户", "#666666",
            "平民", "#333333"
    );
    private static final Map<String, List<String>> MACRO_REGIONS = new LinkedHashMap<>();
    private static final Map<String, List<String>> DYNASTY_TAGS = new LinkedHashMap<>();

    static {
        MACRO_REGIONS.put("华北地区", List.of("北京市", "天津市", "河北省", "山西省", "内蒙古自治区"));
        MACRO_REGIONS.put("东北地区", List.of("辽宁省", "吉林省", "黑龙江省"));
        MACRO_REGIONS.put("华东地区", List.of("上海市", "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省"));
        MACRO_REGIONS.put("华中地区", List.of("河南省", "湖北省", "湖南省"));
        MACRO_REGIONS.put("华南地区", List.of("广东省", "广西壮族自治区", "海南省"));
        MACRO_REGIONS.put("西南地区", List.of("重庆市", "四川省", "贵州省", "云南省", "西藏自治区"));
        MACRO_REGIONS.put("西北地区", List.of("陕西省", "甘肃省", "青海省", "宁夏回族自治区", "新疆维吾尔自治区"));

        DYNASTY_TAGS.put("唐", List.of("雄浑大气", "礼制色彩", "高饱和对比", "皇家气象"));
        DYNASTY_TAGS.put("宋", List.of("素雅含蓄", "青白淡彩", "园林诗意", "文人审美"));
        DYNASTY_TAGS.put("元", List.of("多元交融", "边地风貌", "厚重屋面", "高反差构图"));
        DYNASTY_TAGS.put("明", List.of("朱红秩序", "官式规整", "礼制鲜明", "中轴威仪"));
        DYNASTY_TAGS.put("清", List.of("金碧华彩", "宫廷等级", "彩画繁缛", "工艺成熟"));
    }

    private final DatasetImageRecordService datasetImageRecordService;
    private final ObjectMapper objectMapper;

    public DashboardService(
            DatasetImageRecordService datasetImageRecordService,
            ObjectMapper objectMapper) {
        this.datasetImageRecordService = datasetImageRecordService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getColorLevels(String datasetName) {
        List<DatasetImageRecord> records = loadRecords(datasetName, null);
        Map<String, Long> counts = countBy(records, DatasetImageRecord::getBuildingRank);
        long max = LEVEL_ORDER.stream().mapToLong(level -> counts.getOrDefault(level, 0L)).max().orElse(0);

        List<Map<String, Object>> items = new ArrayList<>();
        for (String level : LEVEL_ORDER) {
            long value = counts.getOrDefault(level, 0L);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", level);
            item.put("value", value);
            item.put("percent", formatPercent(max == 0 ? 0 : value * 1.0 / max));
            item.put("color", LEVEL_COLORS.getOrDefault(level, "#999999"));
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("datasetName", resolveDatasetName(datasetName));
        result.put("colorLevels", items);
        return result;
    }

    public Map<String, Object> getDynastyStats(String datasetName, String dynasty) {
        String normalizedDynasty = normalizeDynasty(dynasty);
        List<DatasetImageRecord> records = loadRecords(datasetName, normalizedDynasty);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dynastyName", fullDynastyName(normalizedDynasty));
        result.put("totalBuildings", records.size());
        result.put("yellowCount", countImagesByFamily(records, Set.of("yellow", "gold")));
        result.put("redCount", countImagesByFamily(records, Set.of("red")));
        result.put("greenCount", countImagesByFamily(records, Set.of("green")));
        return result;
    }

    public Map<String, Object> getMapDistribution(String datasetName, String dynasty) {
        List<DatasetImageRecord> records = loadRecords(datasetName, normalizeDynasty(dynasty));
        List<Map<String, Object>> regions = new ArrayList<>();
        int maxCount = 0;

        for (Map.Entry<String, List<String>> entry : MACRO_REGIONS.entrySet()) {
            int count = (int) records.stream()
                    .filter(record -> entry.getValue().contains(record.getProvinceLevelRegion()))
                    .count();
            maxCount = Math.max(maxCount, count);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", entry.getKey());
            item.put("value", count);
            item.put("provinces", entry.getValue());
            regions.add(item);
        }

        regions.sort((left, right) -> Integer.compare((Integer) right.get("value"), (Integer) left.get("value")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("maxCount", maxCount);
        result.put("regions", regions);
        return result;
    }

    public Map<String, Object> getCoreColors(String datasetName, String dynasty) {
        String normalizedDynasty = normalizeDynasty(dynasty);
        List<DatasetImageRecord> records = loadRecords(datasetName, normalizedDynasty);
        Map<String, ColorAggregate> aggregates = aggregateColors(records);

        List<Map<String, Object>> colors = aggregates.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, ColorAggregate> entry) -> entry.getValue().totalRatio).reversed()
                        .thenComparing((left, right) -> Long.compare(right.getValue().imageCount, left.getValue().imageCount)))
                .limit(4)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", simplifyColorName(entry.getKey()));
                    item.put("hex", colorProfile(entry.getKey()).hex());
                    item.put("count", entry.getValue().imageCount);
                    return item;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dynastyName", fullDynastyName(normalizedDynasty));
        result.put("colors", colors);
        result.put("cultureTags", resolveCultureTags(records, normalizedDynasty));
        return result;
    }

    public Map<String, Object> getColorAnalysis(String datasetName, String dynasty) {
        String normalizedDynasty = normalizeDynasty(dynasty);
        List<DatasetImageRecord> records = loadRecords(datasetName, normalizedDynasty);

        double yellow = averageFamilyRatio(records, Set.of("yellow", "gold"));
        double green = averageFamilyRatio(records, Set.of("green"));
        double cyan = averageFamilyRatio(records, Set.of("cyan", "blue"));
        double red = averageFamilyRatio(records, Set.of("red"));
        double saturation = averageColorMetric(records, ColorProfile::saturation);
        double brightness = averageColorMetric(records, ColorProfile::brightness);

        List<Map<String, Object>> indicators = List.of(
                indicator("黄色使用", yellow),
                indicator("饱和度", saturation / 100.0),
                indicator("明度", brightness / 100.0),
                indicator("青色", cyan),
                indicator("绿色", green),
                indicator("红色", red)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dynastyName", fullDynastyName(normalizedDynasty));
        result.put("indicators", indicators);
        result.put("note", "饱和度与明度基于入库颜色词的启发式色彩映射估算");
        return result;
    }

    public Map<String, Object> getLevelStats(String datasetName, String dynasty) {
        String normalizedDynasty = normalizeDynasty(dynasty);
        List<DatasetImageRecord> records = loadRecords(datasetName, normalizedDynasty);
        Map<String, Long> counts = countBy(records, DatasetImageRecord::getBuildingRank);
        long max = LEVEL_ORDER.stream().mapToLong(level -> counts.getOrDefault(level, 0L)).max().orElse(0);

        List<Map<String, Object>> levels = new ArrayList<>();
        for (String level : LEVEL_ORDER) {
            long value = counts.getOrDefault(level, 0L);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", level + "建筑");
            item.put("value", value);
            item.put("ratio", max == 0 ? 0.0 : round(value * 1.0 / max));
            levels.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dynastyName", fullDynastyName(normalizedDynasty));
        result.put("levels", levels);
        return result;
    }

    public Map<String, Object> getDynastyComparison(String datasetName) {
        List<DatasetImageRecord> records = loadRecords(datasetName, null);
        List<Map<String, Object>> series = new ArrayList<>();

        for (String level : LEVEL_ORDER) {
            List<Long> values = new ArrayList<>();
            for (String dynasty : DYNASTY_ORDER) {
                long count = records.stream()
                        .filter(record -> dynasty.equals(record.getDynastyGuess()))
                        .filter(record -> level.equals(record.getBuildingRank()))
                        .count();
                values.add(count);
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rankName", level);
            item.put("data", values);
            item.put("color", LEVEL_COLORS.getOrDefault(level, "#999999"));
            series.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timeline", DYNASTY_ORDER);
        result.put("series", series);
        result.put("description", "基于当前入库数据，对唐、宋、元、明、清五个朝代的建筑等级分布进行对比。王公、富户若无样本则返回 0。");
        return result;
    }

    public Map<String, Object> getHistoryTrend(String datasetName) {
        List<DatasetImageRecord> records = loadRecords(datasetName, null);
        List<Integer> years = DYNASTY_ORDER.stream().map(DYNASTY_YEAR::get).toList();

        List<Map<String, Object>> series = List.of(
                materialSeries("黄色琉璃瓦", "#f3b746", records, Set.of("yellow", "gold")),
                materialSeries("红色墙体", "#e65d25", records, Set.of("red")),
                materialSeries("绿色琉璃瓦", "#5e9c45", records, Set.of("green")),
                materialSeries("青色瓦片", "#5a7fb4", records, Set.of("cyan", "blue"))
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("years", years);
        result.put("series", series);
        result.put("cultureTags", List.of("礼制色彩", "地域风格", "材质层次", "时代演变"));
        result.put("description", "使用唐、宋、元、明、清五个朝代锚点，按颜色材质代理词统计历史色彩趋势。");
        return result;
    }

    public Map<String, Object> getRegionRankDistribution(String datasetName) {
        List<DatasetImageRecord> records = loadRecords(datasetName, null);
        List<String> provinces = countBy(records, DatasetImageRecord::getProvinceLevelRegion).entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        List<Map<String, Object>> series = new ArrayList<>();
        series.add(regionSeries("皇家", "bar", "#f3b746", null, provinces, records, "皇家"));
        series.add(regionSeries("官员", "bar", "#3c8dbc", null, provinces, records, "官员"));
        series.add(regionSeries("平民", "line", "#666666", null, provinces, records, "平民"));
        series.add(regionSeries("总量", "line", "#111111", "dashed", provinces, records, null));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provinces", provinces);
        result.put("series", series);
        result.put("description", "基于当前数据集中样本数最多的 5 个省级区域，展示主要建筑等级分布与总量趋势。");
        return result;
    }

    public Map<String, Object> getMaterialAnalysis() {
        List<Map<String, Object>> materials = new ArrayList<>();
        materials.add(materialItem("黄色琉璃瓦", List.of(95, 98, 92)));
        materials.add(materialItem("绿色琉璃瓦", List.of(78, 65, 55)));
        materials.add(materialItem("朱红漆料", List.of(88, 85, 82)));
        materials.add(materialItem("青色瓦片", List.of(45, 45, 30)));
        materials.add(materialItem("灰黑瓦片", List.of(25, 15, 10)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dimensions", List.of("工艺难度", "成本指数", "稀有度"));
        result.put("materials", materials);
        result.put("description", "材料成本分析当前为规则化启发式指数，用于大屏材料成本对比展示。");
        return result;
    }

    private Map<String, Object> regionSeries(
            String name,
            String type,
            String color,
            String lineStyle,
            List<String> provinces,
            List<DatasetImageRecord> records,
            String rank) {
        List<Long> data = new ArrayList<>();
        for (String province : provinces) {
            long value = records.stream()
                    .filter(record -> province.equals(record.getProvinceLevelRegion()))
                    .filter(record -> rank == null || rank.equals(record.getBuildingRank()))
                    .count();
            data.add(value);
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("type", type);
        item.put("data", data);
        item.put("color", color);
        if (StringUtils.hasText(lineStyle)) {
            item.put("lineStyle", lineStyle);
        }
        return item;
    }

    private Map<String, Object> materialSeries(
            String materialName,
            String color,
            List<DatasetImageRecord> records,
            Set<String> families) {
        List<Integer> data = new ArrayList<>();
        for (String dynasty : DYNASTY_ORDER) {
            double totalRatio = records.stream()
                    .filter(record -> dynasty.equals(record.getDynastyGuess()))
                    .mapToDouble(record -> familyRatio(record, families))
                    .sum();
            data.add((int) Math.round(totalRatio * 100));
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("materialName", materialName);
        item.put("data", data);
        item.put("color", color);
        return item;
    }

    private Map<String, Object> materialItem(String name, List<Integer> values) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("values", values);
        item.put("colors", List.of("#f3b746", "#e65d25", "#3c8dbc"));
        return item;
    }

    private Map<String, Object> indicator(String name, double ratio) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", Math.max(0, Math.min(100, (int) Math.round(ratio * 100))));
        item.put("max", 100);
        return item;
    }

    private String resolveDatasetName(String datasetName) {
        return StringUtils.hasText(datasetName) ? datasetName : DEFAULT_DATASET_NAME;
    }

    private List<DatasetImageRecord> loadRecords(String datasetName, String dynasty) {
        return datasetImageRecordService.listForStats(
                resolveDatasetName(datasetName),
                null,
                dynasty,
                null,
                null,
                null,
                null,
                "success"
        );
    }

    private String normalizeDynasty(String rawDynasty) {
        if (!StringUtils.hasText(rawDynasty)) {
            return null;
        }
        return switch (rawDynasty.trim().toLowerCase(Locale.ROOT)) {
            case "tang", "唐", "唐朝" -> "唐";
            case "song", "宋", "宋朝" -> "宋";
            case "yuan", "元", "元朝" -> "元";
            case "ming", "明", "明朝" -> "明";
            case "qing", "清", "清朝" -> "清";
            default -> rawDynasty.trim();
        };
    }

    private String fullDynastyName(String dynasty) {
        return StringUtils.hasText(dynasty) ? dynasty + "朝" : "全部朝代";
    }

    private Map<String, Long> countBy(List<DatasetImageRecord> records, Function<DatasetImageRecord, String> extractor) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (DatasetImageRecord record : records) {
            String key = extractor.apply(record);
            if (!StringUtils.hasText(key)) {
                continue;
            }
            counts.merge(key, 1L, Long::sum);
        }
        return counts;
    }

    private String formatPercent(double ratio) {
        return Math.round(ratio * 100) + "%";
    }

    private long countImagesByFamily(List<DatasetImageRecord> records, Set<String> families) {
        return records.stream()
                .filter(record -> familyRatio(record, families) >= 0.08)
                .count();
    }

    private double averageFamilyRatio(List<DatasetImageRecord> records, Set<String> families) {
        if (records.isEmpty()) {
            return 0.0;
        }
        return round(records.stream().mapToDouble(record -> familyRatio(record, families)).average().orElse(0.0));
    }

    private double averageColorMetric(List<DatasetImageRecord> records, Function<ColorProfile, Double> metric) {
        if (records.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (DatasetImageRecord record : records) {
            double recordValue = 0.0;
            for (DatasetColorDistributionItem item : parseColorDistribution(record.getBuildingColorDistributionJson())) {
                ColorProfile profile = colorProfile(item.getColor());
                recordValue += metric.apply(profile) * safeRatio(item.getRatio());
            }
            total += recordValue;
        }
        return round(total / records.size());
    }

    private double familyRatio(DatasetImageRecord record, Set<String> families) {
        double total = 0.0;
        for (DatasetColorDistributionItem item : parseColorDistribution(record.getBuildingColorDistributionJson())) {
            if (families.contains(colorProfile(item.getColor()).family())) {
                total += safeRatio(item.getRatio());
            }
        }
        return round(total);
    }

    private Map<String, ColorAggregate> aggregateColors(List<DatasetImageRecord> records) {
        Map<String, ColorAggregate> aggregates = new LinkedHashMap<>();
        for (DatasetImageRecord record : records) {
            for (DatasetColorDistributionItem item : parseColorDistribution(record.getBuildingColorDistributionJson())) {
                if (!StringUtils.hasText(item.getColor())) {
                    continue;
                }
                ColorAggregate aggregate = aggregates.computeIfAbsent(item.getColor(), key -> new ColorAggregate());
                aggregate.imageCount += 1;
                aggregate.totalRatio += safeRatio(item.getRatio());
            }
        }
        return aggregates;
    }

    private List<String> resolveCultureTags(List<DatasetImageRecord> records, String dynasty) {
        if (StringUtils.hasText(dynasty) && DYNASTY_TAGS.containsKey(dynasty)) {
            return DYNASTY_TAGS.get(dynasty);
        }

        Set<String> tags = new LinkedHashSet<>();
        for (String style : topStyles(records, 4)) {
            for (String token : splitStyleTokens(style)) {
                if (tags.size() >= 4) {
                    break;
                }
                tags.add(token);
            }
        }
        if (tags.isEmpty()) {
            tags.addAll(List.of("礼制色彩", "地域风格", "材料层次", "传统审美"));
        }
        return new ArrayList<>(tags).subList(0, Math.min(tags.size(), 4));
    }

    private List<String> topStyles(List<DatasetImageRecord> records, int limit) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (DatasetImageRecord record : records) {
            for (String style : parseStringList(record.getArchitectureStyleJson())) {
                if (StringUtils.hasText(style)) {
                    counts.merge(style, 1L, Long::sum);
                }
            }
        }
        return counts.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<String> splitStyleTokens(String style) {
        if (!StringUtils.hasText(style)) {
            return List.of();
        }
        return Arrays.stream(style.replace("建筑", "").replace("古建", "").replace("式", " ").split("[、/\\s]+"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private String simplifyColorName(String color) {
        if (!StringUtils.hasText(color)) {
            return "未知";
        }
        return color.replace("色", "");
    }

    private List<DatasetColorDistributionItem> parseColorDistribution(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<DatasetColorDistributionItem>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private double safeRatio(Double ratio) {
        return ratio == null ? 0.0 : ratio;
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private ColorProfile colorProfile(String colorName) {
        String name = colorName == null ? "" : colorName.toLowerCase(Locale.ROOT);
        if (name.contains("金") || name.contains("土黄")) {
            return new ColorProfile("#c9a227", "gold", 72.0, 72.0);
        }
        if (name.contains("黄")) {
            return new ColorProfile("#f3b746", "yellow", 80.0, 82.0);
        }
        if (name.contains("青绿")) {
            return new ColorProfile("#4f8f6f", "cyan", 58.0, 58.0);
        }
        if (name.contains("青")) {
            return new ColorProfile("#5a7fb4", "cyan", 52.0, 60.0);
        }
        if (name.contains("蓝")) {
            return new ColorProfile("#4d79a7", "blue", 56.0, 60.0);
        }
        if (name.contains("绿")) {
            return new ColorProfile("#5e9c45", "green", 60.0, 55.0);
        }
        if (name.contains("红")) {
            return new ColorProfile("#c4473a", "red", 70.0, 64.0);
        }
        if (name.contains("棕")) {
            return new ColorProfile("#7a5536", "brown", 48.0, 42.0);
        }
        if (name.contains("米白")) {
            return new ColorProfile("#efe5cf", "white", 18.0, 90.0);
        }
        if (name.contains("白")) {
            return new ColorProfile("#f2eada", "white", 12.0, 92.0);
        }
        if (name.contains("灰黑")) {
            return new ColorProfile("#4a4a4a", "black", 8.0, 28.0);
        }
        if (name.contains("黑")) {
            return new ColorProfile("#333333", "black", 6.0, 20.0);
        }
        if (name.contains("灰")) {
            return new ColorProfile("#888888", "gray", 5.0, 53.0);
        }
        return new ColorProfile("#999999", "neutral", 30.0, 55.0);
    }

    private static class ColorAggregate {
        private long imageCount;
        private double totalRatio;
    }

    private record ColorProfile(String hex, String family, Double saturation, Double brightness) {
    }
}
