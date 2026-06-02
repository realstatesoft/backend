package com.openroof.openroof.dto.register;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validación de contraseña en registro")
class RegisterRequestValidationTest {

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

    private RegisterRequest valid(String password) {
        return RegisterRequest.builder()
                .name("Test User")
                .email("test@openroof.com")
                .password(password)
                .role("USER")
                .build();
    }

    private boolean hasPasswordViolation(RegisterRequest req) {
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    // ── Contraseñas válidas ──────────────────────────────────────────────────

    @ParameterizedTest(name = "válida: \"{0}\"")
    @ValueSource(strings = {
        "Password1!",       // exactamente 10 chars, cumple todo
        "Segura#2026abc",   // 15 chars con letra especial
        "A1!aaaaaaaa",      // 10 chars mínimo
        "MyP@ssw0rd123",    // típica contraseña fuerte
        "OpenRoof#2026!"    // caso real del proyecto
    })
    @DisplayName("Contraseñas que cumplen todos los criterios son aceptadas")
    void validPasswords_noViolation(String password) {
        assertThat(hasPasswordViolation(valid(password))).isFalse();
    }

    // ── Contraseñas inválidas ────────────────────────────────────────────────

    @Test
    @DisplayName("Contraseña en blanco falla @NotBlank")
    void blank_failsNotBlank() {
        Set<ConstraintViolation<RegisterRequest>> v = validator.validate(valid("   "));
        assertThat(v).extracting(c -> c.getPropertyPath().toString()).contains("password");
    }

    @ParameterizedTest(name = "muy corta: \"{0}\"")
    @ValueSource(strings = {
        "Pass1!",       // 6 chars
        "Passw1!ab",    // 9 chars
        "A1!aaaaaa"     // 9 chars
    })
    @DisplayName("Contraseñas con menos de 10 caracteres son rechazadas")
    void tooShort_failsValidation(String password) {
        assertThat(hasPasswordViolation(valid(password))).isTrue();
    }

    @Test
    @DisplayName("Sin mayúscula es rechazada")
    void noUppercase_failsValidation() {
        assertThat(hasPasswordViolation(valid("password1!ab"))).isTrue();
    }

    @Test
    @DisplayName("Sin minúscula es rechazada")
    void noLowercase_failsValidation() {
        assertThat(hasPasswordViolation(valid("PASSWORD1!AB"))).isTrue();
    }

    @Test
    @DisplayName("Sin dígito es rechazada")
    void noDigit_failsValidation() {
        assertThat(hasPasswordViolation(valid("Password!abc"))).isTrue();
    }

    @Test
    @DisplayName("Sin carácter especial es rechazada")
    void noSpecialChar_failsValidation() {
        assertThat(hasPasswordViolation(valid("Password1abc"))).isTrue();
    }

    // ── AgentSignupRequest aplica la misma regla ─────────────────────────────

    @Test
    @DisplayName("AgentSignupRequest: contraseña válida no produce violación")
    void agentSignup_validPassword_noViolation() {
        AgentSignupRequest req = AgentSignupRequest.builder()
                .name("Agent")
                .email("agent@openroof.com")
                .password("Password1!")
                .phone("+595981000001")
                .build();

        Set<ConstraintViolation<AgentSignupRequest>> v = validator.validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString())
                .doesNotContain("password");
    }

    @Test
    @DisplayName("AgentSignupRequest: contraseña débil produce violación")
    void agentSignup_weakPassword_hasViolation() {
        AgentSignupRequest req = AgentSignupRequest.builder()
                .name("Agent")
                .email("agent@openroof.com")
                .password("weak")
                .phone("+595981000001")
                .build();

        Set<ConstraintViolation<AgentSignupRequest>> v = validator.validate(req);
        assertThat(v).extracting(c -> c.getPropertyPath().toString())
                .contains("password");
    }
}
