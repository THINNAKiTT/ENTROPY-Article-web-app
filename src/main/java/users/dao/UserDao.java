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

public class UserDao implements Serializable {
    private static final long serialVersionUID = 1L;

    public User findById(Long id) throws Exception {
        return findOne("SELECT * FROM users WHERE id = ?", id);
    }

    public User findByUsername(String username) throws Exception {
        return findOne("SELECT * FROM users WHERE username = ?", username);
    }

    public User findByEmail(String email) throws Exception {
        return findOne("SELECT * FROM users WHERE email = ?", email);
    }

    /** Creates the user and returns the new id. Throws IllegalArgumentException if the password is empty. */
    public Long insert(User user) throws Exception {
        String hashed = Passwords.hash(user.getRawPassword());
        String sql = "INSERT INTO users(username, email, password_hash, display_name, bio, avatar_url) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, user.getUsername());
            p.setString(2, user.getEmail());
            p.setString(3, hashed);
            p.setString(4, user.getDisplayName());
            p.setString(5, user.getBio());
            p.setString(6, user.getAvatarUrl());
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : null;
            }
        }
    }

    /** Updates display name, bio and picture, and the password too if rawPassword is set. */
    public void update(User user) throws Exception {
        boolean changePassword = user.getRawPassword() != null && !user.getRawPassword().trim().isEmpty();
        String sql = changePassword
                ? "UPDATE users SET display_name = ?, bio = ?, avatar_url = ?, password_hash = ? WHERE id = ?"
                : "UPDATE users SET display_name = ?, bio = ?, avatar_url = ? WHERE id = ?";
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, user.getDisplayName());
            p.setString(2, user.getBio());
            p.setString(3, user.getAvatarUrl());
            if (changePassword) {
                p.setString(4, Passwords.hash(user.getRawPassword()));
                p.setLong(5, user.getId());
            } else {
                p.setLong(4, user.getId());
            }
            p.executeUpdate();
        }
    }

    /** Removes the user and, through the database's cascades, their articles, comments, likes and follows. */
    public void delete(Long id) throws Exception {
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            p.setLong(1, id);
            p.executeUpdate();
        }
    }

    /** The user whose username or email and password match, or null. */
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
        u.setAvatarUrl(rs.getString("avatar_url"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        u.setUpdatedAt(rs.getTimestamp("updated_at"));
        return u;
    }
}
