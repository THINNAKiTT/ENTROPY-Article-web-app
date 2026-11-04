package comments.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import articles.model.Article;
import comments.model.Comment;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import support.TestData;
import users.model.User;

/** Needs the database: run with  mvn test -Dtest='*IT'  */
public class CommentDaoIT {
    private final TestData data = new TestData();
    private final CommentDao dao = new CommentDao();

    @After
    public void cleanup() throws Exception {
        data.cleanup();
    }

    private Comment add(Article article, User by, Long parent, String text) throws Exception {
        Comment c = new Comment();
        c.setArticleId(article.getId());
        c.setUserId(by.getId());
        c.setParentId(parent);
        c.setContent(text);
        c.setId(dao.insert(c));
        return c;
    }

    @Test
    public void commentsComeBackOldestFirstWithTheirAuthor() throws Exception {
        User writer = data.user("w"), reader = data.user("r");
        Article a = data.published(writer, "talk");
        Comment first = add(a, reader, null, "first");
        add(a, writer, null, "second");

        List<Comment> all = dao.findByArticle(a.getId());
        assertEquals(2, all.size());
        assertEquals("first", all.get(0).getContent());
        assertEquals(reader.getUsername(), all.get(0).getAuthorUsername());
        assertNull("top-level comments have no parent", dao.findById(first.getId()).getParentId());
        assertTrue(!dao.findById(first.getId()).isReply());
    }

    @Test
    public void repliesKeepTheirParentAndDieWithIt() throws Exception {
        User writer = data.user("w"), reader = data.user("r");
        Article a = data.published(writer, "talk");
        Comment parent = add(a, reader, null, "question");
        Comment reply = add(a, writer, parent.getId(), "answer");

        assertEquals(parent.getId(), dao.findById(reply.getId()).getParentId());
        assertTrue(dao.findById(reply.getId()).isReply());

        dao.delete(parent.getId());
        assertTrue("deleting a comment deletes its replies", dao.findByArticle(a.getId()).isEmpty());
        assertNull(dao.findById(reply.getId()));
    }
}
