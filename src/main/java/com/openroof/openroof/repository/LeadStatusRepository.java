package com.openroof.openroof.repository;

import com.openroof.openroof.model.lead.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeadStatusRepository extends JpaRepository<LeadStatus, Long> {

    Optional<LeadStatus> findByName(String name);

    Optional<LeadStatus> findFirstByActiveTrue();
}
