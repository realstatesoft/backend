package com.openroof.openroof.repository;

import com.openroof.openroof.model.property.InteriorFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InteriorFeatureRepository extends JpaRepository<InteriorFeature, Long> {
}
