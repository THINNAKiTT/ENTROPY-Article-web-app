package likes.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import articles.dao.ArticleDao;
import articles.model.Article;
import org.junit.After;
import org.junit.Test;
import support.TestData;
import users.model.User;

/** Needs the database: run with  mvn test -Dtest='*IT'  */
public class LikeDaoIT {
    private final TestData data = new TestData();
    private final LikeDao dao = new LikeDao();
    private final ArticleDao articles = new ArticleDao();

    @After
    public void cleanup() throws Exception {
        data.cleanup();
    }

    @Test
    public void toggleLikesThenUnlikes() throws Exception {
        User writer = data.user("w"), reader = data.user("r");
        Article a = data.published(writer, "likeable");

        assertTrue("first toggle likes", dao.toggle(reader.getId(), a.getId()));
        assertEquals(1, articles.findById(a.getId(), reader.getId()).getLikeCount());
        assertTrue(articles.findById(a.getId(), reader.getId()).isLiked());
        assertFalse("liked by the reader, not by the writer", articles.findById(a.getId(), writer.getId()).isLiked());

        assertFalse("second toggle unlikes", dao.toggle(reader.getId(), a.getId()));
        assertEquals(0, articles.findById(a.getId(), reader.getId()).getLikeCount());
    }
}
