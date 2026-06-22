package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ActivityDto implements Serializable {

    private String action;
    private String detail;
    private String operatorName;
    private Long createTime;
}
