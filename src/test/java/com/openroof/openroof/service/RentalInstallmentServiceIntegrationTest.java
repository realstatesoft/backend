package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.RentalInstallmentResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.InstallmentStatus;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.RentalInstallmentRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:installment-service-test;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS VARCHAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "application.security.jwt.secret-key=${TEST_JWT_SECRET:test-integration-jwt-secret-key-long-enough-for-256bit}",
        "supabase.url=http://localhost:54321",
        "supabase.service-role-key=dummy-key",
        "supabase.storage.bucket=test-bucket",
        "cors.allowed-origins=http://localhost:3000"
})
@ActiveProfiles("test")
@Transactional
class RentalInstallmentServiceIntegrationTest {

    @Autowired RentalInstallmentService installmentService;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired LeaseRepository leaseRepository;
    @Autowired RentalInstallmentRepository installmentRepository;

    private User landlord;
    private User tenant;
    private User outsider;
    private Property property;
    private Lease lease;

    @BeforeEach
    void setUp() {
        landlord = userRepository.save(User.builder()
                .email("landlord@installment-test.com")
                .name("Arrendador")
                .passwordHash("hashed")
                .role(UserRole.AGENT)
                .build());

        tenant = userRepository.save(User.builder()
                .email("tenant@installment-test.com")
                .name("Inquilino")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());

        outsider = userRepository.save(User.builder()
                .email("outsider@installment-test.com")
                .name("Tercero")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());

        property = propertyRepository.save(Property.builder()
                .title("Casa Test")
                .address("Calle Test 123")
                .price(BigDecimal.ZERO)
                .rentAmount(new BigDecimal("1000.00"))
                .propertyType(PropertyType.HOUSE)
                .owner(landlord)
                .build());

        lease = leaseRepository.save(Lease.builder()
                .property(property)
                .landlord(landlord)
                .primaryTenant(tenant)
                .leaseType(LeaseType.FIXED_TERM)
                .status(LeaseStatus.ACTIVE)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusMonths(12))
                .monthlyRent(new BigDecimal("1000.00"))
                .securityDeposit(new BigDecimal("2000.00"))
                .billingFrequency(BillingFrequency.MONTHLY)
                .signedByLandlordAt(LocalDateTime.now())
                .signedByTenantAt(LocalDateTime.now())
                .activatedAt(LocalDateTime.now())
                .dueDay(1)
                .currency("PYG")
                .build());

        for (int i = 1; i <= 3; i++) {
            installmentRepository.save(RentalInstallment.builder()
                    .lease(lease)
                    .installmentNumber(i)
                    .periodStart(LocalDate.now().plusMonths(i - 1))
                    .periodEnd(LocalDate.now().plusMonths(i).minusDays(1))
                    .dueDate(LocalDate.now().plusMonths(i))
                    .baseRent(new BigDecimal("1000.00"))
                    .totalAmount(new BigDecimal("1000.00"))
                    .status(InstallmentStatus.PENDING)
                    .build());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listByLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listByLease()")
    class ListByLease {

        private final PageRequest pageable = PageRequest.of(0, 12, Sort.by("dueDate").descending());

        @Test
        @DisplayName("Landlord obtiene las 3 cuotas del lease")
        void landlordCanList() {
            Page<RentalInstallmentResponse> page = installmentService.listByLease(
                    lease.getId(), landlord.getId(), pageable);

            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getContent()).allMatch(r -> r.leaseId().equals(lease.getId()));
        }

        @Test
        @DisplayName("Tenant obtiene las cuotas del lease")
        void tenantCanList() {
            Page<RentalInstallmentResponse> page = installmentService.listByLease(
                    lease.getId(), tenant.getId(), pageable);

            assertThat(page.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("Tercero sin relación con el lease recibe AccessDeniedException")
        void outsiderGetsDenied() {
            assertThatThrownBy(() -> installmentService.listByLease(
                    lease.getId(), outsider.getId(), pageable))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("La paginación limita los resultados al tamaño de página")
        void paginationLimitsResults() {
            PageRequest smallPage = PageRequest.of(0, 2, Sort.by("dueDate").descending());

            Page<RentalInstallmentResponse> page = installmentService.listByLease(
                    lease.getId(), landlord.getId(), smallPage);

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("Las cuotas se retornan ordenadas por fecha de vencimiento descendente")
        void installmentsOrderedByDueDateDesc() {
            Page<RentalInstallmentResponse> page = installmentService.listByLease(
                    lease.getId(), landlord.getId(), pageable);

            var dates = page.getContent().stream()
                    .map(RentalInstallmentResponse::dueDate)
                    .toList();
            assertThat(dates).isSortedAccordingTo((a, b) -> b.compareTo(a));
        }

        @Test
        @DisplayName("Lease inexistente provoca AccessDeniedException (no se puede acceder)")
        void nonExistentLease_throwsAccessDenied() {
            assertThatThrownBy(() -> installmentService.listByLease(
                    99999L, landlord.getId(), pageable))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getById
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("Landlord obtiene el detalle de una cuota")
        void landlordCanGet() {
            RentalInstallment first = installmentRepository
                    .findByLeaseIdOrderByDueDateAsc(lease.getId()).get(0);

            RentalInstallmentResponse response = installmentService.getById(first.getId(), landlord.getId());

            assertThat(response.id()).isEqualTo(first.getId());
            assertThat(response.leaseId()).isEqualTo(lease.getId());
            assertThat(response.installmentNumber()).isEqualTo(1);
            assertThat(response.status()).isEqualTo(InstallmentStatus.PENDING);
        }

        @Test
        @DisplayName("Tenant obtiene el detalle de la misma cuota")
        void tenantCanGet() {
            RentalInstallment first = installmentRepository
                    .findByLeaseIdOrderByDueDateAsc(lease.getId()).get(0);

            RentalInstallmentResponse response = installmentService.getById(first.getId(), tenant.getId());

            assertThat(response.id()).isEqualTo(first.getId());
        }

        @Test
        @DisplayName("Tercero sin relación con el lease recibe AccessDeniedException")
        void outsiderGetsDenied() {
            RentalInstallment first = installmentRepository
                    .findByLeaseIdOrderByDueDateAsc(lease.getId()).get(0);

            assertThatThrownBy(() -> installmentService.getById(first.getId(), outsider.getId()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Cuota inexistente lanza ResourceNotFoundException")
        void notFound_throwsResourceNotFoundException() {
            assertThatThrownBy(() -> installmentService.getById(99999L, landlord.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("El response incluye baseRent, totalAmount e installmentNumber correctos")
        void responseHasCorrectFields() {
            RentalInstallment second = installmentRepository
                    .findByLeaseIdOrderByDueDateAsc(lease.getId()).get(1);

            RentalInstallmentResponse response = installmentService.getById(second.getId(), tenant.getId());

            assertThat(response.amount()).isEqualByComparingTo("1000.00");
            assertThat(response.totalAmount()).isEqualByComparingTo("1000.00");
            assertThat(response.installmentNumber()).isEqualTo(2);
        }
    }
}
