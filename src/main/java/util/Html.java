package util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public final class Html {

    private Html() {}

    private static final Pattern WITH_SCHEME = Pattern.compile("(?i)^[a-z][a-z0-9+.-]*:.*");
    private static final Pattern WEB_OR_LOCAL = Pattern.compile("(?i)^(https?://|mailto:|tel:|/(?!/)|#).*");

    private static final AttributePolicy LINK = (element, attribute, value) -> {
        String url = value.strip();
        if (url.isEmpty() || WEB_OR_LOCAL.matcher(url).matches()) {
            return url.isEmpty() ? null : url;
        }
        if (WITH_SCHEME.matcher(url).matches() && !url.matches("(?i)^[^/:]+:\\d+(/.*)?$")) {
            return null;
        }
        return "https://" + url.replaceFirst("^/+", "");
    };

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements("p", "br", "h1", "h2", "h3", "strong", "b", "em", "i", "u", "s", "sub", "sup",
                    "blockquote", "pre", "code", "ol", "ul", "li", "span", "a", "img")
            .allowAttributes("class").matching(Pattern.compile("ql-[a-z0-9-]+( ql-[a-z0-9-]+)*")).globally()
            .allowAttributes("data-list").matching(Pattern.compile("ordered|bullet|checked|unchecked")).onElements("li")
            .allowAttributes("href").matching(LINK).onElements("a")
            .allowAttributes("src", "alt").onElements("img")
            .allowStandardUrlProtocols()
            .allowStyling()
            .requireRelNofollowOnLinks()
            .toFactory();

    private static final Pattern TAG = Pattern.compile("<[^>]*>");
    private static final Pattern ENTITY = Pattern.compile("&(#\\d+|#x[0-9a-fA-F]+|amp|lt|gt|quot|apos|nbsp);");
    private static final Pattern IMAGE_TAG = Pattern.compile("<img\\b[^>]*?\\bsrc=\"[^\"]*?/image/(\\d+)\"[^>]*>");
    private static final Pattern IMAGE_SRC = Pattern.compile("src=\"[^\"]*?/image/(\\d+)\"");

    public static String clean(String html) {
        return html == null ? "" : POLICY.sanitize(html);
    }

    public static boolean looksLikeHtml(String content) {
        return content != null && content.stripLeading().startsWith("<");
    }

    public static String render(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        return looksLikeHtml(content) ? clean(content) : fromPlain(content);
    }

    public static String fromPlain(String text) {
        StringBuilder out = new StringBuilder();
        for (String paragraph : text.trim().split("\\R\\s*\\R")) {
            if (!paragraph.isBlank()) {
                out.append("<p>").append(escape(paragraph.trim()).replaceAll("\\R", "<br>")).append("</p>");
            }
        }
        return out.toString();
    }

    public static String text(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String plain = looksLikeHtml(content) ? unescape(TAG.matcher(content.replace("</p>", " ").replace("<br>", " ")).replaceAll(" ")) : content;
        return plain.replaceAll("\\s+", " ").trim();
    }

    public static Set<Long> imageIds(String html) {
        Set<Long> ids = new LinkedHashSet<>();
        if (html != null) {
            Matcher m = IMAGE_SRC.matcher(html);
            while (m.find()) {
                ids.add(Long.parseLong(m.group(1)));
            }
        }
        return ids;
    }

    public static String removeImages(String html, Set<Long> ids) {
        if (html == null || ids.isEmpty()) {
            return html;
        }
        Matcher m = IMAGE_TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            boolean drop = ids.contains(Long.parseLong(m.group(1)));
            m.appendReplacement(out, drop ? "" : Matcher.quoteReplacement(m.group()));
        }
        m.appendTail(out);
        return out.toString();
    }

    public static String withContext(String html, String contextPath) {
        return contextPath.isEmpty() ? html : html.replace("src=\"/image/", "src=\"" + contextPath + "/image/");
    }

    public static String withoutContext(String html, String contextPath) {
        return contextPath.isEmpty() ? html : html.replace("src=\"" + contextPath + "/image/", "src=\"/image/");
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String unescape(String text) {
        Matcher m = ENTITY.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String e = m.group(1);
            String replacement = switch (e) {
                case "amp" -> "&";
                case "lt" -> "<";
                case "gt" -> ">";
                case "quot" -> "\"";
                case "apos" -> "'";
                case "nbsp" -> " ";
                default -> codePoint(e);
            };
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String codePoint(String entity) {
        try {
            int cp = entity.startsWith("#x") ? Integer.parseInt(entity.substring(2), 16) : Integer.parseInt(entity.substring(1));
            return Character.isValidCodePoint(cp) ? new String(Character.toChars(cp)) : "";
        } catch (NumberFormatException e) {
            return "";
        }
    }
}
