package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.CreateRentalApplicationRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.RentalApplicationResponse;
import com.openroof.openroof.dto.rental.SignLeaseRequest;
import com.openroof.openroof.dto.screening.TenantScreeningResponse;
import com.openroof.openroof.dto.screening.UpdateScreeningRequest;
import com.openroof.openroof.model.enums.BackgroundCheckStatus;
import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.EmploymentStatus;
import com.openroof.openroof.model.enums.InstallmentStatus;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.RentalApplicationStatus;
import com.openroof.openroof.model.enums.ScreeningProvider;
import com.openroof.openroof.model.enums.ScreeningRecommendation;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.model.screening.TenantScreening;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.RentalApplicationRepository;
import com.openroof.openroof.repository.RentalInstallmentRepository;
import com.openroof.openroof.repository.TenantScreeningRepository;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
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
        "supabase.url=http://localhost:54321",
        "supabase.service-role-key=dummy-key",
        "supabase.storage.bucket=test-bucket",
        "cors.allowed-origins=http://localhost:3000"
})
@ActiveProfiles("test")
@Transactional
class FullRentalFlowIntegrationTest {

    @Autowired RentalApplicationService applicationService;
    @Autowired TenantScreeningService screeningService;
    @Autowired LeaseService leaseService;
    @Autowired ESignatureService eSignatureService;
    @Autowired LeaseRepository leaseRepository;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired RentalApplicationRepository applicationRepository;
    @Autowired TenantScreeningRepository screeningRepository;
    @Autowired RentalInstallmentRepository installmentRepository;

    @MockitoBean NotificationService notificationService;
    @MockitoBean EmailService emailService;

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
    @DisplayName("Flujo completo: aplicacion -> screening -> aprobacion -> lease -> firma con token -> activacion -> cuotas")
    class FlujoPrincipal {

        private Long applicationId;
        private Long leaseId;
        private List<RentalInstallment> installments;
        private LocalDate startDate;
        private LocalDate endDate;

        @BeforeEach
        void executeFlow() {
            startDate = YearMonth.now().plusMonths(1).atDay(1);
            endDate = startDate.plusMonths(6).minusDays(1);

            // ── 1. Submit application con todos los campos validos ──
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
            applicationId = appResponse.id();

            // ── 2. Iniciar screening INTERNAL ──
            TenantScreeningResponse screeningCreated = screeningService.createScreening(applicationId);

            // ── 3. Completar screening con recomendacion APPROVE ──
            screeningService.updateScreeningResults(screeningCreated.id(),
                    new UpdateScreeningRequest(
                            750,
                            BackgroundCheckStatus.CLEAR,
                            true,
                            true,
                            ScreeningRecommendation.APPROVE,
                            "Screening completado exitosamente"
                    ));

            // ── 4. Aprobar application ──
            applicationService.approveApplication(applicationId, landlord.getEmail());

            // ── 5. Convertir a lease (DRAFT) ──
            LeaseResponse leaseResponse = applicationService.convertToLease(
                    applicationId,
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

            // ── 6. Send-for-signature: genera tokens, status PENDING_SIGNATURE ──
            eSignatureService.sendForSignature(leaseId);

            // ── 7. Firmar con token landlord ──
            Lease leaseWithTokens = leaseRepository.findById(leaseId).orElseThrow();
            UUID landlordToken = leaseWithTokens.getSignatureTokenLandlord();
            eSignatureService.sign(leaseId, landlordToken.toString(),
                    new SignLeaseRequest(null), "127.0.0.1", "PostmanRuntime/7.0");

            // ── 8. Firmar con token tenant (dispara activacion automatica) ──
            Lease leaseAfterLandlord = leaseRepository.findById(leaseId).orElseThrow();
            UUID tenantToken = leaseAfterLandlord.getSignatureTokenTenant();
            eSignatureService.sign(leaseId, tenantToken.toString(),
                    new SignLeaseRequest(null), "127.0.0.1", "PostmanRuntime/7.0");

            // ── 9. Recuperar cuotas generadas ──
            installments = installmentRepository.findByLeaseIdOrderByDueDateAsc(leaseId);
        }

        // ── Application assertions ────────────────────────────────────

        @Test
        @DisplayName("La aplicacion queda en estado APPROVED")
        void applicationIsApproved() {
            RentalApplication app = applicationRepository.findById(applicationId).orElseThrow();
            assertThat(app.getStatus()).isEqualTo(RentalApplicationStatus.APPROVED);
        }

        // ── Screening assertions ──────────────────────────────────────

        @Test
        @DisplayName("El screening queda con recomendacion APPROVE y provider INTERNAL")
        void screeningHasApproveRecommendation() {
            TenantScreening screening = screeningRepository.findByApplicationId(applicationId).orElseThrow();
            assertThat(screening.getRecommendation()).isEqualTo(ScreeningRecommendation.APPROVE);
            assertThat(screening.getProvider()).isEqualTo(ScreeningProvider.INTERNAL);
        }

        @Test
        @DisplayName("El screening tiene creditScore 750 y backgroundCheck CLEAR")
        void screeningHasValidData() {
            TenantScreening screening = screeningRepository.findByApplicationId(applicationId).orElseThrow();
            assertThat(screening.getCreditScore()).isEqualTo(750);
            assertThat(screening.getBackgroundCheckStatus()).isEqualTo(BackgroundCheckStatus.CLEAR);
            assertThat(screening.getIncomeVerified()).isTrue();
            assertThat(screening.getIdentityVerified()).isTrue();
        }

        // ── Lease assertions ──────────────────────────────────────────

        @Test
        @DisplayName("El lease queda en estado ACTIVE")
        void leaseIsActive() {
            Lease lease = leaseRepository.findById(leaseId).orElseThrow();
            assertThat(lease.getStatus()).isEqualTo(LeaseStatus.ACTIVE);
        }

        @Test
        @DisplayName("El lease tiene ambas firmas registradas")
        void leaseIsFullySigned() {
            Lease lease = leaseRepository.findById(leaseId).orElseThrow();
            assertThat(lease.isSigned()).isTrue();
            assertThat(lease.getSignedByLandlordAt()).isNotNull();
            assertThat(lease.getSignedByTenantAt()).isNotNull();
        }

        @Test
        @DisplayName("El lease tiene activatedAt registrado")
        void leaseHasActivatedAt() {
            Lease lease = leaseRepository.findById(leaseId).orElseThrow();
            assertThat(lease.getActivatedAt()).isNotNull();
        }

        @Test
        @DisplayName("El audit trail contiene ambos eventos de firma")
        void leaseHasAuditTrailWithTwoEvents() {
            Lease lease = leaseRepository.findById(leaseId).orElseThrow();
            assertThat(lease.getSignatureAuditTrail()).isNotNull();
            Object events = lease.getSignatureAuditTrail().get("events");
            assertThat(events).isInstanceOf(List.class);
            @SuppressWarnings("unchecked")
            List<Object> eventList = (List<Object>) events;
            assertThat(eventList).hasSize(2);
        }

        @Test
        @DisplayName("Los tokens de firma fueron consumidos (null tras firmar)")
        void signatureTokensAreNullAfterSigning() {
            Lease lease = leaseRepository.findById(leaseId).orElseThrow();
            assertThat(lease.getSignatureTokenLandlord()).isNull();
            assertThat(lease.getSignatureTokenTenant()).isNull();
        }

        // ── Installment assertions ────────────────────────────────────

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
        @DisplayName("Todas las cuotas tienen totalAmount igual a baseRent (sin cargos extra)")
        void allInstallmentsHaveTotalMatchingBaseRent() {
            assertThat(installments)
                    .allMatch(i -> i.getTotalAmount().compareTo(i.getBaseRent()) == 0);
        }

        @Test
        @DisplayName("Las fechas de vencimiento son el dia 1 de cada mes del periodo")
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
        @DisplayName("Los numeros de cuota son consecutivos del 1 al 6")
        void installmentNumbersAreConsecutive() {
            List<Integer> numbers = installments.stream()
                    .map(RentalInstallment::getInstallmentNumber)
                    .toList();
            assertThat(numbers).containsExactly(1, 2, 3, 4, 5, 6);
        }

        @Test
        @DisplayName("Los numeros de factura siguen el formato INV-{leaseId}-{001..006}")
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
        @DisplayName("Los periodos cubren el plazo completo del lease sin huecos")
        void periodsCoverFullLeaseTerm() {
            assertThat(installments.get(0).getPeriodStart()).isEqualTo(startDate);
            assertThat(installments.get(installments.size() - 1).getPeriodEnd()).isEqualTo(endDate);

            for (int i = 0; i < installments.size() - 1; i++) {
                assertThat(installments.get(i).getPeriodEnd().plusDays(1))
                        .as("Brecha entre cuota %d y %d", i + 1, i + 2)
                        .isEqualTo(installments.get(i + 1).getPeriodStart());
            }
        }
    }
}
