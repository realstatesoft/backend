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

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = {"property", "buyer"})
    Page<Reservation> findByBuyer_IdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

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
}