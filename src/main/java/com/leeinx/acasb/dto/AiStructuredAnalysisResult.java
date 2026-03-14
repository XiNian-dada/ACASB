package com.leeinx.acasb.dto;

import lombok.Data;

@Data
public class AiStructuredAnalysisResult {
    private boolean success;
    private String provider;
    private String model;
    private String rawContent;
    private String error;
    private DatasetImageMetadata analysis;

    public static AiStructuredAnalysisResult success(
            String provider,
            String model,
            String rawContent,
            DatasetImageMetadata analysis) {
        AiStructuredAnalysisResult result = new AiStructuredAnalysisResult();
        result.setSuccess(true);
        result.setProvider(provider);
        result.setModel(model);
        result.setRawContent(rawContent);
        result.setAnalysis(analysis);
        return result;
    }

    public static AiStructuredAnalysisResult failed(
            String provider,
            String model,
            String error,
            DatasetImageMetadata analysis) {
        AiStructuredAnalysisResult result = new AiStructuredAnalysisResult();
        result.setSuccess(false);
        result.setProvider(provider);
        result.setModel(model);
        result.setError(error);
        result.setAnalysis(analysis);
        return result;
    }
}
