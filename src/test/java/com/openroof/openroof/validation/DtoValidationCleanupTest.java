package com.openroof.openroof.validation;

import com.openroof.openroof.dto.lead.CreateLeadFromWizardRequest;
import com.openroof.openroof.dto.payment.PaymentRequest;
import com.openroof.openroof.dto.subscription.SubscriptionPlanRequest;
import com.openroof.openroof.model.enums.PaymentType;
import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.enums.PropertyType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTO Cleaned Validations Test")
class DtoValidationCleanupTest {

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

    @Test
    @DisplayName("CreateLeadFromWizardRequest validation with null or typical integer fields is valid")
    void createLeadFromWizardRequest_is_valid() {
        CreateLeadFromWizardRequest request = new CreateLeadFromWizardRequest(
                1L,
                "Juan",
                "Perez",
                "+54911223344",
                "juan.perez@example.com",
                "Av. Libertador 1200",
                -34.6037,
                -58.3816,
                PropertyType.APARTMENT,
                PropertyCategory.RENT,
                "100", "80", "2010",
                3, // bedrooms
                1, // halfBath
                1, // threeQuarterBath
                10, // floors
                true, // hasPool
                2, // parkingSpaces
                true, true, "20",
                "GOOD", "EXCELLENT", "GOOD", "GOOD", "GRANITE",
                false, Collections.emptyList(),
                "OWNER", "IMMEDIATE", Collections.emptyMap()
        );

        Set<ConstraintViolation<CreateLeadFromWizardRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("PaymentRequest validation with standard valid amount is valid")
    void paymentRequest_is_valid() {
        PaymentRequest request = new PaymentRequest(
                PaymentType.OTHER,
                new BigDecimal("1500.50"),
                "Pago de alquiler mensual",
                null
        );

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("PaymentRequest amount with too many decimals is invalid via @Digits constraint")
    void paymentRequest_invalid_decimals() {
        PaymentRequest request = new PaymentRequest(
                PaymentType.OTHER,
                new BigDecimal("1500.555"),
                "Pago de alquiler mensual",
                null
        );

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertThat(violations).extracting(c -> c.getPropertyPath().toString()).contains("amount");
    }

    @Test
    @DisplayName("SubscriptionPlanRequest validation with standard valid price and duration is valid")
    void subscriptionPlanRequest_is_valid() {
        SubscriptionPlanRequest request = new SubscriptionPlanRequest(
                "Plan Anual",
                "Acceso completo durante 12 meses",
                new BigDecimal("299.99"),
                12,
                true
        );

        Set<ConstraintViolation<SubscriptionPlanRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("SubscriptionPlanRequest duration exceeds @Max limit is invalid")
    void subscriptionPlanRequest_invalid_duration() {
        SubscriptionPlanRequest request = new SubscriptionPlanRequest(
                "Plan Anual",
                "Acceso completo",
                new BigDecimal("299.99"),
                121, // Max is 120
                true
        );

        Set<ConstraintViolation<SubscriptionPlanRequest>> violations = validator.validate(request);
        assertThat(violations).extracting(c -> c.getPropertyPath().toString()).contains("durationMonths");
    }
}
