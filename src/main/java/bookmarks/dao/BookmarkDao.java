package bookmarks.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import util.Db;

public class BookmarkDao implements Serializable {
    private static final long serialVersionUID = 1L;

    public boolean toggle(Long userId, Long articleId) throws Exception {
        try (Connection c = Db.connect()) {
            try (PreparedStatement p = c.prepareStatement("DELETE FROM bookmarks WHERE user_id = ? AND article_id = ?")) {
                p.setLong(1, userId);
                p.setLong(2, articleId);
                if (p.executeUpdate() > 0) {
                    return false;
                }
            }
            try (PreparedStatement p = c.prepareStatement("INSERT IGNORE INTO bookmarks(user_id, article_id) VALUES(?, ?)")) {
                p.setLong(1, userId);
                p.setLong(2, articleId);
                p.executeUpdate();
            }
            return true;
        }
    }
}
