package com.openroof.openroof.service;

import com.openroof.openroof.dto.dashboard.TenantDashboardResponse;
import com.openroof.openroof.dto.dashboard.TenantDashboardResponse.*;
import com.openroof.openroof.dto.dashboard.TenantLeaseResponse;
import com.openroof.openroof.dto.dashboard.TenantLeaseResponse.*;
import com.openroof.openroof.dto.dashboard.TenantPaymentsResponse;
import com.openroof.openroof.dto.dashboard.TenantInstallmentItem;
import com.openroof.openroof.dto.dashboard.TenantMaintenanceResponse;
import com.openroof.openroof.dto.dashboard.TenantMaintenanceTicketItem;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.dto.dashboard.CreateMaintenanceRequest;
import com.openroof.openroof.dto.dashboard.RateMaintenanceRequest;
import com.openroof.openroof.model.enums.*;
import com.openroof.openroof.model.maintenance.MaintenanceRequest;
import com.openroof.openroof.model.payment.Payment;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantDashboardService {

    private final UserRepository userRepository;
    private final LeaseRepository leaseRepository;
    private final RentalInstallmentRepository rentalInstallmentRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final MessageRepository messageRepository;
    private final PaymentRepository paymentRepository;
    private final LeasePaymentRepository leasePaymentRepository;

    public TenantDashboardResponse getDashboard(String email) {
        User tenant = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Long tenantId = tenant.getId();

        // Buscar leases activos del tenant
        List<Lease> activeLeases = leaseRepository.findAllByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(tenantId, LeaseStatus.ACTIVE);

        if (activeLeases.isEmpty()) {
            return buildInactiveResponse(messageRepository.countUnreadByUserId(tenantId));
        }

        LocalDate today = LocalDate.now();

        // Información de los leases activos
        List<ActiveLeaseInfo> leaseInfos = activeLeases.stream()
                .map(lease -> buildActiveLeaseInfo(lease, today))
                .toList();

        // Próxima cuota (installment pendiente más próxima entre todos los leases)
        NextInstallmentInfo nextInstallment = activeLeases.stream()
                .map(lease -> buildNextInstallmentInfo(lease.getId(), today))
                .filter(Objects::nonNull)
                .min(Comparator.comparing(NextInstallmentInfo::dueDate))
                .orElse(null);

        // Balance pendiente total
        BigDecimal pendingBalance = activeLeases.stream()
                .map(lease -> rentalInstallmentRepository.sumPendingBalanceByLeaseId(
                        lease.getId(),
                        List.of(InstallmentStatus.PENDING, InstallmentStatus.PARTIAL, InstallmentStatus.OVERDUE)))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tickets de mantenimiento OPEN/IN_PROGRESS
        List<MaintenanceStatus> activeTicketStatuses = List.of(
                MaintenanceStatus.SUBMITTED,
                MaintenanceStatus.ACKNOWLEDGED,
                MaintenanceStatus.IN_PROGRESS);
        long openTickets = maintenanceRequestRepository.countByTenantIdAndStatusIn(tenantId, activeTicketStatuses);

        // Mensajes no leídos
        long unreadMessages = messageRepository.countUnreadByUserId(tenantId);

        // Último pago
        LastPaymentInfo lastPayment = buildLastPaymentInfo(tenantId);

        // 8. Total pagado en el último año
        BigDecimal totalPaidLastYear = paymentRepository.sumCompletedByUserSince(tenantId, PaymentStatus.COMPLETED, LocalDateTime.now().minusYears(1));
        if (totalPaidLastYear == null) totalPaidLastYear = BigDecimal.ZERO;

        // 9. Listas de resumen (Próximas cuotas a pagar)
        List<NextInstallmentInfo> recentInstallments = activeLeases.stream()
                .flatMap(lease -> rentalInstallmentRepository.findTop5ByLeaseIdOrderByDueDateAsc(lease.getId()).stream())
                .filter(i -> i.getStatus() != InstallmentStatus.PAID) // Solo mostrar los no pagados o pendientes en el dashboard
                .map(i -> {
                    long due = ChronoUnit.DAYS.between(today, i.getDueDate());
                    return new NextInstallmentInfo(
                        i.getId(),
                        i.getInstallmentNumber(),
                        i.getTotalAmount(),
                        i.getPaidAmount(),
                        i.getBalance(),
                        i.getDueDate(),
                        i.getStatus().name(),
                        due < 0 ? 0 : due);
                })
                .sorted(Comparator.comparing(NextInstallmentInfo::dueDate))
                .limit(4)
                .toList();

        List<MaintenanceTicketInfo> recentMaintenance = maintenanceRequestRepository
                .findTop5ByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(m -> new MaintenanceTicketInfo(
                        m.getId(),
                        m.getTitle(),
                        m.getCategory() != null ? m.getCategory().name() : null,
                        m.getStatus().name(),
                        m.getCreatedAt().toLocalDate()))
                .limit(2)
                .toList();

        return new TenantDashboardResponse(
                TenantStatus.ACTIVE,
                null,
                leaseInfos,
                nextInstallment,
                pendingBalance,
                openTickets,
                unreadMessages,
                lastPayment,
                totalPaidLastYear,
                recentInstallments,
                recentMaintenance);
    }

    public Page<TenantLeaseResponse> getLeases(String email, Pageable pageable) {
        User tenant = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Page<Lease> leases = leaseRepository.findAllByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(tenant.getId(), LeaseStatus.ACTIVE, pageable);
        return leases.map(this::buildTenantLeaseResponse);
    }

    public TenantLeaseResponse getLeaseById(String email, Long leaseId) {
        User tenant = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado"));

        if (lease.getPrimaryTenant() == null || !lease.getPrimaryTenant().getId().equals(tenant.getId())) {
            throw new ResourceNotFoundException("No tenes permiso para ver este contrato");
        }

        return buildTenantLeaseResponse(lease);
    }

    private TenantLeaseResponse buildTenantLeaseResponse(Lease lease) {
        LocalDate today = LocalDate.now();
        long daysRemaining = ChronoUnit.DAYS.between(today, lease.getEndDate());
        if (daysRemaining < 0) daysRemaining = 0;

        User landlord = lease.getLandlord();
        if (landlord == null) {
            throw new IllegalStateException("Landlord not found for leaseId=" + lease.getId());
        }
        if (lease.getProperty() == null) {
            throw new IllegalStateException("Property not found for leaseId=" + lease.getId());
        }

        LandlordContact landlordContact = new LandlordContact(
                landlord.getId(),
                landlord.getName(),
                landlord.getEmail(),
                landlord.getPhone(),
                landlord.getAvatarUrl());

        List<LeaseDocument> documents = buildLeaseDocuments(lease);

        return new TenantLeaseResponse(
                lease.getId(),
                null,
                lease.getProperty().getId(),
                lease.getProperty().getTitle(),
                lease.getProperty().getAddress(),
                landlordContact,
                lease.getStatus(),
                lease.getStartDate(),
                lease.getEndDate(),
                lease.getLeaseType(),
                lease.getMonthlyRent(),
                lease.getCurrency(),
                lease.getBillingFrequency(),
                lease.getDueDay(),
                lease.getGracePeriodDays(),
                lease.getLateFeeType(),
                lease.getLateFeeValue(),
                lease.getMaxLateFeeCap(),
                lease.getSecurityDeposit(),
                lease.getDepositStatus(),
                lease.getMoveInDate(),
                lease.getAutoRenew(),
                lease.getRenewalNoticeDays(),
                daysRemaining,
                lease.getProperty().getPetPolicy() != null ? lease.getProperty().getPetPolicy().name() : null,
                lease.getProperty().getUtilitiesIncluded() != null ? String.join(", ", lease.getProperty().getUtilitiesIncluded()) : null,
                null, // emergencyContact not yet in DB
                documents,
                lease.isSigned(),
                lease.getSignedByLandlordAt(),
                lease.getSignedByTenantAt(),
                lease.getCreatedAt(),
                lease.getUpdatedAt());
    }

    public TenantPaymentsResponse getPayments(String email, Pageable pageable) {
        User tenant = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<Lease> activeLeases = leaseRepository.findAllByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(tenant.getId(), LeaseStatus.ACTIVE);
        if (activeLeases.isEmpty()) {
            return new TenantPaymentsResponse(
                    BigDecimal.ZERO, 0, 0, null,
                    List.of(), 0, 0, 0);
        }

        List<Long> leaseIds = activeLeases.stream().map(Lease::getId).toList();

        Page<RentalInstallment> installmentsPage = rentalInstallmentRepository.findByLeaseIdsOrderByDueDateDesc(leaseIds, pageable);

        // Fix N+1: Fetch all payments for the installments in the current page in one go
        List<Long> installmentIds = installmentsPage.getContent().stream().map(RentalInstallment::getId).toList();
        Map<Long, List<TenantInstallmentItem.LeasePaymentInfo>> paymentsByInstallment = leasePaymentRepository.findByInstallmentIdIn(installmentIds)
                .stream()
                .collect(Collectors.groupingBy(
                        p -> p.getInstallment().getId(),
                        Collectors.mapping(p -> new TenantInstallmentItem.LeasePaymentInfo(
                                p.getId(),
                                p.getMethod().name(),
                                p.getAmount(),
                                p.getPaidAt(),
                                p.getReceiptPdfUrl()), Collectors.toList())
                ));

        List<TenantInstallmentItem> items = installmentsPage.getContent().stream()
                .map(i -> {
                    List<TenantInstallmentItem.LeasePaymentInfo> payments = paymentsByInstallment.getOrDefault(i.getId(), List.of());

                    return new TenantInstallmentItem(
                            i.getId(),
                            i.getInstallmentNumber(),
                            i.getPeriodStart() + " a " + i.getPeriodEnd(),
                            i.getTotalAmount(),
                            i.getPaidAmount(),
                            i.getBalance(),
                            i.getStatus().name(),
                            i.getDueDate(),
                            payments);
                })
                .toList();

        // Summary header - Corrected "on time" vs "late" logic (across all leases)
        BigDecimal totalPaidYear = paymentRepository.sumCompletedByUserSince(tenant.getId(), PaymentStatus.COMPLETED, LocalDateTime.now().minusYears(1));
        if (totalPaidYear == null) totalPaidYear = BigDecimal.ZERO;

        List<RentalInstallment> allInstallments = rentalInstallmentRepository.findByLeaseIdsOrderByDueDateAsc(leaseIds);

        long onTime = allInstallments.stream()
                .filter(i -> i.getStatus() == InstallmentStatus.PAID)
                .filter(i -> {
                    int graceDays = i.getLease() != null && i.getLease().getGracePeriodDays() != null
                            ? i.getLease().getGracePeriodDays() : 0;
                    return i.getPaidDate() != null && !i.getPaidDate().isAfter(i.getDueDate().plusDays(graceDays));
                })
                .count();

        long late = allInstallments.stream()
                .filter(i -> {
                    if (i.getStatus() == InstallmentStatus.OVERDUE) return true;
                    if (i.getStatus() != InstallmentStatus.PAID || i.getPaidDate() == null) return false;
                    int graceDays = i.getLease() != null && i.getLease().getGracePeriodDays() != null
                            ? i.getLease().getGracePeriodDays() : 0;
                    return i.getPaidDate().isAfter(i.getDueDate().plusDays(graceDays));
                })
                .count();

        NextInstallmentInfo nextPayment = activeLeases.stream()
                .map(lease -> buildNextInstallmentInfo(lease.getId(), LocalDate.now()))
                .filter(Objects::nonNull)
                .min(Comparator.comparing(NextInstallmentInfo::dueDate))
                .orElse(null);

        return new TenantPaymentsResponse(
                totalPaidYear,
                onTime,
                late,
                nextPayment,
                items,
                installmentsPage.getTotalElements(),
                installmentsPage.getTotalPages(),
                installmentsPage.getNumber());
    }

    public TenantMaintenanceResponse getMaintenance(String email, Pageable pageable) {
        User tenant = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<Lease> activeLeases = leaseRepository.findAllByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(tenant.getId(), LeaseStatus.ACTIVE);
        if (activeLeases.isEmpty()) {
            throw new ResourceNotFoundException("No tenes un lease activo actualmente");
        }

        Page<MaintenanceRequest> requestsPage = maintenanceRequestRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId(), pageable);

        List<TenantMaintenanceTicketItem> items = requestsPage.getContent().stream()
                .map(m -> {
                    TenantMaintenanceTicketItem.VendorContact vendorContact = null;
                    if (m.getAssignedVendor() != null) {
                        vendorContact = new TenantMaintenanceTicketItem.VendorContact(
                                m.getAssignedVendor().getCompanyName(),
                                m.getAssignedVendor().getPhone(),
                                m.getAssignedVendor().getEmail());
                    }

                    List<TenantMaintenanceTicketItem.StatusHistoryItem> history = new ArrayList<>();
                    history.add(new TenantMaintenanceTicketItem.StatusHistoryItem(MaintenanceStatus.SUBMITTED.name(), m.getCreatedAt()));
                    if (m.getAcknowledgedAt() != null) {
                        history.add(new TenantMaintenanceTicketItem.StatusHistoryItem(MaintenanceStatus.ACKNOWLEDGED.name(), m.getAcknowledgedAt()));
                    }
                    if (m.getInProgressAt() != null) {
                        history.add(new TenantMaintenanceTicketItem.StatusHistoryItem(MaintenanceStatus.IN_PROGRESS.name(), m.getInProgressAt()));
                    }
                    if (m.getOnHoldAt() != null) {
                        history.add(new TenantMaintenanceTicketItem.StatusHistoryItem(MaintenanceStatus.ON_HOLD.name(), m.getOnHoldAt()));
                    }
                    if (m.getResolvedAt() != null) {
                        history.add(new TenantMaintenanceTicketItem.StatusHistoryItem(MaintenanceStatus.COMPLETED.name(), m.getResolvedAt()));
                    }
                    if (m.getCancelledAt() != null) {
                        history.add(new TenantMaintenanceTicketItem.StatusHistoryItem(MaintenanceStatus.CANCELLED.name(), m.getCancelledAt()));
                    }
                    if (m.getClosedAt() != null) {
                        history.add(new TenantMaintenanceTicketItem.StatusHistoryItem("CLOSED", m.getClosedAt()));
                    }

                    return new TenantMaintenanceTicketItem(
                            m.getId(),
                            m.getTitle(),
                            m.getDescription(),
                            m.getCategory() != null ? m.getCategory().name() : null,
                            m.getPriority().name(),
                            m.getStatus().name(),
                            vendorContact,
                            history,
                            m.getTenantSatisfactionRating(),
                            m.getCreatedAt());
                })
                .toList();

        // Get counts by status from all tickets for this tenant (not just the page)
        List<MaintenanceRequest> allRequests = maintenanceRequestRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId(), Pageable.unpaged()).getContent();
        Map<String, Long> countsByStatus = allRequests.stream()
                .collect(Collectors.groupingBy(m -> m.getStatus().name(), Collectors.counting()));

        return new TenantMaintenanceResponse(items, countsByStatus);
    }

    @Transactional
    public TenantMaintenanceTicketItem createMaintenanceRequest(String email, CreateMaintenanceRequest request) {
        User tenant = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Lease lease = leaseRepository.findById(request.leaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado"));
                
        if (lease.getPrimaryTenant() == null || !lease.getPrimaryTenant().getId().equals(tenant.getId())) {
            throw new ResourceNotFoundException("No tenes permiso para este contrato o el contrato no te pertenece");
        }

        MaintenanceRequest mr = MaintenanceRequest.builder()
                .tenant(tenant)
                .lease(lease)
                .property(lease.getProperty())
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .priority(request.priority())
                .images(request.images())
                .permissionToEnter(request.permissionToEnter())
                .status(MaintenanceStatus.SUBMITTED)
                .build();

        mr = maintenanceRequestRepository.save(mr);

        List<TenantMaintenanceTicketItem.StatusHistoryItem> history = List.of(
                new TenantMaintenanceTicketItem.StatusHistoryItem(MaintenanceStatus.SUBMITTED.name(), mr.getCreatedAt()));

        return new TenantMaintenanceTicketItem(
                mr.getId(),
                mr.getTitle(),
                mr.getDescription(),
                mr.getCategory() != null ? mr.getCategory().name() : null,
                mr.getPriority().name(),
                mr.getStatus().name(),
                null,
                history,
                null,
                mr.getCreatedAt());
    }

    @Transactional
    public void rateMaintenanceRequest(String email, Long id, RateMaintenanceRequest request) {
        User tenant = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        MaintenanceRequest mr = maintenanceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        if (!mr.getTenant().getId().equals(tenant.getId())) {
            throw new ResourceNotFoundException("No tenes permiso para calificar esta solicitud");
        }

        mr.setTenantSatisfactionRating(request.rating());
        maintenanceRequestRepository.save(mr);
    }


    private List<LeaseDocument> buildLeaseDocuments(Lease lease) {
        List<LeaseDocument> docs = new ArrayList<>();

        if (lease.getDocumentUrl() != null && !lease.getDocumentUrl().isBlank()) {
            docs.add(new LeaseDocument(
                    "LEASE_AGREEMENT",
                    "Contrato de Arriendo",
                    lease.getDocumentUrl(),
                    lease.isSigned()));
        }

        if (lease.getSignatureAuditPdfUrl() != null) {
            docs.add(new LeaseDocument(
                    "SIGNATURE_AUDIT",
                    "Acta de Firmas Digitales",
                    lease.getSignatureAuditPdfUrl(),
                    true));
        }

        return docs;
    }

    private TenantDashboardResponse buildInactiveResponse(long unreadMessages) {
        return new TenantDashboardResponse(
                TenantStatus.INACTIVE,
                "No tienes un arriendo activo en este momento. Explora nuestras propiedades disponibles para encontrar tu próximo hogar.",
                List.of(),
                null,
                BigDecimal.ZERO,
                0,
                unreadMessages,
                null,
                BigDecimal.ZERO,
                List.of(),
                List.of());
    }

    private ActiveLeaseInfo buildActiveLeaseInfo(Lease lease, LocalDate today) {
        long daysRemaining = ChronoUnit.DAYS.between(today, lease.getEndDate());
        if (daysRemaining < 0) daysRemaining = 0;

        User landlord = lease.getLandlord();
        return new ActiveLeaseInfo(
                lease.getId(),
                lease.getProperty().getId(),
                lease.getProperty().getTitle(),
                lease.getProperty().getAddress(),
                landlord.getName(),
                landlord.getEmail(),
                landlord.getPhone(),
                lease.getStartDate(),
                lease.getEndDate(),
                daysRemaining,
                lease.getMonthlyRent(),
                lease.getCurrency());
    }

    private NextInstallmentInfo buildNextInstallmentInfo(Long leaseId, LocalDate today) {
        Optional<RentalInstallment> nextOpt = rentalInstallmentRepository
                .findFirstByLeaseIdAndStatusInOrderByDueDateAsc(
                        leaseId, 
                        List.of(InstallmentStatus.PENDING, InstallmentStatus.PARTIAL, InstallmentStatus.OVERDUE)
                );

        if (nextOpt.isEmpty()) return null;

        RentalInstallment next = nextOpt.get();
        long daysUntilDue = ChronoUnit.DAYS.between(today, next.getDueDate());
        if (daysUntilDue < 0) daysUntilDue = 0;

        return new NextInstallmentInfo(
                next.getId(),
                next.getInstallmentNumber(),
                next.getTotalAmount(),
                next.getPaidAmount(),
                next.getBalance(),
                next.getDueDate(),
                next.getStatus().name(),
                daysUntilDue);
    }

    private LastPaymentInfo buildLastPaymentInfo(Long tenantId) {
        var pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Payment> payments = paymentRepository.findByUser_IdAndStatus(tenantId, PaymentStatus.APPROVED, pageable)
                .getContent();

        if (payments.isEmpty()) return null;

        Payment last = payments.get(0);
        return new LastPaymentInfo(
                last.getId(),
                last.getAmount(),
                last.getConcept(),
                last.getCreatedAt(),
                last.getTransactionCode());
    }
}
