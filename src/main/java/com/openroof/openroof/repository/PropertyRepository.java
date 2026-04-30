package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.Visibility;
import com.openroof.openroof.model.property.Property;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Property p WHERE p.id = :id")
    Optional<Property> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE Property p
            SET p.viewCount = COALESCE(p.viewCount, 0) + 1
            WHERE p.id = :id
            """)
    int incrementViewCount(@Param("id") Long id);

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

    @Query("SELECT COUNT(p) FROM Property p WHERE p.deletedAt IS NULL AND p.trashedAt IS NULL AND p.createdAt >= :start AND p.createdAt < :end")
    long countCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Property> findByDeletedAtIsNullAndTrashedAtIsNullAndStatusIn(List<PropertyStatus> statuses);

    @Query("SELECT p FROM Property p WHERE p.status = :status AND p.visibility = :visibility AND p.trashedAt IS NULL AND p.deletedAt IS NULL ORDER BY CASE WHEN p.highlighted = true AND (p.highlightedUntil IS NULL OR p.highlightedUntil > CURRENT_TIMESTAMP) THEN 1 ELSE 0 END DESC, p.createdAt DESC")
    Page<Property> findFeaturedOrRecentProperties(@Param("status") PropertyStatus status, @Param("visibility") Visibility visibility, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Property p WHERE p.deletedAt IS NULL AND p.trashedAt IS NULL AND p.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<PropertyStatus> statuses);

    long countByPropertyTypeAndStatus(PropertyType propertyType, PropertyStatus status);

    long countByOwner_Id(Long ownerId);

    @Query("SELECT COUNT(p) FROM Property p WHERE p.owner.id = :ownerId AND p.trashedAt IS NULL AND p.deletedAt IS NULL")
    long countActiveByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
            SELECT p FROM Property p
            WHERE LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY p.id DESC
            """)
    Page<Property> searchByTitleForAuditPicker(@Param("q") String q, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Property p WHERE p.agent.id = :agentId AND p.status = :status AND p.trashedAt IS NULL AND p.deletedAt IS NULL")
    long countByAgentIdAndStatus(@Param("agentId") Long agentId, @Param("status") PropertyStatus status);

    @Query("""
        SELECT p FROM Property p
        WHERE p.trashedAt IS NULL AND p.deletedAt IS NULL
          AND (p.agent.id = :agentId OR p.owner.id IN (SELECT ac.user.id FROM AgentClient ac WHERE ac.agent.id = :agentId))
        """)
    Page<Property> findByAgentScope(@Param("agentId") Long agentId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(p.price), 0) FROM Property p WHERE p.status IN :statuses AND p.trashedAt IS NULL AND p.deletedAt IS NULL")
    Double findAvgPriceByStatuses(@Param("statuses") List<PropertyStatus> statuses);

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

    // RECOMMENDATION ALGORITHM ---------------

    // search by coordinates
    // optimization: first select properties inside the bounding box to prevent
    // performing heavy calculations trigonometric on all properties

    // VALIDATE STATUS = "PUBLISHED" WHEN APPROVAL IS IMPLEMENTED
    @Query(value = """
    SELECT * FROM (
        SELECT p.*,
               (6371 * acos(cos(radians(:baseLat)) * cos(radians(p.lat)) * 
                cos(radians(p.lng) - radians(:baseLng)) + sin(radians(:baseLat)) * 
                sin(radians(p.lat)))) AS distance_km
        FROM properties p
        WHERE p.id != :propertyId
        AND p.visibility = 'PUBLIC'
        AND p.deleted_at IS NULL
        AND p.trashed_at IS NULL
        AND p.property_type = :propertyType
        AND p.price BETWEEN :minPrice AND :maxPrice
        AND p.lat IS NOT NULL
        AND p.lng IS NOT NULL
        -- filtering by lat and long (bounding box)
        AND p.lat BETWEEN :minLat AND :maxLat
        AND p.lng BETWEEN :minLng AND :maxLng
    ) AS subquery
    WHERE distance_km < :maxDistanceKm
    ORDER BY distance_km ASC, ABS(price - :basePrice) ASC
    LIMIT :limit
    """, nativeQuery = true)
    List<Property> findNearbyProperties(
            @Param("propertyId") Long propertyId,
            @Param("baseLat") Double baseLat,
            @Param("baseLng") Double baseLng,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            @Param("propertyType") String propertyType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("basePrice") BigDecimal basePrice,
            @Param("maxDistanceKm") Double maxDistanceKm,
            @Param("limit") int limit
    );

    // fallback when no coordinates

    // VALIDATE STATUS = "PUBLISHED" WHEN APPROVAL IS IMPLEMENTED
    @Query(value = """
        SELECT p.* FROM properties p
        JOIN locations l ON p.location_id = l.id
        WHERE p.id != :propertyId
        AND p.visibility = 'PUBLIC'
        AND p.deleted_at IS NULL
        AND p.trashed_at IS NULL      
        AND p.property_type = :propertyType
        AND p.price BETWEEN :minPrice AND :maxPrice
        AND l.city = :city
        ORDER BY ABS(p.price - :basePrice) ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Property> findByCity(
            @Param("propertyId") Long propertyId,
            @Param("city") String city,
            @Param("propertyType") String propertyType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("basePrice") BigDecimal basePrice,
            @Param("limit") int limit
    );

    // VALIDATE STATUS = "PUBLISHED" WHEN APPROVAL IS IMPLEMENTED
    @Query(value = """
        SELECT p.* FROM properties p
        WHERE p.id != :propertyId
        AND p.visibility = 'PUBLIC'
        AND p.deleted_at IS NULL
        AND p.trashed_at IS NULL
        AND p.property_type = :propertyType
        AND p.price BETWEEN :minPrice AND :maxPrice
        ORDER BY ABS(p.price - :basePrice) ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Property> findByPropertyTypeOnly(
            @Param("propertyId") Long propertyId,
            @Param("propertyType") String propertyType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("basePrice") BigDecimal basePrice,
            @Param("limit") int limit
    );
}
