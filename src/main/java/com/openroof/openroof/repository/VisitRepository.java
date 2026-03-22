package com.openroof.openroof.repository;

import com.openroof.openroof.model.interaction.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    List<Visit> findByBuyerId(Long buyerId);

    List<Visit> findByAgentId(Long agentId);

    List<Visit> findByPropertyId(Long propertyId);
}
