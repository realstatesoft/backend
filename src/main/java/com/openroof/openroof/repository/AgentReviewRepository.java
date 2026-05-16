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

    // — Métodos usados por el servicio (convención dev) —
    boolean existsByAgent_IdAndUser_Id(Long agentId, Long userId);

    Page<AgentReview> findByAgent_Id(Long agentId, Pageable pageable);

    List<AgentReview> findAllByAgent_Id(Long agentId);

    long countByAgent_Id(Long agentId);

    @Query("SELECT AVG(r.rating) FROM AgentReview r WHERE r.agent.id = :agentId")
    Double avgRatingByAgentId(@Param("agentId") Long agentId);

    // — Métodos adicionales —
    Optional<AgentReview> findByAgent_IdAndUser_Id(Long agentId, Long userId);

    @Query("SELECT r.rating AS rating, COUNT(r) AS count FROM AgentReview r WHERE r.agent.id = :agentId GROUP BY r.rating")
    List<RatingDistribution> countRatingDistributionByAgentId(@Param("agentId") Long agentId);

    List<AgentReview> findTop5ByAgent_IdOrderByCreatedAtDesc(Long agentId);
}
