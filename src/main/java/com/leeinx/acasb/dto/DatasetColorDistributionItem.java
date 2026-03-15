package com.leeinx.acasb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DatasetColorDistributionItem {
    @JsonProperty("color")
    private String color;

    @JsonProperty("ratio")
    private Double ratio;

    @JsonProperty("hex")
    private String hex;
}
