package com.openroof.openroof.repository;

import com.openroof.openroof.model.property.ExteriorFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExteriorFeatureRepository extends JpaRepository<ExteriorFeature, Long> {
}
