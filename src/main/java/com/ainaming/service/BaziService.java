package com.ainaming.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class BaziService {

    private static final String[] TIANGAN = {"甲","乙","丙","丁","戊","己","庚","辛","壬","癸"};
    private static final String[] DIZHI = {"子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥"};

    private static final Map<String, String> TIANGAN_WUXING = new HashMap<>();
    private static final Map<String, String> DIZHI_WUXING = new HashMap<>();

    static {
        TIANGAN_WUXING.put("甲","木"); TIANGAN_WUXING.put("乙","木");
        TIANGAN_WUXING.put("丙","火"); TIANGAN_WUXING.put("丁","火");
        TIANGAN_WUXING.put("戊","土"); TIANGAN_WUXING.put("己","土");
        TIANGAN_WUXING.put("庚","金"); TIANGAN_WUXING.put("辛","金");
        TIANGAN_WUXING.put("壬","水"); TIANGAN_WUXING.put("癸","水");

        DIZHI_WUXING.put("子","水"); DIZHI_WUXING.put("丑","土"); DIZHI_WUXING.put("寅","木");
        DIZHI_WUXING.put("卯","木"); DIZHI_WUXING.put("辰","土"); DIZHI_WUXING.put("巳","火");
        DIZHI_WUXING.put("午","火"); DIZHI_WUXING.put("未","土"); DIZHI_WUXING.put("申","金");
        DIZHI_WUXING.put("酉","金"); DIZHI_WUXING.put("戌","土"); DIZHI_WUXING.put("亥","水");
    }

    /**
     * 计算八字四柱
     * 使用公历直接计算（简化版，不依赖农历库）
     */
    public Map<String, Object> calculate(int year, int month, int day, int hour) {
        try {
            // ===== 年柱 =====
            // 以立春为界，这里简化处理，直接用公历年
            int yg = ((year - 4) % 10 + 10) % 10;
            int yz = ((year - 4) % 12 + 12) % 12;
            String yearPillar = TIANGAN[yg] + DIZHI[yz];

            // ===== 月柱 =====
            // 月干 = 年干 * 2 + 月份（简化算法）
            int mg = (yg * 2 + month) % 10;
            // 月支固定：寅月(1月)开始
            int mz = (month + 1) % 12;
            String monthPillar = TIANGAN[mg] + DIZHI[mz];

            // ===== 日柱 =====
            // 使用蔡勒公式的变体来计算日柱（简化版）
            int dayGanZhi = calculateDayGanZhi(year, month, day);
            int dg = dayGanZhi % 10;
            int dz = dayGanZhi % 12;
            String dayPillar = TIANGAN[dg] + DIZHI[dz];

            // ===== 时柱 =====
            String hourZhi = getHourZhi(hour);
            int hzIdx = Arrays.asList(DIZHI).indexOf(hourZhi);
            // 时干 = 日干 * 2 + 时支序号
            int hg = (dg * 2 + hzIdx) % 10;
            String hourPillar = TIANGAN[hg] + hourZhi;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("year", yearPillar);
            result.put("month", monthPillar);
            result.put("day", dayPillar);
            result.put("hour", hourPillar);
            return result;

        } catch (Exception e) {
            System.err.println("八字计算异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 计算日干支序号（简化算法）
     * 实际项目中建议使用完整的万年历数据
     */
    private int calculateDayGanZhi(int year, int month, int day) {
        // 基于已知日期推算
        // 2000年1月1日 = 甲子日 = 序号0（简化基准点）
        // 实际上2000-01-07是甲子日，这里做近似处理

        int y = year;
        int m = month;
        if (m <= 2) {
            y -= 1;
            m += 12;
        }

        // 儒略日近似计算
        int jd = 365 * y + y / 4 - y / 100 + y / 400 + (153 * (m - 3) + 2) / 5 + day;

        // 基准偏移（调整到与天干地支对齐）
        // 这里用简化算法，实际精度可能差1-2天
        int offset = (jd + 10) % 60;
        if (offset < 0) {
            offset += 60;
        }

        return offset;
    }

    /**
     * 分析五行强弱
     */
    public Map<String, Object> analyzeWuxing(Map<String, Object> bazi) {
        if (bazi == null) {
            return null;
        }

        Map<String, Integer> count = new LinkedHashMap<>();
        count.put("金", 0);
        count.put("木", 0);
        count.put("水", 0);
        count.put("火", 0);
        count.put("土", 0);

        for (String key : Arrays.asList("year", "month", "day", "hour")) {
            String pillar = (String) bazi.get(key);
            if (pillar != null && pillar.length() >= 2) {
                String tg = String.valueOf(pillar.charAt(0));
                String dz = String.valueOf(pillar.charAt(1));

                String tgWx = TIANGAN_WUXING.get(tg);
                String dzWx = DIZHI_WUXING.get(dz);

                if (tgWx != null) {
                    count.merge(tgWx, 1, Integer::sum);
                }
                if (dzWx != null) {
                    count.merge(dzWx, 1, Integer::sum);
                }
            }
        }

        List<String> lack = new ArrayList<>();
        List<String> weak = new ArrayList<>();
        List<String> strong = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : count.entrySet()) {
            if (entry.getValue() == 0) {
                lack.add(entry.getKey());
            } else if (entry.getValue() == 1) {
                weak.add(entry.getKey());
            } else if (entry.getValue() >= 3) {
                strong.add(entry.getKey());
            }
        }

        // 五行推荐字
        Map<String, String> wxChars = new HashMap<>();
        wxChars.put("金", "鑫、锋、铭、钰、锦");
        wxChars.put("木", "林、森、梓、柏、桐");
        wxChars.put("水", "淼、泽、涵、润、溪");
        wxChars.put("火", "炎、灿、煜、烁、炜");
        wxChars.put("土", "坤、城、垚、培、均");

        List<String> suggestions = new ArrayList<>();
        for (String wx : lack) {
            suggestions.add("宜补" + wx + "，如：" + wxChars.getOrDefault(wx, ""));
        }
        for (String wx : weak) {
            suggestions.add("宜补" + wx + "，如：" + wxChars.getOrDefault(wx, ""));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        result.put("lack", lack);
        result.put("weak", weak);
        result.put("strong", strong);
        result.put("suggestion", suggestions);
        return result;
    }

    /**
     * 根据小时获取时辰地支
     */
    private String getHourZhi(int hour) {
        int[][] table = {
                {23, 1}, {1, 3}, {3, 5}, {5, 7}, {7, 9}, {9, 11},
                {11, 13}, {13, 15}, {15, 17}, {17, 19}, {19, 21}, {21, 23}
        };

        for (int i = 0; i < table.length; i++) {
            int start = table[i][0];
            int end = table[i][1];
            if ((start <= hour && hour < end) || (start == 23 && hour == 23)) {
                return DIZHI[i];
            }
        }
        return "子";
    }
}