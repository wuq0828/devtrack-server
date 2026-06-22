package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class VersionDto implements Serializable {

    private Long id;
    private Long projectId;
    private String name;
    private String status;
}
