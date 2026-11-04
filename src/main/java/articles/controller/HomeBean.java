package articles.controller;

import articles.model.Article;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.List;

/** The home page (index.xhtml): the newest articles. */
@Named("homeBean")
@ViewScoped
public class HomeBean extends ArticleListBean {
    private static final long serialVersionUID = 1L;

    @Override
    protected List<Article> fetch(Long viewerId) throws Exception {
        return articleDao.findPublished(null, null, viewerId, PAGE_SIZE);
    }
}
