package likes.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import util.Db;

/** Likes: which user liked which article (table article_likes). */
public class LikeDao implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Likes the article if it isn't liked yet, otherwise removes the like. Returns true if it is liked now. */
    public boolean toggle(Long userId, Long articleId) throws Exception {
        try (Connection c = Db.connect()) {
            try (PreparedStatement p = c.prepareStatement("DELETE FROM article_likes WHERE user_id = ? AND article_id = ?")) {
                p.setLong(1, userId);
                p.setLong(2, articleId);
                if (p.executeUpdate() > 0) {
                    return false;
                }
            }
            try (PreparedStatement p = c.prepareStatement("INSERT IGNORE INTO article_likes(user_id, article_id) VALUES(?, ?)")) {
                p.setLong(1, userId);
                p.setLong(2, articleId);
                p.executeUpdate();
            }
            return true;
        }
    }
}
