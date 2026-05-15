package com.openroof.openroof.repository;

import com.openroof.openroof.model.rental.RecurringCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecurringChargeRepository extends JpaRepository<RecurringCharge, Long> {

    List<RecurringCharge> findByLeaseIdAndIsActiveTrue(Long leaseId);
}
