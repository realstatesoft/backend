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
import com.openroof.openroof.model.enums.InstallmentStatus;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.MaintenanceStatus;
import com.openroof.openroof.model.enums.PaymentStatus;
import com.openroof.openroof.model.maintenance.MaintenanceRequest;
import com.openroof.openroof.model.payment.Payment;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        // Buscar lease activo del tenant
        Optional<Lease> activeLeaseOpt = leaseRepository.findFirstByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(tenantId, LeaseStatus.ACTIVE);

        if (activeLeaseOpt.isEmpty()) {
            return buildInactiveResponse(messageRepository.countUnreadByUserId(tenantId));
        }

        Lease lease = activeLeaseOpt.get();
        LocalDate today = LocalDate.now();

        // Información del lease activo
        ActiveLeaseInfo leaseInfo = buildActiveLeaseInfo(lease, today);

        // Próxima cuota (installment pendiente más próxima)
        NextInstallmentInfo nextInstallment = buildNextInstallmentInfo(lease.getId(), today);

        // Balance pendiente total
        BigDecimal pendingBalance = rentalInstallmentRepository.sumPendingBalanceByLeaseId(
                lease.getId(),
                List.of(InstallmentStatus.PENDING, InstallmentStatus.PARTIAL, InstallmentStatus.OVERDUE));

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

        // 9. Listas de resumen (Top 2)
        List<NextInstallmentInfo> recentInstallments = rentalInstallmentRepository
                .findTop5ByLeaseIdOrderByDueDateDesc(lease.getId())
                .stream()
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
                .limit(2)
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
                leaseInfo,
                nextInstallment,
                pendingBalance,
                openTickets,
                unreadMessages,
                lastPayment,
                totalPaidLastYear,
                recentInstallments,
                recentMaintenance);
    }

    public TenantLeaseResponse getLease(String email) {
        User tenant = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Lease lease = leaseRepository.findFirstByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(tenant.getId(), LeaseStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No tenes un lease activo actualmente"));

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
                documents,
                lease.isSigned(),
                lease.getSignedByLandlordAt(),
                lease.getSignedByTenantAt(),
                lease.getCreatedAt(),
                lease.getUpdatedAt());
    }

    public TenantPaymentsResponse getPayments(String email, org.springframework.data.domain.Pageable pageable) {
        User tenant = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Lease lease = leaseRepository.findFirstByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(tenant.getId(), LeaseStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No tenes un lease activo actualmente"));

        var installmentsPage = rentalInstallmentRepository.findByLeaseIdOrderByDueDateDesc(lease.getId(), pageable);

        List<TenantInstallmentItem> items = installmentsPage.getContent().stream()
                .map(i -> {
                    List<TenantInstallmentItem.LeasePaymentInfo> payments = leasePaymentRepository.findByInstallmentId(i.getId())
                            .stream()
                            .map(p -> new TenantInstallmentItem.LeasePaymentInfo(
                                    p.getMethod().name(),
                                    p.getAmount(),
                                    p.getPaidAt(),
                                    p.getReceiptPdfUrl()))
                            .toList();

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

        // Summary header
        BigDecimal totalPaidYear = paymentRepository.sumCompletedByUserSince(tenant.getId(), PaymentStatus.COMPLETED, LocalDateTime.now().minusYears(1));
        if (totalPaidYear == null) totalPaidYear = BigDecimal.ZERO;

        long onTime = rentalInstallmentRepository.countByLeaseIdAndStatus(lease.getId(), InstallmentStatus.PAID);
        long late = rentalInstallmentRepository.countByLeaseIdAndStatus(lease.getId(), InstallmentStatus.OVERDUE);

        NextInstallmentInfo nextPayment = buildNextInstallmentInfo(lease.getId(), LocalDate.now());

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

    public TenantMaintenanceResponse getMaintenance(String email) {
        User tenant = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Lease lease = leaseRepository.findFirstByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(tenant.getId(), LeaseStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No tenes un lease activo actualmente"));

        List<MaintenanceRequest> requests = maintenanceRequestRepository.findByLeaseIdOrderByCreatedAtDesc(lease.getId());

        List<TenantMaintenanceTicketItem> items = requests.stream()
                .map(m -> {
                    TenantMaintenanceTicketItem.VendorContact vendorContact = null;
                    if (m.getAssignedVendor() != null) {
                        vendorContact = new TenantMaintenanceTicketItem.VendorContact(
                                m.getAssignedVendor().getCompanyName(),
                                m.getAssignedVendor().getPhone(),
                                m.getAssignedVendor().getEmail());
                    }

                    List<TenantMaintenanceTicketItem.StatusHistoryItem> history = new ArrayList<>();
                    history.add(new TenantMaintenanceTicketItem.StatusHistoryItem("SUBMITTED", m.getCreatedAt()));
                    if (m.getAcknowledgedAt() != null) {
                        history.add(new TenantMaintenanceTicketItem.StatusHistoryItem("ACKNOWLEDGED", m.getAcknowledgedAt()));
                    }
                    if (m.getResolvedAt() != null) {
                        history.add(new TenantMaintenanceTicketItem.StatusHistoryItem("RESOLVED", m.getResolvedAt()));
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

        Map<String, Long> countsByStatus = requests.stream()
                .collect(java.util.stream.Collectors.groupingBy(m -> m.getStatus().name(), java.util.stream.Collectors.counting()));

        return new TenantMaintenanceResponse(items, countsByStatus);
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
                null,
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
