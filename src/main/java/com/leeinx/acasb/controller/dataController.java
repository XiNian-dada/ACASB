package com.leeinx.acasb.controller;

import com.leeinx.acasb.config.LocalModelProperties;
import com.leeinx.acasb.dto.AiAnalyzeResult;
import com.leeinx.acasb.dto.BatchUploadResult;
import com.leeinx.acasb.dto.ImageAnalysisResult;
import com.leeinx.acasb.dto.ImageFeatures;
import com.leeinx.acasb.entity.BuildingAnalysis;
import com.leeinx.acasb.entity.BuildingType;
import com.leeinx.acasb.service.BuildingAnalysisService;
import com.leeinx.acasb.service.BuildingTypeService;
import com.leeinx.acasb.service.OpenAiCompatibleBuildingAnalysisService;
import com.leeinx.acasb.service.PythonAnalysisClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/data")
@CrossOrigin(origins = "*")
public class dataController {
    private final PythonAnalysisClient pythonAnalysisClient;
    private final BuildingAnalysisService buildingAnalysisService;
    private final BuildingTypeService buildingTypeService;
    private final OpenAiCompatibleBuildingAnalysisService openAiCompatibleBuildingAnalysisService;
    private final LocalModelProperties localModelProperties;

    public dataController(
            PythonAnalysisClient pythonAnalysisClient,
            BuildingAnalysisService buildingAnalysisService,
            BuildingTypeService buildingTypeService,
            OpenAiCompatibleBuildingAnalysisService openAiCompatibleBuildingAnalysisService,
            LocalModelProperties localModelProperties) {
        this.pythonAnalysisClient = pythonAnalysisClient;
        this.buildingAnalysisService = buildingAnalysisService;
        this.buildingTypeService = buildingTypeService;
        this.openAiCompatibleBuildingAnalysisService = openAiCompatibleBuildingAnalysisService;
        this.localModelProperties = localModelProperties;
    }

    @Value("${app.storage-folder:./uploads}")
    private String storageFolder;

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "enable_ai", required = false) Boolean enableAi) {
        Map<String, Object> result = new HashMap<>();

        try {
            Path storedPath = storeUpload(file);
            ProcessedImageData processedImageData = processStoredImage(storedPath, enableAi);

            result.put("success", true);
            result.put("message", "数据添加成功");
            result.put("analysisId", processedImageData.savedAnalysis().getId());
            result.put("typeId", processedImageData.savedType() != null ? processedImageData.savedType().getId() : null);
            result.put("storedImagePath", storedPath.toString());
            result.put("ai_analyze", processedImageData.imageFeatures().getAiAnalyze());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "处理失败: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/analysis/{id}")
    public ResponseEntity<Map<String, Object>> getAnalysisById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        try {
            BuildingAnalysis analysis = buildingAnalysisService.getAnalysisById(id);
            if (analysis != null) {
                result.put("success", true);
                result.put("data", buildAnalysisResponseItem(analysis));
            } else {
                result.put("success", false);
                result.put("message", "未找到编号为 " + id + " 的分析信息");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/type/{id}")
    public ResponseEntity<Map<String, Object>> getTypeById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        try {
            BuildingType buildingType = buildingTypeService.getTypeById(id);
            if (buildingType != null) {
                result.put("success", true);
                result.put("data", buildingType);
            } else {
                result.put("success", false);
                result.put("message", "未找到编号为 " + id + " 的建筑类型信息");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchUploadResult> batchUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "enable_ai", required = false) Boolean enableAi) {
        BatchUploadResult result = new BatchUploadResult();
        int totalCount = files == null ? 0 : files.length;
        result.setTotalCount(totalCount);
        result.setSuccessCount(0);
        result.setFailureCount(0);
        result.setItems(new ArrayList<>());

        if (totalCount == 0) {
            return ResponseEntity.ok(result);
        }

        for (MultipartFile file : files) {
            BatchUploadResult.UploadItemResult itemResult = new BatchUploadResult.UploadItemResult();
            itemResult.setFileName(file.getOriginalFilename());

            try {
                Path storedPath = storeUpload(file);
                ProcessedImageData processedImageData = processStoredImage(storedPath, enableAi);

                itemResult.setAnalysisId(processedImageData.savedAnalysis().getId());
                itemResult.setTypeId(processedImageData.savedType() != null ? processedImageData.savedType().getId() : null);
                itemResult.setSuccess(true);
                itemResult.setMessage("上传成功");
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                itemResult.setSuccess(false);
                itemResult.setMessage("处理失败: " + e.getMessage());
                result.setFailureCount(result.getFailureCount() + 1);
            }

            result.getItems().add(itemResult);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAnalysesByField(
            @RequestParam(required = false) String field,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String prediction) {
        Map<String, Object> result = new HashMap<>();

        try {
            int normalizedLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 200);
            int fetchLimit = StringUtils.hasText(prediction) ? 200 : normalizedLimit;
            List<BuildingAnalysis> analyses = buildingAnalysisService.getAnalysesByField(field, order, fetchLimit);

            List<Map<String, Object>> resultData = new ArrayList<>();
            for (BuildingAnalysis analysis : analyses) {
                BuildingType buildingType = buildingTypeService.getTypeByAnalysisId(analysis.getId());
                if (StringUtils.hasText(prediction)
                        && (buildingType == null || !prediction.equalsIgnoreCase(buildingType.getPrediction()))) {
                    continue;
                }

                resultData.add(buildAnalysisResponseItem(analysis, buildingType));
                if (resultData.size() >= normalizedLimit) {
                    break;
                }
            }

            result.put("success", true);
            result.put("data", resultData);
            result.put("count", resultData.size());
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    private ProcessedImageData processStoredImage(Path storedPath, Boolean enableAi) {
        ImageFeatures imageFeatures = pythonAnalysisClient.analyze(storedPath.toString());
        if (imageFeatures == null || !imageFeatures.isSuccess()) {
            throw new IllegalStateException("图像分析失败");
        }

        if (shouldEnableAi(enableAi)) {
            AiAnalyzeResult aiAnalyzeResult = openAiCompatibleBuildingAnalysisService.analyze(storedPath);
            if (aiAnalyzeResult.isSuccess()) {
                imageFeatures.setAiAnalyze(aiAnalyzeResult.getContent());
            }
        }

        BuildingAnalysis analysis = buildAnalysisEntity(storedPath.toString(), imageFeatures);
        BuildingAnalysis savedAnalysis = buildingAnalysisService.saveAnalysis(analysis);

        BuildingType savedType = null;
        if (localModelProperties.isPredictionEnabled()) {
            ImageAnalysisResult predictionResult = pythonAnalysisClient.predict(storedPath.toString());
            if (predictionResult != null && predictionResult.isSuccess()) {
                BuildingType buildingType = new BuildingType();
                buildingType.setImagePath(storedPath.toString());
                buildingType.setPrediction(predictionResult.getPrediction());
                buildingType.setConfidence(predictionResult.getConfidence());
                buildingType.setAnalysisId(savedAnalysis.getId());
                savedType = buildingTypeService.saveType(buildingType);
            }
        }

        return new ProcessedImageData(imageFeatures, savedAnalysis, savedType);
    }

    private BuildingAnalysis buildAnalysisEntity(String imagePath, ImageFeatures imageFeatures) {
        BuildingAnalysis analysis = new BuildingAnalysis();
        analysis.setImagePath(imagePath);
        analysis.setRatioYellow(imageFeatures.getRatioYellow());
        analysis.setRatioRed1(imageFeatures.getRatioRed1());
        analysis.setRatioRed2(imageFeatures.getRatioRed2());
        analysis.setRatioBlue(imageFeatures.getRatioBlue());
        analysis.setRatioGreen(imageFeatures.getRatioGreen());
        analysis.setRatioGrayWhite(imageFeatures.getRatioGrayWhite());
        analysis.setRatioBlack(imageFeatures.getRatioBlack());
        analysis.setHMean(imageFeatures.getHMean());
        analysis.setHStd(imageFeatures.getHStd());
        analysis.setSMean(imageFeatures.getSMean());
        analysis.setSStd(imageFeatures.getSStd());
        analysis.setVMean(imageFeatures.getVMean());
        analysis.setVStd(imageFeatures.getVStd());
        analysis.setEdgeDensity(imageFeatures.getEdgeDensity());
        analysis.setEntropy(imageFeatures.getEntropy());
        analysis.setContrast(imageFeatures.getContrast());
        analysis.setDissimilarity(imageFeatures.getDissimilarity());
        analysis.setHomogeneity(imageFeatures.getHomogeneity());
        analysis.setAsm(imageFeatures.getAsm());
        analysis.setRoyalRatio(imageFeatures.getRoyalRatio());
        analysis.setAiAnalyze(imageFeatures.getAiAnalyze());

        return analysis;
    }

    private Path storeUpload(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = resolveFileExtension(originalFilename);

        Path storageDir = Paths.get(storageFolder).toAbsolutePath().normalize();
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        Path storedPath = storageDir.resolve(UUID.randomUUID() + extension);
        file.transferTo(storedPath.toFile());
        return storedPath;
    }

    private String resolveFileExtension(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        return StringUtils.hasText(extension) ? "." + extension : ".jpg";
    }

    private boolean shouldEnableAi(Boolean enableAi) {
        return enableAi != null ? enableAi : openAiCompatibleBuildingAnalysisService.isEnabledByDefault();
    }

    private Map<String, Object> buildAnalysisResponseItem(BuildingAnalysis analysis) {
        return buildAnalysisResponseItem(analysis, buildingTypeService.getTypeByAnalysisId(analysis.getId()));
    }

    private Map<String, Object> buildAnalysisResponseItem(BuildingAnalysis analysis, BuildingType buildingType) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", analysis.getId());
        item.put("imagePath", analysis.getImagePath());
        item.put("ratioYellow", analysis.getRatioYellow());
        item.put("ratioRed1", analysis.getRatioRed1());
        item.put("ratioRed2", analysis.getRatioRed2());
        item.put("ratioBlue", analysis.getRatioBlue());
        item.put("ratioGreen", analysis.getRatioGreen());
        item.put("ratioGrayWhite", analysis.getRatioGrayWhite());
        item.put("ratioBlack", analysis.getRatioBlack());
        item.put("hMean", analysis.getHMean());
        item.put("hStd", analysis.getHStd());
        item.put("sMean", analysis.getSMean());
        item.put("sStd", analysis.getSStd());
        item.put("vMean", analysis.getVMean());
        item.put("vStd", analysis.getVStd());
        item.put("edgeDensity", analysis.getEdgeDensity());
        item.put("entropy", analysis.getEntropy());
        item.put("contrast", analysis.getContrast());
        item.put("dissimilarity", analysis.getDissimilarity());
        item.put("homogeneity", analysis.getHomogeneity());
        item.put("asm", analysis.getAsm());
        item.put("royalRatio", analysis.getRoyalRatio());
        item.put("ai_analyze", analysis.getAiAnalyze());
        item.put("createTime", analysis.getCreateTime());
        item.put("updateTime", analysis.getUpdateTime());

        if (buildingType != null) {
            item.put("prediction", buildingType.getPrediction());
            item.put("confidence", buildingType.getConfidence());
        } else {
            item.put("prediction", null);
            item.put("confidence", null);
        }

        return item;
    }

    private record ProcessedImageData(
            ImageFeatures imageFeatures,
            BuildingAnalysis savedAnalysis,
            BuildingType savedType) {
    }
}
