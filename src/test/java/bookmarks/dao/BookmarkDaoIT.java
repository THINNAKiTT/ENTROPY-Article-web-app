package bookmarks.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import articles.dao.ArticleDao;
import articles.model.Article;
import org.junit.After;
import org.junit.Test;
import support.TestData;
import users.model.User;

/** Needs the database: run with  mvn test -Dtest='*IT'  */
public class BookmarkDaoIT {
    private final TestData data = new TestData();
    private final BookmarkDao dao = new BookmarkDao();
    private final ArticleDao articles = new ArticleDao();

    @After
    public void cleanup() throws Exception {
        data.cleanup();
    }

    @Test
    public void toggleSavesThenUnsaves() throws Exception {
        User writer = data.user("w"), reader = data.user("r");
        Article a = data.published(writer, "worth keeping");

        assertTrue("first toggle saves", dao.toggle(reader.getId(), a.getId()));
        assertTrue(articles.findById(a.getId(), reader.getId()).isBookmarked());
        assertFalse("saved by the reader, not by the writer", articles.findById(a.getId(), writer.getId()).isBookmarked());
        assertTrue(articles.findSavedBy(reader.getId()).stream().anyMatch(x -> x.getId().equals(a.getId())));

        assertFalse("second toggle unsaves", dao.toggle(reader.getId(), a.getId()));
        assertTrue(articles.findSavedBy(reader.getId()).isEmpty());
    }
}
