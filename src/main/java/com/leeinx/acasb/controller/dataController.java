package com.leeinx.acasb.controller;

import com.leeinx.acasb.dto.ImageFeatures;
import com.leeinx.acasb.dto.ImageAnalysisResult;
import com.leeinx.acasb.entity.BuildingAnalysis;
import com.leeinx.acasb.entity.BuildingType;
import com.leeinx.acasb.service.BuildingAnalysisService;
import com.leeinx.acasb.service.BuildingTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/data")
@CrossOrigin(origins = "*")
public class dataController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private BuildingAnalysisService buildingAnalysisService;

    @Autowired
    private BuildingTypeService buildingTypeService;

    @Value("${app.temp-folder:./temp}")
    private String tempFolder;

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addData(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            Path tempDir = Paths.get(tempFolder).toAbsolutePath().normalize();
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            
            Path filePath = tempDir.resolve(uniqueFilename);
            file.transferTo(filePath.toFile());
            
            Map<String, String> request = new HashMap<>();
            request.put("image_path", filePath.toAbsolutePath().toString());
            
            String pythonUrl = "http://localhost:5000/analyze";
            ImageFeatures imageFeatures = restTemplate.postForObject(pythonUrl, request, ImageFeatures.class);
            
            if (imageFeatures != null && imageFeatures.isSuccess()) {
                BuildingAnalysis analysis = new BuildingAnalysis();
                analysis.setImagePath(filePath.toAbsolutePath().toString());
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
                
                BuildingAnalysis savedAnalysis = buildingAnalysisService.saveAnalysis(analysis);
                
                pythonUrl = "http://localhost:5000/predict";
                ImageAnalysisResult predictionResult = restTemplate.postForObject(pythonUrl, request, ImageAnalysisResult.class);
                
                if (predictionResult != null && predictionResult.isSuccess()) {
                    BuildingType buildingType = new BuildingType();
                    buildingType.setImagePath(filePath.toAbsolutePath().toString());
                    buildingType.setPrediction(predictionResult.getPrediction());
                    buildingType.setConfidence(predictionResult.getConfidence());
                    buildingType.setAnalysisId(savedAnalysis.getId());
                    
                    buildingTypeService.saveType(buildingType);
                    
                    result.put("success", true);
                    result.put("message", "数据添加成功");
                    result.put("analysisId", savedAnalysis.getId());
                    result.put("typeId", buildingType.getId());
                } else {
                    result.put("success", false);
                    result.put("message", "预测失败");
                }
            } else {
                result.put("success", false);
                result.put("message", "图像分析失败");
            }
            
            Files.deleteIfExists(filePath);
            
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "文件处理失败: " + e.getMessage());
            e.printStackTrace();
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
                result.put("data", analysis);
            } else {
                result.put("success", false);
                result.put("message", "未找到编号为 " + id + " 的分析信息");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            e.printStackTrace();
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
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(result);
    }
}
