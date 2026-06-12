package com.ai.cs.shared.util;

/**
 * Sensitive data masking utility for display and logging
 */
public class ContentMasker {

    private ContentMasker() {}

    /** Mask phone: 13812341234 -> 138****1234 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) return "";
        if (phone.length() < 7) return phone.charAt(0) + "****";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** Mask ID card: 310101199001011234 -> 310***********1234 */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) return "";
        if (idCard.length() < 8) return idCard.charAt(0) + "****";
        String prefix = idCard.substring(0, 3);
        String suffix = idCard.substring(idCard.length() - 4);
        int maskLen = idCard.length() - 7;
        return prefix + "*".repeat(Math.max(0, maskLen)) + suffix;
    }

    /** Generic middle masking */
    public static String maskMiddle(String text, int prefixLen, int suffixLen) {
        if (text == null || text.isEmpty()) return "";
        if (text.length() <= prefixLen + suffixLen) return text;
        return text.substring(0, prefixLen) + "****" + text.substring(text.length() - suffixLen);
    }
}
