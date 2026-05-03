package com.example.blog.repository;

import com.example.blog.model.SpaceStyleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceStyleVersionRepository extends JpaRepository<SpaceStyleVersion, Long> {
    List<SpaceStyleVersion> findTop12ByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<SpaceStyleVersion> findByIdAndUserId(Long id, Long userId);
}
