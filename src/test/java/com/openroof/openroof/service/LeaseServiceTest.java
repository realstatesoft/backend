package com.openroof.openroof.service;

import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaseService — transitions + notifications")
class LeaseServiceTest {

    @Mock private LeaseRepository leaseRepository;
    @Mock private BillingService billingService;
    @Mock private NotificationService notificationService;

    private LeaseService service;

    private Lease lease;
    private User landlord;
    private User tenant;
    private Property property;

    @BeforeEach
    void setUp() {
        service = new LeaseService(leaseRepository, billingService, notificationService);

        landlord = User.builder().name("Landlord").email("landlord@test.com").build();
        landlord.setId(1L);

        tenant = User.builder().name("Tenant").email("tenant@test.com").build();
        tenant.setId(2L);

        property = Property.builder().title("Depto Centro").owner(landlord).build();
        property.setId(10L);

        lease = Lease.builder()
                .property(property)
                .landlord(landlord)
                .primaryTenant(tenant)
                .status(LeaseStatus.DRAFT)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2027, 6, 1))
                .monthlyRent(new BigDecimal("1000.00"))
                .build();
        lease.setId(100L);
    }

    // ─── sendForSignature ─────────────────────────────────────────────────────

    @Test
    void sendForSignature_fromDraft_setsStatusAndTokens() {
        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));
        when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

        Lease result = service.sendForSignature(100L);

        assertThat(result.getStatus()).isEqualTo(LeaseStatus.PENDING_SIGNATURE);
        assertThat(result.getSignatureTokenLandlord()).isNotNull();
        assertThat(result.getSignatureTokenTenant()).isNotNull();
        assertThat(result.getSignatureTokenExpiresAt()).isAfter(LocalDateTime.now());
        verify(notificationService).notifyLeaseSentForSignature(result);
    }

    @Test
    void sendForSignature_wrongStatus_throwsBadRequest() {
        lease.setStatus(LeaseStatus.ACTIVE);
        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));

        assertThatThrownBy(() -> service.sendForSignature(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DRAFT");
        verify(notificationService, never()).notifyLeaseSentForSignature(any());
    }

    @Test
    void sendForSignature_notFound_throws() {
        when(leaseRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.sendForSignature(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── signByLandlord ───────────────────────────────────────────────────────

    @Test
    void signByLandlord_pendingSignature_setsTimestampAndNotifies() {
        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));
        when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

        Lease result = service.signByLandlord(100L);

        assertThat(result.getSignedByLandlordAt()).isNotNull();
        verify(notificationService).notifyLeaseSigned(result, NotificationService.SignerSide.LANDLORD);
    }

    @Test
    void signByLandlord_alreadySigned_throwsBadRequest() {
        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        lease.setSignedByLandlordAt(LocalDateTime.now());
        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));

        assertThatThrownBy(() -> service.signByLandlord(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already signed");
        verify(notificationService, never()).notifyLeaseSigned(any(), any());
    }

    @Test
    void signByLandlord_wrongStatus_throwsBadRequest() {
        lease.setStatus(LeaseStatus.DRAFT);
        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));

        assertThatThrownBy(() -> service.signByLandlord(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING_SIGNATURE");
    }

    // ─── signByTenant ─────────────────────────────────────────────────────────

    @Test
    void signByTenant_pendingSignature_setsTimestampAndNotifies() {
        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));
        when(leaseRepository.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

        Lease result = service.signByTenant(100L);

        assertThat(result.getSignedByTenantAt()).isNotNull();
        verify(notificationService).notifyLeaseSigned(result, NotificationService.SignerSide.TENANT);
    }

    @Test
    void signByTenant_alreadySigned_throwsBadRequest() {
        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        lease.setSignedByTenantAt(LocalDateTime.now());
        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));

        assertThatThrownBy(() -> service.signByTenant(100L))
                .isInstanceOf(BadRequestException.class);
    }

    // ─── activateLease ────────────────────────────────────────────────────────

    @Test
    void activateLease_fullySigned_activatesAndNotifies() {
        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        lease.setSignedByLandlordAt(LocalDateTime.now());
        lease.setSignedByTenantAt(LocalDateTime.now());

        RentalInstallment first = new RentalInstallment();
        first.setDueDate(LocalDate.of(2026, 7, 1));
        List<RentalInstallment> installments = List.of(first);

        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));
        when(billingService.generateInstallments(lease)).thenReturn(installments);

        List<RentalInstallment> result = service.activateLease(100L);

        assertThat(result).hasSize(1);
        assertThat(lease.getStatus()).isEqualTo(LeaseStatus.ACTIVE);
        assertThat(lease.getActivatedAt()).isNotNull();
        verify(notificationService).notifyLeaseActivated(lease, LocalDate.of(2026, 7, 1));
    }

    @Test
    void activateLease_emptyInstallments_passesNullFirstDueDate() {
        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        lease.setSignedByLandlordAt(LocalDateTime.now());
        lease.setSignedByTenantAt(LocalDateTime.now());

        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));
        when(billingService.generateInstallments(lease)).thenReturn(List.of());

        service.activateLease(100L);

        verify(notificationService).notifyLeaseActivated(lease, null);
    }

    @Test
    void activateLease_notSigned_throwsBadRequest_noNotify() {
        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));

        assertThatThrownBy(() -> service.activateLease(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("signed by both parties");
        verify(notificationService, never()).notifyLeaseActivated(any(), any());
    }

    @Test
    void activateLease_wrongStatus_throwsBadRequest() {
        lease.setStatus(LeaseStatus.ACTIVE);
        lease.setSignedByLandlordAt(LocalDateTime.now());
        lease.setSignedByTenantAt(LocalDateTime.now());
        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));

        assertThatThrownBy(() -> service.activateLease(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING_SIGNATURE");
    }
}
