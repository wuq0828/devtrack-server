package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BurndownDto implements Serializable {

    private String iterationName;
    private int total;
    private List<BurndownPointDto> points;
}
