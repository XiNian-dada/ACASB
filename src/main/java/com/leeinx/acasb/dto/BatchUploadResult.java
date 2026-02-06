package com.leeinx.acasb.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchUploadResult {
    private Integer totalCount;
    private Integer successCount;
    private Integer failureCount;
    private List<UploadItemResult> items;
    
    @Data
    public static class UploadItemResult {
        private String fileName;
        private Long analysisId;
        private Long typeId;
        private String message;
        private Boolean success;
    }
}
