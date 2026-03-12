package com.leeinx.acasb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiBuildingAnalysis {
    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("model")
    private String model;

    @JsonProperty("error")
    private String error;

    @JsonProperty("building_type")
    private String buildingType;

    @JsonProperty("building_type_confidence")
    private Double buildingTypeConfidence;

    @JsonProperty("style")
    private String style;

    @JsonProperty("style_confidence")
    private Double styleConfidence;

    @JsonProperty("estimated_era")
    private String estimatedEra;

    @JsonProperty("estimated_era_reasoning")
    private String estimatedEraReasoning;

    @JsonProperty("roof_type")
    private String roofType;

    @JsonProperty("main_materials")
    private List<String> mainMaterials;

    @JsonProperty("dominant_colors")
    private List<AiColorRatio> dominantColors;

    @JsonProperty("key_features")
    private List<String> keyFeatures;

    @JsonProperty("summary")
    private String summary;

    public static AiBuildingAnalysis failed(String provider, String model, String error) {
        AiBuildingAnalysis analysis = new AiBuildingAnalysis();
        analysis.setEnabled(true);
        analysis.setSuccess(false);
        analysis.setProvider(provider);
        analysis.setModel(model);
        analysis.setError(error);
        return analysis;
    }
}
