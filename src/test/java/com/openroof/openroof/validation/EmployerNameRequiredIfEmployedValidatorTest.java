package com.openroof.openroof.validation;

import com.openroof.openroof.dto.rental.CreateRentalApplicationRequest;
import com.openroof.openroof.model.enums.EmploymentStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@code EmployerNameRequiredIfEmployedValidator}.
 */
@DisplayName("EmployerNameRequiredIfEmployedValidator")
class EmployerNameRequiredIfEmployedValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private CreateRentalApplicationRequest buildRequest(EmploymentStatus status, String employerName) {
        return new CreateRentalApplicationRequest(
                1L,
                "Interested in renting",
                new BigDecimal("3000.00"),
                status,
                employerName,
                List.of("Ref A", "Ref B"),
                2,
                false,
                true
        );
    }

    // ─── EMPLOYED branch ─────────────────────────────────────────────────────

    /**
     * EMPLOYED with a non-blank employerName → valid.
     */
    @Test
    @DisplayName("EMPLOYED with a non-blank employerName → valid")
    void employed_withEmployerName_isValid() {
        CreateRentalApplicationRequest req = buildRequest(EmploymentStatus.EMPLOYED, "Empresa ABC");

        Set<ConstraintViolation<CreateRentalApplicationRequest>> violations =
                validator.validate(req);

        assertThat(violations).filteredOn(v -> v.getPropertyPath().toString().equals("employerName"))
                .isEmpty();
    }

    /**
     * EMPLOYED with null employerName → violation on employerName.
     */
    @Test
    @DisplayName("EMPLOYED with null employerName → violation on employerName")
    void employed_withNullEmployerName_violatesConstraint() {
        CreateRentalApplicationRequest req = buildRequest(EmploymentStatus.EMPLOYED, null);

        Set<ConstraintViolation<CreateRentalApplicationRequest>> violations =
                validator.validate(req);

        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("employerName"))
                .isNotEmpty();
    }

    /**
     * EMPLOYED with blank employerName → violation on employerName.
     */
    @Test
    @DisplayName("EMPLOYED with blank employerName → violation on employerName")
    void employed_withBlankEmployerName_violatesConstraint() {
        CreateRentalApplicationRequest req = buildRequest(EmploymentStatus.EMPLOYED, "   ");

        Set<ConstraintViolation<CreateRentalApplicationRequest>> violations =
                validator.validate(req);

        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("employerName"))
                .isNotEmpty();
    }

    // ─── Non-EMPLOYED branches ───────────────────────────────────────────────

    /**
     * UNEMPLOYED with null employerName → no employerName violation.
     */
    @Test
    @DisplayName("UNEMPLOYED with null employerName → no employerName violation")
    void unemployed_withNullEmployerName_isValid() {
        CreateRentalApplicationRequest req = buildRequest(EmploymentStatus.UNEMPLOYED, null);

        Set<ConstraintViolation<CreateRentalApplicationRequest>> violations =
                validator.validate(req);

        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("employerName"))
                .isEmpty();
    }

    /**
     * SELF_EMPLOYED with null employerName → no employerName violation.
     */
    @Test
    @DisplayName("SELF_EMPLOYED with null employerName → no employerName violation")
    void selfEmployed_withNullEmployerName_isValid() {
        CreateRentalApplicationRequest req = buildRequest(EmploymentStatus.SELF_EMPLOYED, null);

        Set<ConstraintViolation<CreateRentalApplicationRequest>> violations =
                validator.validate(req);

        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("employerName"))
                .isEmpty();
    }

    /**
     * STUDENT with null employerName → no employerName violation.
     */
    @Test
    @DisplayName("STUDENT with null employerName → no employerName violation")
    void student_withNullEmployerName_isValid() {
        CreateRentalApplicationRequest req = buildRequest(EmploymentStatus.STUDENT, null);

        Set<ConstraintViolation<CreateRentalApplicationRequest>> violations =
                validator.validate(req);

        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("employerName"))
                .isEmpty();
    }
}
