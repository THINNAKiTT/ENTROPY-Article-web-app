package articles.controller;

import articles.dao.ArticleDao;
import articles.model.Article;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import users.controller.UserSessionBean;
import web.Faces;
import web.Urls;

/** Backs write.xhtml: write a new article, or edit one of your own (/write?id=5). */
@Named("editorBean")
@ViewScoped
public class EditorBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final int MAX_TITLE = 200;
    private static final int MAX_CONTENT = 100_000;

    @Inject
    private UserSessionBean userSession;

    private final ArticleDao dao = new ArticleDao();

    private Long id;                          // from the address: empty = a new article
    private Article article = new Article();

    /** viewAction (after requireLogin): loads the article when editing. */
    public void load() {
        Long me = userSession.getCurrentUserId();
        if (me == null) {
            return;
        }
        if (id == null) {
            article = new Article();
            article.setAuthorId(me);
            return;
        }
        try {
            Article found = dao.findById(id, me);
            if (found == null || !me.equals(found.getAuthorId())) {
                Faces.notFound(); // not there, or not yours
                return;
            }
            article = found;
        } catch (Exception e) {
            Faces.error("Could not load the article", e.getMessage());
        }
    }

    public String saveDraft() {
        return save(Article.DRAFT);
    }

    public String publish() {
        return save(Article.PUBLISHED);
    }

    private String save(String status) {
        Long me = userSession.getCurrentUserId();
        if (me == null) {
            Faces.promptLogin("You need to log in to save articles.");
            return null;
        }
        String title = article.getTitle() == null ? "" : article.getTitle().trim();
        String content = article.getContent() == null ? "" : article.getContent().trim();
        if (title.isEmpty()) {
            Faces.warn("Title missing", "Give the article a title.");
            return null;
        }
        if (title.length() > MAX_TITLE) {
            Faces.warn("Title too long", "Keep the title under " + MAX_TITLE + " characters.");
            return null;
        }
        if (status.equals(Article.PUBLISHED) && content.isEmpty()) {
            Faces.warn("Nothing to publish", "Write something before publishing. You can save an empty draft.");
            return null;
        }
        if (content.length() > MAX_CONTENT) {
            Faces.warn("Article too long", "The limit is " + MAX_CONTENT + " characters.");
            return null;
        }
        article.setTitle(title);
        article.setContent(content);
        article.setStatus(status);

        try {
            if (article.getId() == null) {
                article.setAuthorId(me);
                dao.insert(article);
            } else {
                if (!me.equals(article.getAuthorId())) {
                    Faces.error("Not allowed", "You can only edit your own articles.");
                    return null;
                }
                dao.update(article);
            }
        } catch (Exception e) {
            Faces.error("Could not save the article", e.getMessage());
            return null;
        }

        Faces.info(status.equals(Article.PUBLISHED) ? "Published" : "Draft saved", article.getTitle());
        // a published article opens at its own address; a draft goes back to your profile, where drafts are listed
        Faces.redirect(status.equals(Article.PUBLISHED)
                ? Urls.article(article.getSlug())
                : Urls.profile(userSession.getCurrentUser().getUsername()));
        return null;
    }

    public String delete() {
        Long me = userSession.getCurrentUserId();
        if (article.getId() == null || me == null || !me.equals(article.getAuthorId())) {
            return null;
        }
        try {
            dao.delete(article.getId());
            Faces.info("Deleted", "The article was deleted.");
            Faces.redirect(Urls.profile(userSession.getCurrentUser().getUsername()));
        } catch (Exception e) {
            Faces.error("Could not delete the article", e.getMessage());
        }
        return null;
    }

    public boolean isEditing() {
        return article != null && article.getId() != null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }
}
