package com.openroof.openroof.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LeasePdfService {

    private static final Color PRIMARY_COLOR = new Color(26, 60, 94); // #1a3c5e
    private static final Color ACCENT_COLOR = new Color(20, 120, 180); // #1478b4
    private static final Color BORDER_COLOR = new Color(186, 230, 253);// #bae6fd
    private static final Color MUTED_TEXT = new Color(100, 116, 139);// #64748b
    private static final Color SUCCESS_COLOR = new Color(22, 163, 74); // #16a34a
    private static final Color ROW_ALT = new Color(248, 250, 252);// #f8fafc

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;

    public byte[] generatePdf(Long leaseId, String requesterEmail) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado"));

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        validateAccess(lease, requester);

        try {
            return buildPdf(lease);
        } catch (Exception e) {
            log.error("Error inesperado generando PDF para lease {}: {}", leaseId, e.getMessage(), e);
            throw new BadRequestException("No se pudo generar el PDF del contrato: " + e.getMessage());
        }
    }

    private void validateAccess(Lease lease, User requester) {
        boolean isTenant = lease.getPrimaryTenant() != null && lease.getPrimaryTenant().getId().equals(requester.getId());
        boolean isLandlord = lease.getLandlord() != null && lease.getLandlord().getId().equals(requester.getId());
        if (!isTenant && !isLandlord) {
            throw new BadRequestException("No tiene permiso para descargar este contrato");
        }
    }

    private byte[] buildPdf(Lease lease) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 60, 50);
            try {
                PdfWriter.getInstance(doc, baos);
                doc.open();

                Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
                Font regular = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
                Font italic = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.BLACK);

                LocalDateTime now = LocalDateTime.now();
                String generatedAt = now.format(DATETIME_FMT);

                addHeader(doc, lease, bold, regular);
                addDivider(doc);
                addInfoSection(doc, lease, generatedAt, bold, regular);
                addPartiesSection(doc, lease, bold, regular, italic);
                addTermsSection(doc, lease, bold, regular);
                addSignaturesSection(doc, lease, bold, regular, italic);
                addFooter(doc, lease, generatedAt, regular, italic);
            } finally {
                if (doc.isOpen()) {
                    doc.close();
                }
            }
            return baos.toByteArray();
        }
    }

    private void addHeader(Document doc, Lease lease, Font bold, Font regular) {
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PRIMARY_COLOR);
        Paragraph brand = new Paragraph("OpenRoof", brandFont);
        brand.setSpacingAfter(2);
        doc.add(brand);

        Font taglineFont = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED_TEXT);
        Paragraph tagline = new Paragraph("Plataforma Inmobiliaria · Contrato de Arrendamiento", taglineFont);
        tagline.setSpacingAfter(16);
        doc.add(tagline);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
        Paragraph title = new Paragraph("CONTRATO DE ARRENDAMIENTO", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED_TEXT);
        String statusLabel = lease.isActive() ? "Activo" : lease.getStatus().name();
        Paragraph subtitle = new Paragraph("Contrato N° L" + lease.getId() + " · " + statusLabel, subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(8);
        doc.add(subtitle);

        Paragraph preamble = new Paragraph();
        preamble.setFont(regular);
        preamble.setAlignment(Element.ALIGN_JUSTIFIED);
        preamble.setSpacingBefore(12);
        preamble.setSpacingAfter(12);

        preamble.add("En la ciudad de Asunción, República del Paraguay, a los ");
        preamble.add(new Chunk(formatDate(lease.getStartDate()), bold));
        preamble.add(", comparecen por una parte ");
        preamble.add(new Chunk(safeName(lease.getLandlord()), bold));
        preamble.add(" en adelante denominado el ");
        preamble.add(new Chunk("ARRENDADOR", bold));
        preamble.add(", y por la otra parte ");
        preamble.add(new Chunk(safeName(lease.getPrimaryTenant()), bold));
        preamble.add(" en adelante denominado el ");
        preamble.add(new Chunk("ARRENDATARIO", bold));
        preamble.add(", quienes convienen en celebrar el presente contrato de arrendamiento sujeto a las siguientes cláusulas.");

        doc.add(preamble);
    }

    private void addDivider(Document doc) {
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(2);
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setBorder(PdfPCell.NO_BORDER);
        divider.addCell(cell);
        divider.setSpacingAfter(16);
        doc.add(divider);
    }

    private void addInfoSection(Document doc, Lease lease, String generatedAt, Font bold, Font regular) {
        addSectionTitle(doc, "1. Información General", bold);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[] { 30, 70 });
        } catch (DocumentException e) {
            log.error("Error setting table widths: {}", e.getMessage());
        }
        table.setSpacingAfter(20);

        String propertyTitle = lease.getProperty() != null ? lease.getProperty().getTitle() : "[PROPIEDAD NO DISPONIBLE]";
        String propertyAddress = lease.getProperty() != null ? lease.getProperty().getAddress() : "—";

        addInfoRow(table, "Propiedad", propertyTitle, bold, regular, false);
        addInfoRow(table, "Dirección", propertyAddress, bold, regular, true);
        addInfoRow(table, "Tipo", resolveLeaseTypeLabel(lease.getLeaseType()), bold, regular, false);
        addInfoRow(table, "Renta mensual", formatCurrency(lease.getMonthlyRent(), lease.getCurrency()), bold, regular, true);
        addInfoRow(table, "Depósito de garantía", formatCurrency(lease.getSecurityDeposit(), lease.getCurrency()), bold, regular, false);
        addInfoRow(table, "Fecha de inicio", formatDate(lease.getStartDate()), bold, regular, true);
        addInfoRow(table, "Fecha de fin", formatDate(lease.getEndDate()), bold, regular, false);
        addInfoRow(table, "Día de vencimiento", lease.getDueDay() != null ? lease.getDueDay() + " de cada mes" : "—", bold, regular, true);
        addInfoRow(table, "Período de gracia", lease.getGracePeriodDays() != null ? lease.getGracePeriodDays() + " días" : "—", bold, regular, false);
        addInfoRow(table, "Renovación automática", Boolean.TRUE.equals(lease.getAutoRenew()) ? "Sí" : "No", bold, regular, true);
        addInfoRow(table, "Generado el", generatedAt, bold, regular, false);

        doc.add(table);
    }

    private void addPartiesSection(Document doc, Lease lease, Font bold, Font regular, Font italic) {
        addSectionTitle(doc, "2. Partes del Contrato", bold);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[] { 25, 37.5f, 37.5f });
        } catch (DocumentException e) {
            log.error("Error setting table widths: {}", e.getMessage());
        }
        table.setSpacingAfter(20);

        addPartiesHeader(table, bold);
        addPartyRow(table, "Arrendador", safeName(lease.getLandlord()), safeEmail(lease.getLandlord()), regular, false);
        addPartyRow(table, "Arrendatario", safeName(lease.getPrimaryTenant()), safeEmail(lease.getPrimaryTenant()), regular, true);

        doc.add(table);
    }

    private void addTermsSection(Document doc, Lease lease, Font bold, Font regular) {
        addSectionTitle(doc, "3. Cláusulas del Contrato", bold);

        String[] clauses = {
            "PRIMERA. OBJETO: El ARRENDADOR da en arrendamiento al ARRENDATARIO el inmueble descrito en la Información General, para uso exclusivo como vivienda.",
            "SEGUNDA. PLAZO: El plazo del arrendamiento es desde el " + formatDate(lease.getStartDate()) + " hasta el " + formatDate(lease.getEndDate()) + ".",
            "TERCERA. RENTA: El ARRENDATARIO se obliga a pagar al ARRENDADOR la suma de " + formatCurrency(lease.getMonthlyRent(), lease.getCurrency()) + " mensualmente, con vencimiento el día " + (lease.getDueDay() != null ? lease.getDueDay() : "—") + " de cada mes.",
            "CUARTA. DEPÓSITO: El ARRENDATARIO entrega en garantía la suma de " + formatCurrency(lease.getSecurityDeposit(), lease.getCurrency()) + " que será devuelta al finalizar el contrato, previa verificación del estado del inmueble.",
            "QUINTA. MANTENIMIENTO: El ARRENDATARIO se obliga a mantener el inmueble en buen estado y a realizar las reparaciones que sean necesarias por su uso ordinario.",
            "SEXTA. SERVICIOS: Los servicios de agua, luz, gas e internet serán contratados y pagados por el ARRENDATARIO, salvo acuerdo expreso en contrario.",
            "SÉPTIMA. SUBARRIENDO: El ARRENDATARIO no podrá subarrendar, ceder o transferir el presente contrato sin autorización escrita del ARRENDADOR.",
            "OCTAVA. TERMINACIÓN: Cualquiera de las partes podrá dar por terminado el contrato notificando con " + (lease.getRenewalNoticeDays() != null ? lease.getRenewalNoticeDays() : "—") + " días de anticipación.",
            "NOVENA. JURISDICCIÓN: Para todos los efectos judiciales las partes se someten a la jurisdicción de los tribunales de Asunción, Paraguay."
        };

        for (String clause : clauses) {
            Paragraph p = new Paragraph(clause, regular);
            p.setAlignment(Element.ALIGN_JUSTIFIED);
            p.setSpacingAfter(8);
            doc.add(p);
        }

        if (lease.getLateFeeType() != null && lease.getLateFeeValue() != null) {
            Paragraph lateFee = new Paragraph(
                "DÉCIMA. MORA: En caso de mora en el pago de la renta, se aplicará una penalidad de " +
                lease.getLateFeeValue() + " " + resolveLateFeeTypeLabel(lease.getLateFeeType()) + ".",
                regular
            );
            lateFee.setAlignment(Element.ALIGN_JUSTIFIED);
            lateFee.setSpacingAfter(8);
            doc.add(lateFee);
        }
    }

    private void addSignaturesSection(Document doc, Lease lease, Font bold, Font regular, Font italic) {
        addSectionTitle(doc, "4. Estado de Firmas", bold);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[] { 25, 25, 20, 30 });
        } catch (DocumentException e) {
            log.error("Error setting signature table widths: {}", e.getMessage());
        }
        table.setSpacingAfter(20);

        String[] headers = { "Parte", "Nombre", "Estado", "Fecha y Hora" };
        Font headerFont = new Font(bold);
        headerFont.setSize(9);
        headerFont.setColor(Color.WHITE);

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(h, headerFont));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setBorder(PdfPCell.NO_BORDER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // Landlord row
        boolean landlordSigned = lease.getSignedByLandlordAt() != null;
        Color bg1 = ROW_ALT;
        table.addCell(makeCell("Arrendador", bold, 9, PRIMARY_COLOR, bg1));
        table.addCell(makeCell(safeName(lease.getLandlord()), regular, 9, Color.BLACK, bg1));
        table.addCell(makeCell(landlordSigned ? "Firmado" : "Pendiente", regular, 9, landlordSigned ? SUCCESS_COLOR : Color.RED, bg1));
        table.addCell(makeCell(landlordSigned ? lease.getSignedByLandlordAt().format(DATETIME_FMT) : "—", regular, 9, Color.BLACK, bg1));

        // Tenant row
        boolean tenantSigned = lease.getSignedByTenantAt() != null;
        Color bg2 = Color.WHITE;
        table.addCell(makeCell("Arrendatario", bold, 9, PRIMARY_COLOR, bg2));
        table.addCell(makeCell(safeName(lease.getPrimaryTenant()), regular, 9, Color.BLACK, bg2));
        table.addCell(makeCell(tenantSigned ? "Firmado" : "Pendiente", regular, 9, tenantSigned ? SUCCESS_COLOR : Color.RED, bg2));
        table.addCell(makeCell(tenantSigned ? lease.getSignedByTenantAt().format(DATETIME_FMT) : "—", regular, 9, Color.BLACK, bg2));

        doc.add(table);

        Font noteFont = new Font(italic);
        noteFont.setSize(8);
        noteFont.setColor(MUTED_TEXT);
        Paragraph note = new Paragraph("Las firmas electrónicas registradas tienen validez legal conforme a la " +
                "legislación paraguaya sobre documentos electrónicos y firma digital (Ley 4017/10).", noteFont);
        note.setSpacingAfter(20);
        doc.add(note);
    }

    private void addFooter(Document doc, Lease lease, String generatedAt, Font regular, Font italic) {
        addDivider(doc);

        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(100);

        Font footerFont = new Font(regular);
        footerFont.setSize(8);
        footerFont.setColor(MUTED_TEXT);

        PdfPCell leftCell = new PdfPCell(new Paragraph("OpenRoof · Plataforma Inmobiliaria", footerFont));
        leftCell.setBorder(PdfPCell.NO_BORDER);
        footer.addCell(leftCell);

        Paragraph rightP = new Paragraph("Contrato N° L" + lease.getId() + " · " +
                "Generado: " + generatedAt, footerFont);
        PdfPCell rightCell = new PdfPCell(rightP);
        rightCell.setBorder(PdfPCell.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        footer.addCell(rightCell);

        doc.add(footer);
    }

    private void addSectionTitle(Document doc, String text, Font bold) {
        Font sectionFont = new Font(bold);
        sectionFont.setSize(12);
        sectionFont.setColor(PRIMARY_COLOR);
        Paragraph p = new Paragraph(text, sectionFont);
        p.setSpacingBefore(12);
        p.setSpacingAfter(8);
        doc.add(p);
    }

    private void addInfoRow(PdfPTable table, String label, String value,
            Font bold, Font regular, boolean alt) {
        Color bg = alt ? ROW_ALT : Color.WHITE;

        Font labelFont = new Font(bold);
        labelFont.setSize(9);
        labelFont.setColor(MUTED_TEXT);

        PdfPCell labelCell = new PdfPCell(new Paragraph(label, labelFont));
        labelCell.setBackgroundColor(bg);
        labelCell.setBorderColor(BORDER_COLOR);
        labelCell.setBorderWidth(0.5f);
        labelCell.setPadding(6);
        table.addCell(labelCell);

        Font valueFont = new Font(regular);
        valueFont.setSize(9);
        valueFont.setColor(Color.BLACK);

        PdfPCell valueCell = new PdfPCell(new Paragraph(value != null ? value : "—", valueFont));
        valueCell.setBackgroundColor(bg);
        valueCell.setBorderColor(BORDER_COLOR);
        valueCell.setBorderWidth(0.5f);
        valueCell.setPadding(6);
        table.addCell(valueCell);
    }

    private void addPartiesHeader(PdfPTable table, Font bold) {
        String[] labels = { "Rol", "Nombre", "Contacto" };
        Font headerFont = new Font(bold);
        headerFont.setSize(9);
        headerFont.setColor(Color.WHITE);

        for (String l : labels) {
            PdfPCell cell = new PdfPCell(new Paragraph(l, headerFont));
            cell.setBackgroundColor(ACCENT_COLOR);
            cell.setBorder(PdfPCell.NO_BORDER);
            cell.setPadding(7);
            table.addCell(cell);
        }
    }

    private void addPartyRow(PdfPTable table, String role, String name, String email,
            Font regular, boolean alt) {
        Color bg = alt ? ROW_ALT : Color.WHITE;
        table.addCell(makeCell(role, regular, 9, PRIMARY_COLOR, bg));
        table.addCell(makeCell(name, regular, 9, Color.BLACK, bg));
        table.addCell(makeCell(email, regular, 8, MUTED_TEXT, bg));
    }

    private PdfPCell makeCell(String text, Font font, float size, Color color, Color bg) {
        Font cellFont = new Font(font);
        cellFont.setSize(size);
        cellFont.setColor(color);

        PdfPCell cell = new PdfPCell(new Paragraph(text != null ? text : "—", cellFont));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBorderWidth(0.5f);
        cell.setPadding(6);
        return cell;
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "—";
    }

    private String formatCurrency(BigDecimal amount, String currencyCode) {
        if (amount == null) return "—";
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.of("es", "PY"));
        try {
            fmt.setCurrency(java.util.Currency.getInstance(currencyCode));
        } catch (Exception e) {
            log.warn("Código de moneda inválido: {}. Usando default.", currencyCode);
        }
        return fmt.format(amount);
    }

    private String resolveLeaseTypeLabel(com.openroof.openroof.model.enums.LeaseType type) {
        return switch (type) {
            case FIXED_TERM -> "Plazo fijo";
            case MONTH_TO_MONTH -> "Mes a mes";
            default -> type.name();
        };
    }

    private String resolveLateFeeTypeLabel(com.openroof.openroof.model.enums.LateFeeType type) {
        return switch (type) {
            case PERCENTAGE -> "%";
            case FIXED_AMOUNT -> "(monto fijo)";
            default -> type.name();
        };
    }

    private String safeName(User user) {
        return (user != null && user.getName() != null) ? user.getName() : "[NOMBRE NO DISPONIBLE]";
    }

    private String safeEmail(User user) {
        return (user != null && user.getEmail() != null) ? user.getEmail() : "[CORREO NO DISPONIBLE]";
    }
}
