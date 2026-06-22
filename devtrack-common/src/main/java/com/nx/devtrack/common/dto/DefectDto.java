package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class DefectDto implements Serializable {

    private Long id;
    private Long projectId;
    /** 对外展示编号,如 PAY-1024 = 项目code + 项目内序号 */
    private String displayKey;
    private String title;
    private String description;
    private String statusCode;
    private String severity;
    private String priority;
    private Long reporterId;
    private Long assigneeId;
    private Long iterationId;
    private Long fixVersionId;
    private String source;
    /** epoch milliseconds,对齐 nx-skyline DTO 的时间戳约定 */
    private Long createTime;
    private Long updateTime;
    /** 自定义字段值 */
    private Map<String, Object> customFields;
}
