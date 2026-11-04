package tags.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import tags.model.TagCount;
import util.Db;
import util.Texts;

/** Tags, and the links between tags and articles. */
public class TagDao implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final int MAX_TAGS_PER_ARTICLE = 8;

    /** The most used tags among published articles. */
    public List<TagCount> popular(int limit) throws Exception {
        String sql = "SELECT t.name, t.slug, COUNT(*) AS cnt FROM tags t "
                   + "JOIN article_tags x ON x.tag_id = t.id "
                   + "JOIN articles a ON a.id = x.article_id AND a.status = 'PUBLISHED' "
                   + "GROUP BY t.id, t.name, t.slug ORDER BY cnt DESC, t.name LIMIT ?";
        List<TagCount> list = new ArrayList<>();
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, limit);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    list.add(new TagCount(rs.getString("name"), rs.getString("slug"), rs.getInt("cnt")));
                }
            }
        }
        return list;
    }

    /** The display name of a tag ("c++ tips") from its slug ("c-tips"), or null if there is no such tag. */
    public String nameOf(String slug) throws Exception {
        try (Connection c = Db.connect(); PreparedStatement p = c.prepareStatement("SELECT name FROM tags WHERE slug = ?")) {
            p.setString(1, Texts.slugify(slug));
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /**
     * Gives an article exactly the tags named in `csv` ("java, jsf"), creating tags that don't exist yet.
     * Runs on the caller's connection so it is part of the caller's transaction (saving the article).
     */
    public void replaceTagsOf(Connection c, Long articleId, String csv) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("DELETE FROM article_tags WHERE article_id = ?")) {
            p.setLong(1, articleId);
            p.executeUpdate();
        }
        for (String name : parse(csv)) {
            Long tagId = findOrCreate(c, name);
            try (PreparedStatement p = c.prepareStatement("INSERT IGNORE INTO article_tags(article_id, tag_id) VALUES(?, ?)")) {
                p.setLong(1, articleId);
                p.setLong(2, tagId);
                p.executeUpdate();
            }
        }
    }

    /** "Java, #JSF ,, java" becomes ["java", "jsf"]: lower case, no duplicates, at most 8. */
    public static List<String> parse(String csv) {
        Set<String> names = new LinkedHashSet<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                String name = part.trim().replaceAll("^#+", "").replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
                if (name.length() > 50) {
                    name = name.substring(0, 50).trim();
                }
                if (!Texts.slugify(name).isEmpty()) {
                    names.add(name);
                }
                if (names.size() == MAX_TAGS_PER_ARTICLE) {
                    break;
                }
            }
        }
        return new ArrayList<>(names);
    }

    private Long findOrCreate(Connection c, String name) throws SQLException {
        String slug = Texts.slugify(name);
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM tags WHERE slug = ?")) {
            p.setString(1, slug);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        try (PreparedStatement p = c.prepareStatement("INSERT INTO tags(name, slug) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, name);
            p.setString(2, slug);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }
}
