package com.leeinx.acasb.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DatasetImportResult {
    private String datasetName;
    private Integer manifestsDiscovered = 0;
    private Integer recordsProcessed = 0;
    private Integer importedCount = 0;
    private Integer updatedCount = 0;
    private Integer failedCount = 0;
    private Integer copiedImageCount = 0;
    private List<String> errors = new ArrayList<>();
}
