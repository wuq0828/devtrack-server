package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class StatsDto implements Serializable {

    private long total;
    private Map<String, Long> byStatus;
    private Map<String, Long> byPriority;
    private Map<String, Long> bySeverity;
}
