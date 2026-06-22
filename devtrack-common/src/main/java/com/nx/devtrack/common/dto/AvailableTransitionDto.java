package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 当前缺陷在当前状态下可执行的流转动作(后端按 wf_transition 算好,前端直接渲染按钮)。
 */
@Data
public class AvailableTransitionDto implements Serializable {

    private String code;
    private String name;
    private boolean requireComment;
}
