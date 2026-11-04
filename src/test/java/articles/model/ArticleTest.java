package articles.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class ArticleTest {

    private static Article withWords(int words) {
        Article a = new Article();
        a.setContent(words == 0 ? "" : "word ".repeat(words));
        return a;
    }

    @Test
    public void readingTimeIsRoundedUpAt200WordsAMinute() {
        assertEquals("1 min read", withWords(0).getReadingTime());
        assertEquals("1 min read", withWords(5).getReadingTime());
        assertEquals("1 min read", withWords(200).getReadingTime());
        assertEquals("2 min read", withWords(201).getReadingTime());
        assertEquals("2 min read", withWords(400).getReadingTime());
        assertEquals("3 min read", withWords(401).getReadingTime());
        assertEquals("5 min read", withWords(1000).getReadingTime());
        assertEquals("1 min read", new Article().getReadingTime());
    }

    @Test
    public void tagListSplitsTheCommaSeparatedTags() {
        Article a = new Article();
        a.setTags("java, jsf ,, big data");
        assertEquals(List.of("java", "jsf", "big data"), a.getTagList());
        assertTrue(new Article().getTagList().isEmpty());
    }

    @Test
    public void heatColorGoesFromQuietToHot() {
        Article quiet = new Article();
        Article hot = new Article();
        hot.setLikeCount(10);
        assertEquals("#9AA8C7", quiet.getHeatColor());
        assertEquals("#E2492F", hot.getHeatColor());
    }

    @Test
    public void onlyPublishedArticlesArePublished() {
        Article a = new Article();
        assertFalse("new articles are drafts", a.isPublished());
        a.setStatus(Article.PUBLISHED);
        assertTrue(a.isPublished());
    }
}
