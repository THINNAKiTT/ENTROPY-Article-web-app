package users.controller;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import users.dao.UserDao;
import users.model.User;
import util.Texts;
import web.Faces;
import web.Urls;

/** Who is logged in on this browser: log in, sign up and log out (the two popups in the layout call these). */
@Named("userSessionBean")
@SessionScoped
public class UserSessionBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UserDao dao = new UserDao();

    private User currentUser;

    // what the log in popup is bound to
    private String loginUsername;
    private String loginPassword;

    // what the sign up popup is bound to
    private User newUser = new User();

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public Long getCurrentUserId() {
        return currentUser == null ? null : currentUser.getId();
    }

    /** viewAction for pages that need an account: visitors go home, where the log in popup opens. */
    public String requireLogin() {
        if (!isLoggedIn()) {
            Faces.promptLogin("You need to log in to see that page.");
        }
        return null;
    }

    /** Called from the log in popup. On success the page you were on is reloaded. */
    public String login() {
        try {
            User user = dao.authenticate(loginUsername, loginPassword);
            if (user == null) {
                Faces.error("Login failed", "Invalid username/email or password");
                return null;
            }
            startSession(user);
            loginUsername = null;
            loginPassword = null; // don't keep the plain password in the session
            Faces.info("Login success", "Welcome, " + user.getDisplayName());
            Faces.reloadPage();
        } catch (Exception e) {
            Faces.error("Error", e.getMessage());
        }
        return null;
    }

    /** Called from the sign up popup: checks the form, creates the account, logs in and reloads the page. */
    public String signup() {
        try {
            if (Texts.isBlank(newUser.getUsername()) || Texts.isBlank(newUser.getEmail()) || Texts.isBlank(newUser.getRawPassword())) {
                Faces.error("Validation error", "Username, email and password are required");
                return null;
            }
            newUser.setUsername(newUser.getUsername().trim());
            newUser.setEmail(newUser.getEmail().trim());
            if (!newUser.getUsername().matches("[A-Za-z0-9_]{3,30}")) {
                Faces.error("Check your username", "Use 3 to 30 letters, numbers or underscores.");
                return null;
            }
            if (!newUser.getEmail().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") || newUser.getEmail().length() > 255) {
                Faces.error("Check your email", "That doesn't look like an email address.");
                return null;
            }
            if (newUser.getRawPassword().length() < 6) {
                Faces.error("Password too short", "Use at least 6 characters.");
                return null;
            }
            if (Texts.isBlank(newUser.getDisplayName())) {
                newUser.setDisplayName(newUser.getUsername());
            }
            if (dao.findByUsername(newUser.getUsername()) != null) {
                Faces.error("Sign up failed", "Username is already taken");
                return null;
            }
            if (dao.findByEmail(newUser.getEmail()) != null) {
                Faces.error("Sign up failed", "Email is already registered");
                return null;
            }

            User created = dao.findById(dao.insert(newUser));
            startSession(created);
            newUser = new User();
            Faces.info("Sign up success", "Account created! Welcome, " + created.getDisplayName());
            Faces.reloadPage();
        } catch (Exception e) {
            Faces.error("Sign up failed", e.getMessage());
        }
        return null;
    }

    public String logout() {
        currentUser = null;
        loginUsername = null;
        loginPassword = null;
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            ExternalContext ec = ctx.getExternalContext();
            String home = ec.getRequestContextPath() + Urls.home();
            ec.invalidateSession();
            try {
                ec.redirect(home); // no message to carry over: the session (and its flash) is gone
            } catch (IOException ignored) {
                // the browser is gone
            }
            ctx.responseComplete();
        }
        return null;
    }

    /** Sets the logged-in user, and gives the session a new id so an old (stolen) id is useless. */
    private void startSession(User user) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null && ctx.getExternalContext().getRequest() instanceof HttpServletRequest request) {
            request.changeSessionId();
        }
        currentUser = user;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User currentUser) { this.currentUser = currentUser; }

    public String getLoginUsername() { return loginUsername; }
    public void setLoginUsername(String loginUsername) { this.loginUsername = loginUsername; }

    public String getLoginPassword() { return loginPassword; }
    public void setLoginPassword(String loginPassword) { this.loginPassword = loginPassword; }

    public User getNewUser() { return newUser; }
    public void setNewUser(User newUser) { this.newUser = newUser; }
}
