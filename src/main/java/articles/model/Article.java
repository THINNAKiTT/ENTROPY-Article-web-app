package articles.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import util.Texts;

/** A row of the `articles` table, plus the numbers and flags the pages show next to it. */
public class Article implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";

    private Long id;
    private String title;
    private Long authorId;
    private String slug;
    private String content;
    private String status = DRAFT;
    private String tags; // comma separated tag names, e.g. "java, jsf"
    private Timestamp publishedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // filled in by ArticleDao when it joins other tables (not columns of `articles`)
    private String authorUsername;
    private String authorName;
    private int likeCount;
    private int commentCount;
    private boolean liked;       // by the person looking at it
    private boolean bookmarked;  // by the person looking at it

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Timestamp getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Timestamp publishedAt) { this.publishedAt = publishedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isBookmarked() { return bookmarked; }
    public void setBookmarked(boolean bookmarked) { this.bookmarked = bookmarked; }

    // ---- helpers used by the pages

    public boolean isPublished() {
        return PUBLISHED.equals(status);
    }

    public String getExcerpt() {
        return Texts.excerpt(content, 220);
    }

    /** Reading time at 200 words a minute, rounded up: 1-200 words = 1 min, 201-400 = 2 min, ... */
    public int getReadingMinutes() {
        if (content == null || content.isBlank()) {
            return 1;
        }
        int words = content.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 200.0));
    }

    /** "1 min read", "5 min read" ... */
    public String getReadingTime() {
        return getReadingMinutes() + " min read";
    }

    /** Publish date, or the creation date for drafts. */
    public String getDate() {
        Timestamp t = publishedAt != null ? publishedAt : createdAt;
        return t == null ? "" : t.toLocalDateTime().format(DATE);
    }

    public List<String> getTagList() {
        List<String> list = new ArrayList<>();
        if (tags != null) {
            for (String t : tags.split(",")) {
                if (!t.isBlank()) {
                    list.add(t.trim());
                }
            }
        }
        return list;
    }

}
