package images.model;

import java.io.Serializable;

public class ArticleImage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String fileName;
    private String contentType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
