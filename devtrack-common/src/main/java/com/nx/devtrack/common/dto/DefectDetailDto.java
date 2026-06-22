package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 缺陷详情:缺陷本体 + 流转历史 + 评论 + 当前可用流转。
 */
@Data
public class DefectDetailDto implements Serializable {

    private DefectDto defect;
    private List<StatusHistoryDto> history;
    private List<CommentItemDto> comments;
    private List<AvailableTransitionDto> availableTransitions;
}
