package tags.model;

import java.io.Serializable;

public class TagCount implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String slug;
    private final int count;

    public TagCount(String name, String slug, int count) {
        this.name = name;
        this.slug = slug;
        this.count = count;
    }

    public String getName() { return name; }
    public String getSlug() { return slug; }
    public int getCount() { return count; }
}
