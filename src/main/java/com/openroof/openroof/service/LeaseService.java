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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BillingService billingService;
    private final LeaseMapper leaseMapper;
    private final LeaseSecurity leaseSecurity;

    @Transactional
    public LeaseResponse createLease(Long landlordId, CreateLeaseRequest dto) {
        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", landlordId));
        User tenant = userRepository.findById(dto.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.tenantId()));
        Property property = propertyRepository.findById(dto.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", dto.propertyId()));

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
                                                  LeaseStatus status, Pageable pageable) {
        Page<Lease> page;
        if (role == UserRole.ADMIN) {
            page = status != null
                    ? leaseRepository.findByStatus(status, pageable)
                    : leaseRepository.findAll(pageable);
        } else if (role == UserRole.AGENT) {
            page = status != null
                    ? leaseRepository.findByLandlordIdAndStatus(userId, status, pageable)
                    : leaseRepository.findByLandlordId(userId, pageable);
        } else {
            page = status != null
                    ? leaseRepository.findByPrimaryTenantIdAndStatus(userId, status, pageable)
                    : leaseRepository.findByPrimaryTenantId(userId, pageable);
        }
        return page.map(leaseMapper::toSummaryResponse);
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
        return billingService.generateInstallments(lease);
    }

    public Lease getById(Long leaseId) {
        return leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", "id", leaseId));
    }
}
