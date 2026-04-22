package com.openroof.openroof.repository;

import com.openroof.openroof.model.interaction.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    List<Visit> findByBuyerId(Long buyerId);

    List<Visit> findByAgentId(Long agentId);

    List<Visit> findByPropertyId(Long propertyId);

    /**
     * Finds a visit if the user has access to it (either as buyer or assigned agent).
     */
    @Query("SELECT v FROM Visit v WHERE v.id = :id AND (v.buyer.id = :userId OR (v.agent IS NOT NULL AND v.agent.user.id = :userId))")
    Optional<Visit> findByIdAndUserAccess(@Param("id") Long id, @Param("userId") Long userId);
}
