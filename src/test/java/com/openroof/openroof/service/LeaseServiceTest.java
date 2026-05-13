package com.openroof.openroof.service;

<<<<<<< HEAD
import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.LeaseSummaryResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.LeaseMapper;
import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.DepositStatus;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.enums.UserRole;
=======
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.LeaseStatus;
>>>>>>> dev
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
<<<<<<< HEAD
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.security.LeaseSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
=======
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
>>>>>>> dev
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
<<<<<<< HEAD
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
=======
>>>>>>> dev

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
<<<<<<< HEAD
import static org.mockito.Mockito.doThrow;
=======
>>>>>>> dev
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
<<<<<<< HEAD
class LeaseServiceTest {

    @Mock private LeaseRepository leaseRepository;
    @Mock private UserRepository userRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private BillingService billingService;
    @Mock private LeaseMapper leaseMapper;
    @Mock private LeaseSecurity leaseSecurity;

    private LeaseService service;

    private User landlord;
    private User tenant;
    private Property property;
    private Lease lease;

    @BeforeEach
    void setUp() {
        service = new LeaseService(
                leaseRepository, userRepository, propertyRepository,
                billingService, leaseMapper, leaseSecurity);

        landlord = User.builder().email("landlord@test.com").role(UserRole.AGENT).build();
        landlord.setId(1L);

        tenant = User.builder().email("tenant@test.com").role(UserRole.USER).build();
        tenant.setId(2L);

        property = Property.builder().title("Casa Test").owner(landlord).build();
        property.setId(100L);
=======
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
>>>>>>> dev

        lease = Lease.builder()
                .property(property)
                .landlord(landlord)
                .primaryTenant(tenant)
<<<<<<< HEAD
                .leaseType(LeaseType.FIXED_TERM)
                .status(LeaseStatus.DRAFT)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusMonths(12))
                .monthlyRent(new BigDecimal("1000.00"))
                .securityDeposit(new BigDecimal("2000.00"))
                .billingFrequency(BillingFrequency.MONTHLY)
                .build();
        lease.setId(10L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createLease()")
    class CreateLease {

        private final CreateLeaseRequest dto = new CreateLeaseRequest(
                100L, 2L, LeaseType.FIXED_TERM,
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(12),
                new BigDecimal("1000.00"), new BigDecimal("2000.00"),
                BillingFrequency.MONTHLY, null, null, null);

        @Test
        @DisplayName("Crea y persiste el lease con status DRAFT")
        void createsDraftLease() {
            LeaseResponse expectedResponse = sampleLeaseResponse(10L, LeaseStatus.DRAFT);
            when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
            when(userRepository.findById(2L)).thenReturn(Optional.of(tenant));
            when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));
            when(leaseMapper.toEntity(dto, property, tenant, landlord)).thenReturn(lease);
            when(leaseRepository.save(lease)).thenReturn(lease);
            when(leaseMapper.toResponse(lease)).thenReturn(expectedResponse);

            LeaseResponse result = service.createLease(1L, dto);

            assertThat(result.status()).isEqualTo(LeaseStatus.DRAFT);
            verify(leaseRepository).save(lease);
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el landlord no existe")
        void landlordNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createLease(1L, dto))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(leaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el tenant no existe")
        void tenantNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createLease(1L, dto))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(leaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si la propiedad no existe")
        void propertyNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
            when(userRepository.findById(2L)).thenReturn(Optional.of(tenant));
            when(propertyRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createLease(1L, dto))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(leaseRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLease()")
    class GetLease {

        @Test
        @DisplayName("Delega la verificación a leaseSecurity y retorna el lease mapeado")
        void returnsLeaseForAuthorizedUser() {
            when(leaseRepository.findById(10L)).thenReturn(Optional.of(lease));
            when(leaseMapper.toResponse(lease)).thenReturn(sampleLeaseResponse(10L, LeaseStatus.DRAFT));

            LeaseResponse result = service.getLease(10L, 1L);

            assertThat(result.id()).isEqualTo(10L);
            verify(leaseSecurity).assertLeaseAccess(1L, 10L);
        }

        @Test
        @DisplayName("Propaga AccessDeniedException de leaseSecurity")
        void propagatesAccessDenied() {
            doThrow(new AccessDeniedException("sin permiso"))
                    .when(leaseSecurity).assertLeaseAccess(99L, 10L);

            assertThatThrownBy(() -> service.getLease(10L, 99L))
                    .isInstanceOf(AccessDeniedException.class);
            verify(leaseRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el lease no existe")
        void leaseNotFound() {
            when(leaseRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLease(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listLeases
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listLeases()")
    class ListLeases {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("ADMIN usa findAllFiltered sin restricción de usuario")
        void adminUsesGlobalQuery() {
            when(leaseRepository.findAllFiltered(null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of(lease)));
            when(leaseMapper.toSummaryResponse(lease)).thenReturn(sampleSummary());

            var page = service.listLeases(1L, UserRole.ADMIN, null, null, pageable);

            assertThat(page.getTotalElements()).isEqualTo(1);
            verify(leaseRepository).findAllFiltered(null, null, pageable);
        }

        @Test
        @DisplayName("AGENT usa findByLandlordFiltered con su userId")
        void agentUsesLandlordQuery() {
            when(leaseRepository.findByLandlordFiltered(1L, LeaseStatus.ACTIVE, null, pageable))
                    .thenReturn(new PageImpl<>(List.of(lease)));
            when(leaseMapper.toSummaryResponse(lease)).thenReturn(sampleSummary());

            var page = service.listLeases(1L, UserRole.AGENT, LeaseStatus.ACTIVE, null, pageable);

            assertThat(page.getContent()).hasSize(1);
            verify(leaseRepository).findByLandlordFiltered(1L, LeaseStatus.ACTIVE, null, pageable);
        }

        @Test
        @DisplayName("USER usa findByTenantFiltered con su userId y filtro de propertyId")
        void userUsesTenantQueryWithPropertyFilter() {
            when(leaseRepository.findByTenantFiltered(2L, null, 100L, pageable))
                    .thenReturn(new PageImpl<>(List.of(lease)));
            when(leaseMapper.toSummaryResponse(lease)).thenReturn(sampleSummary());

            var page = service.listLeases(2L, UserRole.USER, null, 100L, pageable);

            assertThat(page.getContent()).hasSize(1);
            verify(leaseRepository).findByTenantFiltered(2L, null, 100L, pageable);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateLease()")
    class UpdateLease {

        private final CreateLeaseRequest dto = new CreateLeaseRequest(
                100L, 2L, LeaseType.MONTH_TO_MONTH,
                LocalDate.now().plusDays(1), null,
                new BigDecimal("1200.00"), new BigDecimal("2400.00"),
                BillingFrequency.MONTHLY, null, null, null);

        @Test
        @DisplayName("Actualiza el lease en estado DRAFT")
        void updatesDraftLease() {
            when(leaseRepository.findById(10L)).thenReturn(Optional.of(lease));
            when(leaseRepository.save(lease)).thenReturn(lease);
            when(leaseMapper.toResponse(lease)).thenReturn(sampleLeaseResponse(10L, LeaseStatus.DRAFT));

            LeaseResponse result = service.updateLease(10L, dto);

            assertThat(result).isNotNull();
            verify(leaseMapper).updateEntity(lease, dto);
            verify(leaseRepository).save(lease);
        }

        @Test
        @DisplayName("Lanza BadRequestException si el lease no está en DRAFT")
        void rejectsNonDraftLease() {
            lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
            when(leaseRepository.findById(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.updateLease(10L, dto))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("DRAFT");
            verify(leaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el lease no existe")
        void leaseNotFound() {
            when(leaseRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateLease(99L, dto))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // activateLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("activateLease()")
    class ActivateLease {

        @BeforeEach
        void signLease() {
            lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
            lease.setSignedByLandlordAt(LocalDateTime.now());
            lease.setSignedByTenantAt(LocalDateTime.now());
        }

        @Test
        @DisplayName("Activa el lease, lo persiste y genera installments")
        void activatesAndGeneratesInstallments() {
            List<RentalInstallment> installments = List.of(new RentalInstallment());
            when(leaseRepository.findById(10L)).thenReturn(Optional.of(lease));
            when(leaseRepository.save(lease)).thenReturn(lease);
            when(billingService.generateInstallments(lease)).thenReturn(installments);

            List<RentalInstallment> result = service.activateLease(10L);

            assertThat(lease.getStatus()).isEqualTo(LeaseStatus.ACTIVE);
            assertThat(lease.getActivatedAt()).isNotNull();
            assertThat(result).hasSize(1);
            verify(billingService).generateInstallments(lease);
        }

        @Test
        @DisplayName("Lanza BadRequestException si solo firmó el landlord")
        void rejectsIfTenantDidNotSign() {
            lease.setSignedByTenantAt(null);
            when(leaseRepository.findById(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.activateLease(10L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("signed");
            verify(billingService, never()).generateInstallments(any());
        }

        @Test
        @DisplayName("Lanza BadRequestException si solo firmó el tenant")
        void rejectsIfLandlordDidNotSign() {
            lease.setSignedByLandlordAt(null);
            when(leaseRepository.findById(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.activateLease(10L))
                    .isInstanceOf(BadRequestException.class);
            verify(billingService, never()).generateInstallments(any());
        }

        @Test
        @DisplayName("Lanza BadRequestException si el lease no está en PENDING_SIGNATURE")
        void rejectsIfNotPendingSignature() {
            lease.setStatus(LeaseStatus.DRAFT);
            when(leaseRepository.findById(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.activateLease(10L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("PENDING_SIGNATURE");
            verify(billingService, never()).generateInstallments(any());
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el lease no existe")
        void leaseNotFound() {
            when(leaseRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activateLease(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // terminateLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("terminateLease()")
    class TerminateLease {

        @Test
        @DisplayName("Termina un lease ACTIVE")
        void terminatesActiveLease() {
            lease.setStatus(LeaseStatus.ACTIVE);
            when(leaseRepository.findById(10L)).thenReturn(Optional.of(lease));
            when(leaseRepository.save(lease)).thenReturn(lease);
            when(leaseMapper.toResponse(lease)).thenReturn(sampleLeaseResponse(10L, LeaseStatus.TERMINATED));

            LeaseResponse result = service.terminateLease(10L);

            assertThat(lease.getStatus()).isEqualTo(LeaseStatus.TERMINATED);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Termina un lease en estado EXPIRING_SOON")
        void terminatesExpiringSoonLease() {
            lease.setStatus(LeaseStatus.EXPIRING_SOON);
            when(leaseRepository.findById(10L)).thenReturn(Optional.of(lease));
            when(leaseRepository.save(lease)).thenReturn(lease);
            when(leaseMapper.toResponse(lease)).thenReturn(sampleLeaseResponse(10L, LeaseStatus.TERMINATED));

            service.terminateLease(10L);

            assertThat(lease.getStatus()).isEqualTo(LeaseStatus.TERMINATED);
        }

        @Test
        @DisplayName("Lanza BadRequestException si el lease está en DRAFT")
        void rejectsDraftLease() {
            lease.setStatus(LeaseStatus.DRAFT);
            when(leaseRepository.findById(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.terminateLease(10L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("ACTIVE");
            verify(leaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza BadRequestException si el lease ya está TERMINATED")
        void rejectsAlreadyTerminated() {
            lease.setStatus(LeaseStatus.TERMINATED);
            when(leaseRepository.findById(10L)).thenReturn(Optional.of(lease));

            assertThatThrownBy(() -> service.terminateLease(10L))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el lease no existe")
        void leaseNotFound() {
            when(leaseRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.terminateLease(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private LeaseResponse sampleLeaseResponse(Long id, LeaseStatus status) {
        return new LeaseResponse(
                id, 100L, "Casa Test",
                1L, 2L, "Tenant",
                LeaseType.FIXED_TERM, status,
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(12),
                new BigDecimal("1000.00"), new BigDecimal("2000.00"),
                DepositStatus.HELD, BillingFrequency.MONTHLY,
                null, null, LocalDateTime.now());
    }

    private LeaseSummaryResponse sampleSummary() {
        return new LeaseSummaryResponse(
                10L, "Casa Test", "Tenant",
                LeaseStatus.ACTIVE,
                new BigDecimal("1000.00"),
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(12));
=======
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
>>>>>>> dev
    }
}
