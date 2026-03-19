package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.AssignmentStatus;
import com.openroof.openroof.model.property.PropertyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropertyAssignmentRepository extends JpaRepository<PropertyAssignment, Long> {

    List<PropertyAssignment> findByProperty_Id(Long propertyId);

    List<PropertyAssignment> findByAgent_Id(Long agentId);

    List<PropertyAssignment> findByAgent_IdAndStatus(Long agentId, AssignmentStatus status);

    @Query("""
            SELECT COUNT(pa) > 0 FROM PropertyAssignment pa
            WHERE pa.property.id = :propertyId
              AND pa.status = :status
              AND pa.id <> :excludeAssignmentId
            """)
    boolean existsByPropertyAndStatusExcludingAssignment(
            @Param("propertyId") Long propertyId,
            @Param("status") AssignmentStatus status,
            @Param("excludeAssignmentId") Long excludeAssignmentId);

    @Query("""
            SELECT pa FROM PropertyAssignment pa
            WHERE pa.property.id = :propertyId
              AND pa.agent.id = :agentId
              AND pa.status IN :statuses
            """)
    Optional<PropertyAssignment> findActiveByPropertyAndAgent(
            @Param("propertyId") Long propertyId,
            @Param("agentId") Long agentId,
            @Param("statuses") List<AssignmentStatus> statuses);
}
