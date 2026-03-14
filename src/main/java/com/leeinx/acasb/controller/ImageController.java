package com.leeinx.acasb.controller;

import com.leeinx.acasb.PredictionRequest;
import com.leeinx.acasb.dto.AiAnalyzeResult;
import com.leeinx.acasb.dto.AnalyzeRequest;
import com.leeinx.acasb.dto.ImageAnalysisResult;
import com.leeinx.acasb.dto.ImageFeatures;
import com.leeinx.acasb.service.OpenAiCompatibleBuildingAnalysisService;
import com.leeinx.acasb.service.PythonAnalysisClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ImageController {
    private final PythonAnalysisClient pythonAnalysisClient;
    private final OpenAiCompatibleBuildingAnalysisService openAiCompatibleBuildingAnalysisService;

    public ImageController(
            PythonAnalysisClient pythonAnalysisClient,
            OpenAiCompatibleBuildingAnalysisService openAiCompatibleBuildingAnalysisService) {
        this.pythonAnalysisClient = pythonAnalysisClient;
        this.openAiCompatibleBuildingAnalysisService = openAiCompatibleBuildingAnalysisService;
    }

    @Value("${app.temp-folder:./temp}")
    private String tempFolder;

    @PostMapping("/predict")
    public ResponseEntity<ImageAnalysisResult> predict(@RequestBody PredictionRequest request) {
        try {
            ImageAnalysisResult result = pythonAnalysisClient.predict(request.getImage_path());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ImageAnalysisResult errorResult = new ImageAnalysisResult();
            errorResult.setSuccess(false);
            errorResult.setMessage("{\"error\":\"" + e.getMessage() + "\"}");
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageFeatures> analyzeImageForm(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image_path", required = false) String imagePath,
            @RequestParam(value = "enable_ai", required = false) Boolean enableAi) {
        try {
            if (file != null && !file.isEmpty()) {
                return ResponseEntity.ok(analyzeUploadedFile(file, enableAi));
            }

            return ResponseEntity.ok(analyzeByPath(imagePath, enableAi));
        } catch (Exception e) {
            ImageFeatures result = new ImageFeatures();
            result.setSuccess(false);
            result.setMessage("分析失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImageFeatures> analyzeImageJson(@RequestBody AnalyzeRequest request) {
        try {
            return ResponseEntity.ok(analyzeByPath(request.getImagePath(), request.getEnableAi()));
        } catch (Exception e) {
            ImageFeatures result = new ImageFeatures();
            result.setSuccess(false);
            result.setMessage("分析失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/health")
    public String health() {
        return "Java Backend is running!";
    }

    @GetMapping("/test")
    public String test() {
        return "Image API is working!";
    }

    private ImageFeatures analyzeUploadedFile(MultipartFile file, Boolean enableAi) throws IOException {
        Path tempDir = Paths.get(tempFolder).toAbsolutePath().normalize();
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        String uniqueFilename = UUID.randomUUID() + resolveFileExtension(file.getOriginalFilename());
        Path filePath = tempDir.resolve(uniqueFilename);

        try {
            file.transferTo(filePath.toFile());
            return analyzeByPath(filePath.toAbsolutePath().toString(), enableAi);
        } finally {
            Files.deleteIfExists(filePath);
        }
    }

    private ImageFeatures analyzeByPath(String imagePath, Boolean enableAi) {
        ImageFeatures result = new ImageFeatures();
        if (!StringUtils.hasText(imagePath)) {
            result.setSuccess(false);
            result.setMessage("请提供文件或图片路径");
            return result;
        }

        ImageFeatures pythonResult = pythonAnalysisClient.analyze(imagePath);
        if (pythonResult == null) {
            result.setSuccess(false);
            result.setMessage("Python服务返回空结果");
            return result;
        }

        result = pythonResult;
        if (!StringUtils.hasText(result.getMessage())) {
            result.setMessage("图像分析完成");
        }

        if (result.isSuccess() && shouldEnableAi(enableAi)) {
            AiAnalyzeResult aiAnalyzeResult = openAiCompatibleBuildingAnalysisService.analyze(Paths.get(imagePath));
            if (aiAnalyzeResult.isSuccess()) {
                result.setAiAnalyze(aiAnalyzeResult.getContent());
            } else {
                result.setMessage(result.getMessage() + "；AI解析失败: " + aiAnalyzeResult.getError());
            }
        }

        return result;
    }

    private boolean shouldEnableAi(Boolean enableAi) {
        return enableAi != null ? enableAi : openAiCompatibleBuildingAnalysisService.isEnabledByDefault();
    }

    private String resolveFileExtension(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        return StringUtils.hasText(extension) ? "." + extension : ".jpg";
    }
}
