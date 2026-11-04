package articles.controller;

import articles.model.Article;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.List;

/** The popular page (popular.xhtml): the most liked and commented articles. */
@Named("popularBean")
@ViewScoped
public class PopularBean extends ArticleListBean {
    private static final long serialVersionUID = 1L;

    @Override
    protected List<Article> fetch(Long viewerId) throws Exception {
        return articleDao.findPopular(viewerId, PAGE_SIZE);
    }
}
