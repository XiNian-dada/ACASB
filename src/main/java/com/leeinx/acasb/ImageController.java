package com.leeinx.acasb;

import com.leeinx.acasb.dto.ImageAnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/image")
@CrossOrigin(origins = "*")
public class ImageController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.temp-folder}")
    private String tempFolder;

    @PostMapping("/analyze")
    public ResponseEntity<ImageAnalysisResult> analyzeImage(@RequestParam("file") MultipartFile file) {
        ImageAnalysisResult result = new ImageAnalysisResult();
        
        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            Path tempDir = Paths.get(tempFolder);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            
            Path filePath = tempDir.resolve(uniqueFilename);
            file.transferTo(filePath.toFile());
            
            Map<String, String> request = new HashMap<>();
            request.put("image_path", filePath.toString());
            
            String pythonUrl = "http://localhost:5000/predict";
            ImageAnalysisResult pythonResult = restTemplate.postForObject(pythonUrl, request, ImageAnalysisResult.class);
            
            if (pythonResult != null) {
                result = pythonResult;
                result.setSuccess(true);
                result.setMessage("图像分析完成");
            } else {
                result.setSuccess(false);
                result.setMessage("Python服务返回空结果");
            }
            
            Files.deleteIfExists(filePath);
            
        } catch (IOException e) {
            result.setSuccess(false);
            result.setMessage("文件处理失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/test")
    public String test() {
        return "Image API is working!";
    }
}