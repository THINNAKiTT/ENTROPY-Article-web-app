package util;

import java.text.Normalizer;
import java.util.Locale;

public final class Texts {

    private Texts() {}

    public static String slugify(String input) {
        if (input == null) {
            return "";
        }
        String s = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    public static String excerpt(String text, int max) {
        if (text == null) {
            return "";
        }
        String s = text.replaceAll("\\s+", " ").trim();
        if (s.length() <= max) {
            return s;
        }
        int cut = s.lastIndexOf(' ', max);
        if (cut < max / 2) {
            cut = max;
        }
        return s.substring(0, cut).trim() + "…";
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
