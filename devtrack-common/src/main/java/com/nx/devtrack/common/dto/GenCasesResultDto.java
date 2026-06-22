package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class GenCasesResultDto implements Serializable {

    /** claude(真模型) / heuristic(无 key 时的启发式兜底) */
    private String engine;
    private List<GenCaseDto> cases;

    /** 业务流程图(Mermaid flowchart 文本),前端用 mermaid 渲染 */
    private String flowchart;

    /** 测试点脑图(Mermaid mindmap 文本),前端用 mermaid 渲染 */
    private String mindmap;
}
