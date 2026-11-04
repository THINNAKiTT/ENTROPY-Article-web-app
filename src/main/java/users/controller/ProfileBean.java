package users.controller;

import articles.controller.ArticleListBean;
import articles.model.Article;
import follows.dao.FollowDao;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.primefaces.PrimeFaces;
import users.dao.UserDao;
import users.model.User;
import util.Texts;
import web.Faces;

/** Backs profile.xhtml (/@username): a person's articles, followers and following, and the "Edit profile" popup. */
@Named("profileBean")
@ViewScoped
public class ProfileBean extends ArticleListBean {
    private static final long serialVersionUID = 1L;

    private final UserDao userDao = new UserDao();
    private final FollowDao followDao = new FollowDao();

    private String username;           // from the address: /@username
    private Long userId;               // that user's id, looked up from the username
    private String tab = "articles";   // from the address: /@username/followers ...

    private User user;
    private User editUser = new User(); // what the "Edit profile" popup is bound to: a copy of `user`
    private int articleCount;
    private int followersCount;
    private int followingCount;
    private boolean following;
    private boolean ownProfile;

    private List<User> people;

    /** viewAction: loads the profile of /@username, or answers 404. */
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
            username = user.getUsername(); // as stored, in case the address used different capital letters
            ownProfile = Objects.equals(userId, me);
            loadCounts(me);
        } catch (Exception e) {
            Faces.error("Could not load the profile", e.getMessage());
        }
    }

    /** The articles on this profile: everything they wrote (drafts too, if it is your own profile). */
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

    // ---- follow

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

    // ---- the "Edit profile" popup

    /**
     * Called when "Edit profile" is clicked, just before the popup opens: copies your current details
     * into editUser, which is what the popup's fields are bound to. Cancelling or a rejected save
     * therefore never changes what the profile page shows.
     */
    public void startEdit() {
        Long me = userSession.getCurrentUserId();
        if (user == null || me == null || !Objects.equals(user.getId(), me)) {
            return;
        }
        editUser = new User();
        editUser.setId(user.getId());
        editUser.setDisplayName(user.getDisplayName());
        editUser.setBio(user.getBio());
        editUser.setAvatarUrl(user.getAvatarUrl());
    }

    /** Saves the popup. On success the popup closes (the page checks the "saved" value we hand back). */
    public String updateProfile() {
        // checked against the logged-in user right now, not against a flag set earlier
        Long me = userSession.getCurrentUserId();
        if (user == null || me == null || !Objects.equals(user.getId(), me) || !Objects.equals(editUser.getId(), me)) {
            Faces.error("Forbidden", "You can only edit your own profile.");
            return null;
        }
        if (Texts.isBlank(editUser.getDisplayName())) {
            Faces.warn("Name missing", "Your display name cannot be empty.");
            return null;
        }
        String avatar = editUser.getAvatarUrl() == null ? "" : editUser.getAvatarUrl().trim();
        if (!avatar.isEmpty() && !avatar.matches("(?i)https?://[^\\s\"'<>]+")) {
            Faces.warn("Check the picture link", "The picture link must start with http:// or https:// and have no spaces or quotes.");
            return null;
        }
        editUser.setAvatarUrl(avatar.isEmpty() ? null : avatar);
        String newPassword = editUser.getRawPassword();
        if (!Texts.isBlank(newPassword) && newPassword.length() < 6) {
            Faces.warn("Password too short", "Use at least 6 characters.");
            return null;
        }
        try {
            userDao.update(editUser);
            editUser.setRawPassword(null);                          // never keep the plain password around
            user = userDao.findById(me);                            // what the page shows now
            userSession.setCurrentUser(userDao.findById(me));       // and what the header shows
            Faces.info("Profile saved", "Your changes are live.");
            PrimeFaces.current().ajax().addCallbackParam("saved", true);
        } catch (Exception e) {
            Faces.error("Could not save the profile", e.getMessage());
        }
        return null;
    }

    // ---- followers / following lists

    /** The people list for the "followers" or "following" tab. */
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

    /** Position of the current tab in the page's tab menu (articles, followers, following). */
    public int getTabIndex() {
        return switch (tab) {
            case "followers" -> 1;
            case "following" -> 2;
            default -> 0;
        };
    }

    public User getUser() { return user; }
    public User getEditUser() { return editUser; }
    public void setEditUser(User editUser) { this.editUser = editUser; }

    public int getArticleCount() { return articleCount; }
    public int getFollowersCount() { return followersCount; }
    public int getFollowingCount() { return followingCount; }
    public boolean isFollowing() { return following; }
    public boolean isOwnProfile() { return ownProfile; }
}
