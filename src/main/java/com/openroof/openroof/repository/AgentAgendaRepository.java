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
     * Returns events for a given agent whose time range overlaps with [start, end].
     * Intersection condition: startsAt <= endOfMonth AND endsAt >= startOfMonth
     */
    @Query("SELECT a FROM AgentAgenda a WHERE a.agent.id = :agentId " +
           "AND a.startsAt <= :end AND a.endsAt >= :start " +
           "ORDER BY a.startsAt ASC")
    List<AgentAgenda> findByAgentAndMonthOverlap(
            @Param("agentId") Long agentId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
