package users.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String rawPassword;
    private String displayName;
    private String bio;
    private boolean hasAvatar;
    private long avatarVersion;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRawPassword() { return rawPassword; }
    public void setRawPassword(String rawPassword) { this.rawPassword = rawPassword; }

    public String getDisplayName() { return displayName != null ? displayName : username; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public boolean isHasAvatar() { return hasAvatar; }
    public void setHasAvatar(boolean hasAvatar) { this.hasAvatar = hasAvatar; }

    public long getAvatarVersion() { return avatarVersion; }
    public void setAvatarVersion(long avatarVersion) { this.avatarVersion = avatarVersion; }

    public String getAvatarSrc() {
        return hasAvatar && id != null ? "/avatar/" + id + "?v=" + avatarVersion : null;
    }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getInitial() {
        String name = getDisplayName();
        return (name == null || name.isBlank()) ? "?" : name.trim().substring(0, 1).toUpperCase();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
