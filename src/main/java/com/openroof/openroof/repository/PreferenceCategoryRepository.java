package com.openroof.openroof.repository;

import com.openroof.openroof.model.preference.PreferenceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PreferenceCategoryRepository extends JpaRepository<PreferenceCategory, Long> {

    Optional<PreferenceCategory> findByCode(String code);

    @Query("SELECT DISTINCT c FROM PreferenceCategory c LEFT JOIN FETCH c.options o ORDER BY c.id, o.displayOrder")
    List<PreferenceCategory> findAllWithOptions();
}
