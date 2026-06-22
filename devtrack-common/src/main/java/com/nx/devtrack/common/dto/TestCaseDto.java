package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class TestCaseDto implements Serializable {

    private Long id;
    private Long projectId;
    private String title;
    private String preconditions;
    private String steps;
    private String expected;
    private String status;
    private String automationKey;
    private boolean regression;
    private Long createTime;
}
