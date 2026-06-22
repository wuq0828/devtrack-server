package com.nx.devtrack.app.ai;

import com.nx.devtrack.common.dto.GenCaseDto;

import java.util.List;

/**
 * 从 PRD/需求文本生成测试用例草稿。两种实现:Claude 真模型 / 启发式兜底。
 */
public interface TestCaseGenerator {

    boolean available();

    /** claude / heuristic */
    String engine();

    List<GenCaseDto> generate(String prd);
}
