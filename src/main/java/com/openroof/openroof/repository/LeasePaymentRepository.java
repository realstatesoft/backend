package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.LeasePaymentStatus;
import com.openroof.openroof.model.rental.LeasePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LeasePaymentRepository extends JpaRepository<LeasePayment, Long> {
    List<LeasePayment> findByInstallmentId(Long installmentId);

    List<LeasePayment> findByInstallmentIdIn(java.util.Collection<Long> installmentIds);

    List<LeasePayment> findByLeaseIdOrderByCreatedAtDesc(Long leaseId);
    
    @Query("SELECT SUM(p.amount) FROM LeasePayment p WHERE p.payer.id = :tenantId AND p.status = :status AND p.paidAt >= :since")
    BigDecimal sumCompletedByTenantSince(@Param("tenantId") Long tenantId, @Param("status") LeasePaymentStatus status, @Param("since") LocalDateTime since);
}
