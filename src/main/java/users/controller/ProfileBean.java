package users.controller;

import articles.controller.ArticleListBean;
import articles.model.Article;
import follows.dao.FollowDao;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import users.dao.UserDao;
import users.model.User;
import util.Images;
import util.Texts;
import web.Faces;

@Named("profileBean")
@ViewScoped
public class ProfileBean extends ArticleListBean {
    private static final long serialVersionUID = 1L;

    private final UserDao userDao = new UserDao();
    private final FollowDao followDao = new FollowDao();

    private String username;
    private Long userId;
    private String tab = "articles";

    private User user;
    private User editUser = new User();
    private String newPassword;
    private String confirmPassword;
    private int articleCount;
    private int followersCount;
    private int followingCount;
    private boolean following;
    private boolean ownProfile;

    private List<User> people;

    public void init() {
        Long me = userSession.getCurrentUserId();
        if (Texts.isBlank(username)) {
            if (me == null) {
                Faces.promptLogin("Log in to see your profile.");
                return;
            }
            username = userSession.getCurrentUser().getUsername();
        }
        try {
            user = userDao.findByUsername(username);
            if (user == null) {
                Faces.notFound();
                return;
            }
            userId = user.getId();
            username = user.getUsername();
            ownProfile = Objects.equals(userId, me);
            loadCounts(me);
        } catch (Exception e) {
            Faces.error("Could not load the profile", e.getMessage());
        }
    }

    @Override
    protected List<Article> fetch(Long viewerId) throws Exception {
        return articleDao.findByAuthor(userId, ownProfile, viewerId);
    }

    private void loadCounts(Long me) throws Exception {
        articleCount = articleDao.countByAuthor(userId, ownProfile);
        followersCount = followDao.countFollowers(userId);
        followingCount = followDao.countFollowing(userId);
        following = me != null && !ownProfile && followDao.isFollowing(me, userId);
    }

    public String toggleFollow() {
        Long me = userSession.getCurrentUserId();
        if (me == null) {
            Faces.promptLogin("You need an account to follow writers.");
            return null;
        }
        if (ownProfile) {
            Faces.warn("Not possible", "You cannot follow yourself.");
            return null;
        }
        try {
            if (followDao.isFollowing(me, userId)) {
                followDao.unfollow(me, userId);
            } else {
                followDao.follow(me, userId);
            }
            people = null;
            loadCounts(me);
        } catch (Exception e) {
            Faces.error("Something went wrong", e.getMessage());
        }
        return null;
    }

    public void startEdit() {
        Long me = userSession.getCurrentUserId();
        if (user == null || me == null || !Objects.equals(user.getId(), me)) {
            return;
        }
        editUser = new User();
        editUser.setId(user.getId());
        editUser.setDisplayName(user.getDisplayName());
        editUser.setBio(user.getBio());
    }

    public String updateProfile() {
        Long me = userSession.getCurrentUserId();
        if (user == null || me == null || !Objects.equals(user.getId(), me) || !Objects.equals(editUser.getId(), me)) {
            Faces.error("Forbidden", "You can only edit your own profile.");
            return null;
        }
        if (Texts.isBlank(editUser.getDisplayName())) {
            Faces.warn("Name missing", "Your display name cannot be empty.");
            return null;
        }
        try {
            userDao.update(editUser);
            refreshUser(me);
            Faces.info("Profile saved", "Your changes are live.");
            PrimeFaces.current().ajax().addCallbackParam("saved", true);
        } catch (Exception e) {
            Faces.error("Could not save the profile", e.getMessage());
        }
        return null;
    }

    public String changePassword() {
        Long me = userSession.getCurrentUserId();
        if (user == null || me == null || !Objects.equals(user.getId(), me)) {
            Faces.error("Forbidden", "You can only change your own password.");
            return null;
        }
        if (Texts.isBlank(newPassword) || newPassword.length() < 6) {
            Faces.warn("Password too short", "Use at least 6 characters.");
            return null;
        }
        if (!newPassword.equals(confirmPassword)) {
            Faces.warn("Passwords do not match", "Type the same new password twice.");
            return null;
        }
        try {
            userDao.updatePassword(me, newPassword);
            userSession.forgetAllLogins();
            newPassword = null;
            confirmPassword = null;
            Faces.info("Password changed", "Use your new password next time you log in.");
            PrimeFaces.current().ajax().addCallbackParam("saved", true);
        } catch (Exception e) {
            Faces.error("Could not change the password", e.getMessage());
        }
        return null;
    }

    public String deleteAccount() {
        Long me = userSession.getCurrentUserId();
        if (user == null || me == null || !Objects.equals(user.getId(), me)) {
            Faces.error("Forbidden", "You can only remove your own account.");
            return null;
        }
        try {
            userDao.delete(me);
        } catch (Exception e) {
            Faces.error("Could not remove the account", e.getMessage());
            return null;
        }
        return userSession.logout();
    }

    public void uploadAvatar(FileUploadEvent event) {
        Long me = userSession.getCurrentUserId();
        if (user == null || me == null || !Objects.equals(user.getId(), me)) {
            Faces.error("Forbidden", "You can only change your own picture.");
            return;
        }
        UploadedFile file = event.getFile();
        if (file.getSize() > Images.MAX_UPLOAD_BYTES) {
            Faces.warn("Picture too big", "Use a picture under 5 MB.");
            return;
        }
        try {
            userDao.saveAvatar(me, Images.avatar(file.getContent()));
            refreshUser(me);
            Faces.info("Picture saved", "Your new profile picture is live.");
        } catch (IOException e) {
            Faces.warn("Could not use that file", e.getMessage());
        } catch (Exception e) {
            Faces.error("Could not save the picture", e.getMessage());
        }
    }

    public String removeAvatar() {
        Long me = userSession.getCurrentUserId();
        if (user == null || me == null || !Objects.equals(user.getId(), me)) {
            return null;
        }
        try {
            userDao.removeAvatar(me);
            refreshUser(me);
            Faces.info("Picture removed", "Your profile picture was removed.");
        } catch (Exception e) {
            Faces.error("Could not remove the picture", e.getMessage());
        }
        return null;
    }

    private void refreshUser(Long me) throws Exception {
        user = userDao.findById(me);
        userSession.setCurrentUser(userDao.findById(me));
    }

    public List<User> getPeople() {
        if (people == null) {
            try {
                people = "followers".equals(tab) ? followDao.findFollowers(userId) : followDao.findFollowing(userId);
            } catch (Exception e) {
                Faces.error("Could not load the list", e.getMessage());
                return Collections.emptyList();
            }
        }
        return people;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTab() { return tab; }
    public void setTab(String tab) {
        this.tab = tab == null ? "articles" : tab;
        this.people = null;
    }

    public int getTabIndex() {
        return switch (tab) {
            case "followers" -> 1;
            case "following" -> 2;
            default -> 0;
        };
    }

    public User getUser() { return user; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public User getEditUser() { return editUser; }
    public void setEditUser(User editUser) { this.editUser = editUser; }

    public int getArticleCount() { return articleCount; }
    public int getFollowersCount() { return followersCount; }
    public int getFollowingCount() { return followingCount; }
    public boolean isFollowing() { return following; }
    public boolean isOwnProfile() { return ownProfile; }
}
