package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class GenCasesResultDto implements Serializable {

    /** claude(真模型) / heuristic(无 key 时的启发式兜底) */
    private String engine;
    private List<GenCaseDto> cases;
}
