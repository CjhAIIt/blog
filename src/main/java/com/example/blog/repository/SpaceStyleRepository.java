package com.example.blog.repository;

import com.example.blog.model.SpaceStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpaceStyleRepository extends JpaRepository<SpaceStyle, Long> {
    Optional<SpaceStyle> findByUserId(Long userId);
}
