package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.LeaseSummaryResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.LeaseMapper;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.security.LeaseSecurity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LeaseService {

    private static final int SIGNATURE_TOKEN_VALIDITY_DAYS = 30;

    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final LeaseMapper leaseMapper;
    private final LeaseSecurity leaseSecurity;

    @Transactional
    public LeaseResponse createLease(Long landlordId, CreateLeaseRequest dto) {
        Property property = propertyRepository.findById(dto.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", dto.propertyId()));
        User landlord = property.getOwner();
        User tenant = userRepository.findById(dto.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.tenantId()));

        if (tenant.getId().equals(landlord.getId())) {
            throw new BadRequestException("Tenant cannot be the same as the property owner");
        }

        Lease lease = leaseMapper.toEntity(dto, property, tenant, landlord);
        lease = leaseRepository.save(lease);
        log.info("Lease id={} created by landlord id={}", lease.getId(), landlordId);
        return leaseMapper.toResponse(lease);
    }

    public LeaseResponse getLease(Long id, Long userId) {
        leaseSecurity.assertLeaseAccess(userId, id);
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", "id", id));
        return leaseMapper.toResponse(lease);
    }

    public Page<LeaseSummaryResponse> listLeases(Long userId, UserRole role,
                                                  LeaseStatus status, Long propertyId,
                                                  Pageable pageable) {
        Page<Lease> page;
        if (role == UserRole.ADMIN) {
            page = leaseRepository.findAllFiltered(status, propertyId, pageable);
        } else if (role == UserRole.AGENT) {
            page = leaseRepository.findByLandlordFiltered(userId, status, propertyId, pageable);
        } else {
            page = leaseRepository.findByTenantFiltered(userId, status, propertyId, pageable);
        }
        return page.map(leaseMapper::toSummaryResponse);
    }

    @Transactional
    public LeaseResponse terminateLease(Long leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", "id", leaseId));
        if (lease.getStatus() != LeaseStatus.ACTIVE
                && lease.getStatus() != LeaseStatus.EXPIRING_SOON) {
            throw new BadRequestException("Only ACTIVE or EXPIRING_SOON leases can be terminated");
        }
        lease.setStatus(LeaseStatus.TERMINATED);
        leaseRepository.save(lease);
        log.info("Lease id={} terminated", leaseId);
        return leaseMapper.toResponse(lease);
    }

    @Transactional
    public LeaseResponse updateLease(Long id, CreateLeaseRequest dto) {
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", "id", id));
        if (lease.getStatus() != LeaseStatus.DRAFT) {
            throw new BadRequestException("Lease can only be updated in DRAFT status");
        }
        leaseMapper.updateEntity(lease, dto);
        lease = leaseRepository.save(lease);
        log.info("Lease id={} updated", id);
        return leaseMapper.toResponse(lease);
    }

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
        List<RentalInstallment> installments = billingService.generateInstallments(lease);

        LocalDate firstDueDate = installments.isEmpty() ? null : installments.get(0).getDueDate();
        runAfterCommit(() -> notificationService.notifyLeaseActivated(lease, firstDueDate));

        return installments;
    }

    @Transactional
    public Lease sendForSignature(Long leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", "id", leaseId));

        if (lease.getStatus() != LeaseStatus.DRAFT) {
            throw new BadRequestException(
                    "Lease can only be sent for signature from DRAFT status");
        }

        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        lease.setSignatureTokenLandlord(UUID.randomUUID());
        lease.setSignatureTokenTenant(UUID.randomUUID());
        lease.setSignatureTokenExpiresAt(LocalDateTime.now().plusDays(SIGNATURE_TOKEN_VALIDITY_DAYS));
        Lease saved = leaseRepository.save(lease);

        log.info("Lease id={} sent for signature", saved.getId());
        runAfterCommit(() -> notificationService.notifyLeaseSentForSignature(saved));

        return saved;
    }

    @Transactional
    public Lease signByLandlord(Long leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", "id", leaseId));

        if (lease.getStatus() != LeaseStatus.PENDING_SIGNATURE) {
            throw new BadRequestException(
                    "Lease must be PENDING_SIGNATURE to be signed");
        }
        if (lease.getSignedByLandlordAt() != null) {
            throw new BadRequestException("Lease already signed by landlord");
        }

        lease.setSignedByLandlordAt(LocalDateTime.now());
        Lease saved = leaseRepository.save(lease);

        log.info("Lease id={} signed by landlord", saved.getId());
        runAfterCommit(() -> notificationService.notifyLeaseSigned(
                saved, NotificationService.SignerSide.LANDLORD));

        return saved;
    }

    @Transactional
    public Lease signByTenant(Long leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", "id", leaseId));

        if (lease.getStatus() != LeaseStatus.PENDING_SIGNATURE) {
            throw new BadRequestException(
                    "Lease must be PENDING_SIGNATURE to be signed");
        }
        if (lease.getSignedByTenantAt() != null) {
            throw new BadRequestException("Lease already signed by tenant");
        }

        lease.setSignedByTenantAt(LocalDateTime.now());
        Lease saved = leaseRepository.save(lease);

        log.info("Lease id={} signed by tenant", saved.getId());
        runAfterCommit(() -> notificationService.notifyLeaseSigned(
                saved, NotificationService.SignerSide.TENANT));

        return saved;
    }

    public Lease getById(Long leaseId) {
        return leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", "id", leaseId));
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeRun(action);
                }
            });
        } else {
            safeRun(action);
        }
    }

    private void safeRun(Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            log.error("Post-commit notification callback failed; transaction already committed", t);
        }
    }
}
