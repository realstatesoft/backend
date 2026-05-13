package com.openroof.openroof.dto.rental;

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

@DisplayName("CreateRentalApplicationRequest — validación")
class CreateRentalApplicationRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        if (factory != null) factory.close();
    }

    private CreateRentalApplicationRequest valid() {
        return new CreateRentalApplicationRequest(
                1L,
                "Me interesa",
                new BigDecimal("3000.00"),
                EmploymentStatus.EMPLOYED,
                "Empresa SA",
                List.of("ref1", "ref2"),
                2,
                false,
                true);
    }

    private Set<ConstraintViolation<CreateRentalApplicationRequest>> validate(CreateRentalApplicationRequest req) {
        return validator.validate(req);
    }

    @Test
    @DisplayName("DTO completamente válido no produce violaciones")
    void validDtoHasNoViolations() {
        assertThat(validate(valid())).isEmpty();
    }

    @Test
    @DisplayName("employerName vacío con EMPLOYED es inválido")
    void employerNameBlankWithEmployedIsInvalid() {
        CreateRentalApplicationRequest req = new CreateRentalApplicationRequest(
                1L, "Me interesa", new BigDecimal("3000.00"),
                EmploymentStatus.EMPLOYED, "  ",
                List.of("ref1", "ref2"), 2, false, true);

        Set<ConstraintViolation<CreateRentalApplicationRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString())
                .contains("employerName");
    }

    @Test
    @DisplayName("employerName null con EMPLOYED es inválido")
    void employerNameNullWithEmployedIsInvalid() {
        CreateRentalApplicationRequest req = new CreateRentalApplicationRequest(
                1L, "Me interesa", new BigDecimal("3000.00"),
                EmploymentStatus.EMPLOYED, null,
                List.of("ref1", "ref2"), 2, false, true);

        Set<ConstraintViolation<CreateRentalApplicationRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString())
                .contains("employerName");
    }

    @Test
    @DisplayName("employerName vacío con SELF_EMPLOYED es válido")
    void employerNameBlankWithSelfEmployedIsValid() {
        CreateRentalApplicationRequest req = new CreateRentalApplicationRequest(
                1L, "Me interesa", new BigDecimal("3000.00"),
                EmploymentStatus.SELF_EMPLOYED, null,
                List.of("ref1", "ref2"), 2, false, true);

        assertThat(validate(req)).isEmpty();
    }

    @Test
    @DisplayName("references con menos de 2 elementos es inválido")
    void referencesUnderMinimumIsInvalid() {
        CreateRentalApplicationRequest req = new CreateRentalApplicationRequest(
                1L, "Me interesa", new BigDecimal("3000.00"),
                EmploymentStatus.EMPLOYED, "Empresa SA",
                List.of("solo-una"), 2, false, true);

        Set<ConstraintViolation<CreateRentalApplicationRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString())
                .contains("references");
    }

    @Test
    @DisplayName("references null es inválido")
    void referencesNullIsInvalid() {
        CreateRentalApplicationRequest req = new CreateRentalApplicationRequest(
                1L, "Me interesa", new BigDecimal("3000.00"),
                EmploymentStatus.EMPLOYED, "Empresa SA",
                null, 2, false, true);

        Set<ConstraintViolation<CreateRentalApplicationRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString())
                .contains("references");
    }

    @Test
    @DisplayName("monthlyIncome = 0 es inválido")
    void monthlyIncomeZeroIsInvalid() {
        CreateRentalApplicationRequest req = new CreateRentalApplicationRequest(
                1L, "Me interesa", BigDecimal.ZERO,
                EmploymentStatus.EMPLOYED, "Empresa SA",
                List.of("ref1", "ref2"), 2, false, true);

        Set<ConstraintViolation<CreateRentalApplicationRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString())
                .contains("monthlyIncome");
    }

    @Test
    @DisplayName("screeningConsent = false es inválido")
    void screeningConsentFalseIsInvalid() {
        CreateRentalApplicationRequest req = new CreateRentalApplicationRequest(
                1L, "Me interesa", new BigDecimal("3000.00"),
                EmploymentStatus.EMPLOYED, "Empresa SA",
                List.of("ref1", "ref2"), 2, false, false);

        Set<ConstraintViolation<CreateRentalApplicationRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString())
                .contains("screeningConsent");
    }

    @Test
    @DisplayName("screeningConsent null es inválido")
    void screeningConsentNullIsInvalid() {
        CreateRentalApplicationRequest req = new CreateRentalApplicationRequest(
                1L, "Me interesa", new BigDecimal("3000.00"),
                EmploymentStatus.EMPLOYED, "Empresa SA",
                List.of("ref1", "ref2"), 2, false, null);

        Set<ConstraintViolation<CreateRentalApplicationRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString())
                .contains("screeningConsent");
    }
}
