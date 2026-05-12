package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.MaintenanceStatus;
import com.openroof.openroof.model.maintenance.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    @Query("""
           SELECT COUNT(m) FROM MaintenanceRequest m
           WHERE m.tenant.id = :tenantId
             AND m.status IN :statuses
           """)
    long countByTenantIdAndStatusIn(
            @Param("tenantId") Long tenantId,
            @Param("statuses") List<MaintenanceStatus> statuses);

    List<MaintenanceRequest> findByTenantIdAndStatusInOrderByCreatedAtDesc(Long tenantId, List<MaintenanceStatus> statuses);

    List<MaintenanceRequest> findTop5ByTenantIdOrderByCreatedAtDesc(Long tenantId);

    org.springframework.data.domain.Page<MaintenanceRequest> findByLeaseIdOrderByCreatedAtDesc(Long leaseId, org.springframework.data.domain.Pageable pageable);
}
