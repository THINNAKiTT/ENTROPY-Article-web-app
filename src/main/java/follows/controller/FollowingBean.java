package follows.controller;

import articles.controller.ArticleListBean;
import articles.model.Article;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.List;

@Named("followingBean")
@ViewScoped
public class FollowingBean extends ArticleListBean {
    private static final long serialVersionUID = 1L;

    @Override
    protected List<Article> fetch(Long viewerId) throws Exception {
        return viewerId == null ? List.of() : articleDao.findFromFollowed(viewerId, PAGE_SIZE);
    }
}
