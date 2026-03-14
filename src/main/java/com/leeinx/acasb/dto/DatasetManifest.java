package com.leeinx.acasb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DatasetManifest {
    @JsonProperty("schema_version")
    private String schemaVersion;

    @JsonProperty("prompt_version")
    private String promptVersion;

    @JsonProperty("group_name")
    private String groupName;

    @JsonProperty("group_relative_path")
    private String groupRelativePath;

    @JsonProperty("generated_at")
    private String generatedAt;

    @JsonProperty("api_interface")
    private String apiInterface;

    @JsonProperty("model")
    private String model;

    @JsonProperty("summary")
    private DatasetManifestSummary summary;

    @JsonProperty("images")
    private List<DatasetImageMetadata> images = new ArrayList<>();
}
