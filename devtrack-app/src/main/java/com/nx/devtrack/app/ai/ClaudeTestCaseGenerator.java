package com.nx.devtrack.app.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nx.devtrack.common.dto.GenCaseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用 Claude(claude-opus-4-8 + 自适应思考)从 PRD 生成测试用例。
 * 仅当环境变量 ANTHROPIC_API_KEY 存在时可用;否则由 HeuristicTestCaseGenerator 兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeTestCaseGenerator implements TestCaseGenerator {

    private static final String PROMPT_HEADER = """
            你是资深测试工程师。根据下面的 PRD/需求,先拆分关键场景,再产出三部分内容。
            只输出一个 JSON 对象,不要任何解释、不要 markdown 代码块。结构如下:
            {
              "cases": [
                {"title":"用例标题","preconditions":"前置条件","steps":"操作步骤(可多行,用\\n分隔)","expected":"预期结果"}
              ],
              "flowchart": "Mermaid flowchart 文本(业务主流程,含正常/异常分支判断)",
              "mindmap": "Mermaid mindmap 文本(测试点脑图,按模块/场景分层)"
            }
            要求:
            1. cases 覆盖正常流程、边界值、异常输入与权限场景,中文,生成 8-15 条。
            2. flowchart 用 `flowchart TD` 开头,节点文字用中文并放在方括号/花括号里,如 A[开始] --> B{校验通过?};分支用 -->|是| / -->|否|。
            3. mindmap 用 `mindmap` 开头,根节点形如 root((需求名)),下面按"测试模块 -> 具体测试点"两到三层缩进展开。
            4. flowchart 与 mindmap 内部换行用真实换行符(JSON 里写成 \\n),节点文字避免使用英文括号、引号、分号等会破坏 Mermaid 语法的字符。

            需求:
            """;

    private final ObjectMapper objectMapper;

    @Override
    public boolean available() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        return key != null && !key.isBlank();
    }

    @Override
    public String engine() {
        return "claude";
    }

    @Override
    public GenArtifacts generate(String prd) {
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        MessageCreateParams params = MessageCreateParams.builder()
                .model("claude-opus-4-8")
                .maxTokens(16000L)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .addUserMessage(PROMPT_HEADER + prd)
                .build();
        Message response = client.messages().create(params);
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : response.content()) {
            block.text().ifPresent(t -> sb.append(t.text()));
        }
        return parse(sb.toString());
    }

    private GenArtifacts parse(String text) {
        // Carve out the JSON object the model was asked to return.
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                JsonNode root = objectMapper.readTree(text.substring(start, end + 1));
                List<GenCaseDto> cases = root.has("cases")
                        ? objectMapper.convertValue(root.get("cases"), new TypeReference<List<GenCaseDto>>() {
                        })
                        : List.of();
                String flowchart = root.path("flowchart").asText(null);
                String mindmap = root.path("mindmap").asText(null);
                return new GenArtifacts(cases, emptyToNull(flowchart), emptyToNull(mindmap));
            } catch (Exception e) {
                log.warn("[AI] 解析模型 JSON 对象失败,尝试按数组解析: {}", e.getMessage());
            }
        }
        // Backward-compatible fallback: the model may still return a bare cases array.
        return GenArtifacts.ofCases(parseCasesArray(text));
    }

    private List<GenCaseDto> parseCasesArray(String text) {
        try {
            int start = text.indexOf('[');
            int end = text.lastIndexOf(']');
            String json = (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
            return objectMapper.readValue(json, new TypeReference<List<GenCaseDto>>() {
            });
        } catch (Exception e) {
            log.warn("[AI] 解析模型输出失败: {}", e.getMessage());
            return List.of();
        }
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
