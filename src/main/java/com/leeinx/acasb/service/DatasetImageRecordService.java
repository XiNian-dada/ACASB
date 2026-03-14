package com.leeinx.acasb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leeinx.acasb.dto.DatasetColorDistributionItem;
import com.leeinx.acasb.entity.DatasetImageRecord;
import com.leeinx.acasb.mapper.DatasetImageRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatasetImageRecordService extends ServiceImpl<DatasetImageRecordMapper, DatasetImageRecord> {
    private final ObjectMapper objectMapper;

    public DatasetImageRecordService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DatasetImageRecord saveOrUpdateByDatasetAndRelativePath(DatasetImageRecord record) {
        DatasetImageRecord existing = getOne(new LambdaQueryWrapper<DatasetImageRecord>()
                .eq(DatasetImageRecord::getDatasetName, record.getDatasetName())
                .eq(DatasetImageRecord::getRelativePath, record.getRelativePath())
                .last("LIMIT 1"));

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            record.setCreateTime(now);
            record.setUpdateTime(now);
            save(record);
            return record;
        }

        record.setId(existing.getId());
        record.setCreateTime(existing.getCreateTime());
        record.setUpdateTime(now);
        updateById(record);
        return record;
    }

    public DatasetImageRecord getRecordById(Long id) {
        return getById(id);
    }

    public long countFiltered(
            String datasetName,
            String groupName,
            String dynasty,
            String province,
            String rank,
            String sceneType,
            Boolean manualReview,
            String analysisStatus,
            String keyword) {
        return count(buildFilterWrapper(datasetName, groupName, dynasty, province, rank, sceneType, manualReview, analysisStatus, keyword));
    }

    public List<DatasetImageRecord> listFiltered(
            String datasetName,
            String groupName,
            String dynasty,
            String province,
            String rank,
            String sceneType,
            Boolean manualReview,
            String analysisStatus,
            String keyword,
            Integer limit,
            Integer offset) {
        int normalizedLimit = (limit == null || limit <= 0) ? 20 : Math.min(limit, 200);
        int normalizedOffset = (offset == null || offset < 0) ? 0 : offset;

        LambdaQueryWrapper<DatasetImageRecord> wrapper = buildFilterWrapper(
                datasetName, groupName, dynasty, province, rank, sceneType, manualReview, analysisStatus, keyword
        );
        wrapper.orderByAsc(DatasetImageRecord::getDatasetName)
                .orderByAsc(DatasetImageRecord::getGroupName)
                .orderByAsc(DatasetImageRecord::getImageIndex)
                .last("LIMIT " + normalizedLimit + " OFFSET " + normalizedOffset);
        return list(wrapper);
    }

    public List<DatasetImageRecord> listForStats(
            String datasetName,
            String groupName,
            String dynasty,
            String province,
            String rank,
            String sceneType,
            Boolean manualReview,
            String analysisStatus) {
        return list(buildFilterWrapper(datasetName, groupName, dynasty, province, rank, sceneType, manualReview, analysisStatus, null));
    }

    public Map<String, Object> buildRecordView(DatasetImageRecord record) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", record.getId());
        item.put("datasetName", record.getDatasetName());
        item.put("groupName", record.getGroupName());
        item.put("groupRelativePath", record.getGroupRelativePath());
        item.put("originalGroupRelativePath", record.getOriginalGroupRelativePath());
        item.put("schemaVersion", record.getSchemaVersion());
        item.put("promptVersion", record.getPromptVersion());
        item.put("generatedAt", record.getGeneratedAt());
        item.put("apiInterface", record.getApiInterface());
        item.put("model", record.getModel());
        item.put("provinceLevelRegion", record.getProvinceLevelRegion());
        item.put("provinceConfidence", record.getProvinceConfidence());
        item.put("dynastyGuess", record.getDynastyGuess());
        item.put("buildingRank", record.getBuildingRank());
        item.put("sceneType", record.getSceneType());
        item.put("buildingPresent", record.getBuildingPresent());
        item.put("buildingPrimaryColors", parseJson(record.getBuildingPrimaryColorsJson()));
        item.put("buildingColorDistribution", parseJson(record.getBuildingColorDistributionJson()));
        item.put("architectureStyle", parseJson(record.getArchitectureStyleJson()));
        item.put("sceneDescription", record.getSceneDescription());
        item.put("reasoning", parseJson(record.getReasoningJson()));
        item.put("needsManualReview", record.getNeedsManualReview());
        item.put("fileName", record.getFileName());
        item.put("relativePath", record.getRelativePath());
        item.put("originalRelativePath", record.getOriginalRelativePath());
        item.put("storedImagePath", record.getStoredImagePath());
        item.put("sourceImagePath", record.getSourceImagePath());
        item.put("analysisStatus", record.getAnalysisStatus());
        item.put("errorMessage", record.getErrorMessage());
        item.put("imageIndex", record.getImageIndex());
        item.put("rawMetadata", parseJson(record.getRawMetadataJson()));
        item.put("createTime", record.getCreateTime());
        item.put("updateTime", record.getUpdateTime());
        return item;
    }

    public Map<String, Object> buildOverviewStats(List<DatasetImageRecord> records) {
        Map<String, Long> dynastyCounts = aggregateBy(records, DatasetImageRecord::getDynastyGuess);
        Map<String, Long> rankCounts = aggregateBy(records, DatasetImageRecord::getBuildingRank);
        Map<String, Long> sceneTypeCounts = aggregateBy(records, DatasetImageRecord::getSceneType);
        Map<String, Long> provinceCounts = aggregateBy(records, DatasetImageRecord::getProvinceLevelRegion);
        Map<String, Long> groupCounts = aggregateBy(records, DatasetImageRecord::getGroupName);

        long successCount = records.stream().filter(item -> "success".equalsIgnoreCase(item.getAnalysisStatus())).count();
        long manualReviewCount = records.stream().filter(item -> Boolean.TRUE.equals(item.getNeedsManualReview())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRecords", records.size());
        result.put("successCount", successCount);
        result.put("manualReviewCount", manualReviewCount);
        result.put("dynasties", toCountItems(dynastyCounts));
        result.put("ranks", toCountItems(rankCounts));
        result.put("sceneTypes", toCountItems(sceneTypeCounts));
        result.put("provinces", toCountItems(provinceCounts));
        result.put("groups", toCountItems(groupCounts));
        return result;
    }

    public Map<String, Object> buildRegionStats(List<DatasetImageRecord> records) {
        Map<String, RegionAccumulator> accumulators = new LinkedHashMap<>();
        for (DatasetImageRecord record : records) {
            String regionName = StringUtils.hasText(record.getProvinceLevelRegion()) ? record.getProvinceLevelRegion() : "未知";
            RegionAccumulator accumulator = accumulators.computeIfAbsent(regionName, key -> new RegionAccumulator());
            accumulator.count++;
            if (record.getProvinceConfidence() != null) {
                accumulator.confidenceTotal += record.getProvinceConfidence();
                accumulator.confidenceCount++;
            }
            if (Boolean.TRUE.equals(record.getNeedsManualReview())) {
                accumulator.manualReviewCount++;
            }
        }

        List<Map<String, Object>> regions = new ArrayList<>();
        accumulators.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue().count, left.getValue().count))
                .forEach(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("count", entry.getValue().count);
                    item.put("manualReviewCount", entry.getValue().manualReviewCount);
                    item.put(
                            "avgConfidence",
                            entry.getValue().confidenceCount == 0 ? null :
                                    round(entry.getValue().confidenceTotal / entry.getValue().confidenceCount)
                    );
                    regions.add(item);
                });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRecords", records.size());
        result.put("regions", regions);
        return result;
    }

    public Map<String, Object> buildColorStats(List<DatasetImageRecord> records, Integer limit) {
        int normalizedLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 30);
        Map<String, ColorAccumulator> accumulators = new LinkedHashMap<>();

        for (DatasetImageRecord record : records) {
            for (DatasetColorDistributionItem item : parseColorDistribution(record.getBuildingColorDistributionJson())) {
                if (!StringUtils.hasText(item.getColor())) {
                    continue;
                }
                ColorAccumulator accumulator = accumulators.computeIfAbsent(item.getColor(), key -> new ColorAccumulator());
                accumulator.imageCount++;
                accumulator.totalRatio += item.getRatio() == null ? 0.0 : item.getRatio();
            }
        }

        List<Map<String, Object>> colors = accumulators.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, ColorAccumulator> entry) -> entry.getValue().totalRatio).reversed())
                .limit(normalizedLimit)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("imageCount", entry.getValue().imageCount);
                    item.put("totalRatio", round(entry.getValue().totalRatio));
                    item.put("averageRatio", round(entry.getValue().totalRatio / Math.max(entry.getValue().imageCount, 1)));
                    return item;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRecords", records.size());
        result.put("colors", colors);
        return result;
    }

    public Map<String, Object> buildRankStats(List<DatasetImageRecord> records) {
        Map<String, Long> rankCounts = aggregateBy(records, DatasetImageRecord::getBuildingRank);
        long maxCount = rankCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);

        List<Map<String, Object>> ranks = rankCounts.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("count", entry.getValue());
                    item.put("ratio", maxCount == 0 ? 0.0 : round(entry.getValue() * 1.0 / maxCount));
                    return item;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRecords", records.size());
        result.put("ranks", ranks);
        return result;
    }

    public Map<String, Object> buildStyleStats(List<DatasetImageRecord> records, Integer limit) {
        int normalizedLimit = (limit == null || limit <= 0) ? 12 : Math.min(limit, 30);
        Map<String, Long> styleCounts = new LinkedHashMap<>();

        for (DatasetImageRecord record : records) {
            for (String style : parseStringList(record.getArchitectureStyleJson())) {
                if (StringUtils.hasText(style)) {
                    styleCounts.merge(style, 1L, Long::sum);
                }
            }
        }

        List<Map<String, Object>> styles = styleCounts.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(normalizedLimit)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRecords", records.size());
        result.put("styles", styles);
        return result;
    }

    private LambdaQueryWrapper<DatasetImageRecord> buildFilterWrapper(
            String datasetName,
            String groupName,
            String dynasty,
            String province,
            String rank,
            String sceneType,
            Boolean manualReview,
            String analysisStatus,
            String keyword) {
        LambdaQueryWrapper<DatasetImageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(datasetName), DatasetImageRecord::getDatasetName, datasetName)
                .eq(StringUtils.hasText(groupName), DatasetImageRecord::getGroupName, groupName)
                .eq(StringUtils.hasText(dynasty), DatasetImageRecord::getDynastyGuess, dynasty)
                .eq(StringUtils.hasText(province), DatasetImageRecord::getProvinceLevelRegion, province)
                .eq(StringUtils.hasText(rank), DatasetImageRecord::getBuildingRank, rank)
                .eq(StringUtils.hasText(sceneType), DatasetImageRecord::getSceneType, sceneType)
                .eq(manualReview != null, DatasetImageRecord::getNeedsManualReview, manualReview)
                .eq(StringUtils.hasText(analysisStatus), DatasetImageRecord::getAnalysisStatus, analysisStatus);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(DatasetImageRecord::getSceneDescription, keyword)
                    .or().like(DatasetImageRecord::getProvinceLevelRegion, keyword)
                    .or().like(DatasetImageRecord::getArchitectureStyleJson, keyword)
                    .or().like(DatasetImageRecord::getRelativePath, keyword));
        }
        return wrapper;
    }

    private Map<String, Long> aggregateBy(List<DatasetImageRecord> records, java.util.function.Function<DatasetImageRecord, String> extractor) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (DatasetImageRecord record : records) {
            String key = extractor.apply(record);
            if (!StringUtils.hasText(key)) {
                key = "未知";
            }
            counts.merge(key, 1L, Long::sum);
        }
        return counts;
    }

    private List<Map<String, Object>> toCountItems(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .toList();
    }

    private Object parseJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }

        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
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

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private static class RegionAccumulator {
        private long count;
        private long manualReviewCount;
        private double confidenceTotal;
        private long confidenceCount;
    }

    private static class ColorAccumulator {
        private long imageCount;
        private double totalRatio;
    }
}
