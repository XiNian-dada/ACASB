package com.leeinx.acasb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DatasetManifestSummary {
    @JsonProperty("total_images")
    private Integer totalImages;

    @JsonProperty("processed_count")
    private Integer processedCount;

    @JsonProperty("pending_count")
    private Integer pendingCount;

    @JsonProperty("success_count")
    private Integer successCount;

    @JsonProperty("failed_count")
    private Integer failedCount;

    @JsonProperty("manual_review_count")
    private Integer manualReviewCount;
}
