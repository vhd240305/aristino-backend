package com.aristino.backend.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Tao "slug" (duong dan than thien) tu ten tieng Viet: "Ao so mi trang" -> "ao-so-mi-trang". */
public final class SlugUtil {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private SlugUtil() {
    }

    public static String slugify(String input) {
        if (input == null) {
            return "";
        }
        // \u0111 la chu "d" co gach ngang (dong) - khong bi tach dau boi Normalizer nen thay rieng
        String s = input.trim().toLowerCase(Locale.ROOT).replace('\u0111', 'd');
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = DIACRITICS.matcher(s).replaceAll("");
        s = NON_ALNUM.matcher(s).replaceAll("-");
        return s.replaceAll("^-+|-+$", "");
    }
}