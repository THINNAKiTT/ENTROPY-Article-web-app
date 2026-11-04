package tags.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.util.Collections;
import java.util.List;
import tags.dao.TagDao;
import tags.model.TagCount;

/** The "Topics" panel next to the article lists: the most used tags. See the topicsPanel component. */
@Named("topics")
@RequestScoped
public class TopicsBean {

    private final TagDao dao = new TagDao();
    private List<TagCount> popular;

    public List<TagCount> getPopular() {
        if (popular == null) {
            try {
                popular = dao.popular(12);
            } catch (Exception e) {
                return Collections.emptyList(); // the panel is a nice-to-have: leave it out rather than fail the page
            }
        }
        return popular;
    }
}
