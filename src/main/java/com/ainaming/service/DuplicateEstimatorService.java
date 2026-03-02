package com.ainaming.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DuplicateEstimatorService {

    private static final Map<String, Double> SURNAME_POP = new HashMap<>();
    private static final Map<String, Double> HOT_NAMES = new HashMap<>();

    static {
        SURNAME_POP.put("王",0.072); SURNAME_POP.put("李",0.071); SURNAME_POP.put("张",0.068);
        SURNAME_POP.put("刘",0.054); SURNAME_POP.put("陈",0.054); SURNAME_POP.put("杨",0.038);
        SURNAME_POP.put("黄",0.029); SURNAME_POP.put("赵",0.027); SURNAME_POP.put("吴",0.025);
        SURNAME_POP.put("周",0.025);

        HOT_NAMES.put("子轩",0.9); HOT_NAMES.put("梓涵",0.9); HOT_NAMES.put("浩宇",0.85);
        HOT_NAMES.put("子涵",0.88); HOT_NAMES.put("欣怡",0.85); HOT_NAMES.put("一诺",0.82);
        HOT_NAMES.put("宇轩",0.85); HOT_NAMES.put("子豪",0.80);
    }

    public Map<String, Object> estimate(String fullName) {
        long total = 1_400_000_000L;
        String surname = String.valueOf(fullName.charAt(0));
        String given = fullName.substring(1);

        double ratio = SURNAME_POP.getOrDefault(surname, 0.005);
        long sPop = (long) (total * ratio);

        double hot = HOT_NAMES.getOrDefault(given, 0.0);
        long count;
        if (hot > 0) {
            count = (long) (sPop * hot * 0.001);
        } else {
            count = (long) (sPop * 0.0001);
        }
        count = Math.max(10, count);

        String level, color, comment;
        if (count > 50000) {
            level = "大众"; color = "red"; comment = "重名较多，建议换名";
        } else if (count > 10000) {
            level = "普通"; color = "orange"; comment = "有一定重名率";
        } else if (count > 1000) {
            level = "较少"; color = "blue"; comment = "重名较少，不错的选择";
        } else {
            level = "稀有"; color = "green"; comment = "非常独特";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("estimated_count", count);
        result.put("level", level);
        result.put("level_color", color);
        result.put("comment", comment);
        return result;
    }
}