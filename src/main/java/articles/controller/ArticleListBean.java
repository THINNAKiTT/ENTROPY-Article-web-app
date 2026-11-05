package articles.controller;

import articles.dao.ArticleDao;
import articles.model.Article;
import bookmarks.dao.BookmarkDao;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import likes.dao.LikeDao;
import users.controller.UserSessionBean;
import web.Faces;

public abstract class ArticleListBean implements Serializable {
    private static final long serialVersionUID = 1L;

    protected static final int PAGE_SIZE = 50;

    @Inject
    protected UserSessionBean userSession;

    protected final ArticleDao articleDao = new ArticleDao();
    private final LikeDao likeDao = new LikeDao();
    private final BookmarkDao bookmarkDao = new BookmarkDao();

    private List<Article> articles;

    protected abstract List<Article> fetch(Long viewerId) throws Exception;

    public List<Article> getArticles() {
        if (articles == null) {
            try {
                articles = fetch(userSession.getCurrentUserId());
            } catch (Exception e) {
                Faces.error("Could not load articles", e.getMessage());
                return Collections.emptyList();
            }
        }
        return articles;
    }

    protected void refresh() {
        articles = null;
    }

    public String toggleLike(Long articleId) {
        Long me = userSession.getCurrentUserId();
        if (me == null) {
            Faces.promptLogin("You need an account to like articles.");
            return null;
        }
        try {
            likeDao.toggle(me, articleId);
            refresh();
        } catch (Exception e) {
            Faces.error("Something went wrong", e.getMessage());
        }
        return null;
    }

    public String toggleBookmark(Long articleId) {
        Long me = userSession.getCurrentUserId();
        if (me == null) {
            Faces.promptLogin("You need an account to save articles.");
            return null;
        }
        try {
            bookmarkDao.toggle(me, articleId);
            refresh();
        } catch (Exception e) {
            Faces.error("Something went wrong", e.getMessage());
        }
        return null;
    }
}
