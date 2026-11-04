package articles.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import articles.model.Article;
import bookmarks.dao.BookmarkDao;
import comments.dao.CommentDao;
import comments.model.Comment;
import follows.dao.FollowDao;
import java.util.List;
import likes.dao.LikeDao;
import org.junit.After;
import org.junit.Test;
import support.TestData;
import users.model.User;

/** Needs the database: run with  mvn test -Dtest='*IT'  */
public class ArticleDaoIT {
    private final TestData data = new TestData();
    private final ArticleDao dao = new ArticleDao();

    @After
    public void cleanup() throws Exception {
        data.cleanup();
    }

    private static boolean contains(List<Article> list, Article a) {
        return list.stream().anyMatch(x -> x.getId().equals(a.getId()));
    }

    // ---- addresses

    @Test
    public void slugIsTheTitlePlusEightRandomCharacters() throws Exception {
        User me = data.user("w");
        Article hello = data.published(me, "Hello world");
        assertTrue(hello.getSlug(), hello.getSlug().matches("hello-world-[a-z0-9]{8}"));
    }

    @Test
    public void sameTitleGivesDifferentAddresses() throws Exception {
        User me = data.user("w");
        Article one = data.published(me, "Same title");
        Article two = data.published(me, "Same title");
        assertFalse(one.getSlug().equals(two.getSlug()));
        assertEquals(one.getId(), dao.findBySlug(one.getSlug(), null).getId());
        assertEquals(two.getId(), dao.findBySlug(two.getSlug(), null).getId());
    }

    @Test
    public void oddTitles() throws Exception {
        User me = data.user("w");
        assertTrue(data.published(me, "Wörld über Café!!").getSlug().matches("world-uber-cafe-[a-z0-9]{8}"));
        assertTrue(data.published(me, "!!!???").getSlug().matches("article-[a-z0-9]{8}"));
        assertTrue(data.published(me, "login").getSlug().matches("login-[a-z0-9]{8}"));
        assertTrue(data.published(me, "word ".repeat(40)).getSlug().length() <= 109);
    }

    @Test
    public void editingTheTitleDoesNotMoveTheAddress() throws Exception {
        Article a = data.published(data.user("w"), "First title");
        String slug = a.getSlug();
        a.setTitle("A completely different title");
        dao.update(a);
        assertEquals(slug, dao.findById(a.getId(), null).getSlug());
    }

    // ---- who sees what

    @Test
    public void draftsAreOnlyInTheAuthorsOwnList() throws Exception {
        User me = data.user("w");
        Article published = data.published(me, "Published");
        Article draft = data.article(me, "Draft", Article.DRAFT, null);

        assertNotNull(published.getSlug());
        assertNull("a draft has no publish date", dao.findById(draft.getId(), null).getPublishedAt());
        assertTrue(contains(dao.findPublished(null, null, null, 500), published));
        assertFalse(contains(dao.findPublished(null, null, null, 500), draft));
        assertEquals(1, dao.findByAuthor(me.getId(), false, null).size());
        assertEquals(2, dao.findByAuthor(me.getId(), true, me.getId()).size());
        assertEquals(1, dao.countByAuthor(me.getId(), false));
        assertEquals(2, dao.countByAuthor(me.getId(), true));
    }

    @Test
    public void publishingADraftSetsThePublishDate() throws Exception {
        Article draft = data.article(data.user("w"), "Draft", Article.DRAFT, null);
        draft.setStatus(Article.PUBLISHED);
        dao.update(draft);
        assertNotNull(dao.findById(draft.getId(), null).getPublishedAt());
    }

    @Test
    public void searchAndTagFilters() throws Exception {
        User me = data.user("w");
        Article a = data.article(me, "Findable " + data.prefix, Article.PUBLISHED, data.prefix + "tag one, " + data.prefix + "tag two");

        assertTrue("search in the title", contains(dao.findPublished(data.prefix, null, null, 50), a));
        assertTrue("search is not case sensitive", contains(dao.findPublished(data.prefix.toUpperCase(), null, null, 50), a));
        assertTrue("by tag slug", contains(dao.findPublished(null, data.prefix + "tag-one", null, 50), a));
        assertTrue("by tag name", contains(dao.findPublished(null, data.prefix + "tag two", null, 50), a));
        assertTrue(dao.findPublished(null, data.prefix + "nosuchtag", null, 50).isEmpty());
        assertEquals(List.of(data.prefix + "tag one", data.prefix + "tag two"), dao.findById(a.getId(), null).getTagList());
    }

    // ---- the lists that depend on other people's actions

    @Test
    public void popularRanksACommentAsTwoLikesAndIgnoresDrafts() throws Exception {
        User writer = data.user("w");
        User f1 = data.user("f1"), f2 = data.user("f2"), f3 = data.user("f3");
        Article quiet = data.published(writer, "quiet");
        Article liked = data.published(writer, "liked");     // 3 likes = 3
        Article talked = data.published(writer, "talked");   // 2 comments = 4
        Article draft = data.article(writer, "draft", Article.DRAFT, null);

        LikeDao likes = new LikeDao();
        for (User f : new User[] {f1, f2, f3}) {
            likes.toggle(f.getId(), liked.getId());
            likes.toggle(f.getId(), draft.getId());
        }
        CommentDao comments = new CommentDao();
        for (User f : new User[] {f1, f2}) {
            Comment c = new Comment();
            c.setArticleId(talked.getId());
            c.setUserId(f.getId());
            c.setContent("hi");
            comments.insert(c);
        }

        List<Article> popular = dao.findPopular(null, 500);
        int iTalked = indexOf(popular, talked), iLiked = indexOf(popular, liked), iQuiet = indexOf(popular, quiet);
        assertTrue(iTalked >= 0 && iTalked < iLiked);
        assertTrue(iLiked < iQuiet);
        assertEquals("drafts never show up", -1, indexOf(popular, draft));
        assertEquals(3, popular.get(iLiked).getLikeCount());
        assertEquals(2, popular.get(iTalked).getCommentCount());
        assertTrue("the viewer's own like is flagged", dao.findPopular(f1.getId(), 500).get(indexOf(dao.findPopular(f1.getId(), 500), liked)).isLiked());
    }

    @Test
    public void followedAndSavedLists() throws Exception {
        User writer = data.user("w"), reader = data.user("r");
        Article a = data.published(writer, "for followers");
        Article draft = data.article(writer, "not yet", Article.DRAFT, null);

        assertFalse(contains(dao.findFromFollowed(reader.getId(), 50), a));
        new FollowDao().follow(reader.getId(), writer.getId());
        assertTrue(contains(dao.findFromFollowed(reader.getId(), 50), a));
        assertFalse("drafts are not in the feed", contains(dao.findFromFollowed(reader.getId(), 50), draft));

        assertTrue(dao.findSavedBy(reader.getId()).isEmpty());
        new BookmarkDao().toggle(reader.getId(), a.getId());
        assertTrue(contains(dao.findSavedBy(reader.getId()), a));
        assertTrue(dao.findSavedBy(reader.getId()).get(0).isBookmarked());
    }

    @Test
    public void deletingAnArticleRemovesItsTagLinksLikesAndComments() throws Exception {
        User me = data.user("w");
        Article a = data.article(me, "to delete", Article.PUBLISHED, data.prefix + "gone");
        new LikeDao().toggle(me.getId(), a.getId());
        dao.delete(a.getId());
        assertNull(dao.findById(a.getId(), null));
    }

    private static int indexOf(List<Article> list, Article a) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(a.getId())) {
                return i;
            }
        }
        return -1;
    }
}
