package comments.dao;

import comments.model.Comment;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import util.Db;

public class CommentDao implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String SELECT =
        "SELECT c.*, u.username AS author_username, u.display_name AS author_name "
      + "FROM comments c JOIN users u ON u.id = c.user_id ";

    public List<Comment> findByArticle(Long articleId) throws Exception {
        List<Comment> list = new ArrayList<>();
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement(SELECT + "WHERE c.article_id = ? ORDER BY c.created_at, c.id")) {
            p.setLong(1, articleId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Comment findById(Long id) throws Exception {
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement(SELECT + "WHERE c.id = ?")) {
            p.setLong(1, id);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Long insert(Comment cm) throws Exception {
        String sql = "INSERT INTO comments(article_id, user_id, parent_id, content) VALUES(?, ?, ?, ?)";
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setLong(1, cm.getArticleId());
            p.setLong(2, cm.getUserId());
            if (cm.getParentId() == null) {
                p.setNull(3, Types.BIGINT);
            } else {
                p.setLong(3, cm.getParentId());
            }
            p.setString(4, cm.getContent());
            p.executeUpdate();
            try (ResultSet k = p.getGeneratedKeys()) {
                return k.next() ? k.getLong(1) : null;
            }
        }
    }

    public void delete(Long id) throws Exception {
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement("DELETE FROM comments WHERE id = ?")) {
            p.setLong(1, id);
            p.executeUpdate();
        }
    }

    private Comment mapRow(ResultSet rs) throws SQLException {
        Comment cm = new Comment();
        cm.setId(rs.getLong("id"));
        cm.setArticleId(rs.getLong("article_id"));
        cm.setUserId(rs.getLong("user_id"));
        long parent = rs.getLong("parent_id");
        cm.setParentId(rs.wasNull() ? null : parent);
        cm.setContent(rs.getString("content"));
        cm.setCreatedAt(rs.getTimestamp("created_at"));
        cm.setUpdatedAt(rs.getTimestamp("updated_at"));
        cm.setAuthorName(rs.getString("author_name"));
        cm.setAuthorUsername(rs.getString("author_username"));
        return cm;
    }
}
