package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.CreateRentalApplicationRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.RentalApplicationResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.LeaseMapper;
import com.openroof.openroof.mapper.RentalApplicationMapper;
import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.DepositStatus;
import com.openroof.openroof.model.enums.EmploymentStatus;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
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
import com.openroof.openroof.security.PropertySecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalApplicationServiceTest {

    @Mock private RentalApplicationRepository applicationRepository;
    @Mock private LeaseRepository leaseRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PropertySecurity propertySecurity;
    @Mock private NotificationService notificationService;
    @Mock private RentalApplicationMapper applicationMapper;
    @Mock private LeaseMapper leaseMapper;

    private RentalApplicationService service;

    private User applicant;
    private User owner;
    private Property property;

    @BeforeEach
    void setUp() {
        service = new RentalApplicationService(
                applicationRepository, leaseRepository, propertyRepository,
                userRepository, propertySecurity, notificationService,
                applicationMapper, leaseMapper);

        applicant = User.builder().name("Inquilino").email("tenant@test.com").role(UserRole.USER).build();
        applicant.setId(10L);

        owner = User.builder().name("Propietario").email("owner@test.com").role(UserRole.USER).build();
        owner.setId(20L);

        property = Property.builder()
                .title("Apartamento Centro")
                .owner(owner)
                .rentAmount(new BigDecimal("1000.00"))
                .build();
        property.setId(100L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // submitApplication
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitApplication()")
    class Submit {

        private final CreateRentalApplicationRequest dto = new CreateRentalApplicationRequest(
                100L, "Me interesa el apartamento",
                new BigDecimal("3000.00"), EmploymentStatus.EMPLOYED,
                "Empresa SA", java.util.List.of("ref1@x.com", "ref2@x.com"),
                2, false, true);

        @Test
        @DisplayName("Crea la aplicación con incomeToRentRatio calculado")
        void createsApplicationWithRatio() {
            RentalApplication entity = baseApplication(RentalApplicationStatus.SUBMITTED);
            RentalApplicationResponse expectedResponse = sampleApplicationResponse(1L, RentalApplicationStatus.SUBMITTED);

            when(userRepository.findByEmail("tenant@test.com")).thenReturn(Optional.of(applicant));
            when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));
            when(applicationRepository.existsByPropertyIdAndApplicantIdAndStatusIn(
                    eq(100L), eq(10L), anyCollection())).thenReturn(false);
            when(applicationMapper.toEntity(dto, property, applicant)).thenReturn(entity);
            when(applicationRepository.saveAndFlush(entity)).thenReturn(entity);
            when(applicationMapper.toResponse(entity)).thenReturn(expectedResponse);

            RentalApplicationResponse result = service.submitApplication(dto, "tenant@test.com");

            // incomeToRentRatio = 3000 / 1000 = 3.00
            assertThat(entity.getIncomeToRentRatio()).isEqualByComparingTo("3.00");
            assertThat(result.status()).isEqualTo(RentalApplicationStatus.SUBMITTED);
            verify(applicationRepository).saveAndFlush(entity);
            verify(notificationService).notifyApplicationSubmitted(entity);
        }

        @Test
        @DisplayName("Deja incomeToRentRatio en null si rentAmount es null")
        void nullRatioWhenRentAmountIsNull() {
            property.setRentAmount(null);
            RentalApplication entity = baseApplication(RentalApplicationStatus.SUBMITTED);

            when(userRepository.findByEmail("tenant@test.com")).thenReturn(Optional.of(applicant));
            when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));
            when(applicationRepository.existsByPropertyIdAndApplicantIdAndStatusIn(
                    eq(100L), eq(10L), anyCollection())).thenReturn(false);
            when(applicationMapper.toEntity(dto, property, applicant)).thenReturn(entity);
            when(applicationRepository.saveAndFlush(entity)).thenReturn(entity);
            when(applicationMapper.toResponse(entity)).thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.SUBMITTED));

            service.submitApplication(dto, "tenant@test.com");

            assertThat(entity.getIncomeToRentRatio()).isNull();
        }

        @Test
        @DisplayName("Deja incomeToRentRatio en null si rentAmount es cero")
        void nullRatioWhenRentAmountIsZero() {
            property.setRentAmount(BigDecimal.ZERO);
            RentalApplication entity = baseApplication(RentalApplicationStatus.SUBMITTED);

            when(userRepository.findByEmail("tenant@test.com")).thenReturn(Optional.of(applicant));
            when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));
            when(applicationRepository.existsByPropertyIdAndApplicantIdAndStatusIn(
                    eq(100L), eq(10L), anyCollection())).thenReturn(false);
            when(applicationMapper.toEntity(dto, property, applicant)).thenReturn(entity);
            when(applicationRepository.saveAndFlush(entity)).thenReturn(entity);
            when(applicationMapper.toResponse(entity)).thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.SUBMITTED));

            service.submitApplication(dto, "tenant@test.com");

            assertThat(entity.getIncomeToRentRatio()).isNull();
        }

        @Test
        @DisplayName("Rechaza si la propiedad no existe")
        void rejectsPropertyNotFound() {
            when(userRepository.findByEmail("tenant@test.com")).thenReturn(Optional.of(applicant));
            when(propertyRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submitApplication(dto, "tenant@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(applicationRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Rechaza si el applicant es el owner de la propiedad")
        void rejectsOwnerAsApplicant() {
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));

            assertThatThrownBy(() -> service.submitApplication(dto, "owner@test.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("propia");
            verify(applicationRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Rechaza si ya existe una aplicación activa para la misma propiedad")
        void rejectsDuplicateActiveApplication() {
            when(userRepository.findByEmail("tenant@test.com")).thenReturn(Optional.of(applicant));
            when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));
            when(applicationRepository.existsByPropertyIdAndApplicantIdAndStatusIn(
                    eq(100L), eq(10L), anyCollection())).thenReturn(true);

            assertThatThrownBy(() -> service.submitApplication(dto, "tenant@test.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("activa");
            verify(applicationRepository, never()).saveAndFlush(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getApplication
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getApplication()")
    class GetApplication {

        @Test
        @DisplayName("El propio applicant puede ver su aplicación")
        void applicantCanView() {
            RentalApplication app = baseApplication(RentalApplicationStatus.SUBMITTED);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("tenant@test.com")).thenReturn(Optional.of(applicant));
            when(applicationMapper.toResponse(app)).thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.SUBMITTED));

            RentalApplicationResponse result = service.getApplication(1L, "tenant@test.com");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Un manager de la propiedad puede ver la aplicación")
        void managerCanView() {
            RentalApplication app = baseApplication(RentalApplicationStatus.SUBMITTED);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);
            when(applicationMapper.toResponse(app)).thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.SUBMITTED));

            RentalApplicationResponse result = service.getApplication(1L, "owner@test.com");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Un usuario sin relación recibe ForbiddenException")
        void outsiderGetsForbidden() {
            User outsider = User.builder().email("other@test.com").role(UserRole.USER).build();
            outsider.setId(99L);
            RentalApplication app = baseApplication(RentalApplicationStatus.SUBMITTED);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(outsider));
            when(propertySecurity.canModify(100L, outsider)).thenReturn(false);

            assertThatThrownBy(() -> service.getApplication(1L, "other@test.com"))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si la aplicación no existe")
        void notFound() {
            when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getApplication(99L, "tenant@test.com"))
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
        @DisplayName("Manager obtiene lista paginada sin filtro de status")
        void managerListsAll() {
            Pageable pageable = PageRequest.of(0, 10);
            RentalApplication app = baseApplication(RentalApplicationStatus.SUBMITTED);
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);
            when(applicationRepository.findByPropertyIdFiltered(100L, null, pageable))
                    .thenReturn(new PageImpl<>(List.of(app)));
            when(applicationMapper.toResponse(app)).thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.SUBMITTED));

            var page = service.listApplications(100L, null, pageable, "owner@test.com");

            assertThat(page.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Manager filtra por status APPROVED")
        void managerFiltersApproved() {
            Pageable pageable = PageRequest.of(0, 10);
            RentalApplication app = baseApplication(RentalApplicationStatus.APPROVED);
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);
            when(applicationRepository.findByPropertyIdFiltered(100L, RentalApplicationStatus.APPROVED, pageable))
                    .thenReturn(new PageImpl<>(List.of(app)));
            when(applicationMapper.toResponse(app)).thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.APPROVED));

            var page = service.listApplications(100L, RentalApplicationStatus.APPROVED, pageable, "owner@test.com");

            assertThat(page.getContent().get(0).status()).isEqualTo(RentalApplicationStatus.APPROVED);
        }

        @Test
        @DisplayName("Usuario sin permiso recibe ForbiddenException")
        void unauthorizedGetsForbidden() {
            when(userRepository.findByEmail("tenant@test.com")).thenReturn(Optional.of(applicant));
            when(propertySecurity.canModify(100L, applicant)).thenReturn(false);

            assertThatThrownBy(() -> service.listApplications(100L, null, PageRequest.of(0, 10), "tenant@test.com"))
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
        @DisplayName("Pasa la aplicación a APPROVED y notifica al applicant")
        void approvesAndNotifies() {
            RentalApplication app = baseApplication(RentalApplicationStatus.SUBMITTED);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);
            when(applicationRepository.saveAndFlush(app)).thenReturn(app);
            when(applicationMapper.toResponse(app)).thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.APPROVED));

            RentalApplicationResponse result = service.approveApplication(1L, "owner@test.com");

            assertThat(app.getStatus()).isEqualTo(RentalApplicationStatus.APPROVED);
            assertThat(app.getDecidedAt()).isNotNull();
            assertThat(result.status()).isEqualTo(RentalApplicationStatus.APPROVED);
            verify(notificationService).notifyApplicationApproved(app);
        }

        @Test
        @DisplayName("Aprueba también desde UNDER_REVIEW y SCREENING_IN_PROGRESS")
        void approvesFromUnderReview() {
            RentalApplication app = baseApplication(RentalApplicationStatus.UNDER_REVIEW);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);
            when(applicationRepository.saveAndFlush(app)).thenReturn(app);
            when(applicationMapper.toResponse(app)).thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.APPROVED));

            service.approveApplication(1L, "owner@test.com");

            assertThat(app.getStatus()).isEqualTo(RentalApplicationStatus.APPROVED);
        }

        @Test
        @DisplayName("Rechaza si la aplicación ya está APPROVED")
        void rejectsAlreadyApproved() {
            RentalApplication app = baseApplication(RentalApplicationStatus.APPROVED);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);

            assertThatThrownBy(() -> service.approveApplication(1L, "owner@test.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("Rechaza si la aplicación está REJECTED")
        void rejectsAlreadyRejected() {
            RentalApplication app = baseApplication(RentalApplicationStatus.REJECTED);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);

            assertThatThrownBy(() -> service.approveApplication(1L, "owner@test.com"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Usuario sin permiso recibe ForbiddenException")
        void unauthorizedGetsForbidden() {
            RentalApplication app = baseApplication(RentalApplicationStatus.SUBMITTED);
            User outsider = User.builder().email("other@test.com").role(UserRole.USER).build();
            outsider.setId(99L);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(outsider));
            when(propertySecurity.canModify(100L, outsider)).thenReturn(false);

            assertThatThrownBy(() -> service.approveApplication(1L, "other@test.com"))
                    .isInstanceOf(ForbiddenException.class);
            verify(applicationRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("La notificación se dispara con la aplicación correcta")
        void notificationDispatchedWithApplication() {
            RentalApplication app = baseApplication(RentalApplicationStatus.SUBMITTED);
            app.setId(1L);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);
            when(applicationRepository.saveAndFlush(app)).thenReturn(app);
            when(applicationMapper.toResponse(app)).thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.APPROVED));

            service.approveApplication(1L, "owner@test.com");

            verify(notificationService).notifyApplicationApproved(app);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // rejectApplication
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectApplication()")
    class Reject {

        @Test
        @DisplayName("Pasa la aplicación a REJECTED con motivo y notifica al applicant")
        void rejectsAndNotifies() {
            RentalApplication app = baseApplication(RentalApplicationStatus.SUBMITTED);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);
            when(applicationRepository.saveAndFlush(app)).thenReturn(app);
            when(applicationMapper.toResponse(app)).thenReturn(sampleApplicationResponse(1L, RentalApplicationStatus.REJECTED));

            RentalApplicationResponse result = service.rejectApplication(1L, "Historial insuficiente", "owner@test.com");

            assertThat(app.getStatus()).isEqualTo(RentalApplicationStatus.REJECTED);
            assertThat(app.getRejectionReason()).isEqualTo("Historial insuficiente");
            assertThat(app.getDecidedAt()).isNotNull();
            assertThat(result.status()).isEqualTo(RentalApplicationStatus.REJECTED);
            verify(notificationService).notifyApplicationRejected(app, "Historial insuficiente");
        }

        @Test
        @DisplayName("Rechaza si la aplicación ya está REJECTED")
        void rejectsAlreadyRejected() {
            RentalApplication app = baseApplication(RentalApplicationStatus.REJECTED);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);

            assertThatThrownBy(() -> service.rejectApplication(1L, "motivo", "owner@test.com"))
                    .isInstanceOf(BadRequestException.class);
            verify(applicationRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Rechaza si la aplicación ya está APPROVED")
        void rejectsApproved() {
            RentalApplication app = baseApplication(RentalApplicationStatus.APPROVED);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);

            assertThatThrownBy(() -> service.rejectApplication(1L, "motivo", "owner@test.com"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Usuario sin permiso recibe ForbiddenException")
        void unauthorizedGetsForbidden() {
            RentalApplication app = baseApplication(RentalApplicationStatus.SUBMITTED);
            User outsider = User.builder().email("other@test.com").role(UserRole.USER).build();
            outsider.setId(99L);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(outsider));
            when(propertySecurity.canModify(100L, outsider)).thenReturn(false);

            assertThatThrownBy(() -> service.rejectApplication(1L, "motivo", "other@test.com"))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // convertToLease
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("convertToLease()")
    class ConvertToLease {

        private final CreateLeaseRequest leaseDto = new CreateLeaseRequest(
                100L, 10L, LeaseType.FIXED_TERM,
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(12),
                new BigDecimal("1000.00"), new BigDecimal("2000.00"),
                BillingFrequency.MONTHLY, null, null, null);

        @Test
        @DisplayName("Crea un Lease desde una aplicación APPROVED y notifica al applicant")
        void createsLeaseFromApprovedApplication() {
            RentalApplication app = baseApplication(RentalApplicationStatus.APPROVED);
            Lease lease = baseLease();
            LeaseResponse expectedLeaseResponse = sampleLeaseResponse(50L);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);
            when(leaseMapper.toEntity(leaseDto, property, applicant, owner)).thenReturn(lease);
            when(leaseRepository.saveAndFlush(lease)).thenReturn(lease);
            when(leaseMapper.toResponse(lease)).thenReturn(expectedLeaseResponse);

            LeaseResponse result = service.convertToLease(1L, leaseDto, "owner@test.com");

            assertThat(result.id()).isEqualTo(50L);
            verify(leaseRepository).saveAndFlush(lease);
        }

        @Test
        @DisplayName("Rechaza si la aplicación no está APPROVED")
        void rejectsNonApproved() {
            RentalApplication app = baseApplication(RentalApplicationStatus.SUBMITTED);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);

            assertThatThrownBy(() -> service.convertToLease(1L, leaseDto, "owner@test.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("aprobadas");
            verify(leaseRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Rechaza si la aplicación está REJECTED")
        void rejectsFromRejected() {
            RentalApplication app = baseApplication(RentalApplicationStatus.REJECTED);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);

            assertThatThrownBy(() -> service.convertToLease(1L, leaseDto, "owner@test.com"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Usuario sin permiso recibe ForbiddenException")
        void unauthorizedGetsForbidden() {
            RentalApplication app = baseApplication(RentalApplicationStatus.APPROVED);
            User outsider = User.builder().email("other@test.com").role(UserRole.USER).build();
            outsider.setId(99L);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(outsider));
            when(propertySecurity.canModify(100L, outsider)).thenReturn(false);

            assertThatThrownBy(() -> service.convertToLease(1L, leaseDto, "other@test.com"))
                    .isInstanceOf(ForbiddenException.class);
            verify(leaseRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("El Lease se crea con el owner de la propiedad como landlord")
        void leaseUsesPropertyOwnerAsLandlord() {
            RentalApplication app = baseApplication(RentalApplicationStatus.APPROVED);
            Lease lease = baseLease();

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
            when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
            when(propertySecurity.canModify(100L, owner)).thenReturn(true);
            when(leaseMapper.toEntity(leaseDto, property, applicant, owner)).thenReturn(lease);
            when(leaseRepository.saveAndFlush(lease)).thenReturn(lease);
            when(leaseMapper.toResponse(lease)).thenReturn(sampleLeaseResponse(50L));

            service.convertToLease(1L, leaseDto, "owner@test.com");

            verify(leaseMapper).toEntity(leaseDto, property, applicant, owner);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private RentalApplication baseApplication(RentalApplicationStatus status) {
        RentalApplication app = RentalApplication.builder()
                .property(property)
                .applicant(applicant)
                .status(status)
                .monthlyIncome(new BigDecimal("3000.00"))
                .employmentStatus(EmploymentStatus.EMPLOYED)
                .message("Me interesa")
                .submittedAt(LocalDateTime.now())
                .build();
        app.setId(1L);
        app.setCreatedAt(LocalDateTime.now());
        return app;
    }

    private Lease baseLease() {
        Lease lease = Lease.builder()
                .property(property)
                .primaryTenant(applicant)
                .landlord(owner)
                .leaseType(LeaseType.FIXED_TERM)
                .status(LeaseStatus.DRAFT)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusMonths(12))
                .monthlyRent(new BigDecimal("1000.00"))
                .securityDeposit(new BigDecimal("2000.00"))
                .billingFrequency(BillingFrequency.MONTHLY)
                .build();
        lease.setId(50L);
        return lease;
    }

    private RentalApplicationResponse sampleApplicationResponse(Long id, RentalApplicationStatus status) {
        return new RentalApplicationResponse(
                id, 100L, "Apartamento Centro",
                10L, "Inquilino",
                status, "Me interesa",
                new BigDecimal("3000.00"), EmploymentStatus.EMPLOYED,
                2, false,
                LocalDateTime.now(), null, LocalDateTime.now());
    }

    private LeaseResponse sampleLeaseResponse(Long id) {
        return new LeaseResponse(
                id, 100L, "Apartamento Centro",
                20L, 10L, "Inquilino",
                LeaseType.FIXED_TERM, LeaseStatus.DRAFT,
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(12),
                new BigDecimal("1000.00"), new BigDecimal("2000.00"),
                DepositStatus.HELD, BillingFrequency.MONTHLY,
                null, null, LocalDateTime.now());
    }
}
