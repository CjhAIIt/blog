package com.example.blog.service;

import com.example.blog.model.Comment;
import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {
    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public List<Comment> findByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    public long countByPostId(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    public Comment save(Post post, User author, String content) {
        Comment comment = new Comment(content.trim(), post, author);
        return commentRepository.save(comment);
    }
}
