package images.dao;

import images.model.ArticleImage;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import util.Db;
import util.Picture;

public class ImageDao implements Serializable {
    private static final long serialVersionUID = 1L;

    public ArticleImage insert(Long ownerId, String fileName, Picture picture) throws Exception {
        String name = fileName == null || fileName.isBlank() ? "picture" : fileName.strip();
        if (name.length() > 255) {
            name = name.substring(name.length() - 255);
        }
        String sql = "INSERT INTO article_images(owner_id, file_name, content_type, data) VALUES(?, ?, ?, ?)";
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setLong(1, ownerId);
            p.setString(2, name);
            p.setString(3, picture.type());
            p.setBytes(4, picture.data());
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                keys.next();
                ArticleImage image = new ArticleImage();
                image.setId(keys.getLong(1));
                image.setFileName(name);
                image.setContentType(picture.type());
                return image;
            }
        }
    }

    public List<ArticleImage> findByArticle(Long articleId) throws Exception {
        List<ArticleImage> list = new ArrayList<>();
        String sql = "SELECT id, file_name, content_type FROM article_images WHERE article_id = ? ORDER BY id";
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, articleId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    ArticleImage image = new ArticleImage();
                    image.setId(rs.getLong("id"));
                    image.setFileName(rs.getString("file_name"));
                    image.setContentType(rs.getString("content_type"));
                    list.add(image);
                }
            }
        }
        return list;
    }

    public Picture find(Long id) throws Exception {
        String sql = "SELECT data, content_type, UNIX_TIMESTAMP(created_at) AS modified FROM article_images WHERE id = ?";
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, id);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? new Picture(rs.getBytes("data"), rs.getString("content_type"), rs.getLong("modified")) : null;
            }
        }
    }

    public Long modified(Long id) throws Exception {
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement("SELECT UNIX_TIMESTAMP(created_at) AS modified FROM article_images WHERE id = ?")) {
            p.setLong(1, id);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getLong("modified") : null;
            }
        }
    }

    public Set<Long> existing(Collection<Long> ids) throws Exception {
        Set<Long> found = new HashSet<>();
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement("SELECT 1 FROM article_images WHERE id = ?")) {
            for (Long id : ids) {
                p.setLong(1, id);
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) {
                        found.add(id);
                    }
                }
            }
        }
        return found;
    }

    public void attach(Long articleId, Long ownerId, Collection<Long> imageIds) throws Exception {
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement("UPDATE article_images SET article_id = ? WHERE id = ? AND owner_id = ?")) {
            for (Long id : imageIds) {
                p.setLong(1, articleId);
                p.setLong(2, id);
                p.setLong(3, ownerId);
                p.executeUpdate();
            }
        }
    }

    public boolean delete(Long id, Long ownerId) throws Exception {
        try (Connection c = Db.connect();
             PreparedStatement p = c.prepareStatement("DELETE FROM article_images WHERE id = ? AND owner_id = ?")) {
            p.setLong(1, id);
            p.setLong(2, ownerId);
            return p.executeUpdate() > 0;
        }
    }

    public void deleteStale(Long ownerId) throws Exception {
        String sql = "DELETE FROM article_images WHERE owner_id = ? AND article_id IS NULL AND created_at < NOW() - INTERVAL 1 DAY";
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, ownerId);
            p.executeUpdate();
        }
    }
}
