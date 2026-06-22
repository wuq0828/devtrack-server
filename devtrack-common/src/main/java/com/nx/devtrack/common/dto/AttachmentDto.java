package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AttachmentDto implements Serializable {

    private Long id;
    private String fileName;
    private Long sizeBytes;
    private String uploaderName;
    private Long createTime;
}
