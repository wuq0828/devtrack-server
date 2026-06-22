package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class StatusHistoryDto implements Serializable {

    private String fromState;
    private String toState;
    private String transitionName;
    private String operatorName;
    private String comment;
    private Long durationSec;
    private Long createTime;
}
