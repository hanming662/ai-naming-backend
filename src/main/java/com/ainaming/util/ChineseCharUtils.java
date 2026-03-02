package com.ainaming.util;

import java.util.*;
import java.util.regex.Pattern;

public class ChineseCharUtils {

    private static final Pattern CHINESE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]+$");

    // 五行对应字
    private static final Map<String, List<String>> WUXING_CHARS = new HashMap<>();

    static {
        WUXING_CHARS.put("金", Arrays.asList("鑫","锋","铭","钰","锦","银","钊","铂","铃","锐","钧","铠","锡","钦","镇"));
        WUXING_CHARS.put("木", Arrays.asList("林","森","梓","柏","桐","楠","松","柳","桦","榕","杉","楷","栩","槿","橙"));
        WUXING_CHARS.put("水", Arrays.asList("淼","泽","涵","润","溪","澜","清","江","海","洋","波","涛","浩","渊","潮"));
        WUXING_CHARS.put("火", Arrays.asList("炎","焱","灿","煜","烁","炜","晖","烈","焰","熠","炫","燊","煊","焕","炳"));
        WUXING_CHARS.put("土", Arrays.asList("坤","城","垚","培","均","圣","堃","墨","增","坚","域","塘","境","堂","坊"));
    }

    public static boolean isValidChinese(String text) {
        return text != null && CHINESE_PATTERN.matcher(text).matches();
    }

    public static String getWuxingByChar(String ch) {
        for (Map.Entry<String, List<String>> entry : WUXING_CHARS.entrySet()) {
            if (entry.getValue().contains(ch)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static List<String> getCharsByWuxing(String wuxing, int count) {
        List<String> chars = WUXING_CHARS.getOrDefault(wuxing, new ArrayList<>());
        if (chars.size() <= count) {
            return new ArrayList<>(chars);
        }
        List<String> copy = new ArrayList<>(chars);
        Collections.shuffle(copy);
        return copy.subList(0, count);
    }

    public static boolean validateNamingRequest(String surname, String gender, int charCount) {
        if (surname == null || surname.isEmpty()) {
            return false;
        }
        if (!isValidChinese(surname)) {
            return false;
        }
        if (surname.length() > 2) {
            return false;
        }
        if (!"男".equals(gender) && !"女".equals(gender) && !"中性".equals(gender)) {
            return false;
        }
        if (charCount < 1 || charCount > 3) {
            return false;
        }
        return true;
    }

    public static boolean validateName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return false;
        }
        if (!isValidChinese(fullName)) {
            return false;
        }
        return fullName.length() >= 2 && fullName.length() <= 5;
    }

    public static String sanitizeInput(String text) {
        if (text == null) {
            return "";
        }
        text = text.trim();
        if (text.length() > 200) {
            text = text.substring(0, 200);
        }
        return text;
    }
}