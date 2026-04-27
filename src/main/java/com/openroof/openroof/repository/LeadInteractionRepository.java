package com.openroof.openroof.repository;

import com.openroof.openroof.model.lead.LeadInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadInteractionRepository extends JpaRepository<LeadInteraction, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"performedBy", "oldStatus", "newStatus"})
    List<LeadInteraction> findByLeadIdOrderByCreatedAtDesc(Long leadId);
}
