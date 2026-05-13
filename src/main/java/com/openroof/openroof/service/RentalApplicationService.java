package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.CreateRentalApplicationRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.RentalApplicationResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.LeaseMapper;
import com.openroof.openroof.mapper.RentalApplicationMapper;
import com.openroof.openroof.model.enums.RentalApplicationStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.RentalApplicationRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.security.PropertySecurity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalApplicationService {

    private static final Set<RentalApplicationStatus> ACTIVE_STATUSES = EnumSet.of(
            RentalApplicationStatus.SUBMITTED,
            RentalApplicationStatus.UNDER_REVIEW,
            RentalApplicationStatus.SCREENING_IN_PROGRESS,
            RentalApplicationStatus.APPROVED
    );

    private static final Set<RentalApplicationStatus> DECIDABLE_STATUSES = EnumSet.of(
            RentalApplicationStatus.SUBMITTED,
            RentalApplicationStatus.UNDER_REVIEW,
            RentalApplicationStatus.SCREENING_IN_PROGRESS
    );

    private final RentalApplicationRepository applicationRepository;
    private final LeaseRepository leaseRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertySecurity propertySecurity;
    private final NotificationService notificationService;
    private final RentalApplicationMapper applicationMapper;
    private final LeaseMapper leaseMapper;

    @Transactional
    public RentalApplicationResponse submitApplication(CreateRentalApplicationRequest dto, String currentUserEmail) {
        User applicant = getUserByEmail(currentUserEmail);
        Property property = propertyRepository.findById(dto.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada: " + dto.propertyId()));

        if (property.getOwner() != null && property.getOwner().getId().equals(applicant.getId())) {
            throw new BadRequestException("No puedes aplicar a tu propia propiedad");
        }

        if (applicationRepository.existsByPropertyIdAndApplicantIdAndStatusIn(
                property.getId(), applicant.getId(), ACTIVE_STATUSES)) {
            throw new BadRequestException("Ya tienes una aplicación activa para esta propiedad");
        }

        RentalApplication application = applicationMapper.toEntity(dto, property, applicant);
        application.setIncomeToRentRatio(calculateIncomeToRentRatio(dto.monthlyIncome(), property.getRentAmount()));

        RentalApplication saved = applicationRepository.saveAndFlush(application);
        log.info("RentalApplication {} submitted by user {}", saved.getId(), applicant.getId());

        runAfterCommit(() -> notificationService.notifyApplicationSubmitted(saved));

        return applicationMapper.toResponse(saved);
    }

    public RentalApplicationResponse getApplication(Long id, String currentUserEmail) {
        RentalApplication application = getApplicationOrThrow(id);
        User currentUser = getUserByEmail(currentUserEmail);

        boolean isApplicant = application.getApplicant().getId().equals(currentUser.getId());
        boolean canManage = propertySecurity.canModify(application.getProperty().getId(), currentUser);
        if (!isApplicant && !canManage) {
            throw new ForbiddenException("No tienes permiso para ver esta aplicación");
        }
        return applicationMapper.toResponse(application);
    }

    public Page<RentalApplicationResponse> listApplications(
            Long propertyId,
            RentalApplicationStatus status,
            Pageable pageable,
            String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        if (!propertySecurity.canModify(propertyId, currentUser)) {
            throw new ForbiddenException("No tienes permiso para listar las aplicaciones de esta propiedad");
        }
        return applicationRepository.findByPropertyIdFiltered(propertyId, status, pageable)
                .map(applicationMapper::toResponse);
    }

    @Transactional
    public RentalApplicationResponse approveApplication(Long id, String currentUserEmail) {
        RentalApplication application = getApplicationOrThrow(id);
        User manager = getUserByEmail(currentUserEmail);

        if (!propertySecurity.canModify(application.getProperty().getId(), manager)) {
            throw new ForbiddenException("No tienes permiso para aprobar esta aplicación");
        }
        if (!DECIDABLE_STATUSES.contains(application.getStatus())) {
            throw new BadRequestException(
                    "La aplicación no puede ser aprobada en estado: " + application.getStatus());
        }

        application.setStatus(RentalApplicationStatus.APPROVED);
        application.setDecidedAt(LocalDateTime.now());
        RentalApplication saved = applicationRepository.saveAndFlush(application);

        runAfterCommit(() -> notificationService.notifyApplicationApproved(saved));

        log.info("RentalApplication {} approved by manager {}", id, manager.getId());
        return applicationMapper.toResponse(saved);
    }

    @Transactional
    public RentalApplicationResponse rejectApplication(Long id, String reason, String currentUserEmail) {
        RentalApplication application = getApplicationOrThrow(id);
        User manager = getUserByEmail(currentUserEmail);

        if (!propertySecurity.canModify(application.getProperty().getId(), manager)) {
            throw new ForbiddenException("No tienes permiso para rechazar esta aplicación");
        }
        if (!DECIDABLE_STATUSES.contains(application.getStatus())) {
            throw new BadRequestException(
                    "La aplicación no puede ser rechazada en estado: " + application.getStatus());
        }

        application.setStatus(RentalApplicationStatus.REJECTED);
        application.setRejectionReason(reason);
        application.setDecidedAt(LocalDateTime.now());
        RentalApplication saved = applicationRepository.saveAndFlush(application);

        runAfterCommit(() -> notificationService.notifyApplicationRejected(saved, reason));

        log.info("RentalApplication {} rejected by manager {}", id, manager.getId());
        return applicationMapper.toResponse(saved);
    }

    @Transactional
    public LeaseResponse convertToLease(Long id, CreateLeaseRequest leaseDto, String currentUserEmail) {
        RentalApplication application = getApplicationOrThrow(id);
        User manager = getUserByEmail(currentUserEmail);

        if (!propertySecurity.canModify(application.getProperty().getId(), manager)) {
            throw new ForbiddenException("No tienes permiso para convertir esta aplicación a contrato");
        }
        if (application.getStatus() != RentalApplicationStatus.APPROVED) {
            throw new BadRequestException("Solo se pueden convertir a contrato las aplicaciones aprobadas");
        }

        Property property = application.getProperty();
        User tenant = application.getApplicant();
        User landlord = property.getOwner();

        Lease lease = leaseMapper.toEntity(leaseDto, property, tenant, landlord);
        Lease savedLease = leaseRepository.saveAndFlush(lease);

        log.info("Lease {} created from RentalApplication {} by manager {}", savedLease.getId(), id, manager.getId());
        return leaseMapper.toResponse(savedLease);
    }

    private BigDecimal calculateIncomeToRentRatio(BigDecimal monthlyIncome, BigDecimal rentAmount) {
        if (monthlyIncome == null || rentAmount == null || rentAmount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return monthlyIncome.divide(rentAmount, 2, RoundingMode.HALF_UP);
    }

    private RentalApplication getApplicationOrThrow(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aplicación no encontrada: " + id));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
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
