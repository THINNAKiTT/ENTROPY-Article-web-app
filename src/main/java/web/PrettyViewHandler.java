package web;

import jakarta.faces.application.ViewHandler;
import jakarta.faces.application.ViewHandlerWrapper;
import jakarta.faces.context.FacesContext;
import java.util.Map;

/**
 * Forms post back to the address of the page. Without this they would post to "/article.xhtml",
 * and the browser would end up showing that address after a form submit. With it they post back to
 * the clean address the page was opened with. Registered in faces-config.xml.
 */
public class PrettyViewHandler extends ViewHandlerWrapper {

    public PrettyViewHandler(ViewHandler wrapped) {
        super(wrapped);
    }

    @Override
    public String getActionURL(FacesContext context, String viewId) {
        Map<String, Object> request = context.getExternalContext().getRequestMap();
        Object pretty = request.get(PrettyUrlFilter.PRETTY_URL);
        if (pretty != null && viewId.equals(request.get(PrettyUrlFilter.PRETTY_VIEW))) {
            return (String) pretty;
        }
        return super.getActionURL(context, viewId);
    }
}
