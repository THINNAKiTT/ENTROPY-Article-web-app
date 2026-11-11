package images.controller;

import images.dao.ImageDao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import util.Picture;

@WebServlet("/image/*")
public class ImageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final ImageDao dao = new ImageDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || !path.matches("/\\d{1,18}")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long id = Long.parseLong(path.substring(1));
        Picture picture;
        try {
            Long modified = dao.modified(id);
            if (modified == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String etag = "\"" + id + "-" + modified + "\"";
            response.setHeader("ETag", etag);
            response.setHeader("Cache-Control", "no-cache");
            if (etag.equals(request.getHeader("If-None-Match"))) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
            picture = dao.find(id);
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
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getOutputStream().write(picture.data());
    }
}
