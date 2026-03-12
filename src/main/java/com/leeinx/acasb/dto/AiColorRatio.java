package com.leeinx.acasb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiColorRatio {
    @JsonProperty("name")
    private String name;

    @JsonProperty("ratio")
    private Double ratio;

    @JsonProperty("description")
    private String description;
}
