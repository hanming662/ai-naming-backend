package com.ainaming.service;

import com.ainaming.util.StrokeUtils;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SancaiWugeService {

    private static final Set<Integer> JI_NUMBERS = new HashSet<>(Arrays.asList(
            1,3,5,6,7,8,11,13,15,16,17,18,21,23,24,25,29,31,32,33,35,37,39,41,45,47,48
    ));

    public Map<String, Object> calculate(String fullName) {
        if (fullName == null || fullName.length() < 2) return null;

        char surname = fullName.charAt(0);
        String given = fullName.substring(1);

        int ss = StrokeUtils.getStrokes(surname);
        int[] gs = new int[given.length()];
        int gsSum = 0;
        for (int i = 0; i < given.length(); i++) {
            gs[i] = StrokeUtils.getStrokes(given.charAt(i));
            gsSum += gs[i];
        }

        int tiange = ss + 1;
        int renge = ss + (gs.length > 0 ? gs[0] : 0);
        int dige = gsSum > 0 ? gsSum : 1;
        int waige = (gs.length > 1 ? gs[gs.length - 1] : 0) + 1;
        int zongge = ss + gsSum;

        String[] wxMap = {"水","木","木","火","火","土","土","金","金","水"};

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tiange", buildGe(tiange));
        result.put("renge", buildGe(renge));
        result.put("dige", buildGe(dige));
        result.put("waige", buildGe(waige));
        result.put("zongge", buildGe(zongge));

        Map<String, String> sancai = new LinkedHashMap<>();
        sancai.put("tian", wxMap[tiange % 10]);
        sancai.put("ren", wxMap[renge % 10]);
        sancai.put("di", wxMap[dige % 10]);
        result.put("sancai", sancai);

        return result;
    }

    private Map<String, Object> buildGe(int value) {
        int v = value % 81;
        if (v == 0) {
            v = 81;
        }
        Map<String, Object> ge = new LinkedHashMap<>();
        ge.put("value", v);
        ge.put("jixiong", getJixiong(v));
        return ge;
    }

    private String getJixiong(int n) {
        if (JI_NUMBERS.contains(n)) return "大吉";
        if (n % 2 != 0) return "半吉";
        return "凶";
    }
}