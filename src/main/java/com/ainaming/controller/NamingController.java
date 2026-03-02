package com.ainaming.controller;

import com.ainaming.dto.*;
import com.ainaming.entity.NameScore;
import com.ainaming.mapper.NameScoreMapper;
import com.ainaming.service.NamingEngineService;
import com.ainaming.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NamingController {

    private final NamingEngineService namingEngine;
    private final UserService userService;
    private final NameScoreMapper nameScoreMapper;

    @GetMapping("/")
    public ApiResponse<Map<String, String>> root() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("app", "AI智能取名");
        info.put("version", "1.0.0");
        info.put("status", "running");
        return ApiResponse.success(info);
    }

    /**
     * 流式取名接口
     * GET 请求更适合 SSE，参数通过 URL 传递
     */
    @GetMapping(value = "/stream/generate-names", produces = "text/event-stream")
    public SseEmitter streamGenerateNames(
            @RequestParam String openid,
            @RequestParam String surname,
            @RequestParam String gender,
            @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) String birthTime,
            @RequestParam(defaultValue = "现代文雅") String style,
            @RequestParam(required = false) String meaning,
            @RequestParam(required = false) String avoidChars,
            @RequestParam(required = false) String preferChars,
            @RequestParam(defaultValue = "2") Integer charCount
    ) {
        // 设置超时时间为 2 分钟 (120000ms)
        SseEmitter emitter = new SseEmitter(120000L);

        // 检查配额
        if (!userService.checkQuota(openid)) {
            try {
                emitter.send(SseEmitter.event().name("error").data("今日免费次数已用完"));
                emitter.complete();
            } catch (Exception e) {}
            return emitter;
        }

        namingEngine.streamGenerateNames(
                surname, gender, birthDate, birthTime, style,
                meaning, avoidChars, preferChars, charCount, emitter
        );

        return emitter;
    }

    @PostMapping("/generate-names")
    @SuppressWarnings("unchecked")
    public ApiResponse<Map<String, Object>> generateNames(@RequestBody NamingRequest req) {
        // 检查配额
        if (!userService.checkQuota(req.getOpenid())) {
            return ApiResponse.error(403, "今日免费次数已用完，请升级VIP");
        }

        // 调用取名引擎
        Map<String, Object> result = namingEngine.generateNames(
                req.getSurname(), req.getGender(),
                req.getBirthDate(), req.getBirthTime(),
                req.getStyle(), req.getMeaningPreference(),
                req.getAvoidChars(), req.getPreferChars(),
                req.getCharCount());

        if (result.containsKey("error")) {
            return ApiResponse.error(400, (String) result.get("error"));
        }

        // 保存记录到数据库
        List<Map<String, Object>> names = (List<Map<String, Object>>) result.get("names");
        if (names != null) {
            for (Map<String, Object> n : names) {
                try {
                    NameScore record = new NameScore();
                    record.setFullName((String) n.getOrDefault("full_name", ""));
                    record.setSurname(req.getSurname());
                    record.setGivenName((String) n.getOrDefault("given_name", ""));
                    record.setGender(req.getGender());

                    Map<String, Object> scores = (Map<String, Object>)
                            n.getOrDefault("scores", new HashMap<>());
                    record.setTotalScore(getInt(scores, "total"));
                    record.setMeaningScore(getInt(scores, "meaning"));
                    record.setSoundScore(getInt(scores, "sound"));
                    record.setWuxingScore(getInt(scores, "wuxing"));
                    record.setSancaiScore(getInt(scores, "sancai"));
                    record.setCultureScore(getInt(scores, "culture"));
                    record.setModernityScore(getInt(scores, "modernity"));
                    record.setAiComment((String) n.getOrDefault("ai_comment", ""));
                    record.setCreatedAt(LocalDateTime.now());

                    nameScoreMapper.insert(record);
                } catch (Exception ignored) {
                }
            }
        }

        // 返回剩余次数
        result.put("free_times_remaining", userService.getRemaining(req.getOpenid()));
        return ApiResponse.success("生成成功", result);
    }

    @PostMapping("/analyze-name")
    public ApiResponse<Map<String, Object>> analyzeName(@RequestBody AnalyzeRequest req) {
        Map<String, Object> result = namingEngine.analyzeName(
                req.getFullName(), req.getBirthDate(), req.getBirthTime());
        if (result.containsKey("error")) {
            return ApiResponse.error(400, (String) result.get("error"));
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/compare-names")
    public ApiResponse<Map<String, Object>> compareNames(@RequestBody CompareRequest req) {
        Map<String, Object> result = namingEngine.compareNames(
                req.getNames(), req.getBirthDate(), req.getBirthTime());
        if (result.containsKey("error")) {
            return ApiResponse.error(400, (String) result.get("error"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/score-name")
    public ApiResponse<Map<String, Object>> scoreName(@RequestParam String name) {
        Map<String, Object> result = namingEngine.scoreName(name);
        if (result.containsKey("error")) {
            return ApiResponse.error(400, (String) result.get("error"));
        }
        return ApiResponse.success(result);
    }

    private int getInt(Map<String, Object> map, String key) {
        if (map == null) return 0;
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }
}