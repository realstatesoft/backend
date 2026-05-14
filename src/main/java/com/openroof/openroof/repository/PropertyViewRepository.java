package com.openroof.openroof.repository;

import com.openroof.openroof.model.property.PropertyView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyViewRepository extends JpaRepository<PropertyView, Long> {

    /**
     * Deletes property views for the given user and property.
     * <p>Must be invoked from within a {@code @Transactional} context.</p>
     */
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM PropertyView pv WHERE pv.user.id = :userId AND pv.property.id = :propertyId")
    void deleteByUserIdAndPropertyId(@Param("userId") Long userId, @Param("propertyId") Long propertyId);

    long countByProperty_Id(Long propertyId);

    Optional<PropertyView> findFirstByUser_IdAndProperty_IdOrderByCreatedAtDesc(Long userId, Long propertyId);

    @Query("""
            SELECT pv
            FROM PropertyView pv
            JOIN FETCH pv.property p
            WHERE pv.user.id = :userId
            ORDER BY pv.createdAt DESC
            """)
    List<PropertyView> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(pv) FROM PropertyView pv WHERE pv.property.owner.id = :ownerId")
    long countByPropertyOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(pv) FROM PropertyView pv WHERE pv.property.owner.id = :ownerId " +
           "AND pv.createdAt >= :since")
    long countByPropertyOwnerIdSince(
            @Param("ownerId") Long ownerId,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(pv) FROM PropertyView pv WHERE pv.property.agent.id = :agentId")
    long countByPropertyAgentId(@Param("agentId") Long agentId);
}
