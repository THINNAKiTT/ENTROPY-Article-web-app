package users.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import users.dao.UserDao;
import util.Picture;

@WebServlet("/avatar/*")
public class AvatarServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final UserDao dao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || !path.matches("/\\d{1,18}")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Picture picture;
        try {
            picture = dao.findAvatar(Long.parseLong(path.substring(1)));
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        if (picture == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(picture.type());
        response.setContentLength(picture.data().length);
        response.setHeader("Cache-Control", "public, max-age=86400");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getOutputStream().write(picture.data());
    }
}
