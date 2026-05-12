package com.openroof.openroof.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.openroof.openroof.model.agent.AgentReview;

@Repository
public interface AgentReviewRepository extends JpaRepository<AgentReview, Long> {

    interface RatingDistribution {
        Integer getRating();
        Long getCount();
    }

    Page<AgentReview> findByAgentId(Long agentId, Pageable pageable);

    Optional<AgentReview> findByAgentIdAndUserId(Long agentId, Long userId);

    boolean existsByAgentIdAndUserId(Long agentId, Long userId);

    @Query("SELECT r.rating AS rating, COUNT(r) AS count FROM AgentReview r WHERE r.agent.id = :agentId GROUP BY r.rating")
    List<RatingDistribution> countRatingDistributionByAgentId(@Param("agentId") Long agentId);

    List<AgentReview> findTop5ByAgentIdOrderByCreatedAtDesc(Long agentId);
}
