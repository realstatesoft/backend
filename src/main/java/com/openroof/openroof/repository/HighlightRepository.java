package com.openroof.openroof.repository;

import com.openroof.openroof.model.property.Highlight;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface HighlightRepository extends JpaRepository<Highlight, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Highlight> findFirstByProperty_IdAndHighlightedUntilAfterOrderByHighlightedUntilDesc(
            Long propertyId, LocalDateTime now);

    @Modifying
    @Query(value = "UPDATE highlights SET deleted_at = :now, version = version + 1 WHERE highlighted_until < :now AND deleted_at IS NULL", nativeQuery = true)
    void deactivateExpired(@Param("now") LocalDateTime now);
}
