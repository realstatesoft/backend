package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.rental.Lease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaseRepository extends JpaRepository<Lease, Long> {

    List<Lease> findByPropertyIdAndStatus(Long propertyId, LeaseStatus status);

    List<Lease> findByLandlordId(Long landlordId);

    List<Lease> findByPrimaryTenantId(Long tenantId);

    @Query("""
           SELECT l FROM Lease l
           WHERE l.status = :status
             AND l.endDate < :before
           """)
    List<Lease> findByStatusAndEndDateBefore(
            @Param("status") LeaseStatus status,
            @Param("before") LocalDate before);
}
