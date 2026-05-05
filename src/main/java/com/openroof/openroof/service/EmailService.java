package com.openroof.openroof.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class EmailService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${mail.from-name:OpenRoof}")
    private String fromName;

    @Value("${mail.base-url:http://localhost:5173}")
    private String baseUrl;

    // ─── AUTH ─────────────────────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void sendWelcomeEmail(String toEmail, String userName) {
        String subject = "¡Bienvenido a OpenRoof, " + userName + "!";
        String body = buildHtml(
                "¡Bienvenido a OpenRoof!",
                "Hola, " + escapeHtml(userName) + ".",
                """
                <p>Tu cuenta ha sido creada exitosamente. Ya puedes explorar propiedades,
                contactar agentes y gestionar tus procesos inmobiliarios desde un solo lugar.</p>
                <p>Si registraste tu cuenta como <strong>agente</strong>, completa tu perfil
                para aparecer en los resultados de búsqueda.</p>
                """,
                "Explorar propiedades",
                baseUrl + "/properties"
        );
        send(toEmail, subject, body);
    }

    // ─── CONTRATOS ────────────────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void sendContractCreatedEmail(String toEmail, String userName,
                                          String propertyTitle, Long contractId) {
        String subject = "Nuevo contrato en OpenRoof — " + propertyTitle;
        String body = buildHtml(
                "Nuevo contrato creado",
                "Hola, " + escapeHtml(userName) + ".",
                """
                <p>Se ha creado un nuevo contrato para la propiedad
                <strong>%s</strong> y has sido registrado como parte del mismo.</p>
                <p>Ingresa a la plataforma para revisar los detalles, términos y estado del contrato.</p>
                """.formatted(escapeHtml(propertyTitle)),
                "Ver contrato",
                baseUrl + "/contratos/" + contractId
        );
        send(toEmail, subject, body);
    }

    @Async("emailTaskExecutor")
    public void sendContractStatusChangedEmail(String toEmail, String userName,
                                                String propertyTitle, String newStatus, Long contractId) {
        String label = translateContractStatus(newStatus);
        String subject = "Contrato actualizado — " + propertyTitle;
        String body = buildHtml(
                "Estado del contrato actualizado",
                "Hola, " + escapeHtml(userName) + ".",
                """
                <p>El estado del contrato para la propiedad <strong>%s</strong>
                ha cambiado a <strong>%s</strong>.</p>
                <p>Ingresa a la plataforma para ver los detalles actualizados.</p>
                """.formatted(escapeHtml(propertyTitle), label),
                "Ver contrato",
                baseUrl + "/contratos/" + contractId
        );
        send(toEmail, subject, body);
    }

    // ─── VISITAS ──────────────────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void sendVisitRequestCreatedEmail(String toEmail, String recipientName,
                                              String propertyTitle, String buyerName,
                                              LocalDateTime proposedAt) {
        String subject = "Nueva solicitud de visita — " + propertyTitle;
        String body = buildHtml(
                "Nueva solicitud de visita",
                "Hola, " + escapeHtml(recipientName) + ".",
                """
                <p><strong>%s</strong> ha solicitado visitar la propiedad
                <strong>%s</strong> el <strong>%s</strong>.</p>
                <p>Acepta, rechaza o propone una nueva fecha desde la plataforma.</p>
                """.formatted(escapeHtml(buyerName), escapeHtml(propertyTitle), format(proposedAt)),
                "Gestionar solicitud",
                baseUrl + "/visit-requests"
        );
        send(toEmail, subject, body);
    }

    @Async("emailTaskExecutor")
    public void sendVisitRequestAcceptedEmail(String toEmail, String buyerName,
                                               String propertyTitle, LocalDateTime scheduledAt) {
        String subject = "¡Tu visita fue confirmada! — " + propertyTitle;
        String body = buildHtml(
                "Visita confirmada",
                "Hola, " + escapeHtml(buyerName) + ".",
                """
                <p>Tu solicitud de visita para la propiedad <strong>%s</strong>
                ha sido <strong>aceptada</strong>.</p>
                <p>Fecha y hora confirmada: <strong>%s</strong>.</p>
                <p>Recuerda llegar puntualmente. Si necesitas cancelar, hazlo con anticipación.</p>
                """.formatted(escapeHtml(propertyTitle), format(scheduledAt)),
                "Ver mis visitas",
                baseUrl + "/visit-requests"
        );
        send(toEmail, subject, body);
    }

    @Async("emailTaskExecutor")
    public void sendVisitRequestRejectedEmail(String toEmail, String buyerName,
                                               String propertyTitle) {
        String subject = "Solicitud de visita rechazada — " + propertyTitle;
        String body = buildHtml(
                "Solicitud de visita rechazada",
                "Hola, " + escapeHtml(buyerName) + ".",
                """
                <p>Tu solicitud de visita para la propiedad <strong>%s</strong>
                ha sido <strong>rechazada</strong> por el agente.</p>
                <p>Puedes consultar otras propiedades disponibles o contactar directamente al agente.</p>
                """.formatted(escapeHtml(propertyTitle)),
                "Ver propiedades",
                baseUrl + "/properties"
        );
        send(toEmail, subject, body);
    }

    @Async("emailTaskExecutor")
    public void sendVisitCounterProposedEmail(String toEmail, String buyerName,
                                               String propertyTitle,
                                               LocalDateTime counterProposedAt,
                                               String agentMessage) {
        String notes = (agentMessage != null && !agentMessage.isBlank())
                ? "<p><em>Mensaje del agente: \"" + escapeHtml(agentMessage) + "\"</em></p>"
                : "";
        String subject = "Nueva propuesta de fecha para tu visita — " + propertyTitle;
        String body = buildHtml(
                "Propuesta de nueva fecha",
                "Hola, " + escapeHtml(buyerName) + ".",
                """
                <p>El agente ha propuesto una nueva fecha para visitar
                <strong>%s</strong>: <strong>%s</strong>.</p>
                %s
                <p>Acepta o rechaza la propuesta desde la plataforma.</p>
                """.formatted(escapeHtml(propertyTitle), format(counterProposedAt), notes),
                "Ver solicitud",
                baseUrl + "/visit-requests"
        );
        send(toEmail, subject, body);
    }

    @Async("emailTaskExecutor")
    public void sendVisitRequestCancelledEmail(String toEmail, String recipientName,
                                                String propertyTitle, String buyerName) {
        String subject = "Solicitud de visita cancelada — " + propertyTitle;
        String body = buildHtml(
                "Visita cancelada",
                "Hola, " + escapeHtml(recipientName) + ".",
                """
                <p><strong>%s</strong> ha cancelado la solicitud de visita
                para la propiedad <strong>%s</strong>.</p>
                """.formatted(escapeHtml(buyerName), escapeHtml(propertyTitle)),
                "Ver visitas",
                baseUrl + "/visit-requests"
        );
        send(toEmail, subject, body);
    }

    // ─── ASIGNACIONES DE PROPIEDAD ────────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void sendPropertyAssignmentEmail(String toEmail, String agentName,
                                             String propertyTitle, String ownerName) {
        String subject = "Nueva propiedad asignada — " + propertyTitle;
        String body = buildHtml(
                "Tienes una nueva propiedad asignada",
                "Hola, " + escapeHtml(agentName) + ".",
                """
                <p><strong>%s</strong> te ha asignado la propiedad
                <strong>%s</strong> para que la gestiones.</p>
                <p>Acepta o rechaza la asignación desde tu panel de agente.</p>
                """.formatted(escapeHtml(ownerName), escapeHtml(propertyTitle)),
                "Ver asignación",
                baseUrl + "/agent/assignments"
        );
        send(toEmail, subject, body);
    }

    @Async("emailTaskExecutor")
    public void sendPropertyAssignmentResponseEmail(String toEmail, String ownerName,
                                                     String propertyTitle, String agentName,
                                                     String status) {
        boolean accepted = "ACCEPTED".equalsIgnoreCase(status);
        String actionLabel = accepted ? "aceptado" : "rechazado";
        String subject = "Asignación " + actionLabel + " — " + propertyTitle;
        String extra = accepted
                ? "<p>El agente ya puede comenzar a gestionar tu propiedad.</p>"
                : "<p>Puedes asignar otro agente desde tu panel de propietario.</p>";
        String body = buildHtml(
                "Respuesta a asignación de propiedad",
                "Hola, " + escapeHtml(ownerName) + ".",
                """
                <p>El agente <strong>%s</strong> ha <strong>%s</strong>
                la asignación de la propiedad <strong>%s</strong>.</p>
                %s
                """.formatted(escapeHtml(agentName), actionLabel, escapeHtml(propertyTitle), extra),
                "Ver propiedad",
                baseUrl + "/owner/properties"
        );
        send(toEmail, subject, body);
    }

    // ─── DOCUMENTOS KYC ───────────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void sendDocumentApprovedEmail(String toEmail, String userName, String documentType) {
        String subject = "✅ Tu documento fue aprobado — OpenRoof";
        String body = buildHtml(
                "Documento aprobado",
                "¡Buenas noticias, " + escapeHtml(userName) + "!",
                """
                <p>Tu documento <strong>%s</strong> ha sido <strong>aprobado</strong>
                por nuestro equipo de verificación.</p>
                <p>Tu perfil ya cuenta con documentación verificada. Ahora podés acceder
                a todas las funcionalidades de la plataforma.</p>
                """.formatted(escapeHtml(translateDocumentType(documentType))),
                "Ver mi perfil",
                baseUrl + "/profile"
        );
        send(toEmail, subject, body);
    }

    @Async("emailTaskExecutor")
    public void sendDocumentRejectedEmail(String toEmail, String userName,
                                           String documentType, String reason) {
        String reasonHtml = (reason != null && !reason.isBlank())
                ? "<p><strong>Motivo:</strong> " + escapeHtml(reason) + "</p>"
                : "";
        String subject = "❌ Tu documento fue rechazado — OpenRoof";
        String body = buildHtml(
                "Documento rechazado",
                "Hola, " + escapeHtml(userName) + ".",
                """
                <p>Lamentablemente, tu documento <strong>%s</strong> no pudo ser aprobado.</p>
                %s
                <p>Por favor, revisá los requisitos y subí un nuevo documento desde tu perfil.</p>
                """.formatted(escapeHtml(translateDocumentType(documentType)), reasonHtml),
                "Subir nuevo documento",
                baseUrl + "/profile"
        );
        send(toEmail, subject, body);
    }


    // ─── OFERTAS ───────────────────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void sendOfferAcceptedEmail(String toEmail, String buyerName,
                                        String propertyTitle, java.math.BigDecimal amount,
                                        LocalDateTime acceptedAt, String agentMessage) {
        String notes = (agentMessage != null && !agentMessage.isBlank())
                ? "<p><em>Mensaje del agente/propietario: \"" + escapeHtml(agentMessage) + "\"</em></p>"
                : "";
        String subject = "¡Tu oferta ha sido aceptada! — " + propertyTitle;
        String body = buildHtml(
                "Oferta Aceptada",
                "Hola, " + escapeHtml(buyerName) + ".",
                """
                <p>¡Excelentes noticias! Tu oferta por la propiedad <strong>%s</strong> ha sido <strong>aceptada</strong>.</p>
                <p><strong>Detalles de la oferta:</strong></p>
                <ul>
                    <li>Monto aceptado: USD %s</li>
                    <li>Fecha de aceptación: %s</li>
                </ul>
                %s
                <p><strong>Próximos pasos:</strong> Te recomendamos contactar al agente o propietario para coordinar los detalles de la firma o los pasos legales a seguir. Puedes hacerlo desde la plataforma.</p>
                """.formatted(
                        escapeHtml(propertyTitle),
                        String.format("%,.2f", amount),
                        format(acceptedAt),
                        notes
                ),
                "Ver oferta",
                baseUrl + "/offers"
        );
        send(toEmail, subject, body);
    }

    @Async("emailTaskExecutor")
    public void sendNewMatchAlertEmail(String toEmail, String userName,
                                        String propertyTitle, java.math.BigDecimal price,
                                        Long propertyId, String preferenceName) {
        String subject = "¡Encontramos una propiedad para ti! — " + propertyTitle;
        String body = buildHtml(
                "Nueva coincidencia con tu búsqueda",
                "Hola, " + escapeHtml(userName) + ".",
                """
                <p>Una nueva propiedad coincide con tu búsqueda guardada <strong>"%s"</strong>.</p>
                <p><strong>%s</strong></p>
                <p>Precio: <strong>USD %s</strong></p>
                <p>No pierdas la oportunidad de ser el primero en verla.</p>
                """.formatted(
                        escapeHtml(preferenceName),
                        escapeHtml(propertyTitle),
                        String.format("%,.2f", price)
                ),
                "Ver propiedad",
                baseUrl + "/properties/" + propertyId
        );
        send(toEmail, subject, body);
    }

    // ─── MENSAJES ───────────────────────────────────────────────────────────────

    @Async("emailTaskExecutor")
    public void sendNewMessageEmailAsync(String toEmail, String senderName, String messageContent) {
        String preview = messageContent.length() > 100
            ? messageContent.substring(0, 100) + "..."
            : messageContent;
        String subject = "Nuevo mensaje de " + senderName;
        String body = buildHtml(
            "Nuevo mensaje",
            "Hola.",
            """
            <p>Tienes un nuevo mensaje de <strong>%s</strong>:</p>
            <blockquote style="border-left: 3px solid #1a3c5e; padding-left: 16px; color: #666; font-style: italic;">%s</blockquote>
            <p>Ingresa a la plataforma para responder.</p>
            """.formatted(escapeHtml(senderName), escapeHtml(preview)),
            "Ver mensajes",
            baseUrl + "/messages"
        );
        send(toEmail, subject, body);
    }

    // ─── Core sender ──────────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) {
        if (mailSender == null) {
            log.warn("JavaMailSender no disponible — email omitido para {}", maskEmail(to));
            return;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("MAIL_USERNAME no configurado — email omitido para {}", maskEmail(to));
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email enviado a {} — [REDACTED]", maskEmail(to));
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Error enviando email a {} [REDACTED]", maskEmail(to), e);
        }
    }

    // ─── HTML template ────────────────────────────────────────────────────────

    private String buildHtml(String title, String greeting, String content,
                              String ctaText, String ctaUrl) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f4f4f4;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:30px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:8px;overflow:hidden;
                                    box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                        <!-- Header -->
                        <tr>
                          <td style="background:#1a3c5e;padding:28px 40px;text-align:center;">
                            <span style="color:#ffffff;font-size:26px;font-weight:bold;
                                         letter-spacing:1px;">OpenRoof</span>
                          </td>
                        </tr>
                        <!-- Body -->
                        <tr>
                          <td style="padding:36px 40px;">
                            <h2 style="margin:0 0 16px;color:#1a3c5e;font-size:20px;">%s</h2>
                            <p style="margin:0 0 16px;color:#444;font-size:15px;">%s</p>
                            <div style="color:#444;font-size:15px;line-height:1.6;">%s</div>
                            <div style="text-align:center;margin-top:32px;">
                              <a href="%s"
                                 style="background:#1a3c5e;color:#ffffff;text-decoration:none;
                                        padding:12px 32px;border-radius:6px;font-size:15px;
                                        font-weight:bold;display:inline-block;">%s</a>
                            </div>
                          </td>
                        </tr>
                        <!-- Footer -->
                        <tr>
                          <td style="background:#f9f9f9;padding:20px 40px;text-align:center;
                                     border-top:1px solid #eeeeee;">
                            <p style="margin:0;color:#999;font-size:12px;">
                              Este correo fue generado automáticamente por OpenRoof.<br>
                              Si tienes dudas, visita <a href="%s" style="color:#1a3c5e;">nuestro sitio</a>.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(title, greeting, content, ctaUrl, ctaText, baseUrl);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String format(LocalDateTime dt) {
        return dt != null ? dt.format(DATE_FMT) : "—";
    }

    private static String maskEmail(String email) {
        if (email == null) return "[null]";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        int dot = domain.lastIndexOf('.');
        String maskedLocal = local.charAt(0) + "***";
        String maskedDomain = dot > 0 ? domain.charAt(0) + "***" + domain.substring(dot) : "***";
        return maskedLocal + "@" + maskedDomain;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private String translateContractStatus(String status) {
        return switch (status) {
            case "DRAFT"            -> "Borrador";
            case "SENT"             -> "Enviado";
            case "PARTIALLY_SIGNED" -> "Parcialmente firmado";
            case "SIGNED"           -> "Firmado";
            case "REJECTED"         -> "Rechazado";
            case "EXPIRED"          -> "Expirado";
            case "CANCELLED"        -> "Cancelado";
            default                 -> status;
        };
    }

    private String translateDocumentType(String type) {
        if (type == null) return "";
        return switch (type) {
            case "ID_FRONT"         -> "DNI / Documento de identidad (frente)";
            case "ID_BACK"          -> "DNI / Documento de identidad (dorso)";
            case "SELFIE"           -> "Selfi de verificación";
            case "PROOF_OF_ADDRESS" -> "Comprobante de domicilio";
            case "ID"               -> "Documento de identidad";
            case "PROOF_OF_INCOME"  -> "Comprobante de ingresos";
            case "TAX_RETURN"       -> "Declaración de impuestos";
            case "BANK_STATEMENT"   -> "Extracto bancario";
            case "OTHER"            -> "Otro documento";
            default                 -> type;
        };
    }
}
