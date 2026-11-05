package comments.controller;

import comments.dao.CommentDao;
import comments.model.Comment;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import users.controller.UserSessionBean;
import web.Faces;

@Named("commentsBean")
@ViewScoped
public class CommentsBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final int MAX_LENGTH = 5000;

    @Inject
    private UserSessionBean userSession;

    private final CommentDao dao = new CommentDao();

    private Long articleId;
    private Long articleAuthorId;
    private List<Comment> thread = new ArrayList<>();

    private String newComment;
    private Long replyTo;
    private String replyToName;

    public void load(Long articleId, Long articleAuthorId) {
        this.articleId = articleId;
        this.articleAuthorId = articleAuthorId;
        reload();
    }

    public String add() {
        Long me = userSession.getCurrentUserId();
        if (me == null) {
            Faces.promptLogin("You need an account to comment.");
            return null;
        }
        String text = newComment == null ? "" : newComment.trim();
        if (text.isEmpty()) {
            Faces.warn("Empty comment", "Write something before posting.");
            return null;
        }
        if (text.length() > MAX_LENGTH) {
            Faces.warn("Comment too long", "Keep it under " + MAX_LENGTH + " characters.");
            return null;
        }
        try {
            Comment comment = new Comment();
            comment.setArticleId(articleId);
            comment.setUserId(me);
            comment.setContent(text);
            if (replyTo != null) {
                Comment parent = dao.findById(replyTo);
                if (parent != null && Objects.equals(parent.getArticleId(), articleId)) {
                    comment.setParentId(parent.getParentId() != null ? parent.getParentId() : parent.getId());
                }
            }
            dao.insert(comment);
            newComment = null;
            cancelReply();
            reload();
        } catch (Exception e) {
            Faces.error("Could not post the comment", e.getMessage());
        }
        return null;
    }

    public void startReply(Long commentId) {
        for (Comment c : thread) {
            if (c.getId().equals(commentId)) {
                replyTo = commentId;
                replyToName = c.getAuthorName();
                return;
            }
        }
    }

    public void cancelReply() {
        replyTo = null;
        replyToName = null;
    }

    public void delete(Long commentId) {
        try {
            Comment comment = dao.findById(commentId);
            if (comment == null) {
                return;
            }
            if (!canDelete(comment)) {
                Faces.error("Not allowed", "You can only delete your own comments.");
                return;
            }
            dao.delete(commentId);
            if (Objects.equals(replyTo, commentId)) {
                cancelReply();
            }
            reload();
        } catch (Exception e) {
            Faces.error("Could not delete the comment", e.getMessage());
        }
    }

    public boolean canDelete(Comment comment) {
        Long me = userSession.getCurrentUserId();
        return me != null && (me.equals(comment.getUserId()) || me.equals(articleAuthorId));
    }

    private void reload() {
        if (articleId == null) {
            thread = new ArrayList<>();
            return;
        }
        try {
            List<Comment> all = dao.findByArticle(articleId);
            List<Comment> ordered = new ArrayList<>();
            for (Comment top : all) {
                if (!top.isReply()) {
                    ordered.add(top);
                    for (Comment reply : all) {
                        if (top.getId().equals(reply.getParentId())) {
                            ordered.add(reply);
                        }
                    }
                }
            }
            thread = ordered;
        } catch (Exception e) {
            Faces.error("Could not load the comments", e.getMessage());
        }
    }

    public List<Comment> getThread() { return thread; }
    public int getCount() { return thread.size(); }

    public String getNewComment() { return newComment; }
    public void setNewComment(String newComment) { this.newComment = newComment; }

    public String getReplyToName() { return replyToName; }
    public boolean isReplying() { return replyTo != null; }
}
