package com.leeinx.acasb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class PredictionController {

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/predict")
    public ResponseEntity<?> predict(@RequestBody PredictionRequest request) {
        try {
            String pythonUrl = "http://localhost:5000/predict";
            ResponseEntity<?> response = restTemplate.postForEntity(pythonUrl, request, Object.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"Failed to call Python API: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/health")
    public String health() {
        return "Java Backend is running!";
    }
}