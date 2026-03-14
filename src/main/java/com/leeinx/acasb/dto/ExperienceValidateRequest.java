package com.leeinx.acasb.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExperienceValidateRequest {
    private String rankId;
    private List<ExperienceColorSelection> selections;
}
