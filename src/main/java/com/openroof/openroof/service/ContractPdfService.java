package com.openroof.openroof.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.contract.ContractSignature;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;
import com.openroof.openroof.model.enums.SignatureRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.ContractRepository;
import com.openroof.openroof.repository.ContractSignatureRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContractPdfService {

    private static final Color PRIMARY_COLOR = new Color(26, 60, 94); // #1a3c5e
    private static final Color ACCENT_COLOR = new Color(20, 120, 180); // #1478b4
    private static final Color BORDER_COLOR = new Color(186, 230, 253);// #bae6fd
    private static final Color MUTED_TEXT = new Color(100, 116, 139);// #64748b
    private static final Color SUCCESS_COLOR = new Color(22, 163, 74); // #16a34a
    private static final Color ROW_ALT = new Color(248, 250, 252);// #f8fafc

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ContractRepository contractRepository;
    private final ContractSignatureRepository signatureRepository;
    private final UserRepository userRepository;
    private final ContractService contractService;

    /**
     * Genera el PDF del contrato como array de bytes.
     *
     * @param contractId     ID del contrato
     * @param requesterEmail email del usuario autenticado (para validar acceso)
     * @return bytes del PDF generado
     */
    public byte[] generatePdf(Long contractId, String requesterEmail) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado"));

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        validateAccess(contract, requester);

        List<ContractSignature> signatures = signatureRepository.findByContractIdAndDeletedAtIsNull(contractId);

        try {
            return buildPdf(contract, signatures);
        } catch (Exception e) {
            log.error("Error inesperado generando PDF para contrato {}: {}", contractId, e.getMessage(), e);
            throw new BadRequestException("No se pudo generar el PDF del contrato: " + e.getMessage());
        }
    }

    // ─── Validación de acceso ─────────────────────────────────────────────────

    private void validateAccess(Contract contract, User requester) {
        if (!contractService.canAccess(contract, requester)) {
            throw new BadRequestException("No tiene permiso para descargar este contrato");
        }
        if (contract.getSeller() == null || contract.getBuyer() == null || contract.getProperty() == null) {
            throw new BadRequestException("El contrato está incompleto (faltan partes o propiedad) y no puede ser generado.");
        }
    }

    // ─── Construcción del PDF ─────────────────────────────────────────────────

    private byte[] buildPdf(Contract contract, List<ContractSignature> signatures) throws Exception {
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

                addHeader(doc, contract, bold, regular);
                addDivider(doc);
                addInfoSection(doc, contract, generatedAt, bold, regular);
                addPartiesSection(doc, contract, bold, regular, italic);
                
                doc.newPage();
                addTermsSection(doc, contract, bold, regular);
                
                doc.newPage();
                addSignaturesSection(doc, signatures, bold, regular, italic);
                
                addFooter(doc, contract, generatedAt, regular, italic);
            } finally {
                if (doc.isOpen()) {
                    doc.close();
                }
            }
            return baos.toByteArray();
        }
    }

    // ─── Secciones del documento ──────────────────────────────────────────────

    private void addHeader(Document doc, Contract contract, Font bold, Font regular) {
        // Logo / marca
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PRIMARY_COLOR);
        Paragraph brand = new Paragraph("OpenRoof", brandFont);
        brand.setSpacingAfter(2);
        doc.add(brand);

        Font taglineFont = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED_TEXT);
        Paragraph tagline = new Paragraph("Plataforma Inmobiliaria · Contrato Legal", taglineFont);
        tagline.setSpacingAfter(16);
        doc.add(tagline);

        // Título del contrato
        String typeLabel = contract.getContractType() == ContractType.SALE
                ? "COMPRAVENTA"
                : "ARRENDAMIENTO";

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
        Paragraph title = new Paragraph("CONTRATO DE " + typeLabel, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED_TEXT);
        Paragraph subtitle = new Paragraph("Contrato N° " + contract.getId() +
                " · " + resolveStatusLabel(contract.getStatus()), subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(8);
        doc.add(subtitle);

        // Preámbulo Formal
        Paragraph preamble = new Paragraph();
        preamble.setFont(regular);
        preamble.setAlignment(Element.ALIGN_JUSTIFIED);
        preamble.setSpacingBefore(12);
        preamble.setSpacingAfter(12);

        preamble.add("En la ciudad de Asunción, República del Paraguay, comparecen por una parte ");
        preamble.add(new Chunk(safeName(contract.getSeller()), bold));
        preamble.add(" en adelante denominado el ");
        preamble.add(new Chunk(contract.getContractType() == ContractType.SALE ? "VENDEDOR" : "LOCADOR", bold));
        preamble.add(", y por la otra parte ");
        preamble.add(new Chunk(safeName(contract.getBuyer()), bold));
        preamble.add(" en adelante denominado el ");
        preamble.add(new Chunk(contract.getContractType() == ContractType.SALE ? "COMPRADOR" : "LOCATARIO", bold));
        preamble.add(", quienes convienen en celebrar el presente instrumento legal sujeto a las cláusulas descritas a continuación.");

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

    private void addInfoSection(Document doc, Contract contract, String generatedAt, Font bold, Font regular) {
        addSectionTitle(doc, "1. Información General", bold);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[] { 30, 70 });
        } catch (DocumentException e) {
            log.error("Error setting table widths: {}", e.getMessage());
        }
        table.setSpacingAfter(20);

        String propertyTitle = contract.getProperty() != null ? contract.getProperty().getTitle() : "[PROPIEDAD NO DISPONIBLE]";
        addInfoRow(table, "Propiedad", propertyTitle, bold, regular, false);
        addInfoRow(table, "Tipo", resolveTypeLabel(contract.getContractType()), bold, regular, true);
        addInfoRow(table, "Monto", formatCurrency(contract.getAmount(), contract.getCurrencyCode()), bold, regular, false);
        addInfoRow(table, "Fecha de inicio", formatDate(contract.getStartDate()), bold, regular, true);
        addInfoRow(table, "Fecha de fin", formatDate(contract.getEndDate()), bold, regular, false);
        addInfoRow(table, "Comisión total", formatPct(contract.getCommissionPct()), bold, regular, true);
        addInfoRow(table, "Generado el", generatedAt, bold, regular, false);

        doc.add(table);
    }

    private void addPartiesSection(Document doc, Contract contract,
            Font bold, Font regular, Font italic) {
        addSectionTitle(doc, "2. Partes del Contrato", bold);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[] { 25, 37.5f, 37.5f });
        } catch (DocumentException e) {
            log.error("Error setting table widths: {}", e.getMessage());
        }
        table.setSpacingAfter(20);

        // Cabecera
        addPartiesHeader(table, bold);

        // Vendedor
        addPartyRow(table, "Vendedor / Propietario",
                safeName(contract.getSeller()),
                safeEmail(contract.getSeller()), regular, false);

        // Comprador
        addPartyRow(table, "Comprador / Inquilino",
                safeName(contract.getBuyer()),
                safeEmail(contract.getBuyer()), regular, true);

        // Agente listador (opcional)
        if (contract.getListingAgent() != null) {
            addPartyRow(table, "Agente Listador",
                    contract.getListingAgent().getUser().getName(),
                    contract.getListingAgent().getUser().getEmail(), regular, false);
        }

        // Agente comprador (opcional)
        if (contract.getBuyerAgent() != null) {
            addPartyRow(table, "Agente Comprador",
                    contract.getBuyerAgent().getUser().getName(),
                    contract.getBuyerAgent().getUser().getEmail(), regular,
                    contract.getListingAgent() == null); // alt row
        }

        doc.add(table);
    }

    private void addTermsSection(Document doc, Contract contract, Font bold, Font regular) {
        addSectionTitle(doc, "3. Cláusulas del Contrato", bold);

        if (contract.getTerms() == null || contract.getTerms().isBlank()) {
            Font italicFont = new Font(regular);
            italicFont.setStyle(Font.ITALIC);
            doc.add(new Paragraph("No se han definido términos adicionales para este instrumento.", italicFont));
            return;
        }

        String[] lines = contract.getTerms().split("\n");
        for (String line : lines) {
            if (line.isBlank()) {
                doc.add(new Paragraph(" "));
                continue;
            }

            boolean isClauseHeader = line.matches("(?i)^(CLÁUSULA\\s|DÉCIM[OA](\\s+PRIMER[OA])?|PRIMER[OA]|SEGUND[OA]|TERCER[OA]|CUART[OA]|QUINT[OA]|SEXT[OA]|SÉPTIM[OA]|OCTAV[OA]|NOVEN[OA]|UNDÉCIM[OA]|DUODÉCIM[OA]|\\d+[\\.\\)]).*");
            
            Paragraph p = new Paragraph(line, isClauseHeader ? bold : regular);
            p.setAlignment(Element.ALIGN_JUSTIFIED);
            p.setSpacingAfter(isClauseHeader ? 6 : 4);
            if (!isClauseHeader) {
                p.setIndentationLeft(20);
            }
            
            doc.add(p);
        }
    }

    private void addSignaturesSection(Document doc, List<ContractSignature> signatures,
            Font bold, Font regular, Font italic) {
        addSectionTitle(doc, "4. Estado de Firmas", bold);

        if (signatures.isEmpty()) {
            Font mutedItalic = new Font(italic);
            mutedItalic.setSize(10);
            mutedItalic.setColor(MUTED_TEXT);
            Paragraph p = new Paragraph("No se han registrado firmas aún.", mutedItalic);
            p.setSpacingAfter(20);
            doc.add(p);
            return;
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[] { 25, 25, 20, 30 });
        } catch (DocumentException e) {
            log.error("Error setting signature table widths: {}", e.getMessage());
        }
        table.setSpacingAfter(20);

        // Encabezado de tabla
        String[] headers = { "Parte", "Nombre", "Tipo de Firma", "Fecha y Hora" };
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

        boolean alt = false;
        for (ContractSignature sig : signatures) {
            Color bg = alt ? ROW_ALT : Color.WHITE;
            alt = !alt;

            table.addCell(makeCell(resolveRoleLabel(sig.getRole()), bold, 9, PRIMARY_COLOR, bg));
            table.addCell(makeCell(sig.getSigner().getName(), regular, 9, Color.BLACK, bg));
            table.addCell(makeCell(resolveSignatureTypeLabel(sig.getSignatureType().name()), regular, 9,
                    Color.BLACK, bg));
            table.addCell(makeCell(
                    sig.getSignedAt() != null ? sig.getSignedAt().format(DATETIME_FMT) : "—",
                    regular, 9, SUCCESS_COLOR, bg));
        }

        doc.add(table);

        // Nota legal
        Font noteFont = new Font(italic);
        noteFont.setSize(8);
        noteFont.setColor(MUTED_TEXT);
        Paragraph note = new Paragraph("Las firmas electrónicas registradas tienen validez legal conforme a la " +
                "legislación paraguaya sobre documentos electrónicos y firma digital (Ley 4017/10).", noteFont);
        note.setSpacingAfter(20);
        doc.add(note);
    }

    private void addFooter(Document doc, Contract contract, String generatedAt, Font regular, Font italic) {
        addDivider(doc);

        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(100);

        Font footerFont = new Font(regular);
        footerFont.setSize(8);
        footerFont.setColor(MUTED_TEXT);

        PdfPCell leftCell = new PdfPCell(new Paragraph("OpenRoof · Plataforma Inmobiliaria", footerFont));
        leftCell.setBorder(PdfPCell.NO_BORDER);
        footer.addCell(leftCell);

        Paragraph rightP = new Paragraph("Contrato N° " + contract.getId() + " · " +
                "Generado: " + generatedAt, footerFont);
        PdfPCell rightCell = new PdfPCell(rightP);
        rightCell.setBorder(PdfPCell.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        footer.addCell(rightCell);

        doc.add(footer);
    }

    // ─── Helpers de tabla ─────────────────────────────────────────────────────

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

    private PdfPCell makeCell(String text, Font font, float size, Color color,
            Color bg) {
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

    // ─── Helpers de formato ───────────────────────────────────────────────────

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "—";
    }

    private String formatCurrency(BigDecimal amount, String currencyCode) {
        if (amount == null)
            return "—";
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.of("es", "PY"));
        try {
            fmt.setCurrency(java.util.Currency.getInstance(currencyCode));
        } catch (Exception e) {
            log.warn("Código de moneda inválido: {}. Usando default.", currencyCode);
        }
        return fmt.format(amount);
    }

    private String formatPct(BigDecimal pct) {
        if (pct == null)
            return "0%";
        return pct.stripTrailingZeros().toPlainString() + "%";
    }

    private String resolveTypeLabel(ContractType type) {
        return switch (type) {
            case SALE -> "Compraventa";
            case RENT -> "Arrendamiento";
            default -> type.name();
        };
    }

    private String resolveStatusLabel(ContractStatus status) {
        return switch (status) {
            case DRAFT -> "Borrador";
            case SENT -> "Enviado";
            case PARTIALLY_SIGNED -> "Parcialmente firmado";
            case SIGNED -> "Firmado";
            case REJECTED -> "Rechazado";
            case EXPIRED -> "Expirado";
            case CANCELLED -> "Cancelado";
        };
    }

    private String resolveRoleLabel(SignatureRole role) {
        return switch (role) {
            case BUYER -> "Comprador";
            case SELLER -> "Vendedor";
            case LISTING_AGENT -> "Agente Listador";
            case BUYER_AGENT -> "Agente Comprador";
            case AGENT -> "Agente";
            case WITNESS -> "Testigo";
        };
    }

    private String resolveSignatureTypeLabel(String type) {
        return switch (type) {
            case "ELECTRONIC" -> "Electrónica";
            case "DIGITAL" -> "Digital";
            case "HANDWRITTEN_SCAN" -> "Manuscrita";
            default -> type;
        };
    }
 
    private String safeName(User user) {
        return (user != null && user.getName() != null) ? user.getName() : "[NOMBRE NO DISPONIBLE]";
    }
 
    private String safeEmail(User user) {
        return (user != null && user.getEmail() != null) ? user.getEmail() : "[CORREO NO DISPONIBLE]";
    }
}
