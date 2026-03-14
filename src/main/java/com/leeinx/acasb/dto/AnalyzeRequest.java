package com.leeinx.acasb.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AnalyzeRequest {
    @JsonProperty("image_path")
    @JsonAlias("imagePath")
    private String imagePath;

    @JsonProperty("enable_ai")
    @JsonAlias("enableAi")
    private Boolean enableAi;
}
