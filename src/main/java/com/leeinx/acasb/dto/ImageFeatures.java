package com.leeinx.acasb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ImageFeatures {
    private boolean success;
    private String message;
    
    private String prediction;
    private Double confidence;
    
    @JsonProperty("ratio_yellow")
    private double ratioYellow;
    
    @JsonProperty("ratio_red_1")
    private double ratioRed1;
    
    @JsonProperty("ratio_red_2")
    private double ratioRed2;
    
    @JsonProperty("ratio_blue")
    private double ratioBlue;
    
    @JsonProperty("ratio_green")
    private double ratioGreen;
    
    @JsonProperty("ratio_gray_white")
    private double ratioGrayWhite;
    
    @JsonProperty("ratio_black")
    private double ratioBlack;
    
    @JsonProperty("h_mean")
    private double hMean;
    
    @JsonProperty("h_std")
    private double hStd;
    
    @JsonProperty("s_mean")
    private double sMean;
    
    @JsonProperty("s_std")
    private double sStd;
    
    @JsonProperty("v_mean")
    private double vMean;
    
    @JsonProperty("v_std")
    private double vStd;
    
    @JsonProperty("edge_density")
    private double edgeDensity;
    
    @JsonProperty("entropy")
    private double entropy;
    
    @JsonProperty("contrast")
    private double contrast;
    
    @JsonProperty("dissimilarity")
    private double dissimilarity;
    
    @JsonProperty("homogeneity")
    private double homogeneity;
    
    @JsonProperty("asm")
    private double asm;
    
    @JsonProperty("royal_ratio")
    private double royalRatio;
}
