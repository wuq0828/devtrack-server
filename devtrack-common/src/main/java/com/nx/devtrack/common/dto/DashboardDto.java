package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class DashboardDto implements Serializable {

    private long total;
    private long openCount;
    private Map<String, Long> byStatus;
    private Map<String, Long> byPriority;
    private List<ActivityDto> recentActivities;
}
