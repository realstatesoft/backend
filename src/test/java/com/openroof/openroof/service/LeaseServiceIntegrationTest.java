package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.LeaseSummaryResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.BillingFrequency;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración para LeaseService.
 * Valida la lógica de negocio completa contra BD H2 en memoria.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:lease-service-test;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS VARCHAR",
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
class LeaseServiceIntegrationTest {

    @Autowired LeaseService leaseService;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired LeaseRepository leaseRepository;
    @Autowired RentalInstallmentRepository installmentRepository;

    private User landlord;
    private User tenant;
    private User outsider;
    private Property property;

    @BeforeEach
    void setUp() {
        landlord = userRepository.save(User.builder()
                .email("landlord@lease-test.com")
                .name("Arrendador Test")
                .passwordHash("hashed")
                .role(UserRole.AGENT)
                .build());

        tenant = userRepository.save(User.builder()
                .email("tenant@lease-test.com")
                .name("Inquilino Test")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());

        outsider = userRepository.save(User.builder()
                .email("outsider@lease-test.com")
                .name("Tercero Test")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());

        property = propertyRepository.save(Property.builder()
                .title("Casa Integración")
                .address("Calle Test 123")
                .price(BigDecimal.ZERO)
                .rentAmount(new BigDecimal("1000.00"))
                .propertyType(PropertyType.HOUSE)
                .owner(landlord)
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createLease()")
    class CreateLease {

        @Test
        @DisplayName("Persiste el lease con status DRAFT y retorna response")
        void persistsDraftLease() {
            CreateLeaseRequest dto = buildDto();

            LeaseResponse response = leaseService.createLease(landlord.getId(), dto);

            assertThat(response.id()).isNotNull();
            assertThat(response.status()).isEqualTo(LeaseStatus.DRAFT);
            assertThat(response.landlordId()).isEqualTo(landlord.getId());
            assertThat(response.tenantId()).isEqualTo(tenant.getId());

            Lease persisted = leaseRepository.findById(response.id()).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(LeaseStatus.DRAFT);
            assertThat(persisted.getMonthlyRent()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si la propiedad no existe")
        void rejectsUnknownProperty() {
            CreateLeaseRequest dto = new CreateLeaseRequest(
                    9999L, tenant.getId(), LeaseType.FIXED_TERM,
                    LocalDate.now().plusDays(1), LocalDate.now().plusMonths(12),
                    new BigDecimal("1000.00"), new BigDecimal("2000.00"),
                    BillingFrequency.MONTHLY, null, null, null);

            assertThatThrownBy(() -> leaseService.createLease(landlord.getId(), dto))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLease()")
    class GetLease {

        @Test
        @DisplayName("Landlord puede ver su propio lease")
        void landlordCanView() {
            Lease saved = leaseRepository.save(buildLease(LeaseStatus.ACTIVE));

            LeaseResponse response = leaseService.getLease(saved.getId(), landlord.getId());

            assertThat(response.id()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("Tenant puede ver su propio lease")
        void tenantCanView() {
            Lease saved = leaseRepository.save(buildLease(LeaseStatus.ACTIVE));

            LeaseResponse response = leaseService.getLease(saved.getId(), tenant.getId());

            assertThat(response.id()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("Tercero recibe AccessDeniedException")
        void outsiderGetsDenied() {
            Lease saved = leaseRepository.save(buildLease(LeaseStatus.ACTIVE));

            assertThatThrownBy(() -> leaseService.getLease(saved.getId(), outsider.getId()))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listLeases
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listLeases()")
    class ListLeases {

        private final PageRequest pageable = PageRequest.of(0, 10, Sort.by("id"));

        @Test
        @DisplayName("AGENT solo ve sus leases como landlord")
        void agentSeesOwnLeasesOnly() {
            leaseRepository.save(buildLease(LeaseStatus.ACTIVE));
            leaseRepository.save(buildLease(LeaseStatus.DRAFT));

            // Guardar un lease de otro landlord
            User otherLandlord = userRepository.save(User.builder()
                    .email("other@lease-test.com").name("Otro").passwordHash("h")
                    .role(UserRole.AGENT).build());
            Lease otherLease = buildLease(LeaseStatus.ACTIVE);
            otherLease.setLandlord(otherLandlord);
            leaseRepository.save(otherLease);

            Page<LeaseSummaryResponse> page = leaseService.listLeases(
                    landlord.getId(), UserRole.AGENT, null, null, pageable);

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(LeaseSummaryResponse::propertyAddress)
                    .allMatch(addr -> addr != null);
        }

        @Test
        @DisplayName("USER solo ve sus leases como tenant")
        void tenantSeesOwnLeasesOnly() {
            leaseRepository.save(buildLease(LeaseStatus.ACTIVE));

            Page<LeaseSummaryResponse> page = leaseService.listLeases(
                    tenant.getId(), UserRole.USER, null, null, pageable);

            assertThat(page.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Filtro por status funciona correctamente")
        void filtersByStatus() {
            leaseRepository.save(buildLease(LeaseStatus.ACTIVE));
            leaseRepository.save(buildLease(LeaseStatus.DRAFT));

            Page<LeaseSummaryResponse> activePage = leaseService.listLeases(
                    landlord.getId(), UserRole.AGENT, LeaseStatus.ACTIVE, null, pageable);
            Page<LeaseSummaryResponse> draftPage = leaseService.listLeases(
                    landlord.getId(), UserRole.AGENT, LeaseStatus.DRAFT, null, pageable);

            assertThat(activePage.getTotalElements()).isEqualTo(1);
            assertThat(activePage.getContent().get(0).status()).isEqualTo(LeaseStatus.ACTIVE);
            assertThat(draftPage.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("ADMIN ve todos los leases del sistema")
        void adminSeesAll() {
            leaseRepository.save(buildLease(LeaseStatus.ACTIVE));
            leaseRepository.save(buildLease(LeaseStatus.DRAFT));

            User admin = userRepository.save(User.builder()
                    .email("admin@lease-test.com").name("Admin").passwordHash("h")
                    .role(UserRole.ADMIN).build());

            Page<LeaseSummaryResponse> page = leaseService.listLeases(
                    admin.getId(), UserRole.ADMIN, null, null, pageable);

            assertThat(page.getTotalElements()).isEqualTo(2);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateLease()")
    class UpdateLease {

        @Test
        @DisplayName("Actualiza los campos del lease en estado DRAFT")
        void updatesDraftFields() {
            Lease saved = leaseRepository.save(buildLease(LeaseStatus.DRAFT));
            CreateLeaseRequest updDto = new CreateLeaseRequest(
                    property.getId(), tenant.getId(), LeaseType.MONTH_TO_MONTH,
                    LocalDate.now().plusDays(1), null,
                    new BigDecimal("1500.00"), new BigDecimal("3000.00"),
                    BillingFrequency.MONTHLY, null, null, null);

            LeaseResponse response = leaseService.updateLease(saved.getId(), updDto);

            assertThat(response.monthlyRent()).isEqualByComparingTo("1500.00");
            Lease updated = leaseRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getMonthlyRent()).isEqualByComparingTo("1500.00");
            assertThat(updated.getLeaseType()).isEqualTo(LeaseType.MONTH_TO_MONTH);
        }

        @Test
        @DisplayName("Rechaza la actualización si el lease está en PENDING_SIGNATURE")
        void rejectsPendingSignatureLease() {
            Lease saved = leaseRepository.save(buildLease(LeaseStatus.PENDING_SIGNATURE));
            CreateLeaseRequest updDto = buildDto();

            assertThatThrownBy(() -> leaseService.updateLease(saved.getId(), updDto))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("DRAFT");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // activateLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("activateLease()")
    class ActivateLease {

        @Test
        @DisplayName("Activa el lease, guarda activatedAt y genera installments en BD")
        void activatesAndPersistsInstallments() {
            Lease signed = buildLease(LeaseStatus.PENDING_SIGNATURE);
            signed.setSignedByLandlordAt(LocalDateTime.now());
            signed.setSignedByTenantAt(LocalDateTime.now());
            Lease saved = leaseRepository.save(signed);

            List<RentalInstallment> result = leaseService.activateLease(saved.getId());

            assertThat(result).isNotEmpty();

            Lease activated = leaseRepository.findById(saved.getId()).orElseThrow();
            assertThat(activated.getStatus()).isEqualTo(LeaseStatus.ACTIVE);
            assertThat(activated.getActivatedAt()).isNotNull();

            assertThat(installmentRepository.existsByLeaseId(saved.getId())).isTrue();
            List<RentalInstallment> inDb = installmentRepository.findByLeaseIdOrderByDueDateAsc(saved.getId());
            assertThat(inDb).hasSameSizeAs(result);
            assertThat(inDb.get(0).getInstallmentNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Lanza BadRequestException si el lease no está firmado por ambas partes")
        void rejectsUnsignedLease() {
            Lease unsigned = buildLease(LeaseStatus.PENDING_SIGNATURE);
            Lease saved = leaseRepository.save(unsigned);

            assertThatThrownBy(() -> leaseService.activateLease(saved.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("signed");
        }

        @Test
        @DisplayName("Lanza BadRequestException si el lease está en DRAFT")
        void rejectsDraftLease() {
            Lease draft = leaseRepository.save(buildLease(LeaseStatus.DRAFT));

            assertThatThrownBy(() -> leaseService.activateLease(draft.getId()))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // terminateLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("terminateLease()")
    class TerminateLease {

        @Test
        @DisplayName("Cambia el status a TERMINATED y persiste en BD")
        void terminatesAndPersists() {
            Lease active = leaseRepository.save(buildLease(LeaseStatus.ACTIVE));

            LeaseResponse response = leaseService.terminateLease(active.getId());

            assertThat(response.status()).isEqualTo(LeaseStatus.TERMINATED);
            Lease terminated = leaseRepository.findById(active.getId()).orElseThrow();
            assertThat(terminated.getStatus()).isEqualTo(LeaseStatus.TERMINATED);
        }

        @Test
        @DisplayName("También termina leases en estado EXPIRING_SOON")
        void terminatesExpiringSoon() {
            Lease expiring = leaseRepository.save(buildLease(LeaseStatus.EXPIRING_SOON));

            leaseService.terminateLease(expiring.getId());

            Lease updated = leaseRepository.findById(expiring.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(LeaseStatus.TERMINATED);
        }

        @Test
        @DisplayName("Lanza BadRequestException si el lease está en DRAFT")
        void rejectsDraftLease() {
            Lease draft = leaseRepository.save(buildLease(LeaseStatus.DRAFT));

            assertThatThrownBy(() -> leaseService.terminateLease(draft.getId()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el lease no existe")
        void rejectsUnknownLease() {
            assertThatThrownBy(() -> leaseService.terminateLease(99999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private CreateLeaseRequest buildDto() {
        return new CreateLeaseRequest(
                property.getId(), tenant.getId(), LeaseType.FIXED_TERM,
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(12),
                new BigDecimal("1000.00"), new BigDecimal("2000.00"),
                BillingFrequency.MONTHLY, null, null, null);
    }

    private Lease buildLease(LeaseStatus status) {
        return Lease.builder()
                .property(property)
                .landlord(landlord)
                .primaryTenant(tenant)
                .leaseType(LeaseType.FIXED_TERM)
                .status(status)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusMonths(12))
                .monthlyRent(new BigDecimal("1000.00"))
                .securityDeposit(new BigDecimal("2000.00"))
                .billingFrequency(BillingFrequency.MONTHLY)
                .dueDay(1)
                .currency("PYG")
                .build();
    }
}
