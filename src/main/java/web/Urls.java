package web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;
import util.Texts;

/**
 * The address scheme of the site, in one place.
 *
 *   /                         home: the latest articles
 *   /popular                  the most liked and commented articles
 *   /following                articles from people you follow
 *   /saved                    articles you saved
 *   /search/some%20words      search results for "some words"
 *   /tag/some-topic           articles with a topic
 *   /write                    write an article  (/write?id=5 edits article 5)
 *   /hello-world-jdl2i53q     an article: its title plus 8 random characters (see ArticleDao)
 *   /@username                a profile  (/@username/followers, /@username/following)
 *
 * Paths here are relative to the application's context path (the "/entropy" part).
 * Every plain page /xyz is served by the file xyz.xhtml.
 */
public final class Urls {

    private Urls() {}

    /** Pages whose address is /name and whose file is name.xhtml. */
    private static final Set<String> PAGES = Set.of("popular", "following", "saved", "search", "write");

    /** Other page names: /error or /tag are not article addresses, so they are never looked up as one. */
    private static final Set<String> NOT_ARTICLES = Set.of("tag", "index", "article", "profile", "error", "layout");

    private static final int MAX_SEARCH_LENGTH = 100;

    private static final Pattern SLUG = Pattern.compile("[a-z0-9][a-z0-9-]{0,299}");
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,30}");
    private static final Set<String> PROFILE_TABS = Set.of("followers", "following");

    // ------------------------------------------------------------------ building addresses

    public static String home()      { return "/"; }
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

    /** tab: "followers" or "following"; anything else is the profile itself. */
    public static String profile(String username, String tab) {
        return PROFILE_TABS.contains(tab) ? "/@" + username + "/" + tab : profile(username);
    }

    /** A topic page. Takes the tag's name or slug: "Big Data" and "big-data" both give /tag/big-data. */
    public static String tag(String name) {
        return "/tag/" + Texts.slugify(name);
    }

    /**
     * Search results for some words: /search/hello%20world. A "/" in the words would read as another part of
     * the address, so it counts as a space. No words at all goes home.
     */
    public static String search(String text) {
        String words = text == null ? "" : text.replace('/', ' ').replace('\\', ' ').trim();
        if (words.length() > MAX_SEARCH_LENGTH) {
            words = words.substring(0, MAX_SEARCH_LENGTH).trim();
        }
        // a path uses %20 for a space ("+" only means a space in a query string)
        return words.isEmpty() ? home() : "/search/" + URLEncoder.encode(words, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // ------------------------------------------------------------------ reading addresses

    /**
     * Turns an address from the browser into the page that really serves it, e.g.
     * "/my-article" into "/article.xhtml?slug=my-article". Returns null when the request should
     * be left alone (the home page, files, resources, anything that is not one of our addresses).
     */
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
            // `path` is already decoded, so what follows is the words as typed; pass them on as a query
            String words = path.substring(8).trim();
            return words.isEmpty() ? "/search.xhtml" : "/search.xhtml?q=" + URLEncoder.encode(words, StandardCharsets.UTF_8);
        }

        String segment = path.substring(1);
        if (segment.indexOf('/') >= 0 || segment.indexOf('.') >= 0) {
            return null; // a file, a resource, or something deeper that is not ours
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
