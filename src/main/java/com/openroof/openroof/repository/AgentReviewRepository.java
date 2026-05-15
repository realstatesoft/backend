package com.openroof.openroof.repository;

import com.openroof.openroof.model.agent.AgentReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentReviewRepository extends JpaRepository<AgentReview, Long> {

    boolean existsByAgent_IdAndUser_Id(Long agentId, Long userId);

    Page<AgentReview> findByAgent_Id(Long agentId, Pageable pageable);

    List<AgentReview> findAllByAgent_Id(Long agentId);

    long countByAgent_Id(Long agentId);

    @Query("SELECT AVG(r.rating) FROM AgentReview r WHERE r.agent.id = :agentId")
    Double avgRatingByAgentId(Long agentId);
}
