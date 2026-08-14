package com.flourishtravel.domain.mail;

public final class MailAddresses {

    private MailAddresses() {}

    /** "Flourish Travel &lt;a@gmail.com&gt;" hoặc "&lt;a@gmail.com&gt;" → a@gmail.com */
    public static String extractEmail(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        int lt = value.lastIndexOf('<');
        int gt = value.lastIndexOf('>');
        if (lt >= 0 && gt > lt) {
            value = value.substring(lt + 1, gt).trim();
        } else if (value.startsWith("<")) {
            value = value.substring(1).trim();
            if (value.endsWith(">")) {
                value = value.substring(0, value.length() - 1).trim();
            }
        }
        return value;
    }

    public static String stripAppPassword(String raw) {
        if (raw == null) return "";
        return raw.trim().replace(" ", "");
    }
}
