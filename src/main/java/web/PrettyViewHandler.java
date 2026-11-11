package web;

import jakarta.faces.application.ViewHandler;
import jakarta.faces.application.ViewHandlerWrapper;
import jakarta.faces.context.FacesContext;
import java.util.Map;
import java.util.Set;

public class PrettyViewHandler extends ViewHandlerWrapper {

    private static final Set<String> UPLOAD_VIEWS = Set.of("/write.xhtml", "/profile.xhtml");

    public PrettyViewHandler(ViewHandler wrapped) {
        super(wrapped);
    }

    @Override
    public String getActionURL(FacesContext context, String viewId) {
        Map<String, Object> request = context.getExternalContext().getRequestMap();
        Object real = request.get(PrettyUrlFilter.REAL_URL);
        if (real != null && UPLOAD_VIEWS.contains(viewId) && viewId.equals(request.get(PrettyUrlFilter.PRETTY_VIEW))) {
            return (String) real;
        }
        Object pretty = request.get(PrettyUrlFilter.PRETTY_URL);
        if (pretty != null && viewId.equals(request.get(PrettyUrlFilter.PRETTY_VIEW))) {
            return (String) pretty;
        }
        return super.getActionURL(context, viewId);
    }
}
