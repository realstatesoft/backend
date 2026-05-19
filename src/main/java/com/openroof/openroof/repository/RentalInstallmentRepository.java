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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM RentalInstallment i WHERE i.id = :id")
    java.util.Optional<RentalInstallment> findByIdForUpdate(@Param("id") Long id);

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

    java.util.Optional<RentalInstallment> findFirstByLease_IdAndStatusNotOrderByDueDateAsc(Long leaseId, InstallmentStatus status);

    java.util.Optional<RentalInstallment> findFirstByLeaseIdAndStatusInOrderByDueDateAsc(Long leaseId, Collection<InstallmentStatus> statuses);
    List<RentalInstallment> findTop5ByLeaseIdOrderByDueDateDesc(Long leaseId);
    List<RentalInstallment> findTop5ByLeaseIdOrderByDueDateAsc(Long leaseId);

    org.springframework.data.domain.Page<RentalInstallment> findByLeaseIdOrderByDueDateDesc(Long leaseId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT i FROM RentalInstallment i WHERE i.lease.id IN :leaseIds ORDER BY i.dueDate DESC")
    org.springframework.data.domain.Page<RentalInstallment> findByLeaseIdsOrderByDueDateDesc(@Param("leaseIds") java.util.Collection<Long> leaseIds, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT i FROM RentalInstallment i WHERE i.lease.id IN :leaseIds ORDER BY i.dueDate ASC")
    java.util.List<RentalInstallment> findByLeaseIdsOrderByDueDateAsc(@Param("leaseIds") java.util.Collection<Long> leaseIds);

    long countByLeaseIdAndStatus(Long leaseId, InstallmentStatus status);
}
