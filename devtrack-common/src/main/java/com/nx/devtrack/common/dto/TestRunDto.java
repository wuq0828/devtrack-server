package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class TestRunDto implements Serializable {

    private Long id;
    private String name;
    private String status;
    private int total;
    private int passed;
    private int failed;
    private int blocked;
    private int pending;
}
