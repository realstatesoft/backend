package com.openroof.openroof.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailService — render Thymeleaf templates rental application")
class EmailServiceApplicationTest {

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
    void rendersApplicationSubmittedTemplate() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("landlordName", "Pepe Propietario");
        vars.put("applicantName", "Juan Inquilino");
        vars.put("propertyTitle", "Depto Centro");
        vars.put("monthlyIncome", new java.math.BigDecimal("3500.50"));
        vars.put("actionUrl", "http://x/y");

        String html = emailService.renderTemplate("email/application-submitted", vars);

        assertThat(html).contains("Pepe Propietario");
        assertThat(html).contains("Juan Inquilino");
        assertThat(html).contains("Depto Centro");
        assertThat(html).contains("Nueva solicitud de alquiler");
    }

    @Test
    void rendersApplicationApprovedTemplate_withNextSteps() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("applicantName", "Juan");
        vars.put("propertyTitle", "Depto Centro");
        vars.put("nextSteps", "Coordinar firma");
        vars.put("actionUrl", "http://x/y");

        String html = emailService.renderTemplate("email/application-approved", vars);

        assertThat(html).contains("Juan");
        assertThat(html).contains("aprobada");
        assertThat(html).contains("Coordinar firma");
    }

    @Test
    void rendersApplicationApprovedTemplate_withoutNextSteps() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("applicantName", "Juan");
        vars.put("propertyTitle", "Depto Centro");
        vars.put("nextSteps", "");
        vars.put("actionUrl", "http://x/y");

        String html = emailService.renderTemplate("email/application-approved", vars);

        assertThat(html).contains("propietario o agente se pondrá en contacto");
    }

    @Test
    void rendersApplicationRejectedTemplate_withReason() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("applicantName", "Juan");
        vars.put("propertyTitle", "Depto Centro");
        vars.put("publicReason", "Historial insuficiente");
        vars.put("actionUrl", "http://x/y");

        String html = emailService.renderTemplate("email/application-rejected", vars);

        assertThat(html).contains("Juan");
        assertThat(html).contains("Historial insuficiente");
        assertThat(html).contains("Motivo");
    }

    @Test
    void rendersApplicationRejectedTemplate_withoutReason() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("applicantName", "Juan");
        vars.put("propertyTitle", "Depto Centro");
        vars.put("publicReason", "");
        vars.put("actionUrl", "http://x/y");

        String html = emailService.renderTemplate("email/application-rejected", vars);

        assertThat(html).doesNotContain("<strong>Motivo:</strong>");
    }
}
