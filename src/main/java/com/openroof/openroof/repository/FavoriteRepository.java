package com.openroof.openroof.repository;

import com.openroof.openroof.model.interaction.Favorite;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.property.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUser_IdAndProperty_Id(Long userId, Long propertyId);

    Optional<Favorite> findByUser_IdAndProperty_Id(Long userId, Long propertyId);

    @Modifying
    @Query("""
        DELETE FROM Favorite f
        WHERE f.user.id = :userId
          AND f.property.id = :propertyId
    """)
    int deleteByUserAndProperty(@Param("userId") Long userId, @Param("propertyId") Long propertyId);

    @Query("""
        SELECT f.property
        FROM Favorite f
        WHERE f.user.id = :userId
          AND f.property.trashedAt IS NULL
          AND (:status IS NULL OR f.property.status = :status)
          AND f.createdAt >= :addedFrom
          AND f.createdAt < :addedToExclusive
        ORDER BY f.createdAt DESC
    """)
    Page<Property> findFavoritePropertiesByUserId(
            @Param("userId") Long userId,
            @Param("status") PropertyStatus status,
            @Param("addedFrom") LocalDateTime addedFrom,
            @Param("addedToExclusive") LocalDateTime addedToExclusive,
            Pageable pageable);
}
