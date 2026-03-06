package com.openroof.openroof.repository;

import com.openroof.openroof.model.agent.AgentProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentProfileRepository extends JpaRepository<AgentProfile, Long> {

    Optional<AgentProfile> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    @Query("SELECT a FROM AgentProfile a JOIN a.user u " +
           "WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.licenseNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<AgentProfile> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
