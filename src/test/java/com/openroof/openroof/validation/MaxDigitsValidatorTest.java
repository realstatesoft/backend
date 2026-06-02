package com.openroof.openroof.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MaxDigitsValidator Unit Tests")
class MaxDigitsValidatorTest {

    private MaxDigitsValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new MaxDigitsValidator();
        MaxDigits annotation = Mockito.mock(MaxDigits.class);
        Mockito.when(annotation.integer()).thenReturn(20);
        validator.initialize(annotation);
        context = Mockito.mock(ConstraintValidatorContext.class);
    }

    @Test
    @DisplayName("isValid - Con valor null debería retornar true")
    void isValid_withNullValue_returnsTrue() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    @DisplayName("isValid - Con BigDecimal de <= 20 dígitos enteros debería retornar true")
    void isValid_withValidBigDecimal_returnsTrue() {
        BigDecimal bd1 = new BigDecimal("12345678901234567890"); // 20 digits
        BigDecimal bd2 = new BigDecimal("12345678901234567890.123"); // 20 digits, with decimals
        BigDecimal bd3 = new BigDecimal("-12345678901234567890"); // negative, 20 digits
        BigDecimal bd4 = new BigDecimal("0"); // 1 digit

        assertTrue(validator.isValid(bd1, context));
        assertTrue(validator.isValid(bd2, context));
        assertTrue(validator.isValid(bd3, context));
        assertTrue(validator.isValid(bd4, context));
    }

    @Test
    @DisplayName("isValid - Con BigDecimal de > 20 dígitos enteros debería retornar false")
    void isValid_withInvalidBigDecimal_returnsFalse() {
        BigDecimal bd1 = new BigDecimal("123456789012345678901"); // 21 digits
        BigDecimal bd2 = new BigDecimal("-123456789012345678901.45"); // negative, 21 digits

        assertFalse(validator.isValid(bd1, context));
        assertFalse(validator.isValid(bd2, context));
    }

    @Test
    @DisplayName("isValid - Con Integer de <= 20 dígitos debería retornar true")
    void isValid_withValidInteger_returnsTrue() {
        assertTrue(validator.isValid(1234567890, context));
        assertTrue(validator.isValid(-1234567890, context));
    }

    @Test
    @DisplayName("isValid - Con Long de <= 20 dígitos debería retornar true")
    void isValid_withValidLong_returnsTrue() {
        assertTrue(validator.isValid(1234567890123456789L, context)); // 19 digits
    }

    @Test
    @DisplayName("isValid - Con Double de <= 20 dígitos enteros debería retornar true")
    void isValid_withValidDouble_returnsTrue() {
        assertTrue(validator.isValid(1234567890.1234, context));
        assertTrue(validator.isValid(0.0001, context));
    }

    @Test
    @DisplayName("isValid - Con String numérico de <= 20 dígitos enteros debería retornar true")
    void isValid_withValidString_returnsTrue() {
        assertTrue(validator.isValid("12345678901234567890", context));
    }

    @Test
    @DisplayName("isValid - Con String numérico de > 20 dígitos enteros debería retornar false")
    void isValid_withInvalidString_returnsFalse() {
        assertFalse(validator.isValid("123456789012345678901", context));
    }

    @Test
    @DisplayName("isValid - Con String no numérico debería retornar false")
    void isValid_withNonNumericString_returnsFalse() {
        assertFalse(validator.isValid("abc", context));
    }
}
