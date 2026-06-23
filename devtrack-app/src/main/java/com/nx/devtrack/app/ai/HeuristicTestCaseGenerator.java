package com.nx.devtrack.app.ai;

import com.nx.devtrack.common.dto.GenCaseDto;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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

    /** PRD 中内嵌的 Mermaid 脑图代码块(```...mindmap...```)。 */
    private static final Pattern EMBEDDED_MINDMAP = Pattern.compile(
            "```[a-zA-Z]*\\s*\\R(\\s*mindmap\\b[\\s\\S]*?)```");

    /** Markdown 标题:# 文档标题 / ## 模块 / ### 功能点。 */
    private static final Pattern HEADING = Pattern.compile("(?m)^(#{1,4})[ \\t]+(.+?)[ \\t]*$");

    private static final int MAX_MM_MODULES = 12;
    private static final int MAX_MM_CHILDREN = 6;

    /** 元信息类章节(非功能模块),不进测试点脑图。 */
    private static final List<String> SKIP_HEADINGS = List.of(
            "修改记录", "修订", "变更记录", "目录", "术语", "名词解释", "版本说明", "参考", "附录", "文档说明");

    private record Heading(int level, String text) {
    }

    /** 一个功能模块(## 标题)及其下的功能点标题。 */
    private static final class MmModule {
        final String text;
        final List<Heading> children = new ArrayList<>();

        MmModule(String text) {
            this.text = text;
        }
    }

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
        String mindmap = buildMindmap(prd, diagramScenarios);
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

    /**
     * 测试点脑图:优先用文档自带 mindmap;其次按 PRD 章节标题层级生成(最清晰);
     * 文档无标题结构时,按场景兜底。
     */
    private String buildMindmap(String prd, List<String> scenarios) {
        String embedded = extractEmbeddedMindmap(prd);
        if (embedded != null) {
            return embedded;
        }
        String byHeadings = buildMindmapFromHeadings(prd);
        if (byHeadings != null) {
            return byHeadings;
        }
        return buildMindmapFromScenarios(scenarios);
    }

    /** 从 PRD 正文里提取已经写好的 Mermaid 脑图;没有则返回 null。 */
    private String extractEmbeddedMindmap(String prd) {
        if (prd == null) {
            return null;
        }
        Matcher m = EMBEDDED_MINDMAP.matcher(prd);
        if (m.find()) {
            String code = m.group(1).strip();
            return code.isEmpty() ? null : code;
        }
        return null;
    }

    /**
     * 按 Markdown 标题层级生成测试点脑图:# 文档标题为根,## 为功能模块,###/#### 为功能点;
     * 模块下若无子标题则补充标准测试维度。结构不足(有效模块 &lt; 2)时返回 null 交由兜底。
     */
    private String buildMindmapFromHeadings(String prd) {
        if (prd == null) {
            return null;
        }
        List<Heading> heads = new ArrayList<>();
        Matcher m = HEADING.matcher(prd);
        while (m.find()) {
            String text = cleanHeading(m.group(2));
            if (!text.isEmpty()) {
                heads.add(new Heading(m.group(1).length(), text));
            }
        }
        if (heads.isEmpty()) {
            return null;
        }

        // 根节点用 PRD 文档标题(# 一级标题);去掉末尾版本号残留,空则兜底「测试点」。
        String rootLabel = heads.stream().filter(h -> h.level() == 1)
                .map(Heading::text).findFirst().orElse("测试点")
                .replaceAll("(?i)\\s+v[\\d ]*$", "").trim();
        if (rootLabel.isEmpty()) {
            rootLabel = "测试点";
        }

        // 第一遍:把标题分组为「## 模块 -> 其下功能点」(过滤元信息章节,限制数量)。
        List<MmModule> mods = new ArrayList<>();
        MmModule current = null;
        boolean skipping = false;
        for (Heading h : heads) {
            if (h.level() <= 1) {
                continue;
            }
            if (h.level() == 2) {
                if (isSkipHeading(h.text()) || mods.size() >= MAX_MM_MODULES) {
                    skipping = true;
                    current = null;
                    continue;
                }
                skipping = false;
                current = new MmModule(h.text());
                mods.add(current);
            } else if (!skipping && current != null && current.children.size() < MAX_MM_CHILDREN) {
                current.children.add(h);
            }
        }
        // 模块太少说明文档没有清晰标题结构,交给场景兜底。
        if (mods.size() < 2) {
            return null;
        }

        // 第二遍:输出。所有节点统一为纯文本(形状一致,不再「有的有框有的没框」);
        // 层次区分交给 mermaid 脑图自带能力:模块字号更大、各分支自动配色、内容字号更小并继承分支色。
        // 功能点按「归一化深度」缩进,避免 ## 直接跳到 #### 时父子关系错位。
        StringBuilder sb = new StringBuilder("mindmap\n");
        // 用圆角矩形作根节点(而非圆形):mermaid 圆形节点文字常垂直偏移,矩形能可靠居中。
        sb.append("  root(").append(rootLabel).append(")\n");
        for (MmModule mod : mods) {
            sb.append("    ").append(mod.text).append('\n');
            if (mod.children.isEmpty()) {
                sb.append("      正常流程\n      异常输入\n      边界值\n      权限校验\n");
                continue;
            }
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(2); // 模块自身层级,作为深度基准
            for (Heading child : mod.children) {
                while (stack.size() > 1 && stack.peek() >= child.level()) {
                    stack.pop();
                }
                int depth = stack.size() + 1; // 模块=1,功能点=2,子功能点=3
                sb.append(" ".repeat(depth * 2 + 2)).append(child.text()).append('\n');
                stack.push(child.level());
            }
        }
        return sb.toString();
    }

    /** 场景兜底脑图:根 -> 每个场景 -> 正常/异常/边界/权限 测试点。 */
    private String buildMindmapFromScenarios(List<String> scenarios) {
        StringBuilder sb = new StringBuilder("mindmap\n");
        sb.append("  root((测试点))\n");
        if (scenarios.isEmpty()) {
            sb.append("    核心流程\n      正常流程\n      异常输入\n");
            return sb.toString();
        }
        for (String scenario : scenarios) {
            sb.append("    ").append(sanitize(scenario)).append('\n');
            sb.append("      正常流程\n      异常输入\n      边界值\n      权限校验\n");
        }
        return sb.toString();
    }

    /** 是否为元信息类章节(不计入功能模块)。 */
    private boolean isSkipHeading(String text) {
        for (String kw : SKIP_HEADINGS) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /** 清洗标题:去掉前导编号(如 "2.10"、"三、")后再做通用清洗;空则返回空串。 */
    private String cleanHeading(String text) {
        if (text == null) {
            return "";
        }
        String t = text.replaceAll("^[\\s\\d.、)）\\-]+", "").trim();
        if (t.isEmpty()) {
            return "";
        }
        String cleaned = sanitize(t);
        return "节点".equals(cleaned) ? "" : cleaned;
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
                // 下划线会被 Mermaid 当作斜体标记,且常导致换行错位,统一换成空格。
                .replaceAll("_+", " ")
                .replaceAll("-{2,}", " ")
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
