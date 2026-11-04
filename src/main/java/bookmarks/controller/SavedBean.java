package bookmarks.controller;

import articles.controller.ArticleListBean;
import articles.model.Article;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.List;

/** The saved page (saved.xhtml): the articles you saved. The page itself requires a log in. */
@Named("savedBean")
@ViewScoped
public class SavedBean extends ArticleListBean {
    private static final long serialVersionUID = 1L;

    @Override
    protected List<Article> fetch(Long viewerId) throws Exception {
        return viewerId == null ? List.of() : articleDao.findSavedBy(viewerId);
    }
}
