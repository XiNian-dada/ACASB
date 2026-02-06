package com.leeinx.acasb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("building_type")
public class BuildingType {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("image_path")
    private String imagePath;
    
    @TableField("prediction")
    private String prediction;
    
    @TableField("confidence")
    private Double confidence;
    
    @TableField("analysis_id")
    private Long analysisId;
    
    @TableField("create_time")
    private LocalDateTime createTime;
    
    @TableField("update_time")
    private LocalDateTime updateTime;
}
