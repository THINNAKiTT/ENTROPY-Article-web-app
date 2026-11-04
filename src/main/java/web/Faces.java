package web;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.primefaces.PrimeFaces;

/** The few things the beans do with Faces over and over: messages, redirects, "log in first", 404. */
public final class Faces {

    private Faces() {}

    // ------------------------------------------------------------------ messages (shown as popups by p:growl)

    public static void info(String summary, String detail) {
        add(FacesMessage.SEVERITY_INFO, summary, detail);
    }

    public static void warn(String summary, String detail) {
        add(FacesMessage.SEVERITY_WARN, summary, detail);
    }

    public static void error(String summary, String detail) {
        add(FacesMessage.SEVERITY_ERROR, summary, detail);
    }

    private static void add(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            ctx.addMessage(null, new FacesMessage(severity, summary, detail));
        }
    }

    // ------------------------------------------------------------------ moving the browser around

    /**
     * Sends the browser to an address from {@link Urls}, e.g. redirect(Urls.article(slug)).
     * Messages added so far are kept, so they still show up as a popup on the next page.
     * Works for normal and ajax requests.
     */
    public static void redirect(String path) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }
        try {
            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.getExternalContext().redirect(ctx.getExternalContext().getRequestContextPath() + path);
        } catch (IOException e) {
            // the browser is gone; nothing more to do
        }
        ctx.responseComplete();
    }

    /**
     * Asks the visitor to log in, with a popup instead of a separate page.
     * From an ajax action (like, save, follow) the popup opens right where they are.
     * From a normal page request (a page that needs an account) they go to the home page,
     * where the popup opens (the layout looks for the "promptLogin" flash value).
     */
    public static void promptLogin(String detail) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }
        warn("Log in first", detail);
        if (ctx.getPartialViewContext().isAjaxRequest()) {
            PrimeFaces.current().executeScript("PF('loginDialog').show()");
        } else {
            ctx.getExternalContext().getFlash().put("promptLogin", Boolean.TRUE);
            redirect(Urls.home());
        }
    }

    /** Reloads the page the visitor is on, so it shows what they can do now that they are logged in. */
    public static void reloadPage() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }
        ExternalContext ec = ctx.getExternalContext();
        String context = ec.getRequestContextPath();
        String path;
        Object pretty = ec.getRequestMap().get(PrettyUrlFilter.PRETTY_URL);
        if (pretty != null) {
            path = ((String) pretty).substring(context.length());
        } else if (ec.getRequest() instanceof HttpServletRequest request) {
            path = request.getRequestURI().substring(context.length());
            int params = path.indexOf(';');
            if (params >= 0) {
                path = path.substring(0, params);
            }
        } else {
            path = Urls.home();
        }
        if (path.isEmpty() || path.equals("/index.xhtml")) {
            path = Urls.home();
        }
        redirect(path);
    }

    /** Answers the current request with HTTP 404 (the error page from web.xml is shown). */
    public static void notFound() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }
        try {
            ctx.getExternalContext().responseSendError(404, "Not found");
        } catch (IOException e) {
            // nothing more we can do
        }
        ctx.responseComplete();
    }
}
