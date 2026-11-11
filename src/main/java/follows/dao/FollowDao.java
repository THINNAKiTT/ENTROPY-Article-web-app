package follows.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import users.dao.UserDao;
import users.model.User;
import util.Db;

public class FollowDao implements Serializable {
    private static final long serialVersionUID = 1L;

    public boolean isFollowing(Long followerId, Long followingId) throws Exception {
        String sql = "SELECT 1 FROM follows WHERE follower_id = ? AND following_id = ?";
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, followerId);
            p.setLong(2, followingId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void follow(Long followerId, Long followingId) throws Exception {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }
        update("INSERT IGNORE INTO follows(follower_id, following_id) VALUES(?, ?)", followerId, followingId);
    }

    public void unfollow(Long followerId, Long followingId) throws Exception {
        update("DELETE FROM follows WHERE follower_id = ? AND following_id = ?", followerId, followingId);
    }

    public int countFollowers(Long userId) throws Exception {
        return count("SELECT COUNT(*) FROM follows WHERE following_id = ?", userId);
    }

    public int countFollowing(Long userId) throws Exception {
        return count("SELECT COUNT(*) FROM follows WHERE follower_id = ?", userId);
    }

    public List<User> findFollowers(Long userId) throws Exception {
        return people("SELECT u.id, u.username, u.display_name, u.bio, " + UserDao.AVATAR_FLAG + " FROM follows f "
                    + "JOIN users u ON u.id = f.follower_id WHERE f.following_id = ? ORDER BY f.created_at DESC", userId);
    }

    public List<User> findFollowing(Long userId) throws Exception {
        return people("SELECT u.id, u.username, u.display_name, u.bio, " + UserDao.AVATAR_FLAG + " FROM follows f "
                    + "JOIN users u ON u.id = f.following_id WHERE f.follower_id = ? ORDER BY f.created_at DESC", userId);
    }

    private void update(String sql, Long first, Long second) throws Exception {
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, first);
            p.setLong(2, second);
            p.executeUpdate();
        }
    }

    private int count(String sql, Long userId) throws Exception {
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private List<User> people(String sql, Long userId) throws Exception {
        List<User> list = new ArrayList<>();
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getLong("id"));
                    u.setUsername(rs.getString("username"));
                    u.setDisplayName(rs.getString("display_name"));
                    u.setBio(rs.getString("bio"));
                    u.setHasAvatar(rs.getBoolean("has_avatar"));
                    u.setAvatarVersion(rs.getLong("avatar_version"));
                    list.add(u);
                }
            }
        }
        return list;
    }
}
