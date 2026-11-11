package articles.controller;

import articles.dao.ArticleDao;
import articles.model.Article;
import images.dao.ImageDao;
import images.model.ArticleImage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import users.controller.UserSessionBean;
import util.Html;
import util.Images;
import web.Faces;
import web.Urls;

@Named("editorBean")
@ViewScoped
public class EditorBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final int MAX_TITLE = 200;
    private static final int MAX_CONTENT = 100_000;
    private static final int MAX_IMAGES = 30;
    private static final Pattern PASTED_IMAGE =
            Pattern.compile("<img\\b[^>]*?\\bsrc=\"data:image/(?:png|jpe?g|gif);base64,([^\"]+)\"[^>]*>");

    @Inject
    private UserSessionBean userSession;

    private final ArticleDao dao = new ArticleDao();
    private final ImageDao imageDao = new ImageDao();

    private Long id;
    private Article article = new Article();
    private List<ArticleImage> images = new ArrayList<>();

    public void load() {
        Long me = userSession.getCurrentUserId();
        if (me == null) {
            return;
        }
        try {
            imageDao.deleteStale(me);
            if (id == null) {
                article = new Article();
                article.setAuthorId(me);
                return;
            }
            Article found = dao.findById(id, me);
            if (found == null || !me.equals(found.getAuthorId())) {
                Faces.notFound();
                return;
            }
            found.setContent(Html.withContext(Html.render(found.getContent()), Faces.contextPath()));
            article = found;
            images = new ArrayList<>(imageDao.findByArticle(found.getId()));
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
        String content = Html.withoutContext(Html.clean(storePastedImages(article.getContent(), me)), Faces.contextPath()).trim();
        try {
            Set<Long> referenced = Html.imageIds(content);
            Set<Long> missing = new java.util.HashSet<>(referenced);
            missing.removeAll(imageDao.existing(referenced));
            content = Html.removeImages(content, missing).trim();
        } catch (Exception e) {
            Faces.error("Could not check the pictures", e.getMessage());
            return null;
        }
        boolean hasText = !Html.text(content).isEmpty() || content.contains("<img");
        if (title.isEmpty()) {
            Faces.warn("Title missing", "Give the article a title.");
            return null;
        }
        if (title.length() > MAX_TITLE) {
            Faces.warn("Title too long", "Keep the title under " + MAX_TITLE + " characters.");
            return null;
        }
        if (status.equals(Article.PUBLISHED) && !hasText) {
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

        String draftKey = isEditing() ? String.valueOf(article.getId()) : "new";
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
            List<Long> imageIds = new ArrayList<>();
            for (ArticleImage image : images) {
                imageIds.add(image.getId());
            }
            imageDao.attach(article.getId(), me, imageIds);
        } catch (Exception e) {
            Faces.error("Could not save the article", e.getMessage());
            return null;
        }

        Faces.flash("clearDraft", draftKey);
        Faces.info(status.equals(Article.PUBLISHED) ? "Published" : "Draft saved", article.getTitle());
        Faces.redirect(status.equals(Article.PUBLISHED)
                ? Urls.article(article.getSlug())
                : Urls.profile(userSession.getCurrentUser().getUsername()));
        return null;
    }

    private String storePastedImages(String html, Long me) {
        if (html == null) {
            return "";
        }
        Matcher m = PASTED_IMAGE.matcher(html);
        StringBuilder out = new StringBuilder();
        boolean skipped = false;
        while (m.find()) {
            String replacement = "";
            try {
                byte[] data = Base64.getMimeDecoder().decode(m.group(1));
                if (images.size() < MAX_IMAGES && data.length <= Images.MAX_UPLOAD_BYTES) {
                    ArticleImage image = imageDao.insert(me, "pasted-picture", Images.article(data));
                    images.add(image);
                    replacement = "<img src=\"" + Faces.contextPath() + "/image/" + image.getId() + "\">";
                }
            } catch (Exception e) {
            }
            skipped |= replacement.isEmpty();
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        if (skipped) {
            Faces.warn("Some pictures were left out", "A pasted picture was too big, too many, or not a PNG, JPEG or GIF.");
        }
        return out.toString();
    }

    public void uploadImage(FileUploadEvent event) {
        Long me = userSession.getCurrentUserId();
        if (me == null) {
            Faces.promptLogin("You need to log in to upload pictures.");
            return;
        }
        UploadedFile file = event.getFile();
        if (images.size() >= MAX_IMAGES) {
            Faces.warn("Too many pictures", "An article can have up to " + MAX_IMAGES + " pictures.");
            return;
        }
        if (file.getSize() > Images.MAX_UPLOAD_BYTES) {
            Faces.warn("Picture too big", file.getFileName() + " is over 5 MB.");
            return;
        }
        try {
            images.add(imageDao.insert(me, file.getFileName(), Images.article(file.getContent())));
        } catch (IOException e) {
            Faces.warn("Could not use " + file.getFileName(), e.getMessage());
        } catch (Exception e) {
            Faces.error("Could not save " + file.getFileName(), e.getMessage());
        }
    }

    public String removeImage(Long imageId) {
        Long me = userSession.getCurrentUserId();
        if (me == null || imageId == null) {
            return null;
        }
        try {
            if (imageDao.delete(imageId, me) && article.getId() != null) {
                dao.removeImageFromContent(article.getId(), imageId);
            }
            images.removeIf(image -> imageId.equals(image.getId()));
        } catch (Exception e) {
            Faces.error("Could not remove the picture", e.getMessage());
        }
        return null;
    }

    public String delete() {
        Long me = userSession.getCurrentUserId();
        if (article.getId() == null || me == null || !me.equals(article.getAuthorId())) {
            return null;
        }
        try {
            dao.delete(article.getId());
            Faces.flash("clearDraft", String.valueOf(article.getId()));
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

    public List<ArticleImage> getImages() { return images; }

    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }
}
