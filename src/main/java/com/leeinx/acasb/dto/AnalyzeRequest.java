package com.leeinx.acasb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AnalyzeRequest {
    @JsonProperty("image_path")
    private String imagePath;

    @JsonProperty("enable_ai")
    private Boolean enableAi;
}
