package com.leeinx.acasb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DatasetImportManifestRequest {
    @JsonProperty("manifest_path")
    private String manifestPath;

    @JsonProperty("dataset_name")
    private String datasetName;

    @JsonProperty("copy_images")
    private Boolean copyImages;
}
