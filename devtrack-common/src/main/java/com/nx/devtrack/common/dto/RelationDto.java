package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RelationDto implements Serializable {

    private Long id;
    private String sourceType;
    private Long sourceId;
    private String sourceTitle;
    private String targetType;
    private Long targetId;
    private String targetTitle;
    private String relationType;
}
