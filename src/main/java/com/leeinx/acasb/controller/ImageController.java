package com.leeinx.acasb.controller;

import com.leeinx.acasb.PredictionRequest;
import com.leeinx.acasb.dto.ImageAnalysisResult;
import com.leeinx.acasb.dto.ImageFeatures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ImageController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.temp-folder}")
    private String tempFolder;

    @PostMapping("/predict")
    public ResponseEntity<ImageAnalysisResult> predict(@RequestBody PredictionRequest request) {
        try {
            String pythonUrl = "http://localhost:5000/predict";
            ImageAnalysisResult result = restTemplate.postForObject(pythonUrl, request, ImageAnalysisResult.class);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ImageAnalysisResult errorResult = new ImageAnalysisResult();
            errorResult.setSuccess(false);
            errorResult.setMessage("{\"error\":\"" + e.getMessage() + "\"}");
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    @PostMapping("/analyze")
    public ResponseEntity<ImageFeatures> analyzeImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image_path", required = false) String imagePath) {
        ImageFeatures result = new ImageFeatures();
        
        try {
            if (file != null && !file.isEmpty()) {
                String originalFilename = file.getOriginalFilename();
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
                
                Path tempDir = Paths.get(tempFolder).toAbsolutePath();
                if (!Files.exists(tempDir)) {
                    Files.createDirectories(tempDir);
                }
                
                Path filePath = tempDir.resolve(uniqueFilename);
                file.transferTo(filePath.toFile());
                
                String absolutePath = filePath.toAbsolutePath().toString();
                
                Map<String, String> request = new HashMap<>();
                request.put("image_path", absolutePath);
                
                String pythonUrl = "http://localhost:5000/analyze";
                System.out.println("调用 Python 服务: " + pythonUrl);
                System.out.println("请求参数: " + request);
                System.out.println("文件是否存在: " + Files.exists(filePath));
                System.out.println("文件绝对路径: " + absolutePath);
                
                ImageFeatures pythonResult = restTemplate.postForObject(pythonUrl, request, ImageFeatures.class);
                
                System.out.println("Python 服务返回: " + pythonResult);
                
                if (pythonResult != null) {
                    result = pythonResult;
                    result.setSuccess(true);
                    result.setMessage("图像分析完成");
                } else {
                    result.setSuccess(false);
                    result.setMessage("Python服务返回空结果");
                }
                
                Files.deleteIfExists(filePath);
                
            } else if (imagePath != null && !imagePath.isEmpty()) {
                Map<String, String> request = new HashMap<>();
                request.put("image_path", imagePath);
                
                String pythonUrl = "http://localhost:5000/analyze";
                System.out.println("调用 Python 服务: " + pythonUrl);
                System.out.println("请求参数: " + request);
                
                ImageFeatures pythonResult = restTemplate.postForObject(pythonUrl, request, ImageFeatures.class);
                
                System.out.println("Python 服务返回: " + pythonResult);
                
                if (pythonResult != null) {
                    result = pythonResult;
                    result.setSuccess(true);
                    result.setMessage("图像分析完成");
                } else {
                    result.setSuccess(false);
                    result.setMessage("Python服务返回空结果");
                }
                
            } else {
                result.setSuccess(false);
                result.setMessage("请提供文件或图片路径");
            }
            
        } catch (IOException e) {
            result.setSuccess(false);
            result.setMessage("文件处理失败: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("分析失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public String health() {
        return "Java Backend is running!";
    }

    @GetMapping("/test")
    public String test() {
        return "Image API is working!";
    }
}