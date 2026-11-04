package bookmarks.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import util.Db;

/** Saved articles: which user saved which article (table bookmarks). */
public class BookmarkDao implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Saves the article if it isn't saved yet, otherwise removes it from the saved ones. Returns true if it is saved now. */
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
