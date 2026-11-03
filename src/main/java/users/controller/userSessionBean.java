package users.controller;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import users.dao.usersDAO;
import users.model.users;

@Named("userSessionBean")
@SessionScoped
public class userSessionBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private users currentUser;
    private String loginUsername;
    private String loginPassword;

    // Sign Up
    private users newUser = new users();

    private final usersDAO dao = new usersDAO();

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public String login() {
        try {
            users u = dao.authenticate(loginUsername, loginPassword);
            if (u != null) {
                startSession(u);
                this.loginPassword = null; // don't keep the plain password in the session
                addMessage(FacesMessage.SEVERITY_INFO, "Login Success", "Welcome, " + u.getDisplayName());
                return "index?faces-redirect=true";
            }
            addMessage(FacesMessage.SEVERITY_ERROR, "Login Failed", "Invalid username/email or password");
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }
        return null;
    }

    public String signup() {
        try {
            if (isBlank(newUser.getUsername()) || isBlank(newUser.getEmail()) || isBlank(newUser.getRawPassword())) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "Username, Email, and Password are required");
                return null;
            }
            newUser.setUsername(newUser.getUsername().trim());
            newUser.setEmail(newUser.getEmail().trim());
            if (isBlank(newUser.getDisplayName())) {
                newUser.setDisplayName(newUser.getUsername());
            }
            if (dao.findByUsername(newUser.getUsername()) != null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Sign Up Failed", "Username is already taken");
                return null;
            }
            if (dao.findByEmail(newUser.getEmail()) != null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Sign Up Failed", "Email is already registered");
                return null;
            }

            Long id = dao.insert(newUser);
            users created = dao.findById(id);
            startSession(created);
            this.newUser = new users();
            addMessage(FacesMessage.SEVERITY_INFO, "Sign Up Success", "Account created! Welcome, " + created.getDisplayName());
            return "index?faces-redirect=true";
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Sign Up Failed", e.getMessage());
            return null;
        }
    }

    public String logout() {
        this.currentUser = null;
        this.loginUsername = null;
        this.loginPassword = null;
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null && ctx.getExternalContext() != null) {
            ctx.getExternalContext().invalidateSession();
        }
        return "index?faces-redirect=true";
    }

    /** Sets the logged-in user, and gives the session a new id so an old (stolen) id is useless. */
    private void startSession(users u) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null && ctx.getExternalContext().getRequest() instanceof HttpServletRequest req) {
            req.changeSessionId();
        }
        this.currentUser = u;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            // keep the message alive across the redirect that login()/signup() return
            ExternalContext ec = ctx.getExternalContext();
            ec.getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(severity, summary, detail));
        }
    }

    public users getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(users currentUser) {
        this.currentUser = currentUser;
    }

    public String getLoginUsername() {
        return loginUsername;
    }

    public void setLoginUsername(String loginUsername) {
        this.loginUsername = loginUsername;
    }

    public String getLoginPassword() {
        return loginPassword;
    }

    public void setLoginPassword(String loginPassword) {
        this.loginPassword = loginPassword;
    }

    public users getNewUser() {
        return newUser;
    }

    public void setNewUser(users newUser) {
        this.newUser = newUser;
    }
}
