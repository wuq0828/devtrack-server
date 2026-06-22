package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class IterationReportDto implements Serializable {

    private int total;
    private int done;
    private int inProgress;
    private int todo;
    /** 完成率 0-100,保留一位小数 */
    private double completionRate;
    private Map<String, Long> byStatus;
    private Map<String, Long> byPriority;
}
