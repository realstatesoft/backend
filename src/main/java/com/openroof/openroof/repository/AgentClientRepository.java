package com.openroof.openroof.repository;

import com.openroof.openroof.model.agent.AgentClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentClientRepository extends JpaRepository<AgentClient, Long>, JpaSpecificationExecutor<AgentClient> {

    Page<AgentClient> findByAgent_Id(Long agentId, Pageable pageable);

    Page<AgentClient> findByUser_Id(Long userId, Pageable pageable);

    Optional<AgentClient> findByAgent_IdAndUser_Id(Long agentId, Long userId);

    boolean existsByAgent_IdAndUser_Id(Long agentId, Long userId);
}
