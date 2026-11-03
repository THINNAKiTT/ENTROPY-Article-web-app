package users.controller;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Objects;

import users.dao.usersDAO;
import users.model.users;

@Named("profileBean")
@ViewScoped
public class profileBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private userSessionBean userSession;

    private final usersDAO dao = new usersDAO();

    private Long userId;
    private users user;
    private int followersCount;
    private int followingCount;
    private boolean following;
    private boolean isOwnProfile;

    /** Call from the profile page: &lt;f:viewAction action="#{profileBean.init}"/&gt; (after the userId view param). */
    public void init() {
        Long me = currentUserId();
        if (userId == null) {
            userId = me;
        }
        if (userId == null) {
            return;
        }

        try {
            user = dao.findById(userId);
            isOwnProfile = user != null && Objects.equals(userId, me);
            following = false;
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to load profile: " + e.getMessage());
        }
    }

    public void toggleFollow() {
        if (currentUserId() == null) {
            addMessage(FacesMessage.SEVERITY_WARN, "Authentication Required", "Please log in to follow authors.");
            return;
        }
        if (isOwnProfile) {
            addMessage(FacesMessage.SEVERITY_WARN, "Self Follow", "You cannot follow yourself.");
            return;
        }
        // TODO: follow/unfollow is not implemented yet (there is a `follows` table in the DB)
    }

    public void updateProfile() {
        // check against the logged-in user right now, not a flag that was set earlier
        Long me = currentUserId();
        if (user == null || me == null || !Objects.equals(user.getId(), me)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Forbidden", "You can only edit your own profile.");
            return;
        }
        try {
            dao.update(user);
            userSession.setCurrentUser(user);
            addMessage(FacesMessage.SEVERITY_INFO, "Profile Updated", "Your profile changes have been saved.");
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error updating profile", e.getMessage());
        }
    }

    private Long currentUserId() {
        if (userSession == null || userSession.getCurrentUser() == null) {
            return null;
        }
        return userSession.getCurrentUser().getId();
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            ctx.addMessage(null, new FacesMessage(severity, summary, detail));
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public users getUser() {
        return user;
    }

    public void setUser(users user) {
        this.user = user;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    public boolean isFollowing() {
        return following;
    }

    public void setFollowing(boolean following) {
        this.following = following;
    }

    public boolean isOwnProfile() {
        return isOwnProfile;
    }

    public void setOwnProfile(boolean ownProfile) {
        isOwnProfile = ownProfile;
    }
}
