package com.openroof.openroof.repository;

import com.openroof.openroof.model.agent.AgentSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentSpecialtyRepository extends JpaRepository<AgentSpecialty, Long> {
}
