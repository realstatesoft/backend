package com.openroof.openroof.repository;

import com.openroof.openroof.model.agent.AgentClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AgentClientRepository extends JpaRepository<AgentClient, Long>, JpaSpecificationExecutor<AgentClient> {

    Page<AgentClient> findByAgent_Id(Long agentId, Pageable pageable);

    Page<AgentClient> findByUser_Id(Long userId, Pageable pageable);

    Optional<AgentClient> findByAgent_IdAndUser_Id(Long agentId, Long userId);

    boolean existsByAgent_IdAndUser_Id(Long agentId, Long userId);

    @Query("SELECT ac FROM AgentClient ac " +
            "JOIN FETCH ac.agent a " +
            "JOIN FETCH a.user " +
            "WHERE ac.id = :id")
    Optional<AgentClient> findByIdWithAgentAndUser(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AgentClient ac
            SET ac.interactionsCount = :interactionsCount,
                ac.lastContactDate = :lastContactAt
            WHERE ac.id = :agentClientId
            """)
    int updateMetricsById(
            @Param("agentClientId") Long agentClientId,
            @Param("interactionsCount") Integer interactionsCount,
            @Param("lastContactAt") LocalDateTime lastContactAt);
}
