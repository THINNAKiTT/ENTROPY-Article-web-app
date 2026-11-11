package users.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import util.Db;

public class LoginTokenDao implements Serializable {
    private static final long serialVersionUID = 1L;

    public record Token(Long userId, String hash, Timestamp expiresAt) {}

    public void create(Long userId, String selector, String hash, Instant expiresAt) throws Exception {
        try (Connection c = Db.connect()) {
            try (PreparedStatement p = c.prepareStatement("DELETE FROM login_tokens WHERE expires_at < NOW()")) {
                p.executeUpdate();
            }
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO login_tokens(user_id, selector, token_hash, expires_at) VALUES(?, ?, ?, ?)")) {
                p.setLong(1, userId);
                p.setString(2, selector);
                p.setString(3, hash);
                p.setTimestamp(4, Timestamp.from(expiresAt));
                p.executeUpdate();
            }
        }
    }

    public Token find(String selector) throws Exception {
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement(
                     "SELECT user_id, token_hash, expires_at FROM login_tokens WHERE selector = ?")) {
            p.setString(1, selector);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? new Token(rs.getLong("user_id"), rs.getString("token_hash"), rs.getTimestamp("expires_at")) : null;
            }
        }
    }

    public void delete(String selector) throws Exception {
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement("DELETE FROM login_tokens WHERE selector = ?")) {
            p.setString(1, selector);
            p.executeUpdate();
        }
    }

    public void deleteForUser(Long userId) throws Exception {
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement("DELETE FROM login_tokens WHERE user_id = ?")) {
            p.setLong(1, userId);
            p.executeUpdate();
        }
    }
}
