package web;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.primefaces.PrimeFaces;

public final class Faces {

    private Faces() {}

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

    public static void redirect(String path) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }
        try {
            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.getExternalContext().redirect(ctx.getExternalContext().getRequestContextPath() + path);
        } catch (IOException e) {
        }
        ctx.responseComplete();
    }

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

    public static void notFound() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }
        try {
            ctx.getExternalContext().responseSendError(404, "Not found");
        } catch (IOException e) {
        }
        ctx.responseComplete();
    }
}
