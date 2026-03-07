package com.openroof.openroof.repository;

import com.openroof.openroof.model.lead.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    Page<Lead> findByAgentId(Long agentId, Pageable pageable);

    List<Lead> findByAgentIdAndStatusName(Long agentId, String statusName);

    @Query("SELECT l FROM Lead l WHERE l.agent.id = :agentId AND l.status.name = :status ORDER BY l.createdAt DESC")
    List<Lead> findRecentByAgentAndStatus(@Param("agentId") Long agentId, @Param("status") String status);

    long countByAgentId(Long agentId);

    long countByAgentIdAndStatusName(Long agentId, String statusName);
}
