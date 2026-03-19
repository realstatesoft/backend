package com.openroof.openroof.repository;

import com.openroof.openroof.model.interaction.VisitRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRequestRepository extends JpaRepository<VisitRequest, Long> {

    List<VisitRequest> findByBuyerId(Long buyerId);

    List<VisitRequest> findByAgentId(Long agentId);

    List<VisitRequest> findByPropertyId(Long propertyId);
}
