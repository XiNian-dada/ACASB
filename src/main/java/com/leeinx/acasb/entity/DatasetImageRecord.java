package com.leeinx.acasb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dataset_image_record")
public class DatasetImageRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("dataset_name")
    private String datasetName;

    @TableField("group_name")
    private String groupName;

    @TableField("group_relative_path")
    private String groupRelativePath;

    @TableField("original_group_relative_path")
    private String originalGroupRelativePath;

    @TableField("schema_version")
    private String schemaVersion;

    @TableField("prompt_version")
    private String promptVersion;

    @TableField("generated_at")
    private String generatedAt;

    @TableField("api_interface")
    private String apiInterface;

    @TableField("model")
    private String model;

    @TableField("province_level_region")
    private String provinceLevelRegion;

    @TableField("province_confidence")
    private Double provinceConfidence;

    @TableField("dynasty_guess")
    private String dynastyGuess;

    @TableField("building_rank")
    private String buildingRank;

    @TableField("scene_type")
    private String sceneType;

    @TableField("building_present")
    private Boolean buildingPresent;

    @TableField("building_primary_colors_json")
    private String buildingPrimaryColorsJson;

    @TableField("building_color_distribution_json")
    private String buildingColorDistributionJson;

    @TableField("architecture_style_json")
    private String architectureStyleJson;

    @TableField("scene_description")
    private String sceneDescription;

    @TableField("reasoning_json")
    private String reasoningJson;

    @TableField("needs_manual_review")
    private Boolean needsManualReview;

    @TableField("file_name")
    private String fileName;

    @TableField("relative_path")
    private String relativePath;

    @TableField("original_relative_path")
    private String originalRelativePath;

    @TableField("stored_image_path")
    private String storedImagePath;

    @TableField("source_image_path")
    private String sourceImagePath;

    @TableField("analysis_status")
    private String analysisStatus;

    @TableField("error_message")
    private String errorMessage;

    @TableField("image_index")
    private Integer imageIndex;

    @TableField("raw_metadata_json")
    private String rawMetadataJson;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
