package com.ainaming.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class DateTimeUtils {

    private static final String[] ZODIAC = {"鼠","牛","虎","兔","龙","蛇","马","羊","猴","鸡","狗","猪"};

    private static final Map<String, Integer> SHICHEN_MAP = new LinkedHashMap<>();

    static {
        SHICHEN_MAP.put("子时", 0); SHICHEN_MAP.put("丑时", 2); SHICHEN_MAP.put("寅时", 4);
        SHICHEN_MAP.put("卯时", 6); SHICHEN_MAP.put("辰时", 8); SHICHEN_MAP.put("巳时", 10);
        SHICHEN_MAP.put("午时", 12); SHICHEN_MAP.put("未时", 14); SHICHEN_MAP.put("申时", 16);
        SHICHEN_MAP.put("酉时", 18); SHICHEN_MAP.put("戌时", 20); SHICHEN_MAP.put("亥时", 22);
    }

    public static LocalDate parseBirthDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        String[] formats = {"yyyy-MM-dd", "yyyy/MM/dd"};
        for (String fmt : formats) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(fmt));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    public static int parseBirthTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return 12;
        for (Map.Entry<String, Integer> entry : SHICHEN_MAP.entrySet()) {
            if (timeStr.contains(entry.getKey())) return entry.getValue();
        }
        try {
            String h = timeStr.replace("时", "").replace("点", "");
            if (h.contains(":")) h = h.split(":")[0];
            int hour = Integer.parseInt(h.trim());
            if (hour >= 0 && hour <= 23) return hour;
        } catch (Exception ignored) {}
        return 12;
    }

    public static String getShichen(int hour) {
        String[][] table = {
                {"23","1","子时"},{"1","3","丑时"},{"3","5","寅时"},{"5","7","卯时"},
                {"7","9","辰时"},{"9","11","巳时"},{"11","13","午时"},{"13","15","未时"},
                {"15","17","申时"},{"17","19","酉时"},{"19","21","戌时"},{"21","23","亥时"}
        };
        for (String[] row : table) {
            int s = Integer.parseInt(row[0]);
            int e = Integer.parseInt(row[1]);
            if ((s <= hour && hour < e) || (s == 23 && hour == 23)) return row[2];
        }
        return "子时";
    }

    public static String getZodiac(int year) {
        return ZODIAC[(year - 4) % 12];
    }

    public static String getConstellation(int month, int day) {
        String[] names = {"摩羯座","水瓶座","双鱼座","白羊座","金牛座","双子座",
                "巨蟹座","狮子座","处女座","天秤座","天蝎座","射手座","摩羯座"};
        int[] days = {20,19,21,20,21,22,23,23,23,23,22,22};
        int idx = month - 1;
        if (day < days[idx]) return names[idx];
        return names[idx + 1];
    }
}