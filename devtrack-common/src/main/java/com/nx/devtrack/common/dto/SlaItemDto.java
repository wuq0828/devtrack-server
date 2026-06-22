package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SlaItemDto implements Serializable {

    private Long defectId;
    private String displayKey;
    private String title;
    private String priority;
    private String statusCode;
    private long overdueDays;
    private long slaDays;
}
