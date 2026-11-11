package landing.controller;

import articles.dao.ArticleDao;
import articles.model.Article;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Collections;
import java.util.List;
import org.primefaces.model.ResponsiveOption;
import users.controller.UserSessionBean;

@Named("landingBean")
@RequestScoped
public class LandingBean {

    private static final int FEATURED = 9;

    @Inject
    private UserSessionBean userSession;

    private final ArticleDao articleDao = new ArticleDao();

    private List<Article> featured;

    public List<Article> getFeatured() {
        if (featured == null) {
            try {
                featured = articleDao.findPopular(userSession.getCurrentUserId(), FEATURED);
            } catch (Exception e) {
                featured = Collections.emptyList();
            }
        }
        return featured;
    }

    public List<Article> getTrending() {
        List<Article> all = getFeatured();
        return all.subList(0, Math.min(3, all.size()));
    }

    public List<ResponsiveOption> getResponsiveOptions() {
        return List.of(new ResponsiveOption("1280px", 2, 1), new ResponsiveOption("760px", 1, 1));
    }
}
