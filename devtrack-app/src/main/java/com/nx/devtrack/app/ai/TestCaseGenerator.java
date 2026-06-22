package com.nx.devtrack.app.ai;

/**
 * 从 PRD/需求文本生成测试用例草稿 + 业务流程图 + 测试点脑图。
 * 两种实现:Claude 真模型 / 启发式兜底。
 */
public interface TestCaseGenerator {

    boolean available();

    /** claude / heuristic */
    String engine();

    /** 返回用例草稿 + Mermaid 流程图/脑图 */
    GenArtifacts generate(String prd);
}
