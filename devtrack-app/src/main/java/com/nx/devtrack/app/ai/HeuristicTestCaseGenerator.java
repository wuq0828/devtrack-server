package com.nx.devtrack.app.ai;

import com.nx.devtrack.common.dto.GenCaseDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 启发式兜底:把 PRD 按行/句切成场景,每个场景生成 正常流程 + 异常流程 两条用例草稿,
 * 并据此产出确定性的 Mermaid 流程图 + 测试点脑图。
 * 无需任何外部依赖,保证 AI 不可用时仍能产出可用的用例骨架与图。
 */
@Component
public class HeuristicTestCaseGenerator implements TestCaseGenerator {

    private static final int MAX_CASES = 80;
    private static final int MAX_SCENARIOS = 40;
    /** 流程图/脑图只取前若干场景,避免节点过多导致 Mermaid 布局重叠糊成一团(用例数不受此限)。 */
    private static final int MAX_DIAGRAM_SCENARIOS = 8;

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String engine() {
        return "heuristic";
    }

    /** PRD 中内嵌的 Mermaid 流程图代码块(```...flowchart/graph...```)。 */
    private static final Pattern EMBEDDED_FLOWCHART = Pattern.compile(
            "```[a-zA-Z]*\\s*\\R(\\s*(?:flowchart|graph)\\b[\\s\\S]*?)```");

    @Override
    public GenArtifacts generate(String prd) {
        List<String> scenarios = splitScenarios(prd);
        List<GenCaseDto> cases = buildCases(scenarios);
        // 图只取前若干场景,避免节点过多导致 Mermaid 布局重叠(用例数不受此限)。
        List<String> diagramScenarios = scenarios.size() > MAX_DIAGRAM_SCENARIOS
                ? scenarios.subList(0, MAX_DIAGRAM_SCENARIOS)
                : scenarios;
        // 优先采用文档自带的流程图(最贴合真实业务);没有则按(裁剪后的)场景生成。
        String embedded = extractEmbeddedFlowchart(prd);
        String flowchart = embedded != null ? embedded : buildFlowchart(diagramScenarios);
        String mindmap = buildMindmap(diagramScenarios);
        return new GenArtifacts(cases, flowchart, mindmap);
    }

    /** 从 PRD 正文里提取已经写好的 Mermaid 流程图;没有则返回 null。 */
    private String extractEmbeddedFlowchart(String prd) {
        if (prd == null) {
            return null;
        }
        Matcher m = EMBEDDED_FLOWCHART.matcher(prd);
        if (m.find()) {
            String code = m.group(1).strip();
            return code.isEmpty() ? null : code;
        }
        return null;
    }

    /** 把 PRD 切成去重后的场景短语(最多 MAX_SCENARIOS 个)。 */
    private List<String> splitScenarios(String prd) {
        Set<String> scenarios = new LinkedHashSet<>();
        if (prd != null) {
            for (String raw : prd.split("[\\n。;；]")) {
                String line = raw.trim();
                if (line.length() < 4) {
                    continue;
                }
                scenarios.add(line.length() > 40 ? line.substring(0, 40) : line);
                if (scenarios.size() >= MAX_SCENARIOS) {
                    break;
                }
            }
        }
        return new ArrayList<>(scenarios);
    }

    private List<GenCaseDto> buildCases(List<String> scenarios) {
        List<GenCaseDto> cases = new ArrayList<>();
        for (String scenario : scenarios) {
            cases.add(buildCase("[正常] " + scenario,
                    "已登录并进入相关功能页面",
                    "1. 按需求执行:" + scenario + "\n2. 提交/确认操作",
                    "功能按需求预期正确响应,无报错"));
            cases.add(buildCase("[异常] " + scenario,
                    "已登录并进入相关功能页面",
                    "1. 用非法/边界输入执行:" + scenario + "\n2. 提交",
                    "系统给出明确校验提示,不产生脏数据"));
            if (cases.size() >= MAX_CASES) {
                break;
            }
        }
        if (cases.isEmpty()) {
            cases.add(buildCase("基础冒烟用例", "已登录", "1. 打开功能\n2. 执行核心操作", "核心流程可用"));
        }
        return cases;
    }

    /**
     * 主流程:开始 -> 进入功能 -> 逐场景执行(每步一个校验关卡)-> 完成。
     * 设计原则(避免错乱):
     *   1. 单一向下的主干"快乐路径",每个校验的「是」流向下一步,最后到「完成」;
     *   2. 所有校验的「否」统一汇聚到同一个"校验失败"终点,不再各自回环(消除缠绕的回边);
     *   3. 用 classDef 给 开始/失败/完成 上色,层次清晰、更直观。
     */
    private String buildFlowchart(List<String> scenarios) {
        StringBuilder sb = new StringBuilder("flowchart TD\n");
        sb.append("    start([\"开始\"]) --> entry[\"进入功能页面\"]\n");

        if (scenarios.isEmpty()) {
            sb.append("    entry --> done([\"完成\"])\n");
            appendFlowchartStyles(sb, false);
            return sb.toString();
        }

        for (int i = 0; i < scenarios.size(); i++) {
            String label = sanitize(scenarios.get(i));
            String stepId = "s" + i;
            String checkId = "c" + i;

            // 进入当前步骤的边:首步从 entry 进入;其后由上一关卡的「是」分支进入。
            if (i == 0) {
                sb.append("    entry --> ").append(stepId).append("[\"").append(label).append("\"]\n");
            } else {
                sb.append("    c").append(i - 1).append(" -->|是| ").append(stepId)
                        .append("[\"").append(label).append("\"]\n");
            }
            sb.append("    ").append(stepId).append(" --> ").append(checkId).append("{\"校验通过?\"}\n");
            // 「否」统一指向共享的失败终点
            sb.append("    ").append(checkId).append(" -->|否| fail\n");
        }
        // 末关卡的「是」分支抵达完成
        sb.append("    c").append(scenarios.size() - 1).append(" -->|是| done([\"完成\"])\n");
        sb.append("    fail([\"校验失败 提示并终止\"])\n");
        boolean hasFail = true;
        appendFlowchartStyles(sb, hasFail);
        return sb.toString();
    }

    /** 给开始/完成/失败节点上色,使图更直观(成功=绿、失败=红、起点=紫)。 */
    private void appendFlowchartStyles(StringBuilder sb, boolean hasFail) {
        sb.append("    classDef startNode fill:#1e1b4b,stroke:#6366f1,color:#c7d2fe;\n");
        sb.append("    classDef doneNode fill:#064e3b,stroke:#10b981,color:#a7f3d0;\n");
        sb.append("    class start startNode\n");
        sb.append("    class done doneNode\n");
        if (hasFail) {
            sb.append("    classDef failNode fill:#4c1d2e,stroke:#f43f5e,color:#fecdd3;\n");
            sb.append("    class fail failNode\n");
        }
    }

    /** 测试点脑图:根 -> 每个场景 -> 正常/异常/边界 测试点。 */
    private String buildMindmap(List<String> scenarios) {
        StringBuilder sb = new StringBuilder("mindmap\n");
        sb.append("  root((测试点))\n");
        if (scenarios.isEmpty()) {
            sb.append("    核心流程\n      正常流程\n      异常输入\n");
            return sb.toString();
        }
        for (String scenario : scenarios) {
            sb.append("    ").append(sanitize(scenario)).append("\n");
            sb.append("      正常流程\n");
            sb.append("      异常输入\n");
            sb.append("      边界值\n");
            sb.append("      权限校验\n");
        }
        return sb.toString();
    }

    /**
     * 清洗节点文字:去掉会破坏 Mermaid 语法的字符(括号、引号、方括号、分号、连字符箭头等),
     * 折叠空白并截断,保证生成的图一定能被 mermaid 解析。
     */
    private String sanitize(String text) {
        if (text == null) {
            return "节点";
        }
        String cleaned = text
                // 去掉破坏 Mermaid 语法的字符:括号/引号/方括号/分号/管道等,
                // 以及会出现在 URL/路径里的 : / \ ? & = % ~ @ . * 等。
                .replaceAll("[\\[\\]{}()<>\"'`|;#\\\\:/?&=%~@.*!^$]", " ")
                .replaceAll("-+>", " ")
                .replaceAll("[-_]{2,}", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() > 24) {
            cleaned = cleaned.substring(0, 24);
        }
        return cleaned.isEmpty() ? "节点" : cleaned;
    }

    private GenCaseDto buildCase(String title, String pre, String steps, String expected) {
        GenCaseDto c = new GenCaseDto();
        c.setTitle(title);
        c.setPreconditions(pre);
        c.setSteps(steps);
        c.setExpected(expected);
        return c;
    }
}
