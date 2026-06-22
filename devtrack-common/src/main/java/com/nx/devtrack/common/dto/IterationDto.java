package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class IterationDto implements Serializable {

    private Long id;
    private Long projectId;
    private String name;
    /** PLANNING / ACTIVE / CLOSED */
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
}
