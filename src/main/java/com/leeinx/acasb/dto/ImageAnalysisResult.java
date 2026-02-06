package com.leeinx.acasb.dto;

import lombok.Data;

@Data
public class ImageAnalysisResult {
    private boolean success;
    private String message;
    private String prediction;
    private double confidence;
}