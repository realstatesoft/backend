package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.Visibility;
import com.openroof.openroof.model.property.Property;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    Page<Property> findByOwner_Id(Long ownerId, Pageable pageable);

    // filter properties in trashcan
    Page<Property> findAllByTrashedAtIsNull(Pageable pageable);

    Page<Property> findByOwner_IdAndTrashedAtIsNull(Long ownerId, Pageable pageable);

    Page<Property> findByStatusAndVisibility(PropertyStatus status, Visibility visibility, Pageable pageable);

    Page<Property> findByPropertyType(PropertyType propertyType, Pageable pageable);

    Page<Property> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<Property> findByLocation_Id(Long locationId, Pageable pageable);

    @Query("SELECT p FROM Property p LEFT JOIN p.location loc WHERE p.trashedAt IS NULL AND (" +
            "LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(loc.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(loc.city) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(loc.department) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Property> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    long countByStatus(PropertyStatus status);


    // TRASHCAN ─────────────────────────────────────────

    // get properties in trashcan of a given user
    Page<Property> findByOwnerIdAndTrashedAtIsNotNull(Long ownerId, Pageable pageable);

    // executed as a scheduled job
    // sets properties as 'deleted' (definitive)
    @Modifying
    @Query("""
    UPDATE Property p
    SET p.deletedAt = :now,
        p.trashedAt = null
    WHERE p.trashedAt IS NOT NULL
    AND p.trashedAt < :threshold
    """)
    void deleteExpiredTrash(@Param("threshold") LocalDateTime threshold, @Param("now") LocalDateTime now);

    // clear trashcan of a given user, returns deleted count
    @Modifying
    @Query("""
    UPDATE Property p
    SET p.deletedAt = :now
    WHERE p.owner.id = :ownerId
        AND p.trashedAt IS NOT NULL
        AND p.deletedAt IS NULL
    """)
    int clearTrashcanByOwner(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);
}
