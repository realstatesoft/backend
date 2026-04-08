package com.openroof.openroof.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.openroof.openroof.model.enums.NotificationType;
import com.openroof.openroof.model.notification.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Page<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUser_IdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type, Pageable pageable);

    Page<Notification> findByUser_IdAndReadAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUser_IdAndReadAtIsNotNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Notification> findByUser_IdAndReadAtIsNullOrderByCreatedAtDesc(Long userId);

    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);

    long countByUser_IdAndReadAtIsNull(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notification n
               SET n.readAt = :readAt,
                   n.version = n.version + 1
             WHERE n.user.id = :userId
               AND n.readAt IS NULL
               AND n.deletedAt IS NULL
            """)
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.deletedAt = :now WHERE n.id = :id AND n.user.id = :userId")
    int softDelete(@Param("id") Long id, @Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.deletedAt = :now, n.version = n.version + 1 WHERE n.user.id = :userId AND n.deletedAt IS NULL")
    int deleteAllByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.deletedAt = :now, n.version = n.version + 1 WHERE n.user.id = :userId AND n.readAt IS NULL AND n.deletedAt IS NULL")
    int deleteAllUnreadByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.deletedAt = :now, n.version = n.version + 1 WHERE n.user.id = :userId AND n.readAt IS NOT NULL AND n.deletedAt IS NULL")
    int deleteAllReadByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.deletedAt = :now, n.version = n.version + 1 WHERE n.user.id = :userId AND n.type = :type AND n.deletedAt IS NULL")
    int deleteAllByTypeByUser(@Param("userId") Long userId, @Param("type") NotificationType type, @Param("now") LocalDateTime now);
}

