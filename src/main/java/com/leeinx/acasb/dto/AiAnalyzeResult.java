package com.leeinx.acasb.dto;

import lombok.Data;

@Data
public class AiAnalyzeResult {
    private boolean success;
    private String provider;
    private String model;
    private String content;
    private String error;

    public static AiAnalyzeResult success(String provider, String model, String content) {
        AiAnalyzeResult result = new AiAnalyzeResult();
        result.setSuccess(true);
        result.setProvider(provider);
        result.setModel(model);
        result.setContent(content);
        return result;
    }

    public static AiAnalyzeResult failed(String provider, String model, String error) {
        AiAnalyzeResult result = new AiAnalyzeResult();
        result.setSuccess(false);
        result.setProvider(provider);
        result.setModel(model);
        result.setError(error);
        return result;
    }
}
