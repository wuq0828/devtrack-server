package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CustomFieldDefDto implements Serializable {

    private Long id;
    private String fieldKey;
    private String label;
    private String fieldType;
    private List<String> options;
    private boolean required;
}
