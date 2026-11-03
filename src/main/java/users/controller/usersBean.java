package users.controller;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import users.dao.usersDAO;
import users.model.users;

@Named("usersBean")
@ViewScoped
public class usersBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private userSessionBean userSession;

    private users user = new users();
    private List<users> list; // loaded once, cleared after changes
    private final usersDAO dao = new usersDAO();

    public void save() {
        if (!requireLogin()) {
            return;
        }
        try {
            if (user.getId() == null) {
                dao.insert(user);
                addMessage(FacesMessage.SEVERITY_INFO, "Success", "User created: " + user.getUsername());
            } else {
                dao.update(user);
                addMessage(FacesMessage.SEVERITY_INFO, "Success", "User updated: " + user.getUsername());
            }
            user = new users(); // clear form
            list = null;
        } catch (IllegalArgumentException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", e.getMessage());
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
        }
    }

    public void edit(users u) {
        if (u == null || !requireLogin()) {
            return;
        }
        this.user = u;
    }

    public void cancelEdit() {
        this.user = new users();
    }

    public boolean isEditMode() {
        return user != null && user.getId() != null;
    }

    public void delete(Long id) {
        if (!requireLogin()) {
            return;
        }
        if (Objects.equals(id, userSession.getCurrentUser().getId())) {
            addMessage(FacesMessage.SEVERITY_WARN, "Not allowed", "You cannot delete your own account here.");
            return;
        }
        try {
            dao.delete(id);
            list = null;
            addMessage(FacesMessage.SEVERITY_INFO, "Deleted", "User deleted successfully");
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error deleting user", e.getMessage());
        }
    }

    /** Called by EL many times per request, so only hit the database once. */
    public List<users> getList() {
        if (list == null) {
            try {
                list = dao.findAll();
            } catch (Exception e) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Error loading users", e.getMessage());
                return Collections.emptyList();
            }
        }
        return list;
    }

    private boolean requireLogin() {
        if (userSession == null || !userSession.isLoggedIn()) {
            addMessage(FacesMessage.SEVERITY_WARN, "Login required", "Please log in first.");
            return false;
        }
        return true;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            ctx.addMessage(null, new FacesMessage(severity, summary, detail));
        }
    }

    public users getUser() {
        return user;
    }

    public void setUser(users user) {
        this.user = user;
    }
}
