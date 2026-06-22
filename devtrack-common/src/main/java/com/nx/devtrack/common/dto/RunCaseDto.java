package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RunCaseDto implements Serializable {

    private Long testCaseId;
    private String title;
    private String status;
}
