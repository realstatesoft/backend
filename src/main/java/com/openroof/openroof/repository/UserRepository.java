package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND u.createdAt >= :start AND u.createdAt < :end")
    long countCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND (u.suspendedUntil IS NULL OR u.suspendedUntil < :now)")
    long countActiveUsers(@Param("now") LocalDateTime now);
  
    List<User> findByRole(UserRole role);

    List<User> findBySuspendedUntilAfter(LocalDateTime now);
}

