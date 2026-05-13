package com.openroof.openroof.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailService — render Thymeleaf templates lease")
class EmailServiceLeaseTest {

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "templateEngine", engine);
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:5173");
    }

    @Test
    void rendersLeaseSentForSignature() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", "Pepe");
        vars.put("propertyTitle", "Depto Centro");
        vars.put("signatureLink", "http://x/sign?token=abc");
        vars.put("expiresAt", "31/12/2026 a las 23:59");

        String html = emailService.renderTemplate("email/lease-sent-for-signature", vars);

        assertThat(html).contains("Pepe");
        assertThat(html).contains("Depto Centro");
        assertThat(html).contains("http://x/sign?token=abc");
        assertThat(html).contains("Vencimiento del link");
    }

    @Test
    void rendersLeaseSigned_withPendingMessage() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", "Juan");
        vars.put("signerName", "María");
        vars.put("propertyTitle", "Casa Norte");
        vars.put("pendingMessage", "Falta tu firma para activar el contrato.");
        vars.put("actionUrl", "http://x/leases/5");

        String html = emailService.renderTemplate("email/lease-signed", vars);

        assertThat(html).contains("Juan");
        assertThat(html).contains("María");
        assertThat(html).contains("Casa Norte");
        assertThat(html).contains("Falta tu firma");
    }

    @Test
    void rendersLeaseActivatedTenant_withSummary() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("tenantName", "Juan");
        vars.put("propertyTitle", "Depto");
        vars.put("monthlyRent", new BigDecimal("1500.50"));
        vars.put("startDate", LocalDate.of(2026, 6, 1).toString());
        vars.put("endDate", LocalDate.of(2027, 6, 1).toString());
        vars.put("firstInstallmentDueDate", LocalDate.of(2026, 7, 1).toString());
        vars.put("actionUrl", "http://x/y");

        String html = emailService.renderTemplate("email/lease-activated-tenant", vars);

        assertThat(html).contains("Bienvenido a tu nuevo hogar");
        assertThat(html).contains("2026-06-01");
        assertThat(html).contains("2026-07-01");
        assertThat(html).contains("Primera cuota");
    }

    @Test
    void rendersLeaseActivatedTenant_withoutFirstInstallment() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("tenantName", "Juan");
        vars.put("propertyTitle", "Depto");
        vars.put("monthlyRent", new BigDecimal("1000.00"));
        vars.put("startDate", "2026-06-01");
        vars.put("endDate", "2027-06-01");
        vars.put("firstInstallmentDueDate", null);
        vars.put("actionUrl", "http://x/y");

        String html = emailService.renderTemplate("email/lease-activated-tenant", vars);

        assertThat(html).doesNotContain("Primera cuota");
    }

    @Test
    void rendersLeaseActivatedLandlord() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("landlordName", "Ana");
        vars.put("tenantName", "Juan");
        vars.put("propertyTitle", "Depto");
        vars.put("monthlyRent", new BigDecimal("2000"));
        vars.put("startDate", "2026-06-01");
        vars.put("endDate", "2027-06-01");
        vars.put("actionUrl", "http://x/y");

        String html = emailService.renderTemplate("email/lease-activated-landlord", vars);

        assertThat(html).contains("Ana");
        assertThat(html).contains("Juan");
        assertThat(html).contains("Contrato activado");
    }
}
