package com.leeinx.acasb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("building_analysis")
public class BuildingAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("image_path")
    private String imagePath;
    
    @TableField("ratio_yellow")
    private Double ratioYellow;
    
    @TableField("ratio_red_1")
    private Double ratioRed1;
    
    @TableField("ratio_red_2")
    private Double ratioRed2;
    
    @TableField("ratio_blue")
    private Double ratioBlue;
    
    @TableField("ratio_green")
    private Double ratioGreen;
    
    @TableField("ratio_gray_white")
    private Double ratioGrayWhite;
    
    @TableField("ratio_black")
    private Double ratioBlack;
    
    @TableField("h_mean")
    private Double hMean;
    
    @TableField("h_std")
    private Double hStd;
    
    @TableField("s_mean")
    private Double sMean;
    
    @TableField("s_std")
    private Double sStd;
    
    @TableField("v_mean")
    private Double vMean;
    
    @TableField("v_std")
    private Double vStd;
    
    @TableField("edge_density")
    private Double edgeDensity;
    
    @TableField("entropy")
    private Double entropy;
    
    @TableField("contrast")
    private Double contrast;
    
    @TableField("dissimilarity")
    private Double dissimilarity;
    
    @TableField("homogeneity")
    private Double homogeneity;
    
    @TableField("asm")
    private Double asm;
    
    @TableField("royal_ratio")
    private Double royalRatio;
    
    @TableField("create_time")
    private LocalDateTime createTime;
    
    @TableField("update_time")
    private LocalDateTime updateTime;
}
