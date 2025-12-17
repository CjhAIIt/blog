package com.example.blog.service;

import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {
    
    @Autowired
    private PostRepository postRepository;
    
    public List<Post> findAll() {
        return postRepository.findAllOrderByCreatedAtDesc();
    }
    
    public Page<Post> findAll(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }
    
    public List<Post> findByAuthorId(Long authorId) {
        return postRepository.findByAuthorId(authorId);
    }
    
    public Page<Post> findByAuthorId(Long authorId, Pageable pageable) {
        return postRepository.findByAuthorId(authorId, pageable);
    }
    
    public List<Post> findByAuthor(User author) {
        return postRepository.findByAuthor(author);
    }
    
    public List<Post> findByKeyword(String keyword) {
        return postRepository.findByKeyword(keyword);
    }
    
    public Page<Post> findByKeyword(String keyword, Pageable pageable) {
        return postRepository.findByKeyword(keyword, pageable);
    }
    
    public Post save(Post post) {
        return postRepository.save(post);
    }
    
    public void deleteById(Long id) {
        postRepository.deleteById(id);
    }
}