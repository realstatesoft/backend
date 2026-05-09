package com.openroof.openroof.repository;

import com.openroof.openroof.model.enums.RentalApplicationStatus;
import com.openroof.openroof.model.rental.RentalApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalApplicationRepository extends JpaRepository<RentalApplication, Long> {

    List<RentalApplication> findByPropertyId(Long propertyId);

    List<RentalApplication> findByApplicantId(Long applicantId);

    boolean existsByPropertyIdAndApplicantIdAndStatusNot(
            Long propertyId,
            Long applicantId,
            RentalApplicationStatus status);
}
