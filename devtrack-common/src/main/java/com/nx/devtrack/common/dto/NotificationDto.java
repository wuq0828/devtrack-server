package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class NotificationDto implements Serializable {

    private Long id;
    private String type;
    private String content;
    private String refType;
    private Long refId;
    private boolean read;
    private Long createTime;
}
