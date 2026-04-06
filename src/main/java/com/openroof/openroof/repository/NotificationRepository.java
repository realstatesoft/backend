package com.openroof.openroof.repository;

import com.openroof.openroof.model.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Notification> findByUser_IdAndReadAtIsNullOrderByCreatedAtDesc(Long userId);

    long countByUser_IdAndReadAtIsNull(Long userId);
}
