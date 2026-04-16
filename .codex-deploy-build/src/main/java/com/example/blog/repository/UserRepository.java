package com.example.blog.repository;

import com.example.blog.model.RealNameVerificationStatus;
import com.example.blog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRealNameVerificationStatusOrderByRealNameVerificationSubmittedAtAscCreatedAtAsc(
            RealNameVerificationStatus status
    );

    List<User> findByRealNameVerificationStatusInOrderByRealNameVerificationReviewedAtDescCreatedAtDesc(
            Collection<RealNameVerificationStatus> statuses
    );
}
