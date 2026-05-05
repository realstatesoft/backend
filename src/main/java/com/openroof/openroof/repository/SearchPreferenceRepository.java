package com.openroof.openroof.repository;

import com.openroof.openroof.model.search.SearchPreference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SearchPreferenceRepository extends JpaRepository<SearchPreference, Long> {

    Page<SearchPreference> findByUserId(Long userId, Pageable pageable);

    Optional<SearchPreference> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    java.util.List<SearchPreference> findByNotificationsEnabledTrue();
}