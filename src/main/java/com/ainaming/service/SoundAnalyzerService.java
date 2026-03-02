package com.ainaming.service;

import com.ainaming.util.PinyinUtils;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SoundAnalyzerService {

    public Map<String, Object> analyzeTones(String name) {
        List<Integer> tones = new ArrayList<>();
        List<String> pinyins = new ArrayList<>();

        for (char ch : name.toCharArray()) {
            tones.add(PinyinUtils.getTone(ch));
            pinyins.add(PinyinUtils.getPinyin(ch));
        }

        int score = 80;
        List<String> comments = new ArrayList<>();

        // 所有字声调相同
        if (new HashSet<>(tones).size() == 1) {
            score -= 20;
            comments.add("所有字声调相同，缺少抑扬顿挫");
        }

        // 连续三字同声调
        for (int i = 0; i < tones.size() - 2; i++) {
            if (tones.get(i).equals(tones.get(i + 1)) && tones.get(i).equals(tones.get(i + 2))) {
                score -= 15;
                comments.add("连续三字同声调，读起来单调");
                break;
            }
        }

        // 有仄声
        boolean hasZe = tones.stream().anyMatch(t -> t == 3 || t == 4);
        if (hasZe) {
            score += 5;
            comments.add("平仄搭配得当");
        }

        // 尾字上扬
        if (!tones.isEmpty() && (tones.get(tones.size() - 1) == 1 || tones.get(tones.size() - 1) == 2)) {
            score += 5;
            comments.add("尾字声调上扬，余音悠远");
        }

        // 相邻声母相同
        for (int i = 0; i < pinyins.size() - 1; i++) {
            if (!pinyins.get(i).isEmpty() && !pinyins.get(i + 1).isEmpty()
                    && pinyins.get(i).charAt(0) == pinyins.get(i + 1).charAt(0)) {
                score -= 5;
                comments.add("相邻字声母相同，可能绕口");
                break;
            }
        }

        score = Math.max(0, Math.min(100, score));
        if (comments.isEmpty()) comments.add("音韵和谐，朗朗上口");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tones", tones);
        result.put("score", score);
        result.put("comments", comments);
        result.put("full_pinyin", PinyinUtils.getFullPinyin(name));
        return result;
    }

    public Map<String, Object> checkHomophone(String name) {
        String fullPy = PinyinUtils.getFullPinyinNoTone(name);

        String[][] badList = {
                {"siwang","死亡"}, {"shagua","傻瓜"}, {"bendan","笨蛋"},
                {"fangpi","放屁"}, {"goushi","狗屎"}, {"baimu","白目"}
        };

        List<String> warnings = new ArrayList<>();
        for (String[] bad : badList) {
            if (fullPy.contains(bad[0])) {
                warnings.add("可能有「" + bad[1] + "」谐音");
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("has_warning", !warnings.isEmpty());
        result.put("warnings", warnings);
        return result;
    }
}