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

@WebFilter(urlPatterns = "/*")
public class PrettyUrlFilter implements Filter {

    public static final String PRETTY_URL = "entropy.prettyUrl";
    public static final String PRETTY_VIEW = "entropy.prettyView";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String context = request.getContextPath();

        String rawPath = request.getRequestURI().substring(context.length());
        int params = rawPath.indexOf(';');
        if (params >= 0) {
            rawPath = rawPath.substring(0, params);
        }

        String path;
        try {
            path = URLDecoder.decode(rawPath.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException badEncoding) {
            chain.doFilter(req, res);
            return;
        }

        String target = Urls.resolve(path);
        if (target == null) {
            chain.doFilter(req, res);
            return;
        }

        int query = target.indexOf('?');
        request.setAttribute(PRETTY_URL, context + rawPath);
        request.setAttribute(PRETTY_VIEW, query < 0 ? target : target.substring(0, query));
        request.getRequestDispatcher(target).forward(req, res);
    }
}
