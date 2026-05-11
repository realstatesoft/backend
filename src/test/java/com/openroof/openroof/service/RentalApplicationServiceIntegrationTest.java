package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.CreateRentalApplicationRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.RentalApplicationResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.EmploymentStatus;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.RentalApplicationStatus;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.RentalApplicationRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración para RentalApplicationService.
 * Prueba la lógica de negocio completa con BD H2 real.
 */
@SpringBootTest(properties = {
        // H2 datasource — overrides application-test.yml's ${DB_URL} etc.
        "spring.datasource.url=jdbc:h2:mem:rental-test;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS VARCHAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // H2 dialect — overrides PostgreSQLDialect from application.yml
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // Disable Liquibase
        "spring.liquibase.enabled=false",
        // JWT — overrides application-test.yml's ${JWT_SECRET} (no default)
        "application.security.jwt.secret-key=aGVsbG8td29ybGQtdGVzdC1zZWNyZXQta2V5LTI1Ni1iaXQtbG9uZy1lbm91Z2gtZm9yLXRlc3Rpbmc=",
        // Supabase dummy — overrides application-test.yml's ${SUPABASE_URL} etc.
        "supabase.url=http://localhost:54321",
        "supabase.service-role-key=dummy-key",
        "supabase.storage.bucket=test-bucket",
        // CORS — overrides application-test.yml's ${CORS_ALLOWED_ORIGINS}
        "cors.allowed-origins=http://localhost:3000"
})
@ActiveProfiles("test")
@Transactional
class RentalApplicationServiceIntegrationTest {

    @Autowired RentalApplicationService rentalApplicationService;
    @Autowired UserRepository userRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired RentalApplicationRepository applicationRepository;
    @Autowired LeaseRepository leaseRepository;

    @MockitoBean
    NotificationService notificationService;

    private User applicant;
    private User owner;
    private User outsider;
    private Property property;

    @BeforeEach
    void setUp() {
        applicant = userRepository.save(User.builder()
                .email("tenant@test.com")
                .name("Inquilino Test")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());

        owner = userRepository.save(User.builder()
                .email("owner@test.com")
                .name("Propietario Test")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());

        outsider = userRepository.save(User.builder()
                .email("outsider@test.com")
                .name("Tercero Test")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());

        property = propertyRepository.save(Property.builder()
                .title("Apartamento Integración")
                .address("Av. Mcal. López 1234")
                .price(BigDecimal.ZERO)
                .rentAmount(new BigDecimal("1000.00"))
                .propertyType(PropertyType.APARTMENT)
                .owner(owner)
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // submitApplication
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitApplication()")
    class Submit {

        @Test
        @DisplayName("Persiste la aplicación en BD con status SUBMITTED")
        void persistsApplicationInDb() {
            CreateRentalApplicationRequest dto = buildSubmitRequest();

            RentalApplicationResponse response = rentalApplicationService.submitApplication(dto, applicant.getEmail());

            assertThat(response.id()).isNotNull();
            assertThat(response.status()).isEqualTo(RentalApplicationStatus.SUBMITTED);
            assertThat(response.propertyId()).isEqualTo(property.getId());
            assertThat(response.applicantId()).isEqualTo(applicant.getId());

            Optional<RentalApplication> persisted = applicationRepository.findById(response.id());
            assertThat(persisted).isPresent();
            assertThat(persisted.get().getSubmittedAt()).isNotNull();
        }

        @Test
        @DisplayName("Calcula incomeToRentRatio = monthlyIncome / rentAmount y lo persiste")
        void calculatesAndPersistsIncomeToRentRatio() {
            CreateRentalApplicationRequest dto = buildSubmitRequest(); // income = 3000, rent = 1000

            RentalApplicationResponse response = rentalApplicationService.submitApplication(dto, applicant.getEmail());

            RentalApplication persisted = applicationRepository.findById(response.id()).orElseThrow();
            // 3000 / 1000 = 3.00
            assertThat(persisted.getIncomeToRentRatio()).isEqualByComparingTo("3.00");
        }

        @Test
        @DisplayName("incomeToRentRatio es null si la propiedad no tiene rentAmount")
        void nullRatioWhenNoRentAmount() {
            property.setRentAmount(null);
            propertyRepository.save(property);

            RentalApplicationResponse response = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            RentalApplication persisted = applicationRepository.findById(response.id()).orElseThrow();
            assertThat(persisted.getIncomeToRentRatio()).isNull();
        }

        @Test
        @DisplayName("Rechaza si el applicant ya tiene una aplicación activa (SUBMITTED) para la misma propiedad")
        void rejectsDuplicateActiveApplication() {
            rentalApplicationService.submitApplication(buildSubmitRequest(), applicant.getEmail());

            assertThatThrownBy(() -> rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("activa");
        }

        @Test
        @DisplayName("Permite segunda aplicación si la primera fue REJECTED")
        void allowsResubmitAfterRejection() {
            RentalApplicationResponse first = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            // Rechazar la primera aplicación
            rentalApplicationService.rejectApplication(first.id(), "No califica", owner.getEmail());

            // Segunda aplicación debe ser permitida
            RentalApplicationResponse second = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            assertThat(second.id()).isNotEqualTo(first.id());
            assertThat(second.status()).isEqualTo(RentalApplicationStatus.SUBMITTED);
        }

        @Test
        @DisplayName("Rechaza si el applicant es el dueño de la propiedad")
        void rejectsOwnerAsApplicant() {
            assertThatThrownBy(() -> rentalApplicationService.submitApplication(
                    buildSubmitRequest(), owner.getEmail()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("propia");
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si la propiedad no existe")
        void rejectsUnknownProperty() {
            CreateRentalApplicationRequest dto = new CreateRentalApplicationRequest(
                    999999L, "mensaje", new BigDecimal("3000.00"),
                    EmploymentStatus.EMPLOYED, "Empresa", 2, false, true);

            assertThatThrownBy(() -> rentalApplicationService.submitApplication(dto, applicant.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getApplication
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getApplication()")
    class GetApplication {

        @Test
        @DisplayName("El applicant puede ver su propia aplicación")
        void applicantCanViewOwnApplication() {
            RentalApplicationResponse created = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            RentalApplicationResponse fetched = rentalApplicationService.getApplication(
                    created.id(), applicant.getEmail());

            assertThat(fetched.id()).isEqualTo(created.id());
            assertThat(fetched.applicantId()).isEqualTo(applicant.getId());
        }

        @Test
        @DisplayName("El owner de la propiedad puede ver la aplicación")
        void ownerCanViewApplication() {
            RentalApplicationResponse created = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            RentalApplicationResponse fetched = rentalApplicationService.getApplication(
                    created.id(), owner.getEmail());

            assertThat(fetched.id()).isEqualTo(created.id());
        }

        @Test
        @DisplayName("Un tercero sin relación recibe ForbiddenException")
        void outsiderGetsForbidden() {
            RentalApplicationResponse created = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            assertThatThrownBy(() -> rentalApplicationService.getApplication(
                    created.id(), outsider.getEmail()))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si no existe")
        void notFound() {
            assertThatThrownBy(() -> rentalApplicationService.getApplication(999999L, applicant.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listApplications
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listApplications()")
    class ListApplications {

        @Test
        @DisplayName("Owner lista todas las aplicaciones de su propiedad")
        void ownerListsAllApplications() {
            // Crear segundo applicant
            User applicant2 = userRepository.save(User.builder()
                    .email("tenant2@test.com").name("Inquilino 2")
                    .passwordHash("hashed").role(UserRole.USER).build());

            rentalApplicationService.submitApplication(buildSubmitRequest(), applicant.getEmail());
            rentalApplicationService.submitApplication(buildSubmitRequest(), applicant2.getEmail());

            PageRequest pageable = PageRequest.of(0, 10, Sort.by("submittedAt").descending());
            Page<RentalApplicationResponse> page = rentalApplicationService
                    .listApplications(property.getId(), null, pageable, owner.getEmail());

            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Filtra correctamente por status APPROVED")
        void filtersApprovedApplications() {
            RentalApplicationResponse submitted = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());
            rentalApplicationService.approveApplication(submitted.id(), owner.getEmail());

            // Crear una segunda aplicación que queda en SUBMITTED
            User applicant2 = userRepository.save(User.builder()
                    .email("tenant2@test.com").name("Inquilino 2")
                    .passwordHash("hashed").role(UserRole.USER).build());
            rentalApplicationService.submitApplication(buildSubmitRequest(), applicant2.getEmail());

            PageRequest pageable = PageRequest.of(0, 10);
            Page<RentalApplicationResponse> approvedPage = rentalApplicationService
                    .listApplications(property.getId(), RentalApplicationStatus.APPROVED, pageable, owner.getEmail());

            assertThat(approvedPage.getTotalElements()).isEqualTo(1);
            assertThat(approvedPage.getContent().get(0).status()).isEqualTo(RentalApplicationStatus.APPROVED);
        }

        @Test
        @DisplayName("Retorna lista vacía si no hay aplicaciones para la propiedad")
        void returnsEmptyPageWhenNone() {
            PageRequest pageable = PageRequest.of(0, 10);
            Page<RentalApplicationResponse> page = rentalApplicationService
                    .listApplications(property.getId(), null, pageable, owner.getEmail());

            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Un tercero sin permiso recibe ForbiddenException")
        void outsiderGetsForbidden() {
            assertThatThrownBy(() -> rentalApplicationService.listApplications(
                    property.getId(), null, PageRequest.of(0, 10), outsider.getEmail()))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // approveApplication
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approveApplication()")
    class Approve {

        @Test
        @DisplayName("Cambia el status a APPROVED y persiste decidedAt en BD")
        void persistsApprovedStatus() {
            RentalApplicationResponse created = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            RentalApplicationResponse approved = rentalApplicationService.approveApplication(
                    created.id(), owner.getEmail());

            assertThat(approved.status()).isEqualTo(RentalApplicationStatus.APPROVED);

            RentalApplication persisted = applicationRepository.findById(created.id()).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(RentalApplicationStatus.APPROVED);
            assertThat(persisted.getDecidedAt()).isNotNull();
        }

        @Test
        @DisplayName("Admin puede aprobar cualquier aplicación")
        void adminCanApprove() {
            User admin = userRepository.save(User.builder()
                    .email("admin@test.com").name("Admin")
                    .passwordHash("hashed").role(UserRole.ADMIN).build());

            RentalApplicationResponse created = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            RentalApplicationResponse approved = rentalApplicationService.approveApplication(
                    created.id(), admin.getEmail());

            assertThat(approved.status()).isEqualTo(RentalApplicationStatus.APPROVED);
        }

        @Test
        @DisplayName("Rechaza aprobar una aplicación ya APPROVED")
        void rejectsDoubleApproval() {
            RentalApplicationResponse created = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());
            rentalApplicationService.approveApplication(created.id(), owner.getEmail());

            assertThatThrownBy(() -> rentalApplicationService.approveApplication(
                    created.id(), owner.getEmail()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Rechaza aprobar una aplicación REJECTED")
        void rejectsApprovalOfRejected() {
            RentalApplicationResponse created = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());
            rentalApplicationService.rejectApplication(created.id(), "No califica", owner.getEmail());

            assertThatThrownBy(() -> rentalApplicationService.approveApplication(
                    created.id(), owner.getEmail()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Un tercero sin permiso recibe ForbiddenException")
        void outsiderGetsForbidden() {
            RentalApplicationResponse created = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            assertThatThrownBy(() -> rentalApplicationService.approveApplication(
                    created.id(), outsider.getEmail()))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // rejectApplication
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectApplication()")
    class Reject {

        @Test
        @DisplayName("Cambia el status a REJECTED y persiste el motivo en BD")
        void persistsRejectedStatusAndReason() {
            RentalApplicationResponse created = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            RentalApplicationResponse rejected = rentalApplicationService.rejectApplication(
                    created.id(), "Historial crediticio insuficiente", owner.getEmail());

            assertThat(rejected.status()).isEqualTo(RentalApplicationStatus.REJECTED);

            RentalApplication persisted = applicationRepository.findById(created.id()).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(RentalApplicationStatus.REJECTED);
            assertThat(persisted.getRejectionReason()).isEqualTo("Historial crediticio insuficiente");
            assertThat(persisted.getDecidedAt()).isNotNull();
        }

        @Test
        @DisplayName("Rechaza intentar rechazar una aplicación ya REJECTED")
        void rejectsDoubleRejection() {
            RentalApplicationResponse created = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());
            rentalApplicationService.rejectApplication(created.id(), "motivo", owner.getEmail());

            assertThatThrownBy(() -> rentalApplicationService.rejectApplication(
                    created.id(), "otro motivo", owner.getEmail()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Rechaza intentar rechazar una aplicación APPROVED")
        void rejectsRejectionOfApproved() {
            RentalApplicationResponse created = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());
            rentalApplicationService.approveApplication(created.id(), owner.getEmail());

            assertThatThrownBy(() -> rentalApplicationService.rejectApplication(
                    created.id(), "motivo tardío", owner.getEmail()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Un tercero sin permiso recibe ForbiddenException")
        void outsiderGetsForbidden() {
            RentalApplicationResponse created = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            assertThatThrownBy(() -> rentalApplicationService.rejectApplication(
                    created.id(), "motivo", outsider.getEmail()))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // convertToLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("convertToLease()")
    class ConvertToLease {

        @Test
        @DisplayName("Flujo completo: submit → approve → convertToLease persiste el Lease en BD")
        void fullFlowPersistsLease() {
            // 1. Applicant envía solicitud
            RentalApplicationResponse application = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            // 2. Owner aprueba
            rentalApplicationService.approveApplication(application.id(), owner.getEmail());

            // 3. Owner convierte a lease
            CreateLeaseRequest leaseRequest = buildLeaseRequest();
            LeaseResponse lease = rentalApplicationService.convertToLease(
                    application.id(), leaseRequest, owner.getEmail());

            // 4. Verificaciones sobre el response
            assertThat(lease.id()).isNotNull();
            assertThat(lease.status()).isEqualTo(LeaseStatus.DRAFT);
            assertThat(lease.tenantId()).isEqualTo(applicant.getId());
            assertThat(lease.landlordId()).isEqualTo(owner.getId());
            assertThat(lease.monthlyRent()).isEqualByComparingTo("1000.00");

            // 5. El lease está realmente en BD
            Optional<Lease> persisted = leaseRepository.findById(lease.id());
            assertThat(persisted).isPresent();
            assertThat(persisted.get().getPrimaryTenant().getId()).isEqualTo(applicant.getId());
            assertThat(persisted.get().getLandlord().getId()).isEqualTo(owner.getId());
        }

        @Test
        @DisplayName("El landlord del Lease es el owner de la propiedad, no el manager que invoca")
        void landlordIsPropertyOwner() {
            User admin = userRepository.save(User.builder()
                    .email("admin@test.com").name("Admin")
                    .passwordHash("hashed").role(UserRole.ADMIN).build());

            RentalApplicationResponse application = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());
            rentalApplicationService.approveApplication(application.id(), admin.getEmail());

            LeaseResponse lease = rentalApplicationService.convertToLease(
                    application.id(), buildLeaseRequest(), admin.getEmail());

            Lease persisted = leaseRepository.findById(lease.id()).orElseThrow();
            // El landlord debe ser el owner de la propiedad, no el admin
            assertThat(persisted.getLandlord().getId()).isEqualTo(owner.getId());
        }

        @Test
        @DisplayName("Rechaza convertir si la aplicación no está APPROVED")
        void rejectsNonApproved() {
            RentalApplicationResponse application = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());

            assertThatThrownBy(() -> rentalApplicationService.convertToLease(
                    application.id(), buildLeaseRequest(), owner.getEmail()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("aprobadas");
        }

        @Test
        @DisplayName("Rechaza convertir si la aplicación está REJECTED")
        void rejectsFromRejected() {
            RentalApplicationResponse application = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());
            rentalApplicationService.rejectApplication(application.id(), "No califica", owner.getEmail());

            assertThatThrownBy(() -> rentalApplicationService.convertToLease(
                    application.id(), buildLeaseRequest(), owner.getEmail()))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Un tercero sin permiso recibe ForbiddenException")
        void outsiderGetsForbidden() {
            RentalApplicationResponse application = rentalApplicationService.submitApplication(
                    buildSubmitRequest(), applicant.getEmail());
            rentalApplicationService.approveApplication(application.id(), owner.getEmail());

            assertThatThrownBy(() -> rentalApplicationService.convertToLease(
                    application.id(), buildLeaseRequest(), outsider.getEmail()))
                    .isInstanceOf(ForbiddenException.class);

            // Sin lease creado
            List<Lease> leases = leaseRepository.findByPropertyIdAndStatus(
                    property.getId(), LeaseStatus.DRAFT);
            assertThat(leases).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private CreateRentalApplicationRequest buildSubmitRequest() {
        return new CreateRentalApplicationRequest(
                property.getId(),
                "Me interesa el apartamento",
                new BigDecimal("3000.00"),
                EmploymentStatus.EMPLOYED,
                "Empresa SA",
                2,
                false,
                true);
    }

    private CreateLeaseRequest buildLeaseRequest() {
        return new CreateLeaseRequest(
                property.getId(),
                applicant.getId(),
                LeaseType.FIXED_TERM,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusMonths(12),
                new BigDecimal("1000.00"),
                new BigDecimal("2000.00"),
                BillingFrequency.MONTHLY,
                null,
                null,
                null);
    }
}
