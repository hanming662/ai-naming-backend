package com.ainaming.util;

import java.util.*;

public class StrokeUtils {

    private static final Map<Character, Integer> STROKE_DATA = new HashMap<>();

    static {
        // 常见姓氏
        STROKE_DATA.put('王', 4); STROKE_DATA.put('李', 7); STROKE_DATA.put('张', 11);
        STROKE_DATA.put('刘', 15); STROKE_DATA.put('陈', 16); STROKE_DATA.put('杨', 13);
        STROKE_DATA.put('赵', 14); STROKE_DATA.put('黄', 12); STROKE_DATA.put('周', 8);
        STROKE_DATA.put('吴', 7); STROKE_DATA.put('徐', 10); STROKE_DATA.put('孙', 10);
        STROKE_DATA.put('马', 10); STROKE_DATA.put('胡', 11); STROKE_DATA.put('朱', 6);
        STROKE_DATA.put('郭', 15); STROKE_DATA.put('何', 7); STROKE_DATA.put('林', 8);
        // 常用取名字
        STROKE_DATA.put('诗', 13); STROKE_DATA.put('雨', 8); STROKE_DATA.put('梦', 16);
        STROKE_DATA.put('欣', 8); STROKE_DATA.put('怡', 8); STROKE_DATA.put('悦', 11);
        STROKE_DATA.put('涵', 12); STROKE_DATA.put('萱', 15); STROKE_DATA.put('瑶', 15);
        STROKE_DATA.put('婷', 12); STROKE_DATA.put('慧', 15); STROKE_DATA.put('敏', 11);
        STROKE_DATA.put('浩', 11); STROKE_DATA.put('宇', 6); STROKE_DATA.put('轩', 10);
        STROKE_DATA.put('博', 12); STROKE_DATA.put('文', 4); STROKE_DATA.put('睿', 14);
        STROKE_DATA.put('泽', 17); STROKE_DATA.put('铭', 14); STROKE_DATA.put('辰', 7);
        STROKE_DATA.put('逸', 15); STROKE_DATA.put('晨', 11); STROKE_DATA.put('旭', 6);
        STROKE_DATA.put('明', 8); STROKE_DATA.put('昊', 8); STROKE_DATA.put('天', 4);
        STROKE_DATA.put('志', 7); STROKE_DATA.put('俊', 9); STROKE_DATA.put('杰', 12);
        STROKE_DATA.put('安', 6); STROKE_DATA.put('平', 5); STROKE_DATA.put('康', 11);
        STROKE_DATA.put('子', 3); STROKE_DATA.put('一', 1); STROKE_DATA.put('思', 9);
        STROKE_DATA.put('雅', 12); STROKE_DATA.put('若', 11); STROKE_DATA.put('清', 12);
        STROKE_DATA.put('佳', 8); STROKE_DATA.put('心', 4); STROKE_DATA.put('语', 14);
        STROKE_DATA.put('彤', 7); STROKE_DATA.put('妍', 7); STROKE_DATA.put('馨', 20);
        STROKE_DATA.put('瑞', 14); STROKE_DATA.put('德', 15); STROKE_DATA.put('沐', 8);
        STROKE_DATA.put('阳', 12); STROKE_DATA.put('星', 9); STROKE_DATA.put('景', 12);
        STROKE_DATA.put('墨', 15); STROKE_DATA.put('诺', 16); STROKE_DATA.put('奕', 9);
        STROKE_DATA.put('熙', 14); STROKE_DATA.put('知', 8); STROKE_DATA.put('意', 13);
        STROKE_DATA.put('锦', 16); STROKE_DATA.put('书', 10); STROKE_DATA.put('溪', 14);
    }

    public static int getStrokes(char ch) {
        if (STROKE_DATA.containsKey(ch)) {
            return STROKE_DATA.get(ch);
        }
        int code = (int) ch;
        if (code >= 0x4e00 && code <= 0x9fff) {
            return (code - 0x4e00) % 20 + 3;
        }
        return 10;
    }

    public static int getTotalStrokes(String name) {
        int total = 0;
        for (char ch : name.toCharArray()) {
            total += getStrokes(ch);
        }
        return total;
    }

    public static List<Map<String, Object>> getStrokesDetail(String name) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (char ch : name.toCharArray()) {
            Map<String, Object> map = new HashMap<>();
            map.put("char", String.valueOf(ch));
            map.put("strokes", getStrokes(ch));
            list.add(map);
        }
        return list;
    }
}