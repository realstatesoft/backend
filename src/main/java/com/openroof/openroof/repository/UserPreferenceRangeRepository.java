package com.openroof.openroof.repository;

import com.openroof.openroof.model.preference.UserPreferenceRange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPreferenceRangeRepository extends JpaRepository<UserPreferenceRange, Long> {

    List<UserPreferenceRange> findByUserPreferenceId(Long userPreferenceId);
}
