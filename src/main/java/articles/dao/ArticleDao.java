package articles.dao;

import articles.model.Article;
import java.io.Serializable;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import tags.dao.TagDao;
import util.Db;
import util.Texts;

public class ArticleDao implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String CODE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TagDao tagDao = new TagDao();

    private static final String SELECT =
        "SELECT a.*, u.username AS author_username, u.display_name AS author_name, "
      + " (SELECT COUNT(*) FROM article_likes l WHERE l.article_id = a.id) AS like_count, "
      + " (SELECT COUNT(*) FROM comments c WHERE c.article_id = a.id) AS comment_count, "
      + " EXISTS (SELECT 1 FROM article_likes l2 WHERE l2.article_id = a.id AND l2.user_id = ?) AS liked, "
      + " EXISTS (SELECT 1 FROM bookmarks b WHERE b.article_id = a.id AND b.user_id = ?) AS bookmarked, "
      + " (SELECT GROUP_CONCAT(t.name ORDER BY t.name SEPARATOR ', ') FROM article_tags x "
      + "    JOIN tags t ON t.id = x.tag_id WHERE x.article_id = a.id) AS tag_list "
      + "FROM articles a JOIN users u ON u.id = a.author_id ";

    public Article findById(Long id, Long viewerId) throws Exception {
        List<Article> found = query("a.id = ?", List.of(id), "a.id", 1, viewerId);
        return found.isEmpty() ? null : found.get(0);
    }

    public Article findBySlug(String slug, Long viewerId) throws Exception {
        List<Article> found = query("a.slug = ?", List.of(slug), "a.id", 1, viewerId);
        return found.isEmpty() ? null : found.get(0);
    }

    public List<Article> findPublished(String search, String tag, Long viewerId, int limit) throws Exception {
        StringBuilder where = new StringBuilder("a.status = 'PUBLISHED'");
        List<Object> params = new ArrayList<>();
        if (!Texts.isBlank(search)) {
            where.append(" AND (a.title LIKE ? OR a.content LIKE ?)");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (!Texts.isBlank(tag)) {
            where.append(" AND EXISTS (SELECT 1 FROM article_tags x JOIN tags t ON t.id = x.tag_id"
                       + " WHERE x.article_id = a.id AND t.slug = ?)");
            params.add(Texts.slugify(tag));
        }
        return query(where.toString(), params, "a.published_at DESC, a.id DESC", limit, viewerId);
    }

    public List<Article> findPopular(Long viewerId, int limit) throws Exception {
        return query("a.status = 'PUBLISHED'", List.of(),
                "(like_count + 2 * comment_count) DESC, a.published_at DESC, a.id DESC", limit, viewerId);
    }

    public List<Article> findFromFollowed(Long userId, int limit) throws Exception {
        return query("a.status = 'PUBLISHED' AND a.author_id IN "
                   + "(SELECT following_id FROM follows WHERE follower_id = ?)",
                List.of(userId), "a.published_at DESC, a.id DESC", limit, userId);
    }

    public List<Article> findSavedBy(Long userId) throws Exception {
        return query("a.status = 'PUBLISHED' AND EXISTS (SELECT 1 FROM bookmarks bm "
                   + "WHERE bm.article_id = a.id AND bm.user_id = ?)",
                List.of(userId), "a.published_at DESC, a.id DESC", 100, userId);
    }

    public List<Article> findByAuthor(Long authorId, boolean includeDrafts, Long viewerId) throws Exception {
        String where = includeDrafts ? "a.author_id = ?" : "a.author_id = ? AND a.status = 'PUBLISHED'";
        return query(where, List.of(authorId), "COALESCE(a.published_at, a.created_at) DESC, a.id DESC", 100, viewerId);
    }

    public int countByAuthor(Long authorId, boolean includeDrafts) throws Exception {
        String sql = "SELECT COUNT(*) FROM articles WHERE author_id = ?" + (includeDrafts ? "" : " AND status = 'PUBLISHED'");
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, authorId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Long insert(Article article) throws Exception {
        try (Connection c = Db.connect()) {
            c.setAutoCommit(false);
            try {
                String slug = newSlug(c, article.getTitle());
                String sql = "INSERT INTO articles(author_id, title, slug, content, status, published_at) "
                           + "VALUES(?, ?, ?, ?, ?, " + (article.isPublished() ? "NOW()" : "NULL") + ")";
                Long id;
                try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    p.setLong(1, article.getAuthorId());
                    p.setString(2, article.getTitle());
                    p.setString(3, slug);
                    p.setString(4, article.getContent());
                    p.setString(5, article.getStatus());
                    p.executeUpdate();
                    try (ResultSet keys = p.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getLong(1);
                    }
                }
                tagDao.replaceTagsOf(c, id, article.getTags());
                c.commit();
                article.setId(id);
                article.setSlug(slug);
                return id;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void update(Article article) throws Exception {
        try (Connection c = Db.connect()) {
            c.setAutoCommit(false);
            try {
                String sql = "UPDATE articles SET title = ?, content = ?, status = ?, "
                           + "published_at = CASE WHEN ? = 'PUBLISHED' AND published_at IS NULL THEN NOW() ELSE published_at END "
                           + "WHERE id = ?";
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    p.setString(1, article.getTitle());
                    p.setString(2, article.getContent());
                    p.setString(3, article.getStatus());
                    p.setString(4, article.getStatus());
                    p.setLong(5, article.getId());
                    p.executeUpdate();
                }
                tagDao.replaceTagsOf(c, article.getId(), article.getTags());
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void delete(Long id) throws Exception {
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement("DELETE FROM articles WHERE id = ?")) {
            p.setLong(1, id);
            p.executeUpdate();
        }
    }

    private List<Article> query(String where, List<?> params, String orderBy, int limit, Long viewerId) throws Exception {
        String sql = SELECT + (where.isEmpty() ? "" : "WHERE " + where) + " ORDER BY " + orderBy + " LIMIT " + limit;
        List<Article> list = new ArrayList<>();
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            long viewer = viewerId == null ? -1 : viewerId;
            int i = 1;
            p.setLong(i++, viewer);
            p.setLong(i++, viewer);
            for (Object o : params) {
                if (o instanceof Long l) {
                    p.setLong(i++, l);
                } else {
                    p.setString(i++, String.valueOf(o));
                }
            }
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Article map(ResultSet rs) throws SQLException {
        Article a = new Article();
        a.setId(rs.getLong("id"));
        a.setAuthorId(rs.getLong("author_id"));
        a.setTitle(rs.getString("title"));
        a.setSlug(rs.getString("slug"));
        a.setContent(rs.getString("content"));
        a.setStatus(rs.getString("status"));
        a.setPublishedAt(rs.getTimestamp("published_at"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        a.setUpdatedAt(rs.getTimestamp("updated_at"));
        a.setAuthorUsername(rs.getString("author_username"));
        a.setAuthorName(rs.getString("author_name"));
        a.setLikeCount(rs.getInt("like_count"));
        a.setCommentCount(rs.getInt("comment_count"));
        a.setLiked(rs.getBoolean("liked"));
        a.setBookmarked(rs.getBoolean("bookmarked"));
        a.setTags(rs.getString("tag_list"));
        return a;
    }

    private String newSlug(Connection c, String title) throws SQLException {
        String base = Texts.slugify(title);
        if (base.isEmpty()) {
            base = "article";
        }
        if (base.length() > 100) {
            base = base.substring(0, 100).replaceAll("-+$", "");
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            String slug = base + "-" + randomCode(CODE_LENGTH);
            if (!slugExists(c, slug)) {
                return slug;
            }
        }
        throw new SQLException("Could not find a free address for this article, please try again");
    }

    private static String randomCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    private boolean slugExists(Connection c, String slug) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM articles WHERE slug = ?")) {
            p.setString(1, slug);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next();
            }
        }
    }
}
