package com.openroof.openroof.repository;

import com.openroof.openroof.model.property.PropertyMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para PropertyMedia.
 */
@Repository
public interface PropertyMediaRepository extends JpaRepository<PropertyMedia, Long> {

    List<PropertyMedia> findByPropertyIdOrderByOrderIndexAsc(Long propertyId);

    Optional<PropertyMedia> findByPropertyIdAndIsPrimaryTrue(Long propertyId);

    long countByPropertyId(Long propertyId);
}
