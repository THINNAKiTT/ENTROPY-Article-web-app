package users.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import users.dao.LoginTokenDao;
import users.dao.UserDao;
import users.model.User;
import util.Texts;
import web.Cookies;
import web.Faces;
import web.Urls;

@Named("userSessionBean")
@SessionScoped
public class UserSessionBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String LOGIN_COOKIE = "entropy_login";
    private static final int LOGIN_SECONDS = 30 * 24 * 60 * 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserDao dao = new UserDao();
    private final LoginTokenDao tokens = new LoginTokenDao();

    private User currentUser;

    private String loginUsername;
    private String loginPassword;
    private boolean rememberMe = true;

    private User newUser = new User();

    @PostConstruct
    void init() {
        String cookie = Cookies.get(LOGIN_COOKIE);
        if (cookie == null) {
            return;
        }
        String[] parts = cookie.split("\\.", 2);
        try {
            LoginTokenDao.Token token = parts.length == 2 ? tokens.find(parts[0]) : null;
            if (token != null && token.expiresAt().toInstant().isAfter(Instant.now())
                    && MessageDigest.isEqual(sha256(parts[1]).getBytes(StandardCharsets.UTF_8), token.hash().getBytes(StandardCharsets.UTF_8))) {
                currentUser = dao.findById(token.userId());
            }
            if (currentUser == null) {
                Cookies.remove(LOGIN_COOKIE);
            }
        } catch (Exception e) {
            currentUser = null;
        }
    }

    private void rememberLogin(Long userId) throws Exception {
        String selector = randomToken(12);
        String validator = randomToken(32);
        tokens.create(userId, selector, sha256(validator), Instant.now().plusSeconds(LOGIN_SECONDS));
        Cookies.set(LOGIN_COOKIE, selector + "." + validator, LOGIN_SECONDS);
    }

    private void forgetLogin() {
        String cookie = Cookies.get(LOGIN_COOKIE);
        if (cookie != null) {
            try {
                tokens.delete(cookie.split("\\.", 2)[0]);
            } catch (Exception ignored) {
            }
            Cookies.remove(LOGIN_COOKIE);
        }
    }

    private static String randomToken(int bytes) {
        byte[] data = new byte[bytes];
        RANDOM.nextBytes(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    public void forgetAllLogins() throws Exception {
        if (currentUser != null) {
            tokens.deleteForUser(currentUser.getId());
        }
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public Long getCurrentUserId() {
        return currentUser == null ? null : currentUser.getId();
    }

    public String requireLogin() {
        if (!isLoggedIn()) {
            Faces.promptLogin("You need to log in to see that page.");
        }
        return null;
    }

    public String login() {
        try {
            User user = dao.authenticate(loginUsername, loginPassword);
            if (user == null) {
                Faces.error("Login failed", "Invalid username/email or password");
                return null;
            }
            startSession(user);
            if (rememberMe) {
                rememberLogin(user.getId());
            }
            loginUsername = null;
            loginPassword = null;
            Faces.info("Login success", "Welcome, " + user.getDisplayName());
            Faces.reloadPage();
        } catch (Exception e) {
            Faces.error("Error", e.getMessage());
        }
        return null;
    }

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
            rememberLogin(created.getId());
            newUser = new User();
            Faces.info("Sign up success", "Account created! Welcome, " + created.getDisplayName());
            Faces.reloadPage();
        } catch (Exception e) {
            Faces.error("Sign up failed", e.getMessage());
        }
        return null;
    }

    public String logout() {
        forgetLogin();
        currentUser = null;
        loginUsername = null;
        loginPassword = null;
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            ExternalContext ec = ctx.getExternalContext();
            String home = ec.getRequestContextPath() + Urls.home();
            ec.invalidateSession();
            try {
                ec.redirect(home);
            } catch (IOException ignored) {
            }
            ctx.responseComplete();
        }
        return null;
    }

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

    public boolean isRememberMe() { return rememberMe; }
    public void setRememberMe(boolean rememberMe) { this.rememberMe = rememberMe; }

    public String getLoginPassword() { return loginPassword; }
    public void setLoginPassword(String loginPassword) { this.loginPassword = loginPassword; }

    public User getNewUser() { return newUser; }
    public void setNewUser(User newUser) { this.newUser = newUser; }
}
