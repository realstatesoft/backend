package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.Visibility;
import com.openroof.openroof.model.property.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    Page<Property> findByOwner_Id(Long ownerId, Pageable pageable);

    Page<Property> findByStatusAndVisibility(PropertyStatus status, Visibility visibility, Pageable pageable);

    Page<Property> findByPropertyType(PropertyType propertyType, Pageable pageable);

    Page<Property> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<Property> findByLocation_Id(Long locationId, Pageable pageable);

    @Query("SELECT p FROM Property p LEFT JOIN p.location loc WHERE " +
            "LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(loc.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(loc.city) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(loc.department) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Property> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    long countByStatus(PropertyStatus status);
}
