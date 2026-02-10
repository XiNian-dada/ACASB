package com.leeinx.acasb.dto;

import lombok.Data;

@Data
public class SortQueryRequest {
    private String field;
    private String order;
    private Integer limit;
    private Integer page;
    private Integer size;
    private String prediction;
}
