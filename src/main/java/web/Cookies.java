package web;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.Cookie;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class Cookies {

    private Cookies() {}

    public static String get(String name) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return null;
        }
        Object cookie = ctx.getExternalContext().getRequestCookieMap().get(name);
        if (cookie instanceof Cookie c && c.getValue() != null && !c.getValue().isEmpty()) {
            try {
                return URLDecoder.decode(c.getValue(), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    public static void set(String name, String value, int maxAgeSeconds) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }
        ExternalContext ec = ctx.getExternalContext();
        Map<String, Object> properties = new HashMap<>();
        properties.put("maxAge", maxAgeSeconds);
        properties.put("path", ec.getRequestContextPath() + "/");
        properties.put("httpOnly", Boolean.TRUE);
        ec.addResponseCookie(name, URLEncoder.encode(value, StandardCharsets.UTF_8), properties);
    }

    public static void remove(String name) {
        set(name, "", 0);
    }
}
