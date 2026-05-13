package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.rental.Lease;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<Lease> findByLandlordId(Long landlordId, Pageable pageable);

    Page<Lease> findByLandlordIdAndStatus(Long landlordId, LeaseStatus status, Pageable pageable);

    List<Lease> findByPrimaryTenantId(Long tenantId);

    Page<Lease> findByPrimaryTenantId(Long tenantId, Pageable pageable);

    Page<Lease> findByPrimaryTenantIdAndStatus(Long tenantId, LeaseStatus status, Pageable pageable);

    Page<Lease> findByStatus(LeaseStatus status, Pageable pageable);

    @Query("SELECT l FROM Lease l WHERE l.primaryTenant.id = :tenantId AND l.status = :status AND l.startDate <= :now AND l.endDate >= :now")
    java.util.Optional<Lease> findActiveByTenantId(@Param("tenantId") Long tenantId, @Param("status") LeaseStatus status, @Param("now") LocalDate now);

    java.util.Optional<Lease> findFirstByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, LeaseStatus status);

    @Query("""
           SELECT l FROM Lease l
           WHERE l.status = :status
             AND l.endDate < :before
           """)
    List<Lease> findByStatusAndEndDateBefore(
            @Param("status") LeaseStatus status,
            @Param("before") LocalDate before);

    @Query("""
            SELECT l FROM Lease l
            WHERE (:status IS NULL OR l.status = :status)
              AND (:propertyId IS NULL OR l.property.id = :propertyId)
            """)
    Page<Lease> findAllFiltered(@Param("status") LeaseStatus status,
                                @Param("propertyId") Long propertyId,
                                Pageable pageable);

    @Query("""
            SELECT l FROM Lease l
            WHERE l.landlord.id = :landlordId
              AND (:status IS NULL OR l.status = :status)
              AND (:propertyId IS NULL OR l.property.id = :propertyId)
            """)
    Page<Lease> findByLandlordFiltered(@Param("landlordId") Long landlordId,
                                       @Param("status") LeaseStatus status,
                                       @Param("propertyId") Long propertyId,
                                       Pageable pageable);

    @Query("""
            SELECT l FROM Lease l
            WHERE l.primaryTenant.id = :tenantId
              AND (:status IS NULL OR l.status = :status)
              AND (:propertyId IS NULL OR l.property.id = :propertyId)
            """)
    Page<Lease> findByTenantFiltered(@Param("tenantId") Long tenantId,
                                     @Param("status") LeaseStatus status,
                                     @Param("propertyId") Long propertyId,
                                     Pageable pageable);
}
