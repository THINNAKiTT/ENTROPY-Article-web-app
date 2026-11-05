package articles.controller;

import articles.dao.ArticleDao;
import articles.model.Article;
import bookmarks.dao.BookmarkDao;
import comments.controller.CommentsBean;
import follows.dao.FollowDao;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import likes.dao.LikeDao;
import users.controller.UserSessionBean;
import users.dao.UserDao;
import users.model.User;
import web.Faces;
import web.Urls;

@Named("articleBean")
@ViewScoped
public class ArticleBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private UserSessionBean userSession;
    @Inject
    private CommentsBean comments;

    private final ArticleDao articleDao = new ArticleDao();
    private final UserDao userDao = new UserDao();
    private final FollowDao followDao = new FollowDao();
    private final LikeDao likeDao = new LikeDao();
    private final BookmarkDao bookmarkDao = new BookmarkDao();

    private String slug;
    private Article article;
    private User writer;
    private boolean followingWriter;

    public void load() {
        try {
            article = slug == null ? null : articleDao.findBySlug(slug, userSession.getCurrentUserId());
            if (article == null || (!article.isPublished() && !isAuthor())) {
                article = null;
                Faces.notFound();
                return;
            }
            writer = userDao.findById(article.getAuthorId());
            refreshFollowing();
            comments.load(article.getId(), article.getAuthorId());
        } catch (Exception e) {
            Faces.error("Could not load the article", e.getMessage());
        }
    }

    public String toggleLike() {
        Long me = userSession.getCurrentUserId();
        if (me == null) {
            Faces.promptLogin("You need an account to like articles.");
            return null;
        }
        try {
            likeDao.toggle(me, article.getId());
            reloadArticle();
        } catch (Exception e) {
            Faces.error("Something went wrong", e.getMessage());
        }
        return null;
    }

    public String toggleBookmark() {
        Long me = userSession.getCurrentUserId();
        if (me == null) {
            Faces.promptLogin("You need an account to save articles.");
            return null;
        }
        try {
            bookmarkDao.toggle(me, article.getId());
            reloadArticle();
        } catch (Exception e) {
            Faces.error("Something went wrong", e.getMessage());
        }
        return null;
    }

    public String toggleFollowWriter() {
        Long me = userSession.getCurrentUserId();
        if (me == null) {
            Faces.promptLogin("You need an account to follow writers.");
            return null;
        }
        if (isAuthor()) {
            Faces.warn("Not possible", "You cannot follow yourself.");
            return null;
        }
        try {
            if (followDao.isFollowing(me, article.getAuthorId())) {
                followDao.unfollow(me, article.getAuthorId());
            } else {
                followDao.follow(me, article.getAuthorId());
            }
            refreshFollowing();
        } catch (Exception e) {
            Faces.error("Something went wrong", e.getMessage());
        }
        return null;
    }

    public String deleteArticle() {
        if (!isAuthor()) {
            Faces.error("Not allowed", "Only the author can delete this article.");
            return null;
        }
        try {
            articleDao.delete(article.getId());
            Faces.info("Deleted", "The article was deleted.");
            Faces.redirect(Urls.home());
        } catch (Exception e) {
            Faces.error("Could not delete the article", e.getMessage());
        }
        return null;
    }

    public boolean isAuthor() {
        Long me = userSession.getCurrentUserId();
        return me != null && article != null && me.equals(article.getAuthorId());
    }

    private void reloadArticle() throws Exception {
        article = articleDao.findById(article.getId(), userSession.getCurrentUserId());
    }

    private void refreshFollowing() throws Exception {
        Long me = userSession.getCurrentUserId();
        followingWriter = me != null && !isAuthor() && followDao.isFollowing(me, article.getAuthorId());
    }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public Article getArticle() { return article; }
    public User getWriter() { return writer; }
    public boolean isFollowingWriter() { return followingWriter; }
}
