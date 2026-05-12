package com.openroof.openroof.repository;

import com.openroof.openroof.model.screening.TenantScreening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantScreeningRepository extends JpaRepository<TenantScreening, Long> {

    Optional<TenantScreening> findByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);
}
