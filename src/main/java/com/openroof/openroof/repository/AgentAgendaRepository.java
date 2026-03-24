package com.openroof.openroof.repository;

import com.openroof.openroof.model.interaction.AgentAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgentAgendaRepository extends JpaRepository<AgentAgenda, Long>, JpaSpecificationExecutor<AgentAgenda> {
    List<AgentAgenda> findByAgentIdAndStartsAtBetweenOrderByStartsAtAsc(Long agentId, LocalDateTime start, LocalDateTime end);
}
