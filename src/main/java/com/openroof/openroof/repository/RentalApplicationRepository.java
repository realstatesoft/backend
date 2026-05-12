package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.RentalApplicationStatus;
import com.openroof.openroof.model.rental.RentalApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RentalApplicationRepository extends JpaRepository<RentalApplication, Long> {

    List<RentalApplication> findByPropertyId(Long propertyId);

    List<RentalApplication> findByApplicantId(Long applicantId);

    boolean existsByPropertyIdAndApplicantIdAndStatusNot(
            Long propertyId,
            Long applicantId,
            RentalApplicationStatus status);

    boolean existsByPropertyIdAndApplicantIdAndStatusIn(
            Long propertyId,
            Long applicantId,
            Collection<RentalApplicationStatus> statuses);

    @Query("""
            SELECT a FROM RentalApplication a
            WHERE a.property.id = :propertyId
              AND (:status IS NULL OR a.status = :status)
            ORDER BY a.submittedAt DESC
            """)
    Page<RentalApplication> findByPropertyIdFiltered(
            @Param("propertyId") Long propertyId,
            @Param("status") RentalApplicationStatus status,
            Pageable pageable);
}
