package com.ainaming.service;

import com.ainaming.config.AppConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final AppConfig appConfig;
    private final OkHttpClient httpClient;

    // 创建一个线程池用于异步处理SSE流，防止阻塞主线程
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 流式生成名字（核心修改）
     */
    public void streamGenerateNames(String prompt, double temperature, SseEmitter emitter) {

        // 异步执行，不阻塞Controller
        executorService.execute(() -> {
            try {
                streamCallAI(prompt, temperature, emitter);
            } catch (Exception e) {
                log.error("流式生成异常", e);
                try {
                    // 发送错误事件给前端
                    emitter.send(SseEmitter.event().name("error").data("生成失败: " + e.getMessage()));
                } catch (IOException ex) {
                    // ignore
                }
                emitter.completeWithError(e);
            }
        });
    }

    /**
     * 通用流式调用方法
     */
    private void streamCallAI(String prompt, double temperature, SseEmitter emitter) throws IOException {
        JSONObject body = new JSONObject();
        body.put("model", appConfig.getModel());
        body.put("temperature", temperature);
        body.put("max_tokens", 4000);
        body.put("stream", true); // ✅ 关键：开启流式模式

        JSONArray messages = new JSONArray();
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", "你是一位精通易经、诗词的取名大师。请直接输出结果，不要输出思考过程。");
        messages.add(sysMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        body.put("messages", messages);

        Request request = new Request.Builder()
                .url(appConfig.getApiUrl())
                .addHeader("Authorization", "Bearer " + appConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        body.toJSONString(),
                        MediaType.parse("application/json; charset=utf-8")
                ))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("AI API 调用失败: " + response.code());
            }

            // 读取流
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // OpenAI/DeepSeek 格式通常是: data: {...}
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        break; // 结束
                    }

                    try {
                        JSONObject json = JSON.parseObject(data);
                        String content = json.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("delta")
                                .getString("content");

                        if (content != null) {
                            // 实时发送给前端
                            emitter.send(content);
                        }
                    } catch (Exception e) {
                        // 忽略解析错误的行（比如 ping）
                    }
                }
            }
            // 完成
            emitter.complete();
        }
    }


    /**
     * 调用 AI API
     */
    public String callAI(String prompt, double temperature) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", appConfig.getModel());
            body.put("temperature", temperature);
            body.put("max_tokens", 4000);

            JSONArray messages = new JSONArray();
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", "你是一位精通中国传统文化的取名大师。");
            messages.add(sysMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);

            body.put("messages", messages);

            Request request = new Request.Builder()
                    .url(appConfig.getApiUrl())
                    .addHeader("Authorization", "Bearer " + appConfig.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            body.toJSONString(),
                            MediaType.parse("application/json; charset=utf-8")
                    ))
                    .build();

            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                String respBody = response.body().string();
                JSONObject respJson = JSON.parseObject(respBody);
                return respJson
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            } else {
                log.error("AI API 调用失败: {}", response.code());
                throw new RuntimeException("AI API 调用失败: " + response.code());
            }
        } catch (Exception e) {
            log.error("AI API 异常: {}", e.getMessage());
            throw new RuntimeException("AI 服务暂时不可用: " + e.getMessage());
        }
    }

    /**
     * 生成名字
     */
    public List<Map<String, Object>> generateNames(
            String surname, String gender, String style,
            String meaning, int charCount,
            Map<String, Object> baziInfo, Map<String, Object> wuxingAnalysis,
            String avoidChars, String preferChars) {

        String prompt = buildNamingPrompt(surname, gender, style, meaning,
                charCount, baziInfo, wuxingAnalysis, avoidChars, preferChars);

        String response = callAI(prompt, 0.8);
        return parseNamesResponse(response, surname);
    }

    /**
     * 分析名字
     */
    public Map<String, Object> analyzeName(String fullName) {
        String prompt = "请对姓名「" + fullName + "」进行全面深度分析。\n\n" +
                "返回JSON：\n" +
                "{\n" +
                "  \"characters_analysis\": [{\"char\":\"字\",\"pinyin\":\"拼音\",\"meaning\":\"字义\",\"wuxing\":\"五行\"}],\n" +
                "  \"overall_meaning\": \"整体寓意\",\n" +
                "  \"sound_analysis\": \"音韵分析\",\n" +
                "  \"cultural_source\": \"文化出处（无则null）\",\n" +
                "  \"source_text\": \"原文（无则null）\",\n" +
                "  \"famous_people\": [],\n" +
                "  \"pros\": [],\n" +
                "  \"cons\": [],\n" +
                "  \"scores\": {\"total\":85,\"meaning\":90,\"sound\":85,\"culture\":80,\"modernity\":82},\n" +
                "  \"ai_comment\": \"AI总评\"\n" +
                "}\n只返回JSON。";

        String response = callAI(prompt, 0.7);
        return parseJsonResponse(response);
    }

    /**
     * 构建取名 Prompt
     */
    private String buildNamingPrompt(
            String surname, String gender, String style, String meaning,
            int charCount, Map<String, Object> bazi, Map<String, Object> wuxing,
            String avoid, String prefer) {

        StringBuilder sb = new StringBuilder();
        sb.append("你是精通中国传统文化、诗词典故、易经八卦的取名大师。\n\n");
        sb.append("## 基本信息\n");
        sb.append("- 姓氏：").append(surname).append("\n");
        sb.append("- 性别：").append(gender).append("\n");
        sb.append("- 字数：").append(charCount).append("个字（不含姓）\n");
        sb.append("- 风格：").append(style).append("\n");
        sb.append("- 期望寓意：").append(meaning != null && !meaning.isEmpty() ? meaning : "无特别要求").append("\n");

        if (avoid != null && !avoid.isEmpty()) {
            sb.append("- 避讳字：").append(avoid).append("\n");
        }
        if (prefer != null && !prefer.isEmpty()) {
            sb.append("- 偏好字：").append(prefer).append("\n");
        }

        if (bazi != null && wuxing != null) {
            sb.append("\n## 八字\n");
            sb.append("- 年柱：").append(bazi.get("year")).append("  月柱：").append(bazi.get("month")).append("\n");
            sb.append("- 日柱：").append(bazi.get("day")).append("  时柱：").append(bazi.get("hour")).append("\n");
            sb.append("\n## 五行\n");
            sb.append("- 统计：").append(wuxing.get("count")).append("\n");
            sb.append("- 缺失：").append(wuxing.get("lack")).append("\n");
            sb.append("- 建议：").append(wuxing.get("suggestion")).append("\n");
        }

        sb.append("\n## 要求\n");
        sb.append("1. 推荐8个名字\n2. 避免烂大街名字\n3. 考虑五行平衡\n4. 有文化底蕴\n5. 避免生僻字和多音字\n\n");
        sb.append("## 输出（严格JSON数组）\n");
        sb.append("[\n  {\n");
        sb.append("    \"given_name\":\"名\",\"full_name\":\"姓+名\",\n");
        sb.append("    \"characters\":[{\"char\":\"字\",\"pinyin\":\"拼音\",\"meaning\":\"字义\",\"wuxing\":\"五行\",\"strokes\":10}],\n");
        sb.append("    \"overall_meaning\":\"寓意\",\"source\":\"出处\",\"source_text\":\"原文\",\n");
        sb.append("    \"sound_analysis\":\"音韵分析\",\"style_tags\":[\"标签\"],\n");
        sb.append("    \"scores\":{\"total\":85,\"meaning\":90,\"sound\":85,\"wuxing\":80,\"culture\":88,\"modernity\":82},\n");
        sb.append("    \"ai_comment\":\"推荐理由\"\n");
        sb.append("  }\n]\n只返回JSON。");

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseNamesResponse(String response, String surname) {
        try {
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start >= 0 && end > start) {
                String jsonStr = response.substring(start, end + 1);
                List<Map<String, Object>> names = (List<Map<String, Object>>)
                        JSON.parse(jsonStr);

                for (Map<String, Object> name : names) {
                    if (!name.containsKey("full_name") || name.get("full_name") == null) {
                        name.put("full_name", surname + name.getOrDefault("given_name", ""));
                    }
                }
                return names;
            }
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String response) {
        try {
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String jsonStr = response.substring(start, end + 1);
                return (Map<String, Object>) JSON.parse(jsonStr);
            }
        } catch (Exception e) {
            log.error("解析JSON失败: {}", e.getMessage());
        }
        return new HashMap<>();
    }
}