package tags.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.util.Collections;
import java.util.List;
import tags.dao.TagDao;
import tags.model.TagCount;

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
                return Collections.emptyList();
            }
        }
        return popular;
    }
}
