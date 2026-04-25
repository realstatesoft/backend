package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.payment.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Page<Payment> findByUser_Id(Long userId, Pageable pageable);

    Page<Payment> findByUser_IdAndStatus(Long userId, PaymentStatus status, Pageable pageable);
}
