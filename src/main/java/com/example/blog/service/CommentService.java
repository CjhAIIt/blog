package com.example.blog.service;

import com.example.blog.model.Comment;
import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.repository.CommentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
    }

    public List<Comment> findByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        if (comments.isEmpty()) {
            return comments;
        }

        Map<Long, List<Comment>> repliesByParentId = new LinkedHashMap<>();
        List<Comment> rootComments = new ArrayList<>();
        for (Comment comment : comments) {
            if (comment.getParentComment() == null || comment.getParentComment().getId() == null) {
                rootComments.add(comment);
                continue;
            }
            repliesByParentId.computeIfAbsent(comment.getParentComment().getId(), key -> new ArrayList<>()).add(comment);
        }

        List<Comment> ordered = new ArrayList<>(comments.size());
        Set<Long> appendedCommentIds = new HashSet<>(comments.size());
        for (Comment rootComment : rootComments) {
            appendThread(rootComment, repliesByParentId, ordered, appendedCommentIds);
        }
        for (Comment comment : comments) {
            Long commentId = comment.getId();
            if (commentId == null || !appendedCommentIds.contains(commentId)) {
                appendThread(comment, repliesByParentId, ordered, appendedCommentIds);
            }
        }
        return ordered;
    }

    public long countByPostId(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    public long countByPostAuthorId(Long authorId) {
        return commentRepository.countByPostAuthorId(authorId);
    }

    public java.util.Optional<Comment> findByIdAndPostId(Long commentId, Long postId) {
        return commentRepository.findByIdAndPostId(commentId, postId);
    }

    public boolean canManage(Comment comment, User user) {
        if (comment == null || user == null) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        if (comment.getAuthor() != null && user.getId() != null && user.getId().equals(comment.getAuthor().getId())) {
            return true;
        }
        return comment.getPost() != null
                && comment.getPost().getAuthor() != null
                && user.getId() != null
                && user.getId().equals(comment.getPost().getAuthor().getId());
    }

    @Transactional
    public Comment save(Post post, User author, String content) {
        return save(post, author, content, null);
    }

    @Transactional
    public Comment save(Post post, User author, String content, Long parentCommentId) {
        Comment comment = new Comment(content.trim(), post, author);
        if (parentCommentId != null) {
            Comment parentComment = commentRepository.findByIdAndPostId(parentCommentId, post.getId())
                    .orElseThrow(() -> new IllegalArgumentException("回复的评论不存在"));
            comment.setParentComment(parentComment);
        }
        Comment savedComment = commentRepository.save(comment);
        notificationService.notifyForComment(savedComment);
        return savedComment;
    }

    @Transactional
    public void deleteByPostId(Long postId) {
        commentRepository.deleteByPostId(postId);
    }

    @Transactional
    public void delete(Comment comment) {
        if (comment == null || comment.getId() == null) {
            return;
        }
        notificationService.deleteByCommentId(comment.getId());
        commentRepository.delete(comment);
    }

    private void appendThread(Comment comment,
                              Map<Long, List<Comment>> repliesByParentId,
                              List<Comment> ordered,
                              Set<Long> appendedCommentIds) {
        if (comment == null || comment.getId() == null || appendedCommentIds.contains(comment.getId())) {
            return;
        }
        ordered.add(comment);
        appendedCommentIds.add(comment.getId());
        for (Comment reply : repliesByParentId.getOrDefault(comment.getId(), List.of())) {
            appendThread(reply, repliesByParentId, ordered, appendedCommentIds);
        }
    }
}
