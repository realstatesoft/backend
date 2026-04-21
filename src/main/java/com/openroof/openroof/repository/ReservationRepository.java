package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.ReservationStatus;
import com.openroof.openroof.model.reservation.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = {"property", "buyer"})
    @Query("""
           SELECT r FROM Reservation r
           WHERE r.buyer.id = :buyerId
             AND (:status IS NULL OR r.status = :status)
           ORDER BY r.createdAt DESC
           """)
    Page<Reservation> findByBuyerFiltered(
            @Param("buyerId") Long buyerId,
            @Param("status") ReservationStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"property", "buyer"})
    @Query("""
           SELECT r FROM Reservation r
           WHERE r.property.owner.id = :ownerId
             AND (:status IS NULL OR r.status = :status)
           ORDER BY r.createdAt DESC
           """)
    Page<Reservation> findByPropertyOwnerFiltered(
            @Param("ownerId") Long ownerId,
            @Param("status") ReservationStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"property", "buyer"})
    @Query("""
           SELECT r FROM Reservation r
           WHERE r.property.agent.user.id = :agentUserId
             AND (:status IS NULL OR r.status = :status)
           ORDER BY r.createdAt DESC
           """)
    Page<Reservation> findByPropertyAgentUserFiltered(
            @Param("agentUserId") Long agentUserId,
            @Param("status") ReservationStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"property", "buyer"})
    List<Reservation> findByProperty_IdOrderByCreatedAtDesc(Long propertyId);

    @Query("""
           SELECT COUNT(r) > 0 FROM Reservation r
           WHERE r.property.id = :propertyId
             AND r.status IN :blockingStatuses
           """)
    boolean existsBlockingReservation(
            @Param("propertyId") Long propertyId,
            @Param("blockingStatuses") Collection<ReservationStatus> blockingStatuses);

    @Query("""
           SELECT r FROM Reservation r
           WHERE r.status IN :activeStatuses
             AND r.expiresAt IS NOT NULL
             AND r.expiresAt < :now
         """)
    List<Reservation> findExpired(
            @Param("activeStatuses") Collection<ReservationStatus> activeStatuses,
            @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"property", "buyer"})
    Optional<Reservation> findFirstByProperty_IdAndBuyer_IdAndStatusInOrderByCreatedAtDesc(
            Long propertyId,
            Long buyerId,
            Collection<ReservationStatus> statuses);
}
