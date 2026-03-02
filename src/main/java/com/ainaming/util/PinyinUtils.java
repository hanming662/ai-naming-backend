package com.ainaming.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

public class PinyinUtils {

    private static final HanyuPinyinOutputFormat FORMAT_WITH_TONE;
    private static final HanyuPinyinOutputFormat FORMAT_WITHOUT_TONE;

    static {
        FORMAT_WITH_TONE = new HanyuPinyinOutputFormat();
        FORMAT_WITH_TONE.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT_WITH_TONE.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
        FORMAT_WITH_TONE.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);

        FORMAT_WITHOUT_TONE = new HanyuPinyinOutputFormat();
        FORMAT_WITHOUT_TONE.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT_WITHOUT_TONE.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    /**
     * 获取带声调的拼音
     */
    public static String getPinyinWithTone(char ch) {
        try {
            String[] arr = PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT_WITH_TONE);
            return (arr != null && arr.length > 0) ? arr[0] : String.valueOf(ch);
        } catch (Exception e) {
            return String.valueOf(ch);
        }
    }

    /**
     * 获取不带声调的拼音
     */
    public static String getPinyin(char ch) {
        try {
            String[] arr = PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT_WITHOUT_TONE);
            return (arr != null && arr.length > 0) ? arr[0] : String.valueOf(ch);
        } catch (Exception e) {
            return String.valueOf(ch);
        }
    }

    /**
     * 获取声调数字 (1-4)
     */
    public static int getTone(char ch) {
        try {
            HanyuPinyinOutputFormat fmt = new HanyuPinyinOutputFormat();
            fmt.setToneType(HanyuPinyinToneType.WITH_TONE_NUMBER);
            String[] arr = PinyinHelper.toHanyuPinyinStringArray(ch, fmt);
            if (arr != null && arr.length > 0) {
                String py = arr[0];
                char last = py.charAt(py.length() - 1);
                if (Character.isDigit(last)) {
                    return Character.getNumericValue(last);
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    /**
     * 获取整个名字的拼音（带声调）
     */
    public static String getFullPinyin(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(getPinyinWithTone(name.charAt(i)));
        }
        return sb.toString();
    }

    /**
     * 获取整个名字的拼音（不带声调）
     */
    public static String getFullPinyinNoTone(String name) {
        StringBuilder sb = new StringBuilder();
        for (char ch : name.toCharArray()) {
            sb.append(getPinyin(ch));
        }
        return sb.toString();
    }

    /**
     * 判断是否多音字
     */
    public static boolean isPolyphonic(char ch) {
        try {
            String[] arr = PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT_WITHOUT_TONE);
            if (arr == null) {
                return false;
            }
            java.util.Set<String> set = new java.util.HashSet<>(java.util.Arrays.asList(arr));
            return set.size() > 1;
        } catch (Exception e) {
            return false;
        }
    }
}