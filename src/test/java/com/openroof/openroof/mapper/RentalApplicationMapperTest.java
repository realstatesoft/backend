package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.rental.CreateRentalApplicationRequest;
import com.openroof.openroof.dto.rental.RentalApplicationResponse;
import com.openroof.openroof.dto.rental.RentalApplicationSummaryResponse;
import com.openroof.openroof.model.enums.EmploymentStatus;
import com.openroof.openroof.model.enums.RentalApplicationStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RentalApplicationMapper")
class RentalApplicationMapperTest {

    private final RentalApplicationMapper mapper = new RentalApplicationMapper();

    // ─── toResponse ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("toResponse_withFullEntity_mapsAllFields")
    void toResponse_withFullEntity_mapsAllFields() {
        Property property = mock(Property.class);
        when(property.getId()).thenReturn(10L);
        when(property.getTitle()).thenReturn("Casa en Luque");

        User applicant = mock(User.class);
        when(applicant.getId()).thenReturn(5L);
        when(applicant.getName()).thenReturn("Juan Perez");

        LocalDateTime submitted = LocalDateTime.of(2025, 1, 10, 9, 0);
        LocalDateTime decided   = LocalDateTime.of(2025, 1, 11, 10, 0);

        RentalApplication app = RentalApplication.builder()
                .property(property)
                .applicant(applicant)
                .status(RentalApplicationStatus.SUBMITTED)
                .message("Me interesa")
                .monthlyIncome(new BigDecimal("3000000"))
                .employmentStatus(EmploymentStatus.EMPLOYED)
                .numberOfOccupants(2)
                .hasPets(true)
                .submittedAt(submitted)
                .decidedAt(decided)
                .build();

        RentalApplicationResponse dto = mapper.toResponse(app);

        assertThat(dto.propertyId()).isEqualTo(10L);
        assertThat(dto.propertyAddress()).isEqualTo("Casa en Luque");
        assertThat(dto.applicantId()).isEqualTo(5L);
        assertThat(dto.applicantName()).isEqualTo("Juan Perez");
        assertThat(dto.status()).isEqualTo(RentalApplicationStatus.SUBMITTED);
        assertThat(dto.message()).isEqualTo("Me interesa");
        assertThat(dto.monthlyIncome()).isEqualByComparingTo("3000000");
        assertThat(dto.employmentStatus()).isEqualTo(EmploymentStatus.EMPLOYED);
        assertThat(dto.numberOfOccupants()).isEqualTo(2);
        assertThat(dto.hasPets()).isTrue();
        assertThat(dto.submittedAt()).isEqualTo(submitted);
        assertThat(dto.reviewedAt()).isEqualTo(decided);
    }

    @Test
    @DisplayName("toResponse_nullProperty_returnsNullPropertyFields")
    void toResponse_nullProperty_returnsNullPropertyFields() {
        User applicant = mock(User.class);
        when(applicant.getId()).thenReturn(3L);
        when(applicant.getName()).thenReturn("Ana");

        RentalApplication app = RentalApplication.builder()
                .property(null)
                .applicant(applicant)
                .status(RentalApplicationStatus.SUBMITTED)
                .build();

        RentalApplicationResponse dto = mapper.toResponse(app);

        assertThat(dto.propertyId()).isNull();
        assertThat(dto.propertyAddress()).isNull();
        assertThat(dto.applicantId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("toResponse_nullApplicant_returnsNullApplicantFields")
    void toResponse_nullApplicant_returnsNullApplicantFields() {
        Property property = mock(Property.class);
        when(property.getId()).thenReturn(7L);
        when(property.getTitle()).thenReturn("Apto Centro");

        RentalApplication app = RentalApplication.builder()
                .property(property)
                .applicant(null)
                .status(RentalApplicationStatus.SUBMITTED)
                .build();

        RentalApplicationResponse dto = mapper.toResponse(app);

        assertThat(dto.applicantId()).isNull();
        assertThat(dto.applicantName()).isNull();
        assertThat(dto.propertyId()).isEqualTo(7L);
    }

    // ─── toSummaryResponse ──────────────────────────────────────────────────

    @Test
    @DisplayName("toSummaryResponse_withFullEntity_mapsAllFields")
    void toSummaryResponse_withFullEntity_mapsAllFields() {
        Property property = mock(Property.class);
        when(property.getTitle()).thenReturn("Loft Centro");

        User applicant = mock(User.class);
        when(applicant.getName()).thenReturn("Maria Lopez");

        LocalDateTime submitted = LocalDateTime.of(2025, 3, 1, 8, 0);

        RentalApplication app = RentalApplication.builder()
                .property(property)
                .applicant(applicant)
                .status(RentalApplicationStatus.SUBMITTED)
                .submittedAt(submitted)
                .build();

        RentalApplicationSummaryResponse dto = mapper.toSummaryResponse(app);

        assertThat(dto.propertyAddress()).isEqualTo("Loft Centro");
        assertThat(dto.applicantName()).isEqualTo("Maria Lopez");
        assertThat(dto.status()).isEqualTo(RentalApplicationStatus.SUBMITTED);
        assertThat(dto.submittedAt()).isEqualTo(submitted);
    }

    @Test
    @DisplayName("toSummaryResponse_nullPropertyAndApplicant_returnsNulls")
    void toSummaryResponse_nullPropertyAndApplicant_returnsNulls() {
        RentalApplication app = RentalApplication.builder()
                .property(null)
                .applicant(null)
                .status(RentalApplicationStatus.SUBMITTED)
                .build();

        RentalApplicationSummaryResponse dto = mapper.toSummaryResponse(app);

        assertThat(dto.propertyAddress()).isNull();
        assertThat(dto.applicantName()).isNull();
    }

    // ─── toEntity ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("toEntity_withScreeningConsentTrue_setsConsentAt")
    void toEntity_withScreeningConsentTrue_setsConsentAt() {
        CreateRentalApplicationRequest dto = new CreateRentalApplicationRequest(
                1L, "Interesado", new BigDecimal("5000000"),
                EmploymentStatus.EMPLOYED, "Empresa SA",
                List.of("ref1", "ref2"), 2, false, true);

        RentalApplication entity = mapper.toEntity(dto, mock(Property.class), mock(User.class));

        assertThat(entity.getScreeningConsent()).isTrue();
        assertThat(entity.getScreeningConsentAt()).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(RentalApplicationStatus.SUBMITTED);
        assertThat(entity.getEmployerName()).isEqualTo("Empresa SA");
        assertThat(entity.getEmploymentStatus()).isEqualTo(EmploymentStatus.EMPLOYED);
        assertThat(entity.getMonthlyIncome()).isEqualByComparingTo("5000000");
        assertThat(entity.getNumberOfOccupants()).isEqualTo(2);
        assertThat(entity.getHasPets()).isFalse();
        assertThat(entity.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("toEntity_withScreeningConsentFalse_noConsentAt")
    void toEntity_withScreeningConsentFalse_noConsentAt() {
        CreateRentalApplicationRequest dto = new CreateRentalApplicationRequest(
                1L, "Texto", new BigDecimal("5000000"),
                EmploymentStatus.EMPLOYED, null,
                List.of("r1", "r2"), 1, false, false);

        RentalApplication entity = mapper.toEntity(dto, mock(Property.class), mock(User.class));

        assertThat(entity.getScreeningConsent()).isFalse();
        assertThat(entity.getScreeningConsentAt()).isNull();
    }

    @Test
    @DisplayName("toEntity_withNullReferences_tenantReferencesNull")
    void toEntity_withNullReferences_tenantReferencesNull() {
        CreateRentalApplicationRequest dto = new CreateRentalApplicationRequest(
                1L, "Texto", new BigDecimal("3000000"),
                EmploymentStatus.UNEMPLOYED, null,
                null, null, null, true);

        RentalApplication entity = mapper.toEntity(dto, mock(Property.class), mock(User.class));

        assertThat(entity.getTenantReferences()).isNull();
    }

    @Test
    @DisplayName("toEntity_withReferences_wrapsEachAsContactMap")
    void toEntity_withReferences_wrapsEachAsContactMap() {
        CreateRentalApplicationRequest dto = new CreateRentalApplicationRequest(
                1L, "Texto", new BigDecimal("3000000"),
                EmploymentStatus.EMPLOYED, "Acme Corp",
                List.of("555-1111", "555-2222"), null, null, true);

        RentalApplication entity = mapper.toEntity(dto, mock(Property.class), mock(User.class));

        assertThat(entity.getTenantReferences()).hasSize(2);
        assertThat(entity.getTenantReferences().get(0)).containsKey("contact");
        assertThat(entity.getTenantReferences().get(0).get("contact")).isEqualTo("555-1111");
        assertThat(entity.getTenantReferences().get(1).get("contact")).isEqualTo("555-2222");
    }
}
