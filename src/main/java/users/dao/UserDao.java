package users.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import users.model.User;
import util.Db;
import util.Passwords;
import util.Picture;

public class UserDao implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String AVATAR_FLAG =
        "(u.avatar IS NOT NULL) AS has_avatar, COALESCE(UNIX_TIMESTAMP(u.updated_at), 0) AS avatar_version";

    private static final String SELECT =
        "SELECT u.id, u.username, u.email, u.password_hash, u.display_name, u.bio, u.created_at, u.updated_at, "
      + AVATAR_FLAG + " FROM users u ";

    public User findById(Long id) throws Exception {
        return findOne(SELECT + "WHERE u.id = ?", id);
    }

    public User findByUsername(String username) throws Exception {
        return findOne(SELECT + "WHERE u.username = ?", username);
    }

    public User findByEmail(String email) throws Exception {
        return findOne(SELECT + "WHERE u.email = ?", email);
    }

    public Long insert(User user) throws Exception {
        String hashed = Passwords.hash(user.getRawPassword());
        String sql = "INSERT INTO users(username, email, password_hash, display_name, bio) VALUES(?, ?, ?, ?, ?)";
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, user.getUsername());
            p.setString(2, user.getEmail());
            p.setString(3, hashed);
            p.setString(4, user.getDisplayName());
            p.setString(5, user.getBio());
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : null;
            }
        }
    }

    public void update(User user) throws Exception {
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement("UPDATE users SET display_name = ?, bio = ? WHERE id = ?")) {
            p.setString(1, user.getDisplayName());
            p.setString(2, user.getBio());
            p.setLong(3, user.getId());
            p.executeUpdate();
        }
    }

    public void updatePassword(Long id, String newPassword) throws Exception {
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
            p.setString(1, Passwords.hash(newPassword));
            p.setLong(2, id);
            p.executeUpdate();
        }
    }

    public void saveAvatar(Long id, Picture picture) throws Exception {
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement("UPDATE users SET avatar = ?, avatar_type = ? WHERE id = ?")) {
            p.setBytes(1, picture.data());
            p.setString(2, picture.type());
            p.setLong(3, id);
            p.executeUpdate();
        }
    }

    public void removeAvatar(Long id) throws Exception {
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement("UPDATE users SET avatar = NULL, avatar_type = NULL WHERE id = ?")) {
            p.setLong(1, id);
            p.executeUpdate();
        }
    }

    public Picture findAvatar(Long id) throws Exception {
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement("SELECT avatar, avatar_type, UNIX_TIMESTAMP(updated_at) AS modified FROM users WHERE id = ? AND avatar IS NOT NULL")) {
            p.setLong(1, id);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? new Picture(rs.getBytes("avatar"), rs.getString("avatar_type"), rs.getLong("modified")) : null;
            }
        }
    }

    public void delete(Long id) throws Exception {
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            p.setLong(1, id);
            p.executeUpdate();
        }
    }

    public User authenticate(String usernameOrEmail, String password) throws Exception {
        User user = findByUsername(usernameOrEmail);
        if (user == null) {
            user = findByEmail(usernameOrEmail);
        }
        return (user != null && Passwords.verify(password, user.getPasswordHash())) ? user : null;
    }

    private User findOne(String sql, Object parameter) throws Exception {
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            if (parameter instanceof Long id) {
                p.setLong(1, id);
            } else {
                p.setString(1, (String) parameter);
            }
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setDisplayName(rs.getString("display_name"));
        u.setBio(rs.getString("bio"));
        u.setHasAvatar(rs.getBoolean("has_avatar"));
        u.setAvatarVersion(rs.getLong("avatar_version"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        u.setUpdatedAt(rs.getTimestamp("updated_at"));
        return u;
    }
}
