package com.openroof.openroof.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
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

    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(26, 60, 94); // #1a3c5e
    private static final DeviceRgb ACCENT_COLOR = new DeviceRgb(20, 120, 180); // #1478b4
    private static final DeviceRgb LIGHT_BG = new DeviceRgb(240, 249, 255);// #f0f9ff
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(186, 230, 253);// #bae6fd
    private static final DeviceRgb MUTED_TEXT = new DeviceRgb(100, 116, 139);// #64748b
    private static final DeviceRgb SUCCESS_COLOR = new DeviceRgb(22, 163, 74); // #16a34a
    private static final DeviceRgb WARNING_COLOR = new DeviceRgb(217, 119, 6); // #d97706
    private static final DeviceRgb ROW_ALT = new DeviceRgb(248, 250, 252);// #f8fafc

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FMT = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    private final ContractRepository contractRepository;
    private final ContractSignatureRepository signatureRepository;
    private final UserRepository userRepository;

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
        } catch (IOException e) {
            log.error("Error generando PDF para contrato {}: {}", contractId, e.getMessage(), e);
            throw new BadRequestException("No se pudo generar el PDF del contrato");
        }
    }

    // ─── Validación de acceso ─────────────────────────────────────────────────

    private void validateAccess(Contract contract, User requester) {
        Long uid = requester.getId();
        boolean isAdmin = requester.getRole().name().equals("ADMIN");
        boolean isBuyer = contract.getBuyer() != null && uid.equals(contract.getBuyer().getId());
        boolean isSeller = contract.getSeller() != null && uid.equals(contract.getSeller().getId());
        boolean isListing = contract.getListingAgent() != null &&
                uid.equals(contract.getListingAgent().getUser().getId());
        boolean isBuyerAg = contract.getBuyerAgent() != null &&
                uid.equals(contract.getBuyerAgent().getUser().getId());

        if (!isAdmin && !isBuyer && !isSeller && !isListing && !isBuyerAg) {
            throw new BadRequestException("No tiene permiso para descargar este contrato");
        }
    }

    // ─── Construcción del PDF ─────────────────────────────────────────────────

    private byte[] buildPdf(Contract contract, List<ContractSignature> signatures) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(50, 50, 60, 50);

        PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont italic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

        addHeader(doc, contract, bold, regular);
        addDivider(doc);
        addInfoSection(doc, contract, bold, regular);
        addPartiesSection(doc, contract, bold, regular, italic);
        
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addTermsSection(doc, contract, bold, regular);
        
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        addSignaturesSection(doc, signatures, bold, regular, italic);
        
        addFooter(doc, contract, regular, italic);

        doc.close();
        return baos.toByteArray();
    }

    // ─── Secciones del documento ──────────────────────────────────────────────

    private void addHeader(Document doc, Contract contract, PdfFont bold, PdfFont regular) {
        // Logo / marca
        Paragraph brand = new Paragraph("OpenRoof")
                .setFont(bold)
                .setFontSize(22)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(2);
        doc.add(brand);

        Paragraph tagline = new Paragraph("Plataforma Inmobiliaria · Contrato Legal")
                .setFont(regular)
                .setFontSize(9)
                .setFontColor(MUTED_TEXT)
                .setMarginBottom(16);
        doc.add(tagline);

        // Título del contrato
        String typeLabel = contract.getContractType() == ContractType.SALE
                ? "COMPRAVENTA"
                : "ARRENDAMIENTO";

        Paragraph title = new Paragraph("CONTRATO DE " + typeLabel)
                .setFont(bold)
                .setFontSize(18)
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4);
        doc.add(title);

        Paragraph subtitle = new Paragraph("Contrato N° " + contract.getId() +
                " · " + resolveStatusLabel(contract.getStatus()))
                .setFont(regular)
                .setFontSize(10)
                .setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(8);
        doc.add(subtitle);

        // Preámbulo Formal
        Paragraph preamble = new Paragraph()
                .setFont(regular)
                .setFontSize(10)
                .setMarginTop(12)
                .setMarginBottom(12)
                .setTextAlignment(TextAlignment.JUSTIFIED);

        preamble.add("En la ciudad de Asunción, República del Paraguay, comparecen por una parte ");
        preamble.add(new Text(contract.getSeller().getName()).setFont(bold));
        preamble.add(" en adelante denominado el ");
        preamble.add(new Text(contract.getContractType() == ContractType.SALE ? "VENDEDOR" : "LOCADOR").setFont(bold));
        preamble.add(", y por la otra parte ");
        preamble.add(new Text(contract.getBuyer().getName()).setFont(bold));
        preamble.add(" en adelante denominado el ");
        preamble.add(new Text(contract.getContractType() == ContractType.SALE ? "COMPRADOR" : "LOCATARIO").setFont(bold));
        preamble.add(", quienes convienen en celebrar el presente instrumento legal sujeto a las cláusulas descritas a continuación.");

        doc.add(preamble);
    }

    private void addDivider(Document doc) {
        Table divider = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell cell = new Cell()
                .setHeight(2)
                .setBackgroundColor(PRIMARY_COLOR)
                .setBorder(Border.NO_BORDER)
                .setMarginBottom(16);
        divider.addCell(cell);
        doc.add(divider);
    }

    private void addInfoSection(Document doc, Contract contract, PdfFont bold, PdfFont regular) {
        addSectionTitle(doc, "1. Información General", bold);

        Table table = new Table(UnitValue.createPercentArray(new float[] { 30, 70 }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        addInfoRow(table, "Propiedad", contract.getProperty().getTitle(), bold, regular, false);
        addInfoRow(table, "Tipo", resolveTypeLabel(contract.getContractType()), bold, regular, true);
        addInfoRow(table, "Monto", formatCurrency(contract.getAmount()), bold, regular, false);
        addInfoRow(table, "Fecha de inicio", formatDate(contract.getStartDate()), bold, regular, true);
        addInfoRow(table, "Fecha de fin", formatDate(contract.getEndDate()), bold, regular, false);
        addInfoRow(table, "Comisión total", formatPct(contract.getCommissionPct()), bold, regular, true);
        addInfoRow(table, "Generado el", LocalDateTime.now().format(DATETIME_FMT), bold, regular, false);

        doc.add(table);
    }

    private void addPartiesSection(Document doc, Contract contract,
            PdfFont bold, PdfFont regular, PdfFont italic) {
        addSectionTitle(doc, "2. Partes del Contrato", bold);

        Table table = new Table(UnitValue.createPercentArray(new float[] { 25, 37.5f, 37.5f }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Cabecera
        addPartiesHeader(table, bold);

        // Vendedor
        addPartyRow(table, "Vendedor / Propietario",
                contract.getSeller().getName(),
                contract.getSeller().getEmail(), regular, false);

        // Comprador
        addPartyRow(table, "Comprador / Inquilino",
                contract.getBuyer().getName(),
                contract.getBuyer().getEmail(), regular, true);

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

    private void addTermsSection(Document doc, Contract contract, PdfFont bold, PdfFont regular) {
        Paragraph title = new Paragraph("CLÁUSULAS DEL CONTRATO")
                .setFont(bold)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        doc.add(title);

        if (contract.getTerms() == null || contract.getTerms().isBlank()) {
            doc.add(new Paragraph("No se han definido términos adicionales para este instrumento.")
                    .setFont(regular).setFontSize(10).setItalic());
            return;
        }

        String[] lines = contract.getTerms().split("\n");
        for (String line : lines) {
            if (line.isBlank()) {
                doc.add(new Paragraph("\u00a0").setFontSize(6));
                continue;
            }

            boolean isClauseHeader = line.matches("(?i)^(CLÁUSULA|DÉCIMA|PRIMERA|SEGUNDA|TERCERA|CUARTA|QUINTA|SEXTA|SÉPTIMA|OCTAVA|NOVENA|\\d+\\.).*");
            
            Paragraph p = new Paragraph(line)
                    .setFont(isClauseHeader ? bold : regular)
                    .setFontSize(isClauseHeader ? 10 : 10)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(isClauseHeader ? 6 : 4)
                    .setFirstLineIndent(isClauseHeader ? 0 : 20);
            
            doc.add(p);
        }
    }

    private void addSignaturesSection(Document doc, List<ContractSignature> signatures,
            PdfFont bold, PdfFont regular, PdfFont italic) {
        addSectionTitle(doc, "4. Estado de Firmas", bold);

        if (signatures.isEmpty()) {
            doc.add(new Paragraph("No se han registrado firmas aún.")
                    .setFont(italic)
                    .setFontSize(10)
                    .setFontColor(MUTED_TEXT)
                    .setMarginBottom(20));
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[] { 25, 25, 20, 30 }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Encabezado de tabla
        String[] headers = { "Parte", "Nombre", "Tipo de Firma", "Fecha y Hora" };
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(PRIMARY_COLOR)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(6)
                    .add(new Paragraph(h)
                            .setFont(bold)
                            .setFontSize(9)
                            .setFontColor(ColorConstants.WHITE)));
        }

        boolean alt = false;
        for (ContractSignature sig : signatures) {
            com.itextpdf.kernel.colors.Color bg = alt ? ROW_ALT : ColorConstants.WHITE;
            alt = !alt;

            table.addCell(makeCell(resolveRoleLabel(sig.getRole()), bold, 9, PRIMARY_COLOR, bg));
            table.addCell(makeCell(sig.getSigner().getName(), regular, 9, ColorConstants.BLACK, bg));
            table.addCell(makeCell(resolveSignatureTypeLabel(sig.getSignatureType().name()), regular, 9,
                    ColorConstants.BLACK, bg));
            table.addCell(makeCell(
                    sig.getSignedAt() != null ? sig.getSignedAt().format(DATETIME_FMT) : "—",
                    regular, 9, SUCCESS_COLOR, bg));
        }

        doc.add(table);

        // Nota legal
        doc.add(new Paragraph("Las firmas electrónicas registradas tienen validez legal conforme a la " +
                "legislación paraguaya sobre documentos electrónicos y firma digital (Ley 4017/10).")
                .setFont(italic)
                .setFontSize(8)
                .setFontColor(MUTED_TEXT)
                .setMarginBottom(20));
    }

    private void addFooter(Document doc, Contract contract, PdfFont regular, PdfFont italic) {
        addDivider(doc);

        Table footer = new Table(UnitValue.createPercentArray(new float[] { 50, 50 }))
                .useAllAvailableWidth();

        footer.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("OpenRoof · Plataforma Inmobiliaria")
                        .setFont(regular).setFontSize(8).setFontColor(MUTED_TEXT)));

        footer.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Contrato N° " + contract.getId() + " · " +
                        "Generado: " + LocalDateTime.now().format(DATETIME_FMT))
                        .setFont(regular).setFontSize(8).setFontColor(MUTED_TEXT)
                        .setTextAlignment(TextAlignment.RIGHT)));

        doc.add(footer);
    }

    // ─── Helpers de tabla ─────────────────────────────────────────────────────

    private void addSectionTitle(Document doc, String text, PdfFont bold) {
        doc.add(new Paragraph(text)
                .setFont(bold)
                .setFontSize(12)
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(12)
                .setMarginBottom(8));
    }

    private void addInfoRow(Table table, String label, String value,
            PdfFont bold, PdfFont regular, boolean alt) {
        com.itextpdf.kernel.colors.Color bg = alt ? ROW_ALT : ColorConstants.WHITE;

        table.addCell(new Cell()
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(BORDER_COLOR, 0.5f))
                .setPadding(6)
                .add(new Paragraph(label)
                        .setFont(bold)
                        .setFontSize(9)
                        .setFontColor(MUTED_TEXT)));

        table.addCell(new Cell()
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(BORDER_COLOR, 0.5f))
                .setPadding(6)
                .add(new Paragraph(value != null ? value : "—")
                        .setFont(regular)
                        .setFontSize(9)
                        .setFontColor(ColorConstants.BLACK)));
    }

    private void addPartiesHeader(Table table, PdfFont bold) {
        String[] labels = { "Rol", "Nombre", "Contacto" };
        for (String l : labels) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(ACCENT_COLOR)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(7)
                    .add(new Paragraph(l)
                            .setFont(bold)
                            .setFontSize(9)
                            .setFontColor(ColorConstants.WHITE)));
        }
    }

    private void addPartyRow(Table table, String role, String name, String email,
            PdfFont regular, boolean alt) {
        com.itextpdf.kernel.colors.Color bg = alt ? ROW_ALT : ColorConstants.WHITE;
        table.addCell(makeCell(role, regular, 9, PRIMARY_COLOR, bg));
        table.addCell(makeCell(name, regular, 9, ColorConstants.BLACK, bg));
        table.addCell(makeCell(email, regular, 8, MUTED_TEXT, bg));
    }

    private Cell makeCell(String text, PdfFont font, float size, com.itextpdf.kernel.colors.Color color,
            com.itextpdf.kernel.colors.Color bg) {
        return new Cell()
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(BORDER_COLOR, 0.5f))
                .setPadding(6)
                .add(new Paragraph(text != null ? text : "—")
                        .setFont(font)
                        .setFontSize(size)
                        .setFontColor(color));
    }

    // ─── Helpers de formato ───────────────────────────────────────────────────

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "—";
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null)
            return "—";
        return CURRENCY_FMT.format(amount);
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
}
