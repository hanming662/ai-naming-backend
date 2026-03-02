package com.ainaming.service;

import com.ainaming.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NamingEngineService {

    private final AIService aiService;
    private final BaziService baziService;
    private final SancaiWugeService sancaiWugeService;
    private final SoundAnalyzerService soundAnalyzerService;
    private final DuplicateEstimatorService duplicateEstimatorService;

    /**
     * 流式生成接口
     */
    public void streamGenerateNames(
            String surname, String gender, String birthDate, String birthTime,
            String style, String meaning, String avoidChars, String preferChars,
            int charCount, SseEmitter emitter) {

        // 1. 参数验证
        if (!ChineseCharUtils.validateNamingRequest(surname, gender, charCount)) {
            try {
                emitter.send(SseEmitter.event().name("error").data("参数验证失败"));
                emitter.complete();
            } catch (Exception e) {}
            return;
        }

        // 2. 八字计算 (同之前逻辑)
        Map<String, Object> baziInfo = null;
        Map<String, Object> wuxingAnalysis = null;
        if (birthDate != null && !birthDate.isEmpty()) {
            LocalDate dt = DateTimeUtils.parseBirthDate(birthDate);
            if (dt != null) {
                int hour = DateTimeUtils.parseBirthTime(birthTime);
                baziInfo = baziService.calculate(dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth(), hour);
                if (baziInfo != null) {
                    wuxingAnalysis = baziService.analyzeWuxing(baziInfo);
                }
            }
        }


    }

    /**
     * AI 智能取名
     */
    public Map<String, Object> generateNames(
            String surname, String gender, String birthDate, String birthTime,
            String style, String meaning, String avoidChars, String preferChars,
            int charCount) {

        if (!ChineseCharUtils.validateNamingRequest(surname, gender, charCount)) {
            return errorResult("参数验证失败");
        }

        meaning = ChineseCharUtils.sanitizeInput(meaning);
        avoidChars = ChineseCharUtils.sanitizeInput(avoidChars);
        preferChars = ChineseCharUtils.sanitizeInput(preferChars);

        // 八字
        Map<String, Object> baziInfo = null;
        Map<String, Object> wuxingAnalysis = null;
        String zodiac = null, constellation = null;

        if (birthDate != null && !birthDate.isEmpty()) {
            LocalDate dt = DateTimeUtils.parseBirthDate(birthDate);
            if (dt != null) {
                int hour = DateTimeUtils.parseBirthTime(birthTime);
                zodiac = DateTimeUtils.getZodiac(dt.getYear());
                constellation = DateTimeUtils.getConstellation(dt.getMonthValue(), dt.getDayOfMonth());
                baziInfo = baziService.calculate(dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth(), hour);
                if (baziInfo != null) {
                    baziInfo.put("shichen", DateTimeUtils.getShichen(hour));
                    wuxingAnalysis = baziService.analyzeWuxing(baziInfo);
                }
            }
        }

        // AI 生成
        List<Map<String, Object>> aiNames = aiService.generateNames(
                surname, gender, style, meaning, charCount,
                baziInfo, wuxingAnalysis, avoidChars, preferChars);

        // 本地增强
        List<Map<String, Object>> enhanced = new ArrayList<>();
        for (Map<String, Object> name : aiNames) {
            enhanced.add(enhanceName(name, surname));
        }
        enhanced.sort((a, b) -> {
            int sa = getNestedScore(a);
            int sb = getNestedScore(b);
            return sb - sa;
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("names", enhanced);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("surname", surname);
        meta.put("gender", gender);
        meta.put("style", style);
        meta.put("total_count", enhanced.size());
        meta.put("generated_at", LocalDateTime.now().toString());
        result.put("meta", meta);

        if (baziInfo != null) {
            result.put("bazi_info", baziInfo);
        }
        if (wuxingAnalysis != null) {
            result.put("wuxing_analysis", wuxingAnalysis);
        }
        if (zodiac != null) {
            result.put("zodiac", zodiac);
        }
        if (constellation != null) {
            result.put("constellation", constellation);
        }

        return result;
    }

    /**
     * 名字深度解析
     */
    public Map<String, Object> analyzeName(String fullName, String birthDate, String birthTime) {
        log.info("参数：[{}]",fullName);
        if (!ChineseCharUtils.validateName(fullName)) {
            return errorResult("姓名格式不正确");
        }

        Map<String, Object> aiAnalysis = aiService.analyzeName(fullName);
        Map<String, Object> sancaiWuge = sancaiWugeService.calculate(fullName);
        Map<String, Object> soundAnalysis = soundAnalyzerService.analyzeTones(fullName);
        Map<String, Object> homoCheck = soundAnalyzerService.checkHomophone(fullName);
        Map<String, Object> dupInfo = duplicateEstimatorService.estimate(fullName);

        // 字义五行
        List<Map<String, Object>> charWuxing = new ArrayList<>();
        for (char c : fullName.toCharArray()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("char", String.valueOf(c));
            String wx = ChineseCharUtils.getWuxingByChar(String.valueOf(c));
            m.put("wuxing", wx != null ? wx : "未知");
            m.put("pinyin", PinyinUtils.getPinyinWithTone(c));
            m.put("strokes", StrokeUtils.getStrokes(c));
            charWuxing.add(m);
        }

        // 多音字
        List<String> polyphonic = new ArrayList<>();
        for (char c : fullName.toCharArray()) {
            if (PinyinUtils.isPolyphonic(c)) {
                polyphonic.add(String.valueOf(c));
            }
        }

        // 基本信息
        Map<String, Object> basicInfo = new LinkedHashMap<>();
        basicInfo.put("full_name", fullName);
        basicInfo.put("surname", String.valueOf(fullName.charAt(0)));
        basicInfo.put("given_name", fullName.substring(1));
        basicInfo.put("pinyin", PinyinUtils.getFullPinyin(fullName));
        basicInfo.put("total_strokes", StrokeUtils.getTotalStrokes(fullName));
        basicInfo.put("strokes_detail", StrokeUtils.getStrokesDetail(fullName));
        basicInfo.put("has_polyphonic", !polyphonic.isEmpty());
        basicInfo.put("polyphonic_chars", polyphonic);

        // 八字
        Map<String, Object> baziInfo = null;
        Map<String, Object> wuxingAnalysis = null;
        if (birthDate != null && !birthDate.isEmpty()) {
            LocalDate dt = DateTimeUtils.parseBirthDate(birthDate);
            if (dt != null) {
                int hour = DateTimeUtils.parseBirthTime(birthTime);
                baziInfo = baziService.calculate(dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth(), hour);
                if (baziInfo != null) {
                    wuxingAnalysis = baziService.analyzeWuxing(baziInfo);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("basic_info", basicInfo);
        result.put("ai_analysis", aiAnalysis);
        result.put("characters_wuxing", charWuxing);
        result.put("sancai_wuge", sancaiWuge);
        result.put("sound_analysis", soundAnalysis);
        result.put("homophone_check", homoCheck);
        result.put("duplicate_info", dupInfo);
        result.put("bazi_info", baziInfo);
        result.put("wuxing_analysis", wuxingAnalysis);
        result.put("analyzed_at", LocalDateTime.now().toString());
        return result;
    }

    /**
     * 名字 PK 对比
     */
    public Map<String, Object> compareNames(List<String> names, String birthDate, String birthTime) {
        if (names.size() < 2) {
            return errorResult("至少2个名字");
        }
        if (names.size() > 5) {
            return errorResult("最多5个名字");
        }

        List<Map<String, Object>> analyses = new ArrayList<>();
        List<Map<String, Object>> comparison = new ArrayList<>();

        for (String name : names) {
            Map<String, Object> analysis = analyzeName(name, birthDate, birthTime);
            analyses.add(analysis);

            @SuppressWarnings("unchecked")
            Map<String, Object> aiScores = (Map<String, Object>) ((Map<String, Object>)
                    analysis.getOrDefault("ai_analysis", new HashMap<>())).getOrDefault("scores", new HashMap<>());

            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("name", name);
            comp.put("total", getIntValue(aiScores, "total", 75));
            comp.put("meaning", getIntValue(aiScores, "meaning", 75));
            comp.put("sound", getIntValue((Map<String, Object>) analysis.getOrDefault("sound_analysis", new HashMap<>()), "score", 75));
            comp.put("culture", getIntValue(aiScores, "culture", 75));
            comp.put("modernity", getIntValue(aiScores, "modernity", 75));
            comparison.add(comp);
        }

        String bestName = comparison.stream()
                .max(Comparator.comparingInt(c -> (int) c.getOrDefault("total", 0)))
                .map(c -> (String) c.get("name"))
                .orElse("");

        // AI推荐
        String recommendation;
        try {
            String prompt = "请对比以下名字并给出推荐：" + String.join("、", names) +
                    "\n简短点评每个名字，然后给出首选推荐和理由。";
            recommendation = aiService.callAI(prompt, 0.7);
        } catch (Exception e) {
            recommendation = "AI推荐暂不可用";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("names", names);
        result.put("comparison", comparison);
        result.put("best_name", bestName);
        result.put("recommendation", recommendation);
        return result;
    }

    /**
     * 快速评分（不调AI）
     */
    public Map<String, Object> scoreName(String fullName) {
        if (!ChineseCharUtils.validateName(fullName)) {
            return errorResult("姓名格式不正确");
        }

        Map<String, Object> sound = soundAnalyzerService.analyzeTones(fullName);
        Map<String, Object> homo = soundAnalyzerService.checkHomophone(fullName);
        Map<String, Object> sw = sancaiWugeService.calculate(fullName);
        Map<String, Object> dup = duplicateEstimatorService.estimate(fullName);
        int totalStrokes = StrokeUtils.getTotalStrokes(fullName);

        int soundScore = (int) sound.getOrDefault("score", 75);

        // 三才五格评分
        int sancaiScore = 75;
        if (sw != null) {
            Map<String, Integer> jxMap = new HashMap<>();
            jxMap.put("大吉", 95); jxMap.put("吉", 85); jxMap.put("半吉", 70); jxMap.put("凶", 50);
            int sum = 0, cnt = 0;
            for (String key : Arrays.asList("tiange", "renge", "dige", "waige", "zongge")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ge = (Map<String, Object>) sw.get(key);
                if (ge != null) {
                    String jx = (String) ge.getOrDefault("jixiong", "半吉");
                    sum += jxMap.getOrDefault(jx, 65);
                    cnt++;
                }
            }
            if (cnt > 0) {
                sancaiScore = sum / cnt;
            }
        }

        int strokesScore = 80;
        if (totalStrokes > 40) {
            strokesScore = 60;
        } else if (totalStrokes > 30) {
            strokesScore = 70;
        } else if (totalStrokes < 10) {
            strokesScore = 70;
        }

        boolean hasWarning = (boolean) homo.getOrDefault("has_warning", false);
        int homoPen = hasWarning ? 10 : 0;

        int polyPen = 0;
        for (char c : fullName.toCharArray()) {
            if (PinyinUtils.isPolyphonic(c)) {
                polyPen += 3;
            }
        }

        String level = (String) dup.getOrDefault("level", "");
        int dupPen = "大众".equals(level) ? 8 : ("普通".equals(level) ? 3 : 0);

        int total = (int) (soundScore * 0.3 + sancaiScore * 0.35 + strokesScore * 0.15 + 80 * 0.2)
                - homoPen - polyPen - dupPen;
        total = Math.max(50, Math.min(99, total));

        String comment;
        if (total >= 90) {
            comment = "优秀！非常好的名字";
        } else if (total >= 80) {
            comment = "良好！各方面都不错";
        } else if (total >= 70) {
            comment = "中等，可以优化";
        } else if (total >= 60) {
            comment = "一般，建议调整";
        } else {
            comment = "较弱，建议重新考虑";
        }

        Map<String, Object> detailScores = new LinkedHashMap<>();
        detailScores.put("sound", soundScore);
        detailScores.put("sancai", sancaiScore);
        detailScores.put("strokes", strokesScore);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("full_name", fullName);
        result.put("total_score", total);
        result.put("detail_scores", detailScores);
        result.put("sound_analysis", sound);
        result.put("sancai_wuge", sw);
        result.put("duplicate_info", dup);
        result.put("homophone_check", homo);
        result.put("total_strokes", totalStrokes);
        result.put("comment", comment);
        return result;
    }

    // ---------- 内部方法 ----------

    @SuppressWarnings("unchecked")
    private Map<String, Object> enhanceName(Map<String, Object> nameData, String surname) {
        String fn = (String) nameData.getOrDefault("full_name",
                surname + nameData.getOrDefault("given_name", ""));
        nameData.put("full_name", fn);
        nameData.put("sancai_wuge", sancaiWugeService.calculate(fn));
        nameData.put("sound_detail", soundAnalyzerService.analyzeTones(fn));
        nameData.put("homophone_check", soundAnalyzerService.checkHomophone(fn));
        nameData.put("duplicate_info", duplicateEstimatorService.estimate(fn));
        nameData.put("strokes_detail", StrokeUtils.getStrokesDetail(fn));
        nameData.put("total_strokes", StrokeUtils.getTotalStrokes(fn));

        List<String> poly = new ArrayList<>();
        for (char c : fn.toCharArray()) {
            if (PinyinUtils.isPolyphonic(c)) {
                poly.add(String.valueOf(c));
            }
        }
        nameData.put("has_polyphonic", !poly.isEmpty());
        nameData.put("polyphonic_chars", poly);

        nameData.put("scores", recalcScores(nameData));
        return nameData;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> recalcScores(Map<String, Object> nd) {
        Map<String, Object> ai = (Map<String, Object>) nd.getOrDefault("scores", new HashMap<>());
        int meaning = getIntValue(ai, "meaning", 80);
        int culture = getIntValue(ai, "culture", 75);
        int modernity = getIntValue(ai, "modernity", 80);
        int wuxing = getIntValue(ai, "wuxing", 75);
        int sound = getIntValue((Map<String, Object>) nd.getOrDefault("sound_detail", new HashMap<>()), "score", 80);

        int sancai = 75;
        Map<String, Object> sw = (Map<String, Object>) nd.get("sancai_wuge");
        if (sw != null) {
            Map<String, Integer> jxMap = new HashMap<>();
            jxMap.put("大吉", 95); jxMap.put("吉", 85); jxMap.put("半吉", 70); jxMap.put("凶", 50);
            int sum = 0, cnt = 0;
            for (String key : Arrays.asList("tiange", "renge", "dige", "waige", "zongge")) {
                Map<String, Object> ge = (Map<String, Object>) sw.get(key);
                if (ge != null) {
                    sum += jxMap.getOrDefault(ge.getOrDefault("jixiong", "半吉"), 65);
                    cnt++;
                }
            }
            if (cnt > 0) {
                sancai = sum / cnt;
            }
        }

        Map<String, Object> homoCheck = (Map<String, Object>) nd.getOrDefault("homophone_check", new HashMap<>());
        int hp = Boolean.TRUE.equals(homoCheck.get("has_warning")) ? 10 : 0;
        int pp = Boolean.TRUE.equals(nd.get("has_polyphonic")) ? 5 : 0;

        Map<String, Object> dupInfo = (Map<String, Object>) nd.getOrDefault("duplicate_info", new HashMap<>());
        String level = (String) dupInfo.getOrDefault("level", "");
        int dp = "大众".equals(level) ? 8 : ("普通".equals(level) ? 3 : 0);

        int total = (int) (meaning * 0.25 + sound * 0.20 + wuxing * 0.15 + sancai * 0.15
                + culture * 0.15 + modernity * 0.10) - hp - pp - dp;
        total = Math.max(50, Math.min(99, total));

        Map<String, Object> scores = new LinkedHashMap<>();
        scores.put("total", total);
        scores.put("meaning", meaning);
        scores.put("sound", sound);
        scores.put("wuxing", wuxing);
        scores.put("sancai", sancai);
        scores.put("culture", culture);
        scores.put("modernity", modernity);
        return scores;
    }

    @SuppressWarnings("unchecked")
    private int getNestedScore(Map<String, Object> name) {
        Map<String, Object> scores = (Map<String, Object>) name.getOrDefault("scores", new HashMap<>());
        return getIntValue(scores, "total", 0);
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultVal) {
        if (map == null) {
            return defaultVal;
        }
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultVal;
    }

    private Map<String, Object> errorResult(String msg) {
        Map<String, Object> result = new HashMap<>();
        result.put("error", msg);
        return result;
    }
}