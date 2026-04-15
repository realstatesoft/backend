package com.openroof.openroof.repository;

import com.openroof.openroof.model.preference.PreferenceOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreferenceOptionRepository extends JpaRepository<PreferenceOption, Long> {

    List<PreferenceOption> findByCategoryCode(String code);
}
