package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RequirementDto implements Serializable {

    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private Long createTime;
}
