package com.openroof.openroof.dto.offer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OfferRequestDTO — validación")
class OfferRequestDTOValidationTest {

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

    private OfferRequestDTO valid() {
        return OfferRequestDTO.builder()
                .propertyId(1L)
                .amount(new BigDecimal("150000.00"))
                .message("Mi oferta para esta propiedad.")
                .build();
    }

    private Set<ConstraintViolation<OfferRequestDTO>> validate(OfferRequestDTO dto) {
        return validator.validate(dto);
    }

    @Test
    @DisplayName("DTO completamente válido no produce violaciones")
    void validDtoHasNoViolations() {
        assertThat(validate(valid())).isEmpty();
    }

    @Test
    @DisplayName("propertyId null es inválido")
    void propertyIdNullIsInvalid() {
        OfferRequestDTO dto = valid();
        dto.setPropertyId(null);
        
        Set<ConstraintViolation<OfferRequestDTO>> violations = validate(dto);
        assertThat(violations).extracting(c -> c.getPropertyPath().toString()).contains("propertyId");
        assertThat(violations).extracting(ConstraintViolation::getMessage).contains("El ID de la propiedad es obligatorio");
    }

    @Test
    @DisplayName("amount null es inválido")
    void amountNullIsInvalid() {
        OfferRequestDTO dto = valid();
        dto.setAmount(null);
        
        Set<ConstraintViolation<OfferRequestDTO>> violations = validate(dto);
        assertThat(violations).extracting(c -> c.getPropertyPath().toString()).contains("amount");
        assertThat(violations).extracting(ConstraintViolation::getMessage).contains("El monto es obligatorio");
    }

    @Test
    @DisplayName("amount menor o igual a cero es inválido")
    void amountZeroOrNegativeIsInvalid() {
        OfferRequestDTO dto1 = valid();
        dto1.setAmount(BigDecimal.ZERO);

        OfferRequestDTO dto2 = valid();
        dto2.setAmount(new BigDecimal("-100.00"));
        
        Set<ConstraintViolation<OfferRequestDTO>> violations1 = validate(dto1);
        assertThat(violations1).extracting(c -> c.getPropertyPath().toString()).contains("amount");
        assertThat(violations1).extracting(ConstraintViolation::getMessage).contains("El monto debe ser mayor a 0");

        Set<ConstraintViolation<OfferRequestDTO>> violations2 = validate(dto2);
        assertThat(violations2).extracting(c -> c.getPropertyPath().toString()).contains("amount");
        assertThat(violations2).extracting(ConstraintViolation::getMessage).contains("El monto debe ser mayor a 0");
    }

    @Test
    @DisplayName("amount con más de 10 dígitos enteros es inválido")
    void amountWithTooManyIntegerDigitsIsInvalid() {
        OfferRequestDTO dto = valid();
        // 11 digits in the integer part
        dto.setAmount(new BigDecimal("12345678901.00"));
        
        Set<ConstraintViolation<OfferRequestDTO>> violations = validate(dto);
        assertThat(violations).extracting(c -> c.getPropertyPath().toString()).contains("amount");
        assertThat(violations).extracting(ConstraintViolation::getMessage).contains("El monto excede el límite permitido");
    }

    @Test
    @DisplayName("amount con más de 2 decimales es inválido")
    void amountWithTooManyDecimalsIsInvalid() {
        OfferRequestDTO dto = valid();
        // 3 decimal places
        dto.setAmount(new BigDecimal("150000.123"));
        
        Set<ConstraintViolation<OfferRequestDTO>> violations = validate(dto);
        assertThat(violations).extracting(c -> c.getPropertyPath().toString()).contains("amount");
        assertThat(violations).extracting(ConstraintViolation::getMessage).contains("El monto excede el límite permitido");
    }

    @Test
    @DisplayName("amount con exactamente 10 dígitos enteros y 2 decimales es válido")
    void amountAtLimitIsValid() {
        OfferRequestDTO dto = valid();
        dto.setAmount(new BigDecimal("9999999999.99"));
        assertThat(validate(dto)).isEmpty();
    }
}
