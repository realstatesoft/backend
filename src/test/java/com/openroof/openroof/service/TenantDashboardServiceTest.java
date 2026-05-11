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
        when(leaseRepository.findByPrimaryTenantId(32L)).thenReturn(List.of());

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

        when(leaseRepository.findByPrimaryTenantId(32L)).thenReturn(List.of(lease));
        when(rentalInstallmentRepository.sumPendingBalanceByLeaseId(eq(100L), any())).thenReturn(new BigDecimal("500"));
        when(maintenanceRequestRepository.countByTenantIdAndStatusIn(eq(32L), any())).thenReturn(2L);
        when(messageRepository.countUnreadByUserId(32L)).thenReturn(5L);
        when(paymentRepository.sumCompletedByUserSince(eq(32L), any())).thenReturn(new BigDecimal("12000"));
        
        // Mock para buildLastPaymentInfo
        org.springframework.data.domain.Page<com.openroof.openroof.model.payment.Payment> paymentPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of());
        when(paymentRepository.findByUser_IdAndStatus(eq(32L), any(), any())).thenReturn(paymentPage);

        // Listas vacías para simplificar el mock de repositorios de installments y maintenance
        when(rentalInstallmentRepository.findTop5ByLeaseIdOrderByDueDateDesc(100L)).thenReturn(List.of());
        when(maintenanceRequestRepository.findTop5ByTenantIdOrderByCreatedAtDesc(32L)).thenReturn(List.of());

        TenantDashboardResponse response = tenantDashboardService.getDashboard(testEmail);

        assertThat(response.status()).isEqualTo(TenantDashboardResponse.TenantStatus.ACTIVE);
        assertThat(response.activeLease()).isNotNull();
        assertThat(response.activeLease().propertyTitle()).isEqualTo("Propiedad Test");
        assertThat(response.pendingBalance()).isEqualByComparingTo("500");
        assertThat(response.openMaintenanceTickets()).isEqualTo(2);
        assertThat(response.unreadMessages()).isEqualTo(5);
        assertThat(response.totalPaidLastYear()).isEqualByComparingTo("12000");
    }
}
