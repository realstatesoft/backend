package com.openroof.openroof.service;

import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.repository.LeaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final BillingService billingService;

    @Transactional
    public List<RentalInstallment> activateLease(Long leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", "id", leaseId));

        if (!lease.isSigned()) {
            throw new BadRequestException(
                    "Lease must be signed by both parties before activation");
        }

        if (lease.getStatus() != LeaseStatus.PENDING_SIGNATURE) {
            throw new BadRequestException(
                    "Lease can only be activated from PENDING_SIGNATURE status");
        }

        lease.setStatus(LeaseStatus.ACTIVE);
        lease.setActivatedAt(LocalDateTime.now());
        leaseRepository.save(lease);

        log.info("Lease id={} activated, generating installments", lease.getId());
        return billingService.generateInstallments(lease);
    }

    public Lease getById(Long leaseId) {
        return leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", "id", leaseId));
    }
}
