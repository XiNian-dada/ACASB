package com.leeinx.acasb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DatasetImageMetadata {
    @JsonProperty("province_level_region")
    private String provinceLevelRegion;

    @JsonProperty("province_confidence")
    private Double provinceConfidence;

    @JsonProperty("dynasty_guess")
    private String dynastyGuess;

    @JsonProperty("building_rank")
    private String buildingRank;

    @JsonProperty("scene_type")
    private String sceneType;

    @JsonProperty("building_present")
    private Boolean buildingPresent;

    @JsonProperty("building_primary_colors")
    private List<String> buildingPrimaryColors;

    @JsonProperty("building_color_distribution")
    private List<DatasetColorDistributionItem> buildingColorDistribution;

    @JsonProperty("architecture_style")
    private List<String> architectureStyle;

    @JsonProperty("scene_description")
    private String sceneDescription;

    @JsonProperty("reasoning")
    private List<String> reasoning;

    @JsonProperty("needs_manual_review")
    private Boolean needsManualReview;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("relative_path")
    private String relativePath;

    @JsonProperty("analysis_status")
    private String analysisStatus;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("image_index")
    private Integer imageIndex;
}
