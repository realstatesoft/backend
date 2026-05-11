package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.payment.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<Payment> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Payment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Payment> findByUser_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Payment> findByUser_IdAndStatus(Long userId, PaymentStatus status, Pageable pageable);

    java.util.Optional<Payment> findFirstByUser_IdAndStatusOrderByCreatedAtDesc(Long userId, PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.user.id = :userId AND p.status = :status AND p.createdAt >= :since")
    BigDecimal sumCompletedByUserSince(@Param("userId") Long userId, @Param("status") PaymentStatus status, @Param("since") LocalDateTime since);

}
