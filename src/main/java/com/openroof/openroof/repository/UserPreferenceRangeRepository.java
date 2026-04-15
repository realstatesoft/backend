package com.openroof.openroof.repository;

import com.openroof.openroof.model.preference.UserPreferenceRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserPreferenceRangeRepository extends JpaRepository<UserPreferenceRange, Long> {

    List<UserPreferenceRange> findByUserPreferenceId(Long userPreferenceId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserPreferenceRange r WHERE r.userPreference.id = :userPreferenceId")
    void deleteByUserPreferenceId(@Param("userPreferenceId") Long userPreferenceId);
}
