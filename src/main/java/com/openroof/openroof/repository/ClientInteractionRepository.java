package com.openroof.openroof.repository;

import com.openroof.openroof.model.agent.ClientInteraction;
import com.openroof.openroof.model.enums.InteractionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ClientInteractionRepository extends JpaRepository<ClientInteraction, Long> {

    interface AgentClientInteractionMetrics {
        long getInteractionsCount();

        LocalDateTime getLastContactAt();
    }

    Page<ClientInteraction> findByAgentClient_Id(Long agentClientId, Pageable pageable);

    Page<ClientInteraction> findByAgentClient_IdAndDeletedAtIsNull(Long agentClientId, Pageable pageable);

    Page<ClientInteraction> findByAgentClient_IdAndType(Long agentClientId, InteractionType type, Pageable pageable);

    Page<ClientInteraction> findByAgentClient_IdAndTypeAndDeletedAtIsNull(Long agentClientId, InteractionType type,
                                                                          Pageable pageable);

    Optional<ClientInteraction> findByIdAndAgentClient_Id(Long id, Long agentClientId);

    Optional<ClientInteraction> findByIdAndAgentClient_IdAndDeletedAtIsNull(Long id, Long agentClientId);

    @Query("""
            SELECT COUNT(ci) AS interactionsCount,
                   MAX(ci.occurredAt) AS lastContactAt
            FROM ClientInteraction ci
            WHERE ci.agentClient.id = :agentClientId
              AND ci.deletedAt IS NULL
            """)
    AgentClientInteractionMetrics calculateMetricsByAgentClientId(@Param("agentClientId") Long agentClientId);
}
