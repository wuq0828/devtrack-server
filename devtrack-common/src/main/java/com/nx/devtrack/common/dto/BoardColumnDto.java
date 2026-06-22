package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BoardColumnDto implements Serializable {

    private String statusCode;
    private String statusName;
    private String category;
    private int count;
    private List<DefectDto> defects;
}
