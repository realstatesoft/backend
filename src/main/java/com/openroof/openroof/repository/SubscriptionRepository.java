package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.SubscriptionStatus;
import com.openroof.openroof.model.subscription.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Page<Subscription> findByUser_Id(Long userId, Pageable pageable);

    Page<Subscription> findByStatus(SubscriptionStatus status, Pageable pageable);

    Optional<Subscription> findFirstByUser_IdAndStatusOrderByExpiresAtDesc(Long userId, SubscriptionStatus status);

    boolean existsByPlan_IdAndStatusIn(Long planId, List<SubscriptionStatus> statuses);

    @Modifying
    @Query("UPDATE Subscription s SET s.status = :expired, s.updatedAt = :now " +
           "WHERE s.status = :active AND s.expiresAt < :now AND s.deletedAt IS NULL")
    int expireOlderThan(
            SubscriptionStatus active,
            SubscriptionStatus expired,
            LocalDateTime now
    );
}
