package com.openroof.openroof.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.openroof.openroof.model.interaction.AgentAgenda;

@Repository
public interface AgentAgendaRepository extends JpaRepository<AgentAgenda, Long>, JpaSpecificationExecutor<AgentAgenda> {

    /**
     * Returns events for a given user whose time range overlaps with [start, end].
     * Intersection condition: startsAt <= endOfMonth AND endsAt >= startOfMonth
     */
    @Query("SELECT a FROM AgentAgenda a " +
           "LEFT JOIN FETCH a.visit v LEFT JOIN FETCH v.buyer " +
           "WHERE a.user.id = :userId " +
           "AND a.startsAt <= :end AND a.endsAt >= :start " +
           "ORDER BY a.startsAt ASC")
    List<AgentAgenda> findByUserAndMonthOverlap(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Finds a single event by its ID and owning user in one query.
     * Returns empty if the event doesn't exist OR belongs to a different user,
     * preventing ID enumeration (callers should always throw 404 on empty).
     */
    @Query("SELECT a FROM AgentAgenda a " +
           "LEFT JOIN FETCH a.visit v LEFT JOIN FETCH v.buyer " +
           "WHERE a.id = :id AND a.user.id = :userId")
    java.util.Optional<AgentAgenda> findByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId);

    /**
     * Finds upcoming events for a user, ordered by start date.
     */
    @Query("SELECT a FROM AgentAgenda a " +
           "LEFT JOIN FETCH a.visit v LEFT JOIN FETCH v.buyer " +
           "WHERE a.user.id = :userId AND a.startsAt >= :now ORDER BY a.startsAt ASC")
    List<AgentAgenda> findUpcoming(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            org.springframework.data.domain.Pageable pageable);
}
