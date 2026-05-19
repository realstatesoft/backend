package com.openroof.openroof.dto.agent;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateAgentReviewRequest — validación")
class CreateAgentReviewRequestValidationTest {

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

    private CreateAgentReviewRequest valid() {
        return new CreateAgentReviewRequest(5, "Excelente agente, muy profesional y atento.", null);
    }

    private Set<ConstraintViolation<CreateAgentReviewRequest>> validate(CreateAgentReviewRequest req) {
        return validator.validate(req);
    }

    @Test
    @DisplayName("DTO completamente válido no produce violaciones")
    void validDtoHasNoViolations() {
        assertThat(validate(valid())).isEmpty();
    }

    @Test
    @DisplayName("DTO válido con propertyId no produce violaciones")
    void validDtoWithPropertyIdHasNoViolations() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(3, "Buen trato pero tardó en responder mis consultas.", 42L);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    @DisplayName("rating null es inválido")
    void ratingNullIsInvalid() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(null, "Comentario suficientemente largo.", null);
        Set<ConstraintViolation<CreateAgentReviewRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString()).contains("rating");
    }

    @Test
    @DisplayName("rating 0 es inválido (menor al mínimo)")
    void ratingBelowMinIsInvalid() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(0, "Comentario suficientemente largo.", null);
        Set<ConstraintViolation<CreateAgentReviewRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString()).contains("rating");
    }

    @Test
    @DisplayName("rating 6 es inválido (mayor al máximo)")
    void ratingAboveMaxIsInvalid() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(6, "Comentario suficientemente largo.", null);
        Set<ConstraintViolation<CreateAgentReviewRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString()).contains("rating");
    }

    @Test
    @DisplayName("rating 1 es el mínimo válido")
    void ratingOneIsValid() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(1, "Muy mala experiencia con este agente.", null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    @DisplayName("rating 5 es el máximo válido")
    void ratingFiveIsValid() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(5, "Excelente agente, muy profesional.", null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    @DisplayName("comment null es inválido")
    void commentNullIsInvalid() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(4, null, null);
        Set<ConstraintViolation<CreateAgentReviewRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString()).contains("comment");
    }

    @Test
    @DisplayName("comment en blanco es inválido")
    void commentBlankIsInvalid() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(4, "   ", null);
        Set<ConstraintViolation<CreateAgentReviewRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString()).contains("comment");
    }

    @Test
    @DisplayName("comment con menos de 10 caracteres es inválido")
    void commentTooShortIsInvalid() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(4, "Corto.", null);
        Set<ConstraintViolation<CreateAgentReviewRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString()).contains("comment");
    }

    @Test
    @DisplayName("comment con más de 1000 caracteres es inválido")
    void commentTooLongIsInvalid() {
        String tooLong = "A".repeat(1001);
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(4, tooLong, null);
        Set<ConstraintViolation<CreateAgentReviewRequest>> v = validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString()).contains("comment");
    }

    @Test
    @DisplayName("comment con exactamente 10 caracteres es válido")
    void commentExactlyMinLengthIsValid() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(4, "1234567890", null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    @DisplayName("comment con exactamente 1000 caracteres es válido")
    void commentExactlyMaxLengthIsValid() {
        String maxLength = "A".repeat(1000);
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(4, maxLength, null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    @DisplayName("propertyId null es válido (campo opcional)")
    void propertyIdNullIsValid() {
        CreateAgentReviewRequest req = new CreateAgentReviewRequest(4, "Muy buena atención al cliente.", null);
        assertThat(validate(req)).isEmpty();
    }
}
