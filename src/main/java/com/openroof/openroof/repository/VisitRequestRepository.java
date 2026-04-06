package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.VisitRequestStatus;
import com.openroof.openroof.model.interaction.VisitRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface VisitRequestRepository extends JpaRepository<VisitRequest, Long> {

    List<VisitRequest> findByBuyerId(Long buyerId);

    List<VisitRequest> findByAgentId(Long agentId);

    List<VisitRequest> findByPropertyId(Long propertyId);

    long countByAgentIdAndStatus(Long agentId, VisitRequestStatus status);

    @Query("SELECT COUNT(vr) FROM VisitRequest vr WHERE vr.property.owner.id = :ownerId")
    long countByPropertyOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT vr FROM VisitRequest vr " +
           "JOIN FETCH vr.property JOIN FETCH vr.buyer " +
           "WHERE vr.property.owner.id = :ownerId ORDER BY vr.createdAt DESC")
    List<VisitRequest> findByPropertyOwnerIdWithDetails(@Param("ownerId") Long ownerId);

    /**
     * Finds visit requests for a given agent that are in one of the specified statuses
     * and whose effective date falls within [start, end).
     * Effective date is counterProposedAt if status is COUNTER_PROPOSED, 
     * or the appropriate field if ACCEPTED, or proposedAt if PENDING.
     */
    @Query("SELECT vr FROM VisitRequest vr " +
           "WHERE vr.agent.id = :agentId " +
           "AND vr.status IN :statuses " +
           "AND (" +
           "  (vr.proposedAt >= :start AND vr.proposedAt < :end) " +
           "  OR " +
           "  (vr.counterProposedAt >= :start AND vr.counterProposedAt < :end)" +
           ")")
    List<VisitRequest> findBusyVisits(
            @Param("agentId") Long agentId,
            @Param("statuses") Collection<VisitRequestStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);



}
