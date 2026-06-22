package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class TagDto implements Serializable {

    private Long id;
    private String name;
    private String color;
}
