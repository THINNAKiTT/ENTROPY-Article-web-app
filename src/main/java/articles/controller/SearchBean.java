package articles.controller;

import articles.model.Article;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.List;
import util.Texts;
import web.Faces;
import web.Urls;

@Named("searchBean")
@ViewScoped
public class SearchBean extends ArticleListBean {
    private static final long serialVersionUID = 1L;

    private String q;

    @Override
    protected List<Article> fetch(Long viewerId) throws Exception {
        return Texts.isBlank(q) ? List.of() : articleDao.findPublished(q, null, viewerId, PAGE_SIZE);
    }

    public String search() {
        Faces.redirect(Urls.search(q));
        return null;
    }

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
}
