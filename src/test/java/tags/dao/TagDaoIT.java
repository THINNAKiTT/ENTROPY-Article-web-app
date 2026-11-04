package tags.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import articles.model.Article;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import support.TestData;
import tags.model.TagCount;

/** Needs the database: run with  mvn test -Dtest='*IT'  */
public class TagDaoIT {
    private final TestData data = new TestData();
    private final TagDao dao = new TagDao();

    @After
    public void cleanup() throws Exception {
        data.cleanup();
    }

    @Test
    public void tagNameComesBackWithItsOriginalPunctuation() throws Exception {
        data.article(data.user("w"), "t", Article.PUBLISHED, data.prefix + " Big Data, " + data.prefix + " C++ Tips");

        assertEquals(data.prefix + " big data", dao.nameOf(data.prefix + "-big-data"));
        assertEquals(data.prefix + " c++ tips", dao.nameOf(data.prefix + "-c-tips"));
        assertEquals("the name form works too", data.prefix + " big data", dao.nameOf(data.prefix + " Big Data"));
        assertNull(dao.nameOf("zz-no-such-tag-" + data.prefix));
    }

    @Test
    public void popularCountsPublishedArticlesOnly() throws Exception {
        data.article(data.user("w"), "one", Article.PUBLISHED, data.prefix + "hot");
        data.article(data.user("x"), "two", Article.PUBLISHED, data.prefix + "hot");
        data.article(data.user("y"), "three", Article.DRAFT, data.prefix + "hot, " + data.prefix + "cold");

        List<TagCount> popular = dao.popular(500);
        TagCount hot = popular.stream().filter(t -> t.getSlug().equals(data.prefix + "hot")).findFirst().orElseThrow();
        assertEquals(2, hot.getCount());
        assertTrue("a tag used only by drafts is not listed",
                popular.stream().noneMatch(t -> t.getSlug().equals(data.prefix + "cold")));
    }
}
