package support;

import articles.dao.ArticleDao;
import articles.model.Article;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import users.dao.UserDao;
import users.model.User;
import util.Db;

/**
 * Throwaway data for the database tests (*IT classes). Everything it creates has a name that starts
 * with a prefix unique to this run, and cleanup() removes it again, including the tags it created.
 */
public class TestData {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    /** "zz" + time + counter: lower case so tag slugs made from it start with the same text. */
    public final String prefix = "zz" + System.currentTimeMillis() + COUNTER.incrementAndGet();

    private final UserDao userDao = new UserDao();
    private final ArticleDao articleDao = new ArticleDao();
    private final List<Long> userIds = new ArrayList<>();

    /** A new user called <prefix>_<name>. */
    public User user(String name) throws Exception {
        User u = new User();
        u.setUsername(prefix + "_" + name);
        u.setEmail(prefix + "_" + name + "@example.test");
        u.setRawPassword("s3cret!");
        u.setId(userDao.insert(u));
        userIds.add(u.getId());
        return u;
    }

    public Article article(User author, String title, String status, String tags) throws Exception {
        Article a = new Article();
        a.setAuthorId(author.getId());
        a.setTitle(title);
        a.setContent("some words in the body");
        a.setStatus(status);
        a.setTags(tags);
        articleDao.insert(a);
        return a;
    }

    public Article published(User author, String title) throws Exception {
        return article(author, title, Article.PUBLISHED, null);
    }

    /** Deleting the users deletes their articles, comments, likes, bookmarks and follows (ON DELETE CASCADE). */
    public void cleanup() throws Exception {
        for (Long id : userIds) {
            userDao.delete(id);
        }
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement("DELETE FROM tags WHERE slug LIKE ?")) {
            p.setString(1, prefix + "%");
            p.executeUpdate();
        }
    }
}
