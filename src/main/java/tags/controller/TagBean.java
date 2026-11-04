package tags.controller;

import articles.controller.ArticleListBean;
import articles.model.Article;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.List;
import tags.dao.TagDao;

/** A topic page (tag.xhtml, /tag/some-topic): the articles with that tag. */
@Named("tagBean")
@ViewScoped
public class TagBean extends ArticleListBean {
    private static final long serialVersionUID = 1L;

    private final TagDao tagDao = new TagDao();

    private String slug;   // from the address
    private String name;   // the tag's real name, looked up from the slug

    @Override
    protected List<Article> fetch(Long viewerId) throws Exception {
        return articleDao.findPublished(null, slug, viewerId, PAGE_SIZE);
    }

    public String getSlug() { return slug; }
    public void setSlug(String slug) {
        this.slug = slug;
        this.name = null;
    }

    /** The topic by its real name ("c++ tips") rather than its address form ("c-tips"). */
    public String getName() {
        if (name == null && slug != null) {
            try {
                name = tagDao.nameOf(slug);
            } catch (Exception e) {
                // fall back to the slug below
            }
            if (name == null) {
                name = slug;
            }
        }
        return name;
    }
}
