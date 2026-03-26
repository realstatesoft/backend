package com.openroof.openroof.repository;

import com.openroof.openroof.model.agent.ClientInteraction;
import com.openroof.openroof.model.enums.InteractionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientInteractionRepository extends JpaRepository<ClientInteraction, Long> {

    Page<ClientInteraction> findByAgentClient_Id(Long agentClientId, Pageable pageable);

    Page<ClientInteraction> findByAgentClient_IdAndType(Long agentClientId, InteractionType type, Pageable pageable);

    Optional<ClientInteraction> findByIdAndAgentClient_Id(Long id, Long agentClientId);
}
