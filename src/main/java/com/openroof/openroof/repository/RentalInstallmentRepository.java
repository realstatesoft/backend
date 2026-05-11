package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.InstallmentStatus;
import com.openroof.openroof.model.rental.RentalInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface RentalInstallmentRepository extends JpaRepository<RentalInstallment, Long> {

    boolean existsByLeaseId(Long leaseId);

    List<RentalInstallment> findByLeaseIdOrderByDueDateAsc(Long leaseId);

    @Query("""
           SELECT i FROM RentalInstallment i
           WHERE i.status = :status
             AND i.dueDate < :before
           """)
    List<RentalInstallment> findByStatusAndDueDateBefore(
            @Param("status") InstallmentStatus status,
            @Param("before") LocalDate before);

    @Query("""
           SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0)
           FROM RentalInstallment i
           WHERE i.lease.id = :leaseId
             AND i.status IN :statuses
           """)
    BigDecimal sumPendingBalanceByLeaseId(
            @Param("leaseId") Long leaseId,
            @Param("statuses") Collection<InstallmentStatus> statuses);
}
