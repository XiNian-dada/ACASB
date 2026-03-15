package com.leeinx.acasb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leeinx.acasb.dto.DatasetColorDistributionItem;
import com.leeinx.acasb.entity.DatasetImageRecord;
import com.leeinx.acasb.mapper.DatasetImageRecordMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatasetImageRecordService extends ServiceImpl<DatasetImageRecordMapper, DatasetImageRecord> {
    private final ObjectMapper objectMapper;

    @Value("${app.dataset-storage-folder:./dataset-storage}")
    private String datasetStorageFolder;

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

    public DatasetImageRecord getRecordByGroupAndImageIndex(String datasetName, String groupName, Integer imageIndex) {
        if (!StringUtils.hasText(datasetName) || !StringUtils.hasText(groupName) || imageIndex == null || imageIndex <= 0) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<DatasetImageRecord>()
                .eq(DatasetImageRecord::getDatasetName, datasetName)
                .eq(DatasetImageRecord::getGroupName, groupName)
                .eq(DatasetImageRecord::getImageIndex, imageIndex)
                .last("LIMIT 1"));
    }

    public DatasetImageRecord getRecordByGroupAndFileName(String datasetName, String groupName, String fileName) {
        if (!StringUtils.hasText(datasetName) || !StringUtils.hasText(groupName) || !StringUtils.hasText(fileName)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<DatasetImageRecord>()
                .eq(DatasetImageRecord::getDatasetName, datasetName)
                .eq(DatasetImageRecord::getGroupName, groupName)
                .eq(DatasetImageRecord::getFileName, fileName)
                .last("LIMIT 1"));
    }

    public DatasetImageRecord getRecordByDatasetAndRelativePath(String datasetName, String relativePath) {
        if (!StringUtils.hasText(datasetName) || !StringUtils.hasText(relativePath)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<DatasetImageRecord>()
                .eq(DatasetImageRecord::getDatasetName, datasetName)
                .eq(DatasetImageRecord::getRelativePath, relativePath)
                .last("LIMIT 1"));
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
        item.put("buildingColorDistribution", parseAndEnrichColorDistribution(record.getBuildingColorDistributionJson()));
        item.put("architectureStyle", parseJson(record.getArchitectureStyleJson()));
        item.put("sceneDescription", record.getSceneDescription());
        item.put("reasoning", parseJson(record.getReasoningJson()));
        item.put("needsManualReview", record.getNeedsManualReview());
        item.put("fileName", record.getFileName());
        item.put("relativePath", record.getRelativePath());
        item.put("originalRelativePath", record.getOriginalRelativePath());
        item.put("storedImagePath", resolveCurrentStoredImagePath(record));
        item.put("sourceImagePath", resolveExistingSourceImagePath(record));
        item.put("imageUrl", buildImageUrl(record));
        item.put("imageAvailable", hasStoredImage(record));
        item.put("analysisStatus", record.getAnalysisStatus());
        item.put("errorMessage", record.getErrorMessage());
        item.put("imageIndex", record.getImageIndex());
        item.put("rawMetadata", parseRawMetadataWithHex(record.getRawMetadataJson()));
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
            for (DatasetColorDistributionItem item : parseAndEnrichColorDistribution(record.getBuildingColorDistributionJson())) {
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
                    item.put("hex", ColorPaletteResolver.resolve(entry.getKey()).hex());
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

    private String resolveCurrentStoredImagePath(DatasetImageRecord record) {
        Path resolved = resolveCurrentStoredImage(record);
        return resolved == null ? null : resolved.toString();
    }

    private String resolveExistingSourceImagePath(DatasetImageRecord record) {
        if (!StringUtils.hasText(record.getSourceImagePath())) {
            return null;
        }
        try {
            Path sourcePath = Paths.get(record.getSourceImagePath()).toAbsolutePath().normalize();
            return Files.exists(sourcePath) ? sourcePath.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean hasStoredImage(DatasetImageRecord record) {
        Path resolved = resolveCurrentStoredImage(record);
        return resolved != null && Files.exists(resolved);
    }

    private String buildImageUrl(DatasetImageRecord record) {
        if (!StringUtils.hasText(record.getDatasetName()) || !StringUtils.hasText(record.getRelativePath())) {
            return null;
        }
        return "/media/dataset/"
                + UriUtils.encodePathSegment(record.getDatasetName(), StandardCharsets.UTF_8)
                + "/"
                + encodeRelativePath(record.getRelativePath());
    }

    private String encodeRelativePath(String relativePath) {
        return java.util.Arrays.stream(relativePath.replace("\\", "/").split("/"))
                .filter(StringUtils::hasText)
                .map(part -> UriUtils.encodePathSegment(part, StandardCharsets.UTF_8))
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
    }

    private Path resolveCurrentStoredImage(DatasetImageRecord record) {
        if (!StringUtils.hasText(record.getDatasetName()) || !StringUtils.hasText(record.getRelativePath())) {
            return null;
        }

        Path storageRoot = Paths.get(datasetStorageFolder).toAbsolutePath().normalize();
        Path datasetRoot = storageRoot.resolve(sanitizePathSegment(record.getDatasetName())).normalize();

        Path relativePath;
        try {
            relativePath = Paths.get(record.getRelativePath()).normalize();
        } catch (Exception e) {
            relativePath = Paths.get(sanitizePathSegment(record.getFileName()));
        }

        if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
            relativePath = Paths.get(sanitizePathSegment(record.getFileName()));
        }

        Path resolved = datasetRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(datasetRoot)) {
            resolved = datasetRoot.resolve(sanitizePathSegment(record.getFileName())).normalize();
        }
        return resolved;
    }

    public Path resolveMediaPath(String datasetName, String relativePath) {
        DatasetImageRecord directRecord = new DatasetImageRecord();
        directRecord.setDatasetName(datasetName);
        directRecord.setRelativePath(relativePath);
        directRecord.setFileName(extractFileName(relativePath));

        Path directPath = resolveCurrentStoredImage(directRecord);
        if (directPath != null && Files.exists(directPath)) {
            return directPath;
        }

        DatasetImageRecord record = getRecordByDatasetAndRelativePath(datasetName, relativePath);
        if (record == null) {
            return null;
        }

        Path currentStoredPath = resolveCurrentStoredImage(record);
        if (currentStoredPath != null && Files.exists(currentStoredPath)) {
            return currentStoredPath;
        }

        Path legacyOriginalPath = resolveOriginalRelativeImage(record);
        if (legacyOriginalPath != null && Files.exists(legacyOriginalPath)) {
            return legacyOriginalPath;
        }

        if (StringUtils.hasText(record.getStoredImagePath())) {
            try {
                Path storedImagePath = Paths.get(record.getStoredImagePath()).toAbsolutePath().normalize();
                if (Files.exists(storedImagePath)) {
                    return storedImagePath;
                }
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }

    private Path resolveOriginalRelativeImage(DatasetImageRecord record) {
        if (!StringUtils.hasText(record.getDatasetName()) || !StringUtils.hasText(record.getOriginalRelativePath())) {
            return null;
        }

        Path storageRoot = Paths.get(datasetStorageFolder).toAbsolutePath().normalize();
        Path datasetRoot = storageRoot.resolve(sanitizePathSegment(record.getDatasetName())).normalize();
        try {
            Path originalPath = Paths.get(record.getOriginalRelativePath()).normalize();
            if (originalPath.isAbsolute() || originalPath.startsWith("..")) {
                return null;
            }
            Path resolved = datasetRoot.resolve(originalPath).normalize();
            if (!resolved.startsWith(datasetRoot)) {
                return null;
            }
            return resolved;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractFileName(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return null;
        }
        int index = relativePath.replace("\\", "/").lastIndexOf('/');
        return index >= 0 ? relativePath.substring(index + 1) : relativePath;
    }

    private String sanitizePathSegment(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        String sanitized = value.trim().replace("\\", "/").replaceAll("/+", "/");
        sanitized = sanitized.replaceAll("[^A-Za-z0-9._/-]", "-");
        sanitized = sanitized.replaceAll("-{2,}", "-");
        sanitized = sanitized.replaceAll("^[-/]+|[-/]+$", "");
        return sanitized.isEmpty() ? "unknown" : sanitized;
    }

    private Object parseRawMetadataWithHex(String json) {
        Object rawMetadata = parseJson(json);
        if (rawMetadata instanceof Map<?, ?> metadataMap) {
            Object distribution = metadataMap.get("building_color_distribution");
            if (distribution instanceof List<?> distributionItems) {
                enrichDistributionMaps(distributionItems);
            }
        }
        return rawMetadata;
    }

    private void enrichDistributionMaps(List<?> distributionItems) {
        for (Object item : distributionItems) {
            if (!(item instanceof Map<?, ?> rawItem)) {
                continue;
            }
            Object colorValue = rawItem.get("color");
            if (!(colorValue instanceof String color) || !StringUtils.hasText(color)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> mutableItem = (Map<String, Object>) rawItem;
            mutableItem.put("hex", ColorPaletteResolver.resolve(color).hex());
        }
    }

    private List<DatasetColorDistributionItem> parseAndEnrichColorDistribution(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }

        try {
            List<DatasetColorDistributionItem> items = objectMapper.readValue(json, new TypeReference<List<DatasetColorDistributionItem>>() {
            });
            ColorPaletteResolver.enrichDistributionItems(items);
            return items;
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
