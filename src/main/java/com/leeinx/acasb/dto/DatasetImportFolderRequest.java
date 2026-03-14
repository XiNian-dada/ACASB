package com.leeinx.acasb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DatasetImportFolderRequest {
    @JsonProperty("dataset_path")
    private String datasetPath;

    @JsonProperty("dataset_name")
    private String datasetName;

    @JsonProperty("copy_images")
    private Boolean copyImages;
}
