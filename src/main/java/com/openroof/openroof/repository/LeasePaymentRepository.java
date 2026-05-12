package com.openroof.openroof.repository;

import com.openroof.openroof.model.rental.LeasePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeasePaymentRepository extends JpaRepository<LeasePayment, Long> {
    List<LeasePayment> findByInstallmentId(Long installmentId);

    List<LeasePayment> findByInstallmentIdIn(java.util.Collection<Long> installmentIds);
}
