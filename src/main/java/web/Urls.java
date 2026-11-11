package web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;
import util.Texts;

public final class Urls {

    private Urls() {}

    private static final Set<String> PAGES = Set.of("latest", "popular", "following", "saved", "search", "write");

    private static final Set<String> NOT_ARTICLES = Set.of("tag", "index", "article", "profile", "error", "layout");

    private static final int MAX_SEARCH_LENGTH = 100;

    private static final Pattern SLUG = Pattern.compile("[a-z0-9][a-z0-9-]{0,299}");
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,30}");
    private static final Set<String> PROFILE_TABS = Set.of("followers", "following");

    public static String home()      { return "/"; }
    public static String latest()    { return "/latest"; }
    public static String popular()   { return "/popular"; }
    public static String following() { return "/following"; }
    public static String saved()     { return "/saved"; }
    public static String write()     { return "/write"; }

    public static String edit(Long articleId) {
        return "/write?id=" + articleId;
    }

    public static String article(String slug) {
        return "/" + slug;
    }

    public static String profile(String username) {
        return "/@" + username;
    }

    public static String profile(String username, String tab) {
        return PROFILE_TABS.contains(tab) ? "/@" + username + "/" + tab : profile(username);
    }

    public static String tag(String name) {
        return "/tag/" + Texts.slugify(name);
    }

    public static String search(String text) {
        String words = text == null ? "" : text.replace('/', ' ').replace('\\', ' ').trim();
        if (words.length() > MAX_SEARCH_LENGTH) {
            words = words.substring(0, MAX_SEARCH_LENGTH).trim();
        }
        return words.isEmpty() ? latest() : "/search/" + URLEncoder.encode(words, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static String resolve(String path) {
        if (path == null || path.length() < 2) {
            return null;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (path.startsWith("/@")) {
            String[] parts = path.substring(2).split("/", -1);
            if (parts.length > 2 || !USERNAME.matcher(parts[0]).matches()) {
                return null;
            }
            if (parts.length == 1) {
                return "/profile.xhtml?username=" + parts[0];
            }
            return PROFILE_TABS.contains(parts[1]) ? "/profile.xhtml?username=" + parts[0] + "&tab=" + parts[1] : null;
        }

        if (path.startsWith("/tag/")) {
            String slug = path.substring(5);
            return SLUG.matcher(slug).matches() ? "/tag.xhtml?slug=" + slug : null;
        }

        if (path.startsWith("/search/")) {
            String words = path.substring(8).trim();
            return words.isEmpty() ? "/search.xhtml" : "/search.xhtml?q=" + URLEncoder.encode(words, StandardCharsets.UTF_8);
        }

        String segment = path.substring(1);
        if (segment.indexOf('/') >= 0 || segment.indexOf('.') >= 0) {
            return null;
        }
        if (PAGES.contains(segment)) {
            return "/" + segment + ".xhtml";
        }
        if (NOT_ARTICLES.contains(segment)) {
            return null;
        }
        return SLUG.matcher(segment).matches() ? "/article.xhtml?slug=" + segment : null;
    }
}
