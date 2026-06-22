package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommentItemDto implements Serializable {

    private Long id;
    private String content;
    private String authorName;
    private Long createTime;
}
