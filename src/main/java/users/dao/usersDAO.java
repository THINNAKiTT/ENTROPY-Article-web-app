package users.dao;

import java.io.Serializable;
import java.sql.*;
import java.util.*;

import util.SecurityUtil;
import users.model.users;
import util.utilDB;

public class usersDAO implements Serializable {
    private static final long serialVersionUID = 1L;

    public List<users> findAll() throws Exception {
        List<users> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection c = utilDB.getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public users findById(Long id) throws Exception {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection c = utilDB.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, id);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public users findByUsername(String username) throws Exception {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = utilDB.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public users findByEmail(String email) throws Exception {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection c = utilDB.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Long insert(users r) throws Exception {
        // throws IllegalArgumentException if the password is empty
        String hashed = SecurityUtil.hashPassword(r.getRawPassword());

        String sql = "INSERT INTO users(username, email, password_hash, display_name, bio, avatar_url) VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection c = utilDB.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, r.getUsername());
            p.setString(2, r.getEmail());
            p.setString(3, hashed);
            p.setString(4, r.getDisplayName());
            p.setString(5, r.getBio());
            p.setString(6, r.getAvatarUrl());
            p.executeUpdate();

            try (ResultSet k = p.getGeneratedKeys()) {
                 return k.next() ? k.getLong(1) : null; 
            }
        }
    }

    public void update(users r) throws Exception {
        boolean updatePassword = r.getRawPassword() != null && !r.getRawPassword().trim().isEmpty();
        String sql = updatePassword
                ? "UPDATE users SET display_name = ?, bio = ?, avatar_url = ?, password_hash = ? WHERE id = ?"
                : "UPDATE users SET display_name = ?, bio = ?, avatar_url = ? WHERE id = ?";

        try (Connection c = utilDB.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, r.getDisplayName());
            p.setString(2, r.getBio());
            p.setString(3, r.getAvatarUrl());
            if (updatePassword) {
                p.setString(4, SecurityUtil.hashPassword(r.getRawPassword()));
                p.setLong(5, r.getId());
            } else {
                p.setLong(4, r.getId());
            }
            p.executeUpdate();
        }
    }

    public void delete(Long id) throws Exception {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection c = utilDB.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, id);
            p.executeUpdate();
        }
    }

    public users authenticate(String usernameOrEmail, String password) throws Exception {
        users u = findByUsername(usernameOrEmail);
        if (u == null) {
            u = findByEmail(usernameOrEmail);
        }
        if (u != null && SecurityUtil.verifyPassword(password, u.getPasswordHash())) {
            return u;
        }
        return null;
    }

    private users mapRow(ResultSet rs) throws SQLException {
        users u = new users();
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
