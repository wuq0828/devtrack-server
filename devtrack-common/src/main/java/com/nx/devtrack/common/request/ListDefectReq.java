package com.nx.devtrack.common.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ListDefectReq extends BaseInnerRequest {

    private Long projectId;
    /** 状态码,空表示不筛选 */
    private String statusCode;
    private String priority;
    private Long assigneeId;
    /** 标题关键字 */
    private String keyword;

    private int pn = 1;
    private int ps = 20;
}
