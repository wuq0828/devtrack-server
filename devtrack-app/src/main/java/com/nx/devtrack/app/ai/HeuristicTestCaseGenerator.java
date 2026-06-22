package com.nx.devtrack.app.ai;

import com.nx.devtrack.common.dto.GenCaseDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    public GenArtifacts generate(String prd) {
        List<String> scenarios = splitScenarios(prd);
        List<GenCaseDto> cases = buildCases(scenarios);
        // Cap the diagrams to a readable subset so a large PRD doesn't overload Mermaid.
        List<String> diagramScenarios = scenarios.size() > MAX_DIAGRAM_SCENARIOS
                ? scenarios.subList(0, MAX_DIAGRAM_SCENARIOS)
                : scenarios;
        String flowchart = buildFlowchart(diagramScenarios);
        String mindmap = buildMindmap(diagramScenarios);
        return new GenArtifacts(cases, flowchart, mindmap);
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

    /** 主流程:开始 -> 进入功能 -> 逐场景执行(每步带校验分支)-> 完成。 */
    private String buildFlowchart(List<String> scenarios) {
        StringBuilder sb = new StringBuilder("flowchart TD\n");
        sb.append("    start([开始]) --> entry[进入功能页面]\n");
        String prev = "entry";
        if (scenarios.isEmpty()) {
            sb.append("    entry --> done([完成])\n");
            return sb.toString();
        }
        for (int i = 0; i < scenarios.size(); i++) {
            String label = sanitize(scenarios.get(i));
            String stepId = "s" + i;
            String checkId = "c" + i;
            sb.append("    ").append(prev).append(" --> ").append(stepId)
                    .append("[").append(label).append("]\n");
            sb.append("    ").append(stepId).append(" --> ").append(checkId).append("{校验通过?}\n");
            sb.append("    ").append(checkId).append(" -->|否| err").append(i).append("[提示错误并拦截]\n");
            sb.append("    err").append(i).append(" --> ").append(checkId).append("\n");
            prev = checkId;
        }
        // The last check's "是" branch leads to completion.
        sb.append("    ").append(prev).append(" -->|是| done([完成])\n");
        return sb.toString();
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
                .replaceAll("[\\[\\]{}()<>\"'`|;#\\\\]", " ")
                .replaceAll("-+>", " ")
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
