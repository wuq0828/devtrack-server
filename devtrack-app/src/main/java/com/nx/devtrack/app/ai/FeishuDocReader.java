package com.nx.devtrack.app.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 读取飞书云空间文件/文档的正文。
 * 复用本机已登录的 lark-cli(零额外凭证):当 PRD 传入的是飞书链接时,
 * 拉取真实文档内容作为生成依据。仅支持当前飞书账号有权限访问的文档。
 */
@Slf4j
@Component
public class FeishuDocReader {

    /** lark-cli 可执行文件路径(Homebrew 默认安装位置)。 */
    private static final String LARK_CLI = "/opt/homebrew/bin/lark-cli";

    /** 飞书/Lark 链接:捕获资源类型与 token(token 被限制为字母数字,天然防注入)。 */
    private static final Pattern URL = Pattern.compile(
            "https?://[\\w.-]*(?:feishu\\.cn|larksuite\\.com)/(file|docx|doc|wiki|sheets)/([A-Za-z0-9]+)");

    private static final long TIMEOUT_SEC = 40;

    /** 文本本身是否就是一个飞书链接(而非一大段含链接的文字)。 */
    public boolean isFeishuUrl(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        return t.length() < 300 && URL.matcher(t).find();
    }

    /** 读取链接对应文件的纯文本内容;失败抛出 IllegalStateException(带友好提示)。 */
    public String readContent(String url) {
        Matcher m = URL.matcher(url.trim());
        if (!m.find()) {
            throw new IllegalStateException("不是有效的飞书文档链接");
        }
        String type = m.group(1);
        String token = m.group(2);
        if (!"file".equals(type)) {
            throw new IllegalStateException("暂仅支持飞书云空间「上传文件」(/file/)链接,当前类型: " + type);
        }

        Path tmp;
        try {
            tmp = Files.createTempDirectory("dvt-feishu");
        } catch (IOException e) {
            throw new IllegalStateException("创建临时目录失败: " + e.getMessage(), e);
        }
        try {
            String out = "content.bin";
            // lark-cli 要求 --output 为当前工作目录下的相对路径,故指定 workDir 为临时目录。
            List<String> cmd = List.of(LARK_CLI, "drive", "+download",
                    "--file-token", token, "--output", "./" + out, "--overwrite");
            int code = run(cmd, tmp);
            Path file = tmp.resolve(out);
            if (code != 0 || !Files.exists(file)) {
                throw new IllegalStateException(
                        "读取飞书文档失败:请确认该文档已对当前飞书账号开放访问权限");
            }
            String content = Files.readString(file, StandardCharsets.UTF_8);
            log.info("[Feishu] 读取文档成功:{} 字符", content.length());
            return content;
        } catch (IOException e) {
            throw new IllegalStateException("读取飞书文档异常: " + e.getMessage(), e);
        } finally {
            deleteQuietly(tmp);
        }
    }

    private int run(List<String> cmd, Path workDir) throws IOException {
        Process p = new ProcessBuilder(cmd)
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .start();
        try {
            if (!p.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IllegalStateException("读取飞书文档超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("读取飞书文档被中断", e);
        }
        return p.exitValue();
    }

    private void deleteQuietly(Path dir) {
        try (var s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(pp -> {
                try {
                    Files.deleteIfExists(pp);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            });
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
