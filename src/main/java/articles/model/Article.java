package articles.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import util.Html;
import util.Texts;

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
    private String tags;
    private Timestamp publishedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    private String authorUsername;
    private String authorName;
    private int likeCount;
    private int commentCount;
    private boolean liked;
    private boolean bookmarked;

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

    public boolean isPublished() {
        return PUBLISHED.equals(status);
    }

    public String getExcerpt() {
        return Texts.excerpt(Html.text(content), 220);
    }

    public int getReadingMinutes() {
        String text = Html.text(content);
        if (text.isEmpty()) {
            return 1;
        }
        int words = text.split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 200.0));
    }

    public String getReadingTime() {
        return getReadingMinutes() + " min read";
    }

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
