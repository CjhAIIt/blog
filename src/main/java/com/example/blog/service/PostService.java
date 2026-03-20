package com.example.blog.service;

import com.example.blog.model.Post;
import com.example.blog.model.PostCategory;
import com.example.blog.model.User;
import com.example.blog.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final MarkdownService markdownService;

    public PostService(PostRepository postRepository, MarkdownService markdownService) {
        this.postRepository = postRepository;
        this.markdownService = markdownService;
    }

    public List<Post> findAll() {
        return postRepository.findAllOrderByCreatedAtDesc();
    }

    public org.springframework.data.domain.Page<Post> findAll(org.springframework.data.domain.Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<Post> findByCategory(PostCategory category) {
        return postRepository.findByCategoryOrderByCreatedAtDesc(category);
    }

    public org.springframework.data.domain.Page<Post> findByCategory(PostCategory category, org.springframework.data.domain.Pageable pageable) {
        return postRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
    }

    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    public List<Post> findByAuthorId(Long authorId) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
    }

    public org.springframework.data.domain.Page<Post> findByAuthorId(Long authorId, org.springframework.data.domain.Pageable pageable) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable);
    }

    public List<Post> findByAuthor(User author) {
        return postRepository.findByAuthorOrderByCreatedAtDesc(author);
    }

    public List<Post> findByKeyword(String keyword) {
        return postRepository.findByKeyword(keyword);
    }

    public org.springframework.data.domain.Page<Post> findByKeyword(String keyword, org.springframework.data.domain.Pageable pageable) {
        return postRepository.findByKeyword(keyword, pageable);
    }

    public Post save(Post post) {
        return postRepository.save(post);
    }

    public void deleteById(Long id) {
        postRepository.deleteById(id);
    }

    public boolean canManage(Post post, String username) {
        return post != null
                && post.getAuthor() != null
                && username != null
                && username.equals(post.getAuthor().getUsername());
    }

    public String renderMarkdown(Post post) {
        return markdownService.render(post == null ? null : post.getContent());
    }

    public String excerpt(Post post, int maxLength) {
        return markdownService.excerpt(post == null ? null : post.getContent(), maxLength);
    }
}
