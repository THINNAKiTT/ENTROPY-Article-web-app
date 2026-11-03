package articles.model;

import java.sql.Timestamp;

public class articles {
    private Long id;
    private String title;
    private Long authorId;
    private String slug;
    private String content;
    private String status;
    private String tags;
    private Timestamp publishedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public articles() {}

    public articles(Long id, String title, String slug, String content, String status, String tags) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.status = status;
        this.tags = tags;
    }

    public articles(Long id, String title, Long authorId, String slug, String content, String status, String tags, Timestamp publishedAt, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.title = title;
        this.authorId = authorId;
        this.slug = slug;
        this.content = content;
        this.status = status;
        this.tags = tags;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

    public String toSlug() {
        return slug != null ? slug : String.valueOf(id);
    }
}
