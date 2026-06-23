package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

/** 用户简要信息:用于「处理人」下拉选择与 ID→名字 展示。 */
@Data
public class UserBriefDto implements Serializable {

    private Long userId;
    private String name;
}
