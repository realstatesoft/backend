package com.openroof.openroof.repository;

import com.openroof.openroof.model.search.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    Page<Alert> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Alert> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    boolean existsByUserIdAndPropertyIdAndSearchPreferenceId(Long userId, Long propertyId, Long searchPreferenceId);
}
