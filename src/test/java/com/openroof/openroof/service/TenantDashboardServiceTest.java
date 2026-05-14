package com.openroof.openroof.service;

import com.openroof.openroof.dto.dashboard.TenantDashboardResponse;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantDashboardServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private LeaseRepository leaseRepository;
    @Mock private RentalInstallmentRepository rentalInstallmentRepository;
    @Mock private MaintenanceRequestRepository maintenanceRequestRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private LeasePaymentRepository leasePaymentRepository;

    @InjectMocks
    private TenantDashboardService tenantDashboardService;

    private User testUser;
    private String testEmail = "tenant@test.com";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(testEmail)
                .name("Test Tenant")
                .build();
        testUser.setId(32L);
    }

    @Test
    @DisplayName("getDashboard() - Usuario sin lease activo → retorna status INACTIVE")
    void getDashboard_noActiveLease_returnsInactive() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(leaseRepository.findAllByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(testUser.getId(), LeaseStatus.ACTIVE)).thenReturn(List.of());
        when(messageRepository.countUnreadByUserId(testUser.getId())).thenReturn(0L);

        TenantDashboardResponse response = tenantDashboardService.getDashboard(testEmail);

        assertThat(response.status()).isEqualTo(TenantDashboardResponse.TenantStatus.INACTIVE);
        assertThat(response.statusMessage()).contains("No tienes un arriendo activo");
    }

    @Test
    @DisplayName("getDashboard() - Usuario con lease activo → retorna datos completos del dashboard")
    void getDashboard_withActiveLease_returnsFullData() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        User landlord = User.builder().name("Landlord Test").build();
        Property property = Property.builder().title("Propiedad Test").address("Calle Falsa 123").build();

        Lease lease = Lease.builder()
                .status(LeaseStatus.ACTIVE)
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusMonths(11))
                .monthlyRent(new BigDecimal("1000"))
                .currency("USD")
                .landlord(landlord)
                .property(property)
                .build();
        lease.setId(100L);

        when(leaseRepository.findAllByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(testUser.getId(), LeaseStatus.ACTIVE)).thenReturn(List.of(lease));
        when(rentalInstallmentRepository.sumPendingBalanceByLeaseId(eq(100L), any())).thenReturn(new BigDecimal("500"));
        when(maintenanceRequestRepository.countByTenantIdAndStatusIn(eq(testUser.getId()), any())).thenReturn(2L);
        when(messageRepository.countUnreadByUserId(testUser.getId())).thenReturn(5L);
        when(paymentRepository.sumCompletedByUserSince(eq(testUser.getId()), any(com.openroof.openroof.model.enums.PaymentStatus.class), any())).thenReturn(new BigDecimal("12000"));

        // Mock para buildLastPaymentInfo
        org.springframework.data.domain.Page<com.openroof.openroof.model.payment.Payment> paymentPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of());
        when(paymentRepository.findByUser_IdAndStatus(eq(testUser.getId()), any(), any())).thenReturn(paymentPage);

        // Listas vacías para simplificar el mock de repositorios de installments y maintenance
        when(rentalInstallmentRepository.findTop5ByLeaseIdOrderByDueDateDesc(100L)).thenReturn(List.of());
        when(maintenanceRequestRepository.findTop5ByTenantIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(List.of());

        TenantDashboardResponse response = tenantDashboardService.getDashboard(testEmail);

        assertThat(response.status()).isEqualTo(TenantDashboardResponse.TenantStatus.ACTIVE);
        assertThat(response.activeLeases()).isNotNull().hasSize(1);
        assertThat(response.activeLeases().get(0).propertyTitle()).isEqualTo("Propiedad Test");
        assertThat(response.pendingBalance()).isEqualByComparingTo("500");
        assertThat(response.openMaintenanceTickets()).isEqualTo(2);
        assertThat(response.unreadMessages()).isEqualTo(5);
        assertThat(response.totalPaidLastYear()).isEqualByComparingTo("12000");
    }

    @Test
    @DisplayName("getLeases() - Happy path con landlord, property y signature audit")
    void getLeases_returnsLeaseData() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        User landlord = User.builder().email("landlord@test.com").name("Landlord Test").phone("1234").build();
        landlord.setId(55L);
        Property property = Property.builder().title("Propiedad Test").address("Calle Falsa 123").build();
        property.setId(88L);

        Lease lease = Lease.builder()
                .status(LeaseStatus.ACTIVE)
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusDays(15))
                .monthlyRent(new BigDecimal("1000"))
                .currency("USD")
                .landlord(landlord)
                .property(property)
                .primaryTenant(testUser)
                .signatureAuditTrail(java.util.Map.of("auditPdfUrl", "http://audit.pdf"))
                .build();
        lease.setId(100L);

        when(leaseRepository.findAllByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(testUser.getId(), LeaseStatus.ACTIVE)).thenReturn(List.of(lease));

        List<com.openroof.openroof.dto.dashboard.TenantLeaseResponse> responses = tenantDashboardService.getLeases(testEmail);

        assertThat(responses).hasSize(1);
        com.openroof.openroof.dto.dashboard.TenantLeaseResponse response = responses.get(0);
        assertThat(response.daysRemaining()).isEqualTo(15);
        assertThat(response.landlord().userId()).isEqualTo(55L);
        assertThat(response.landlord().name()).isEqualTo("Landlord Test");

        assertThat(response.documents()).hasSize(1);
        assertThat(response.documents().get(0).type()).isEqualTo("SIGNATURE_AUDIT");
        assertThat(response.documents().get(0).fileUrl()).isEqualTo("http://audit.pdf");
    }

    @Test
    @DisplayName("getLeases() - No existe lease activo → retorna lista vacía")
    void getLeases_noActiveLease_returnsEmptyList() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(leaseRepository.findAllByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(testUser.getId(), LeaseStatus.ACTIVE)).thenReturn(List.of());

        List<com.openroof.openroof.dto.dashboard.TenantLeaseResponse> responses = tenantDashboardService.getLeases(testEmail);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("getLeaseById() - Happy path devuelve contrato del tenant")
    void getLeaseById_returnsLeaseData() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        User landlord = User.builder().email("landlord@test.com").name("Landlord Test").phone("1234").build();
        landlord.setId(55L);
        Property property = Property.builder().title("Propiedad Test").address("Calle Falsa 123").build();
        property.setId(88L);

        Lease lease = Lease.builder()
                .status(LeaseStatus.ACTIVE)
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusDays(15))
                .monthlyRent(new BigDecimal("1000"))
                .currency("USD")
                .landlord(landlord)
                .property(property)
                .primaryTenant(testUser)
                .build();
        lease.setId(100L);

        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));

        com.openroof.openroof.dto.dashboard.TenantLeaseResponse response = tenantDashboardService.getLeaseById(testEmail, 100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.landlord().name()).isEqualTo("Landlord Test");
    }

    @Test
    @DisplayName("getLeaseById() - Contrato no pertenece al tenant → arroja ResourceNotFoundException")
    void getLeaseById_notOwnedByTenant_throwsException() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        User otherTenant = User.builder().email("other@test.com").name("Other").build();
        otherTenant.setId(99L);

        Lease lease = Lease.builder()
                .status(LeaseStatus.ACTIVE)
                .primaryTenant(otherTenant)
                .build();
        lease.setId(100L);

        when(leaseRepository.findById(100L)).thenReturn(Optional.of(lease));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> tenantDashboardService.getLeaseById(testEmail, 100L))
                .isInstanceOf(com.openroof.openroof.exception.ResourceNotFoundException.class)
                .hasMessageContaining("No tenes permiso para ver este contrato");
    }

    @Test
    @DisplayName("getPayments() - Retorna historial de cuotas paginado")
    void getPayments_returnsPaginatedInstallments() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        Lease lease = Lease.builder().status(LeaseStatus.ACTIVE).build();
        lease.setId(100L);
        when(leaseRepository.findAllByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(testUser.getId(), LeaseStatus.ACTIVE)).thenReturn(List.of(lease));

        com.openroof.openroof.model.rental.RentalInstallment installment = com.openroof.openroof.model.rental.RentalInstallment.builder()
                .installmentNumber(1)
                .periodStart(LocalDate.now())
                .periodEnd(LocalDate.now().plusMonths(1))
                .totalAmount(new BigDecimal("1000"))
                .paidAmount(new BigDecimal("1000"))
                .status(com.openroof.openroof.model.enums.InstallmentStatus.PAID)
                .dueDate(LocalDate.now().plusDays(5))
                .build();
        installment.setId(200L);

        org.springframework.data.domain.Page<com.openroof.openroof.model.rental.RentalInstallment> page = new org.springframework.data.domain.PageImpl<>(List.of(installment));
        when(rentalInstallmentRepository.findByLeaseIdsOrderByDueDateDesc(eq(List.of(100L)), any())).thenReturn(page);
        when(leasePaymentRepository.findByInstallmentIdIn(any())).thenReturn(List.of());
        when(rentalInstallmentRepository.findByLeaseIdsOrderByDueDateAsc(eq(List.of(100L)))).thenReturn(List.of(installment));

        var response = tenantDashboardService.getPayments(testEmail, org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(response.installments()).hasSize(1);
        assertThat(response.installments().get(0).installmentNumber()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getMaintenance() - Retorna tickets del lease activo")
    void getMaintenance_returnsTickets() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        Lease lease = Lease.builder().status(LeaseStatus.ACTIVE).build();
        lease.setId(100L);
        when(leaseRepository.findAllByPrimaryTenantIdAndStatusOrderByCreatedAtDesc(testUser.getId(), LeaseStatus.ACTIVE)).thenReturn(List.of(lease));

        com.openroof.openroof.model.maintenance.MaintenanceRequest request = com.openroof.openroof.model.maintenance.MaintenanceRequest.builder()
                .title("Fuga de agua")
                .status(com.openroof.openroof.model.enums.MaintenanceStatus.SUBMITTED)
                .priority(com.openroof.openroof.model.enums.MaintenancePriority.HIGH)
                .build();
        request.setId(300L);
        request.setCreatedAt(java.time.LocalDateTime.now());

        org.springframework.data.domain.Page<com.openroof.openroof.model.maintenance.MaintenanceRequest> page = new org.springframework.data.domain.PageImpl<>(List.of(request));
        when(maintenanceRequestRepository.findByTenantIdOrderByCreatedAtDesc(eq(testUser.getId()), any())).thenReturn(page);

        var response = tenantDashboardService.getMaintenance(testEmail, org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(response.tickets()).hasSize(1);
        assertThat(response.tickets().get(0).title()).isEqualTo("Fuga de agua");
        assertThat(response.countsByStatus()).containsEntry("SUBMITTED", 1L);
    }
}
