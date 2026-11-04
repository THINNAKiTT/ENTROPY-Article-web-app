package web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Makes the clean addresses work: "/my-article", "/@alice" and "/popular" are forwarded, inside the
 * server, to article.xhtml, profile.xhtml and popular.xhtml. The browser keeps seeing the clean address.
 * The mapping itself is {@link Urls#resolve}.
 */
@WebFilter(urlPatterns = "/*")
public class PrettyUrlFilter implements Filter {

    /** Request attributes read by {@link PrettyViewHandler}. */
    public static final String PRETTY_URL = "entropy.prettyUrl";
    public static final String PRETTY_VIEW = "entropy.prettyView";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String context = request.getContextPath();

        // the address exactly as the browser sent it (still %-encoded, e.g. /search/hello%20world)
        String rawPath = request.getRequestURI().substring(context.length());
        int params = rawPath.indexOf(';');        // ";jsessionid=..." is not part of the path
        if (params >= 0) {
            rawPath = rawPath.substring(0, params);
        }

        String path;
        try {
            path = URLDecoder.decode(rawPath.replace("+", "%2B"), StandardCharsets.UTF_8); // a "+" in a path is just a plus
        } catch (IllegalArgumentException badEncoding) {
            chain.doFilter(req, res); // not an address of ours; the server will answer it
            return;
        }

        String target = Urls.resolve(path);
        if (target == null) {
            chain.doFilter(req, res);
            return;
        }

        int query = target.indexOf('?');
        // forms on the page post back to the encoded address, which is always safe to put in a link
        request.setAttribute(PRETTY_URL, context + rawPath);
        request.setAttribute(PRETTY_VIEW, query < 0 ? target : target.substring(0, query));
        // the query string of the clean address (?id=...) travels along with the forward
        request.getRequestDispatcher(target).forward(req, res);
    }
}
