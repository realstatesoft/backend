package com.openroof.openroof.repository;

import com.openroof.openroof.model.preference.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    @Query("SELECT DISTINCT up FROM UserPreference up LEFT JOIN FETCH up.selectedOptions LEFT JOIN FETCH up.ranges WHERE up.user.id = :userId")
    Optional<UserPreference> findByUserId(@Param("userId") Long userId);
}
