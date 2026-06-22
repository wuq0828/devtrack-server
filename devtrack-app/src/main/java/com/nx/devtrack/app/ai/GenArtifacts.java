package com.nx.devtrack.app.ai;

import com.nx.devtrack.common.dto.GenCaseDto;

import java.util.List;

/**
 * 一次生成的全部产物:测试用例草稿 + 业务流程图 + 测试点脑图。
 * flowchart / mindmap 均为 Mermaid 文本,可能为空(模型未给出时)。
 */
public record GenArtifacts(List<GenCaseDto> cases, String flowchart, String mindmap) {

    public static GenArtifacts ofCases(List<GenCaseDto> cases) {
        return new GenArtifacts(cases, null, null);
    }
}
