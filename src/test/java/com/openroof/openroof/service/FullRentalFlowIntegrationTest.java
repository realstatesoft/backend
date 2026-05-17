package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.CreateRentalApplicationRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.RentalApplicationResponse;
import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.EmploymentStatus;
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
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:full-flow-test;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS VARCHAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "application.security.jwt.secret-key=aGVsbG8td29ybGQtdGVzdC1zZWNyZXQta2V5LTI1Ni1iaXQtbG9uZy1lbm91Z2gtZm9yLXRlc3Rpbmc=",
        "supabase.url=http://localhost:54321",
        "supabase.service-role-key=dummy-key",
        "supabase.storage.bucket=test-bucket",
        "cors.allowed-origins=http://localhost:3000"
})
@ActiveProfiles("test")
@Transactional
class FullRentalFlowIntegrationTest {

    @Autowired RentalApplicationService applicationService;
    @Autowired LeaseService leaseService;
    @Autowired LeaseRepository leaseRepository;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;

    @MockitoBean
    NotificationService notificationService;

    private User landlord;
    private User tenant;
    private Property property;

    @BeforeEach
    void setUp() {
        landlord = userRepository.save(User.builder()
                .email("landlord@fullflow-test.com")
                .name("Landlord FullFlow")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());

        tenant = userRepository.save(User.builder()
                .email("tenant@fullflow-test.com")
                .name("Tenant FullFlow")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());

        property = propertyRepository.save(Property.builder()
                .title("Casa Full Flow")
                .address("Av. Test 456")
                .price(BigDecimal.ZERO)
                .rentAmount(new BigDecimal("1000000"))
                .propertyType(PropertyType.HOUSE)
                .owner(landlord)
                .build());
    }

    @Nested
    @DisplayName("Flujo completo: aplicación → aprobación → lease → activación → cuotas")
    class FlujoPrincipal {

        private Long leaseId;
        private List<RentalInstallment> installments;
        private LocalDate startDate;
        private LocalDate endDate;

        @BeforeEach
        void executeFlow() {
            startDate = YearMonth.now().plusMonths(1).atDay(1);
            endDate = startDate.plusMonths(6).minusDays(1);

            RentalApplicationResponse appResponse = applicationService.submitApplication(
                    new CreateRentalApplicationRequest(
                            property.getId(),
                            "Quiero alquilar esta propiedad",
                            new BigDecimal("5000000"),
                            EmploymentStatus.EMPLOYED,
                            "Empresa SA",
                            List.of("Referencia 1", "Referencia 2"),
                            2,
                            false,
                            true
                    ),
                    tenant.getEmail());

            applicationService.approveApplication(appResponse.id(), landlord.getEmail());

            LeaseResponse leaseResponse = applicationService.convertToLease(
                    appResponse.id(),
                    new CreateLeaseRequest(
                            property.getId(),
                            tenant.getId(),
                            LeaseType.FIXED_TERM,
                            startDate,
                            endDate,
                            new BigDecimal("1000000"),
                            new BigDecimal("2000000"),
                            BillingFrequency.MONTHLY,
                            null,
                            null,
                            null
                    ),
                    landlord.getEmail());
            leaseId = leaseResponse.id();

            leaseService.sendForSignature(leaseId);
            leaseService.signByLandlord(leaseId);
            leaseService.signByTenant(leaseId);
            installments = leaseService.activateLease(leaseId);
        }

        @Test
        @DisplayName("El lease queda en estado ACTIVE")
        void leaseIsActive() {
            Lease lease = leaseRepository.findById(leaseId).orElseThrow();
            assertThat(lease.getStatus()).isEqualTo(LeaseStatus.ACTIVE);
        }

        @Test
        @DisplayName("Se generan exactamente 6 cuotas")
        void sixInstallmentsGenerated() {
            assertThat(installments).hasSize(6);
        }

        @Test
        @DisplayName("Todas las cuotas tienen estado PENDING")
        void allInstallmentsPending() {
            assertThat(installments)
                    .allMatch(i -> i.getStatus() == InstallmentStatus.PENDING);
        }

        @Test
        @DisplayName("Todas las cuotas tienen baseRent igual al alquiler mensual")
        void allInstallmentsHaveCorrectBaseRent() {
            assertThat(installments)
                    .allMatch(i -> i.getBaseRent().compareTo(new BigDecimal("1000000")) == 0);
        }

        @Test
        @DisplayName("Las fechas de vencimiento son el día 1 de cada mes del período")
        void dueDatesAreFirstOfEachPeriodMonth() {
            List<LocalDate> expected = IntStream.range(0, 6)
                    .mapToObj(i -> startDate.plusMonths(i))
                    .toList();
            List<LocalDate> actual = installments.stream()
                    .map(RentalInstallment::getDueDate)
                    .toList();
            assertThat(actual).containsExactlyElementsOf(expected);
        }

        @Test
        @DisplayName("Los números de cuota son consecutivos del 1 al 6")
        void installmentNumbersAreConsecutive() {
            List<Integer> numbers = installments.stream()
                    .map(RentalInstallment::getInstallmentNumber)
                    .toList();
            assertThat(numbers).containsExactly(1, 2, 3, 4, 5, 6);
        }

        @Test
        @DisplayName("Los números de factura siguen el formato INV-{leaseId}-{001..006}")
        void invoiceNumbersFollowExpectedFormat() {
            List<String> expected = IntStream.rangeClosed(1, 6)
                    .mapToObj(i -> String.format("INV-%d-%03d", leaseId, i))
                    .toList();
            List<String> actual = installments.stream()
                    .map(RentalInstallment::getInvoiceNumber)
                    .toList();
            assertThat(actual).containsExactlyElementsOf(expected);
        }

        @Test
        @DisplayName("Los períodos cubren el plazo completo del lease sin huecos")
        void periodsCoverFullLeaseTerm() {
            assertThat(installments.get(0).getPeriodStart()).isEqualTo(startDate);
            assertThat(installments.get(installments.size() - 1).getPeriodEnd()).isEqualTo(endDate);
        }
    }
}
