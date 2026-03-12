package com.leeinx.acasb.service;

import com.leeinx.acasb.PredictionRequest;
import com.leeinx.acasb.config.PythonServiceProperties;
import com.leeinx.acasb.dto.ImageAnalysisResult;
import com.leeinx.acasb.dto.ImageFeatures;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PythonAnalysisClient {
    private final RestTemplate restTemplate;
    private final PythonServiceProperties pythonServiceProperties;

    public PythonAnalysisClient(RestTemplate restTemplate, PythonServiceProperties pythonServiceProperties) {
        this.restTemplate = restTemplate;
        this.pythonServiceProperties = pythonServiceProperties;
    }

    public ImageFeatures analyze(String imagePath) {
        return restTemplate.postForObject(
                pythonServiceProperties.buildUrl("/analyze"),
                new PredictionRequest(imagePath),
                ImageFeatures.class
        );
    }

    public ImageAnalysisResult predict(String imagePath) {
        return restTemplate.postForObject(
                pythonServiceProperties.buildUrl("/predict"),
                new PredictionRequest(imagePath),
                ImageAnalysisResult.class
        );
    }
}
