package com.leeinx.acasb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leeinx.acasb.dto.DatasetImageMetadata;
import com.leeinx.acasb.dto.DatasetImportResult;
import com.leeinx.acasb.dto.DatasetManifest;
import com.leeinx.acasb.entity.DatasetImageRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatasetImportService {
    private static final Map<String, String> PATH_ALIAS = new LinkedHashMap<>();

    static {
        PATH_ALIAS.put("计算机设计大赛", "competition-dataset");
        PATH_ALIAS.put("华北地区", "north-china");
        PATH_ALIAS.put("华东地区", "east-china");
        PATH_ALIAS.put("中南地区", "south-central");
        PATH_ALIAS.put("西南地区", "southwest");
        PATH_ALIAS.put("东北地区", "northeast");
    }

    private final ObjectMapper objectMapper;
    private final DatasetImageRecordService datasetImageRecordService;

    @Value("${app.dataset-storage-folder:./dataset-storage}")
    private String datasetStorageFolder;

    public DatasetImportService(
            ObjectMapper objectMapper,
            DatasetImageRecordService datasetImageRecordService) {
        this.objectMapper = objectMapper;
        this.datasetImageRecordService = datasetImageRecordService;
    }

    public DatasetImportResult importDatasetFolder(String datasetPath, String datasetName, Boolean copyImages) throws IOException {
        Path datasetRoot = resolveDatasetRoot(datasetPath);
        String resolvedDatasetName = StringUtils.hasText(datasetName)
                ? datasetName
                : datasetRoot.getFileName().toString();
        boolean shouldCopyImages = copyImages == null || copyImages;

        DatasetImportResult result = new DatasetImportResult();
        result.setDatasetName(resolvedDatasetName);

        List<Path> manifests = Files.walk(datasetRoot, 2)
                .filter(path -> path.getFileName().toString().equalsIgnoreCase("analysis.json"))
                .sorted()
                .toList();

        result.setManifestsDiscovered(manifests.size());
        for (Path manifestPath : manifests) {
            importManifestFromPath(manifestPath, datasetRoot, resolvedDatasetName, shouldCopyImages, result);
        }

        return result;
    }

    public DatasetImportResult importManifestPath(String manifestPath, String datasetName, Boolean copyImages) throws IOException {
        Path resolvedManifestPath = Paths.get(manifestPath).toAbsolutePath().normalize();
        if (!Files.exists(resolvedManifestPath)) {
            throw new IOException("manifest 文件不存在: " + resolvedManifestPath);
        }

        Path datasetRoot = resolvedManifestPath.getParent() != null && resolvedManifestPath.getParent().getParent() != null
                ? resolvedManifestPath.getParent().getParent()
                : resolvedManifestPath.getParent();
        String resolvedDatasetName = StringUtils.hasText(datasetName)
                ? datasetName
                : datasetRoot.getFileName().toString();

        DatasetImportResult result = new DatasetImportResult();
        result.setDatasetName(resolvedDatasetName);
        result.setManifestsDiscovered(1);
        importManifestFromPath(resolvedManifestPath, datasetRoot, resolvedDatasetName, copyImages == null || copyImages, result);
        return result;
    }

    public DatasetImportResult importUploadedManifest(MultipartFile manifestFile, MultipartFile[] images, String datasetName) throws IOException {
        if (manifestFile == null || manifestFile.isEmpty()) {
            throw new IOException("manifest 文件不能为空");
        }

        DatasetManifest manifest = objectMapper.readValue(manifestFile.getInputStream(), DatasetManifest.class);
        String resolvedDatasetName = StringUtils.hasText(datasetName) ? datasetName : "uploaded-dataset";
        Map<String, MultipartFile> imageMap = new HashMap<>();
        if (images != null) {
            for (MultipartFile image : images) {
                imageMap.put(image.getOriginalFilename(), image);
            }
        }

        DatasetImportResult result = new DatasetImportResult();
        result.setDatasetName(resolvedDatasetName);
        result.setManifestsDiscovered(1);

        for (DatasetImageMetadata metadata : manifest.getImages()) {
            result.setRecordsProcessed(result.getRecordsProcessed() + 1);
            try {
                MultipartFile imageFile = imageMap.get(metadata.getFileName());
                if (imageFile == null || imageFile.isEmpty()) {
                    throw new IOException("找不到与 manifest 对应的图片文件: " + metadata.getFileName());
                }

                Path storedPath = storeUploadedImage(resolvedDatasetName, metadata.getRelativePath(), imageFile);
                DatasetImageRecord record = buildRecord(
                        manifest,
                        metadata,
                        resolvedDatasetName,
                        storedPath.toString(),
                        null
                );
                DatasetImageRecord saved = datasetImageRecordService.saveOrUpdateByDatasetAndRelativePath(record);
                if (saved.getCreateTime() != null && saved.getUpdateTime() != null && saved.getCreateTime().equals(saved.getUpdateTime())) {
                    result.setImportedCount(result.getImportedCount() + 1);
                } else {
                    result.setUpdatedCount(result.getUpdatedCount() + 1);
                }
                result.setCopiedImageCount(result.getCopiedImageCount() + 1);
            } catch (Exception e) {
                result.setFailedCount(result.getFailedCount() + 1);
                result.getErrors().add(metadata.getFileName() + ": " + e.getMessage());
            }
        }

        return result;
    }

    public Map<String, Object> uploadSingleRecord(
            MultipartFile imageFile,
            String metadataJson,
            String datasetName,
            String groupName,
            String groupRelativePath,
            String schemaVersion,
            String promptVersion,
            String generatedAt,
            String apiInterface,
            String model) throws IOException {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IOException("图片文件不能为空");
        }
        if (!StringUtils.hasText(metadataJson)) {
            throw new IOException("metadata 不能为空");
        }

        DatasetImageMetadata metadata = objectMapper.readValue(metadataJson, DatasetImageMetadata.class);
        DatasetManifest manifest = new DatasetManifest();
        manifest.setSchemaVersion(schemaVersion);
        manifest.setPromptVersion(promptVersion);
        manifest.setGeneratedAt(generatedAt);
        manifest.setApiInterface(apiInterface);
        manifest.setModel(model);
        manifest.setGroupName(groupName);
        manifest.setGroupRelativePath(groupRelativePath);

        String resolvedDatasetName = StringUtils.hasText(datasetName) ? datasetName : "manual-upload";
        Path storedPath = storeUploadedImage(resolvedDatasetName, metadata.getRelativePath(), imageFile);
        DatasetImageRecord record = buildRecord(manifest, metadata, resolvedDatasetName, storedPath.toString(), null);
        DatasetImageRecord saved = datasetImageRecordService.saveOrUpdateByDatasetAndRelativePath(record);

        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("datasetName", saved.getDatasetName());
        result.put("relativePath", saved.getRelativePath());
        result.put("storedImagePath", saved.getStoredImagePath());
        return result;
    }

    private void importManifestFromPath(
            Path manifestPath,
            Path datasetRoot,
            String datasetName,
            boolean copyImages,
            DatasetImportResult result) throws IOException {
        DatasetManifest manifest = objectMapper.readValue(manifestPath.toFile(), DatasetManifest.class);

        for (DatasetImageMetadata metadata : manifest.getImages()) {
            result.setRecordsProcessed(result.getRecordsProcessed() + 1);
            try {
                Path sourceImagePath = resolveSourceImagePath(datasetRoot, manifestPath.getParent(), metadata);
                Path storedImagePath = copyImages
                        ? copySourceImage(datasetName, metadata.getRelativePath(), sourceImagePath)
                        : sourceImagePath;

                DatasetImageRecord record = buildRecord(
                        manifest,
                        metadata,
                        datasetName,
                        storedImagePath == null ? null : storedImagePath.toString(),
                        sourceImagePath.toString()
                );
                DatasetImageRecord previous = datasetImageRecordService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DatasetImageRecord>()
                        .eq(DatasetImageRecord::getDatasetName, record.getDatasetName())
                        .eq(DatasetImageRecord::getRelativePath, record.getRelativePath())
                        .last("LIMIT 1"));
                datasetImageRecordService.saveOrUpdateByDatasetAndRelativePath(record);

                if (previous == null) {
                    result.setImportedCount(result.getImportedCount() + 1);
                } else {
                    result.setUpdatedCount(result.getUpdatedCount() + 1);
                }
                if (copyImages) {
                    result.setCopiedImageCount(result.getCopiedImageCount() + 1);
                }
            } catch (Exception e) {
                result.setFailedCount(result.getFailedCount() + 1);
                result.getErrors().add(manifestPath.getFileName() + " / " + metadata.getFileName() + ": " + e.getMessage());
            }
        }
    }

    private DatasetImageRecord buildRecord(
            DatasetManifest manifest,
            DatasetImageMetadata metadata,
            String datasetName,
            String storedImagePath,
            String sourceImagePath) throws IOException {
        enrichMetadataColors(metadata);

        DatasetImageRecord record = new DatasetImageRecord();
        record.setDatasetName(datasetName);
        record.setGroupName(manifest.getGroupName());
        record.setGroupRelativePath(normalizeGroupPath(manifest.getGroupRelativePath(), manifest.getGroupName()));
        record.setOriginalGroupRelativePath(manifest.getGroupRelativePath());
        record.setSchemaVersion(manifest.getSchemaVersion());
        record.setPromptVersion(manifest.getPromptVersion());
        record.setGeneratedAt(manifest.getGeneratedAt());
        record.setApiInterface(manifest.getApiInterface());
        record.setModel(manifest.getModel());
        record.setProvinceLevelRegion(metadata.getProvinceLevelRegion());
        record.setProvinceConfidence(metadata.getProvinceConfidence());
        record.setDynastyGuess(metadata.getDynastyGuess());
        record.setBuildingRank(metadata.getBuildingRank());
        record.setSceneType(metadata.getSceneType());
        record.setBuildingPresent(metadata.getBuildingPresent());
        record.setBuildingPrimaryColorsJson(writeJson(metadata.getBuildingPrimaryColors()));
        record.setBuildingColorDistributionJson(writeJson(metadata.getBuildingColorDistribution()));
        record.setArchitectureStyleJson(writeJson(metadata.getArchitectureStyle()));
        record.setSceneDescription(metadata.getSceneDescription());
        record.setReasoningJson(writeJson(metadata.getReasoning()));
        record.setNeedsManualReview(metadata.getNeedsManualReview());
        record.setFileName(metadata.getFileName());
        record.setRelativePath(normalizeRelativePath(manifest, metadata));
        record.setOriginalRelativePath(resolveOriginalRelativePath(manifest, metadata));
        record.setStoredImagePath(storedImagePath);
        record.setSourceImagePath(sourceImagePath);
        record.setAnalysisStatus(metadata.getAnalysisStatus());
        record.setErrorMessage(metadata.getErrorMessage());
        record.setImageIndex(metadata.getImageIndex());
        record.setRawMetadataJson(objectMapper.writeValueAsString(metadata));
        return record;
    }

    private void enrichMetadataColors(DatasetImageMetadata metadata) {
        if (metadata == null) {
            return;
        }
        ColorPaletteResolver.enrichDistributionItems(metadata.getBuildingColorDistribution());
    }

    private Path resolveDatasetRoot(String datasetPath) {
        Path resolvedPath;
        if (StringUtils.hasText(datasetPath)) {
            resolvedPath = Paths.get(datasetPath).toAbsolutePath().normalize();
        } else {
            resolvedPath = Paths.get("计算机设计大赛").toAbsolutePath().normalize();
        }
        return resolvedPath;
    }

    private Path resolveSourceImagePath(Path datasetRoot, Path manifestParent, DatasetImageMetadata metadata) throws IOException {
        if (StringUtils.hasText(metadata.getRelativePath())) {
            Path relativePath = Paths.get(metadata.getRelativePath()).normalize();
            if (relativePath.startsWith("..")) {
                throw new IOException("relative_path 非法: " + metadata.getRelativePath());
            }
            Path candidate = datasetRoot.resolve(relativePath).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        if (StringUtils.hasText(metadata.getFileName())) {
            Path candidate = manifestParent.resolve(metadata.getFileName()).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        throw new IOException("找不到图片文件: " + metadata.getFileName());
    }

    private Path copySourceImage(String datasetName, String relativePath, Path sourceImagePath) throws IOException {
        Path destination = resolveStoragePath(datasetName, relativePath, sourceImagePath.getFileName().toString());
        Files.createDirectories(destination.getParent());
        Files.copy(sourceImagePath, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    private Path storeUploadedImage(String datasetName, String relativePath, MultipartFile imageFile) throws IOException {
        String originalFilename = imageFile.getOriginalFilename();
        Path destination = resolveStoragePath(datasetName, relativePath, originalFilename);
        Files.createDirectories(destination.getParent());
        try (InputStream inputStream = imageFile.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return destination;
    }

    private Path resolveStoragePath(String datasetName, String relativePath, String fallbackFileName) {
        Path storageRoot = Paths.get(datasetStorageFolder).toAbsolutePath().normalize();
        Path datasetRoot = storageRoot.resolve(sanitizePathSegment(datasetName));
        String resolvedRelativePath = StringUtils.hasText(relativePath) ? relativePath : fallbackFileName;
        Path normalizedRelativePath = Paths.get(resolvedRelativePath).normalize();
        if (normalizedRelativePath.isAbsolute() || normalizedRelativePath.startsWith("..")) {
            normalizedRelativePath = Paths.get(sanitizePathSegment(fallbackFileName));
        }

        Path destination = datasetRoot.resolve(normalizedRelativePath).normalize();
        if (!destination.startsWith(datasetRoot)) {
            destination = datasetRoot.resolve(sanitizePathSegment(fallbackFileName)).normalize();
        }
        return destination;
    }

    private String resolveOriginalRelativePath(DatasetManifest manifest, DatasetImageMetadata metadata) {
        if (StringUtils.hasText(metadata.getRelativePath())) {
            return metadata.getRelativePath();
        }
        if (StringUtils.hasText(manifest.getGroupRelativePath()) && StringUtils.hasText(metadata.getFileName())) {
            return Paths.get(manifest.getGroupRelativePath(), metadata.getFileName()).toString();
        }
        return metadata.getFileName();
    }

    private String normalizeRelativePath(DatasetManifest manifest, DatasetImageMetadata metadata) {
        String originalRelativePath = resolveOriginalRelativePath(manifest, metadata);
        if (!StringUtils.hasText(originalRelativePath)) {
            return sanitizeFileName(metadata.getFileName());
        }

        Path relativePath = Paths.get(originalRelativePath).normalize();
        if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
            return sanitizeFileName(metadata.getFileName());
        }

        List<String> normalizedSegments = new java.util.ArrayList<>();
        for (Path segment : relativePath) {
            normalizedSegments.add(normalizePathToken(segment.toString()));
        }
        return String.join("/", normalizedSegments);
    }

    private String normalizeGroupPath(String groupRelativePath, String groupName) {
        if (StringUtils.hasText(groupRelativePath)) {
            return normalizeRelativeSegments(groupRelativePath);
        }
        if (StringUtils.hasText(groupName)) {
            return normalizePathToken(groupName);
        }
        return "unknown-group";
    }

    private String normalizeRelativeSegments(String input) {
        Path path = Paths.get(input).normalize();
        if (path.isAbsolute() || path.startsWith("..")) {
            return normalizePathToken(input);
        }
        List<String> normalizedSegments = new java.util.ArrayList<>();
        for (Path segment : path) {
            normalizedSegments.add(normalizePathToken(segment.toString()));
        }
        return String.join("/", normalizedSegments);
    }

    private String writeJson(Object value) throws IOException {
        return value == null ? null : objectMapper.writeValueAsString(value);
    }

    private String sanitizePathSegment(String input) {
        if (!StringUtils.hasText(input)) {
            return "unknown";
        }
        return normalizePathToken(input);
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "unknown.jpg";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return normalizePathToken(fileName);
        }

        String baseName = fileName.substring(0, dotIndex);
        String extension = fileName.substring(dotIndex).toLowerCase();
        return normalizePathToken(baseName) + extension;
    }

    private String normalizePathToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "unknown";
        }
        String alias = PATH_ALIAS.get(token);
        if (alias != null) {
            return alias;
        }

        StringBuilder builder = new StringBuilder();
        for (char current : token.toCharArray()) {
            if ((current >= 'a' && current <= 'z') || (current >= 'A' && current <= 'Z') || (current >= '0' && current <= '9')) {
                builder.append(Character.toLowerCase(current));
            } else if (current == '.' || current == '_' || current == '-') {
                builder.append(current);
            } else {
                builder.append('u').append(Integer.toHexString(current));
            }
        }

        String normalized = builder.toString().replaceAll("-{2,}", "-");
        return StringUtils.hasText(normalized) ? normalized : "unknown";
    }
}
