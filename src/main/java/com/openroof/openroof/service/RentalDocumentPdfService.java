package com.openroof.openroof.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.LeasePayment;
import com.openroof.openroof.model.rental.RentalInstallment;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class RentalDocumentPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Color PRIMARY = new Color(26, 86, 219);
    private static final Color DARK = new Color(31, 41, 55);
    private static final Color MUTED = new Color(107, 114, 128);
    private static final Color LIGHT_BG = new Color(243, 246, 251);
    private static final Color BORDER = new Color(220, 226, 235);

    public byte[] generateInvoice(RentalInstallment installment) {
        Lease lease = installment.getLease();
        String number = installment.getInvoiceNumber() != null ? installment.getInvoiceNumber() : "FAC-" + installment.getId();
        return buildPdf("FACTURA", "Factura de alquiler", number, document -> {
            addParties(document, "Facturar a", userName(lease.getPrimaryTenant()), lease.getPrimaryTenant().getEmail(),
                    "Emitido por", userName(lease.getLandlord()), lease.getLandlord().getEmail());

            PdfPTable meta = infoTable();
            addInfoRow(meta, "Contrato", "#" + lease.getId());
            addInfoRow(meta, "Propiedad", propertyTitle(lease));
            addInfoRow(meta, "Periodo", formatDate(installment.getPeriodStart()) + " a " + formatDate(installment.getPeriodEnd()));
            addInfoRow(meta, "Vencimiento", formatDate(installment.getDueDate()));
            addInfoRow(meta, "Estado", statusLabel(String.valueOf(installment.getStatus())));
            document.add(meta);

            addSectionTitle(document, "Detalle de la factura");
            PdfPTable detail = detailTable();
            addDetailHeader(detail);
            addDetailRow(detail,
                    "Renta del periodo " + formatDate(installment.getPeriodStart()) + " - " + formatDate(installment.getPeriodEnd()),
                    "1",
                    money(installment.getTotalAmount(), lease.getCurrency()),
                    money(installment.getTotalAmount(), lease.getCurrency()));
            document.add(detail);

            addTotals(document, lease.getCurrency(), installment.getTotalAmount(), installment.getPaidAmount(), installment.getBalance());
        });
    }

    public byte[] generateReceipt(LeasePayment payment) {
        Lease lease = payment.getLease();
        RentalInstallment installment = payment.getInstallment();
        String number = payment.getReceiptNumber() != null ? payment.getReceiptNumber() : "REC-" + payment.getId();
        return buildPdf("RECIBO", "Recibo de pago", number, document -> {
            addParties(document, "Recibido de", userName(payment.getPayer()), payment.getPayer().getEmail(),
                    "Recibido por", userName(lease.getLandlord()), lease.getLandlord().getEmail());

            PdfPTable meta = infoTable();
            addInfoRow(meta, "Contrato", "#" + lease.getId());
            addInfoRow(meta, "Propiedad", propertyTitle(lease));
            addInfoRow(meta, "Fecha de pago", payment.getPaidAt() != null ? payment.getPaidAt().format(DATE) : "-");
            addInfoRow(meta, "Metodo", methodLabel(String.valueOf(payment.getMethod())));
            addInfoRow(meta, "Estado", statusLabel(String.valueOf(payment.getStatus())));
            if (installment != null) {
                addInfoRow(meta, "Cuota", "#" + installment.getInstallmentNumber());
                addInfoRow(meta, "Periodo", formatDate(installment.getPeriodStart()) + " a " + formatDate(installment.getPeriodEnd()));
            }
            document.add(meta);

            addSectionTitle(document, "Detalle del recibo");
            PdfPTable detail = detailTable();
            addDetailHeader(detail);
            addDetailRow(detail,
                    installment != null ? "Pago de cuota #" + installment.getInstallmentNumber() : "Pago de alquiler",
                    "1",
                    money(payment.getAmount(), payment.getCurrency()),
                    money(payment.getAmount(), payment.getCurrency()));
            document.add(detail);

            addPaidSummary(document, payment.getCurrency(), payment.getAmount());
        });
    }

    private byte[] buildPdf(String documentType, String title, String number, DocumentWriter writer) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 42, 42, 40, 42);
            PdfWriter.getInstance(document, out);
            document.addTitle(title + " " + (number != null ? number : ""));
            document.addAuthor("OpenRoof");
            document.open();

            addHeader(document, documentType, title, number);
            writer.write(document);
            addFooter(document);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    private void addHeader(Document document, String documentType, String title, String number) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.4f, 1f});
        header.setSpacingAfter(24);

        PdfPCell brand = noBorderCell();
        Paragraph logo = new Paragraph("OpenRoof", font(FontFactory.HELVETICA_BOLD, 22, PRIMARY));
        Paragraph subtitle = new Paragraph("Gestion inmobiliaria y pagos de alquiler", font(FontFactory.HELVETICA, 9, MUTED));
        brand.addElement(logo);
        brand.addElement(subtitle);

        PdfPCell docInfo = noBorderCell();
        docInfo.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph type = new Paragraph(documentType, font(FontFactory.HELVETICA_BOLD, 20, DARK));
        type.setAlignment(Element.ALIGN_RIGHT);
        Paragraph name = new Paragraph(title, font(FontFactory.HELVETICA, 10, MUTED));
        name.setAlignment(Element.ALIGN_RIGHT);
        Paragraph docNumber = new Paragraph("Nro. " + safe(number), font(FontFactory.HELVETICA_BOLD, 11, PRIMARY));
        docNumber.setAlignment(Element.ALIGN_RIGHT);
        Paragraph date = new Paragraph("Fecha de emision: " + LocalDate.now().format(DATE), font(FontFactory.HELVETICA, 9, MUTED));
        date.setAlignment(Element.ALIGN_RIGHT);
        docInfo.addElement(type);
        docInfo.addElement(name);
        docInfo.addElement(docNumber);
        docInfo.addElement(date);

        header.addCell(brand);
        header.addCell(docInfo);
        document.add(header);
    }

    private void addParties(Document document, String leftTitle, String leftName, String leftEmail,
                            String rightTitle, String rightName, String rightEmail) throws Exception {
        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100);
        parties.setWidths(new float[]{1f, 1f});
        parties.setSpacingAfter(18);
        parties.addCell(partyCell(leftTitle, leftName, leftEmail));
        parties.addCell(partyCell(rightTitle, rightName, rightEmail));
        document.add(parties);
    }

    private PdfPCell partyCell(String title, String name, String email) {
        PdfPCell cell = borderedCell();
        cell.setBackgroundColor(LIGHT_BG);
        cell.addElement(new Paragraph(title, font(FontFactory.HELVETICA_BOLD, 9, PRIMARY)));
        cell.addElement(new Paragraph(safe(name), font(FontFactory.HELVETICA_BOLD, 11, DARK)));
        cell.addElement(new Paragraph(safe(email), font(FontFactory.HELVETICA, 9, MUTED)));
        return cell;
    }

    private PdfPTable infoTable() throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 2.5f});
        table.setSpacingAfter(18);
        return table;
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = borderedCell(new Phrase(label, font(FontFactory.HELVETICA_BOLD, 9, MUTED)));
        PdfPCell valueCell = borderedCell(new Phrase(safe(value), font(FontFactory.HELVETICA, 10, DARK)));
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addSectionTitle(Document document, String title) throws Exception {
        Paragraph paragraph = new Paragraph(title, font(FontFactory.HELVETICA_BOLD, 12, DARK));
        paragraph.setSpacingBefore(4);
        paragraph.setSpacingAfter(8);
        document.add(paragraph);
    }

    private PdfPTable detailTable() throws Exception {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3.2f, 0.7f, 1.2f, 1.2f});
        table.setSpacingAfter(14);
        return table;
    }

    private void addDetailHeader(PdfPTable table) {
        addHeaderCell(table, "Descripcion");
        addHeaderCell(table, "Cant.");
        addHeaderCell(table, "Precio");
        addHeaderCell(table, "Total");
    }

    private void addDetailRow(PdfPTable table, String description, String quantity, String price, String total) {
        addBodyCell(table, description, Element.ALIGN_LEFT);
        addBodyCell(table, quantity, Element.ALIGN_CENTER);
        addBodyCell(table, price, Element.ALIGN_RIGHT);
        addBodyCell(table, total, Element.ALIGN_RIGHT);
    }

    private void addTotals(Document document, String currency, BigDecimal total, BigDecimal paid, BigDecimal balance) throws Exception {
        PdfPTable table = totalsTable();
        addTotalRow(table, "Subtotal", money(total, currency), false);
        addTotalRow(table, "Pagado", money(paid, currency), false);
        addTotalRow(table, "Saldo", money(balance, currency), true);
        document.add(table);
    }

    private void addPaidSummary(Document document, String currency, BigDecimal amount) throws Exception {
        PdfPTable table = totalsTable();
        addTotalRow(table, "Total recibido", money(amount, currency), true);
        document.add(table);
    }

    private PdfPTable totalsTable() throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(48);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setWidths(new float[]{1.1f, 1.2f});
        table.setSpacingAfter(26);
        return table;
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean highlight) {
        PdfPCell labelCell = borderedCell(new Phrase(label, font(FontFactory.HELVETICA_BOLD, highlight ? 11 : 9, highlight ? PRIMARY : MUTED)));
        PdfPCell valueCell = borderedCell(new Phrase(value, font(FontFactory.HELVETICA_BOLD, highlight ? 11 : 9, highlight ? PRIMARY : DARK)));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (highlight) {
            labelCell.setBackgroundColor(LIGHT_BG);
            valueCell.setBackgroundColor(LIGHT_BG);
        }
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addFooter(Document document) throws Exception {
        Paragraph thanks = new Paragraph("Gracias por utilizar OpenRoof.", font(FontFactory.HELVETICA_BOLD, 10, DARK));
        thanks.setSpacingBefore(8);
        thanks.setAlignment(Element.ALIGN_CENTER);
        document.add(thanks);

        Paragraph note = new Paragraph("Documento generado automaticamente. Conserve este comprobante para sus registros.", font(FontFactory.HELVETICA, 8, MUTED));
        note.setAlignment(Element.ALIGN_CENTER);
        document.add(note);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = borderedCell(new Phrase(text, font(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        cell.setBackgroundColor(PRIMARY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = borderedCell(new Phrase(safe(text), font(FontFactory.HELVETICA, 9, DARK)));
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private PdfPCell noBorderCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }

    private PdfPCell borderedCell() {
        PdfPCell cell = new PdfPCell();
        styleCell(cell);
        return cell;
    }

    private PdfPCell borderedCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        styleCell(cell);
        return cell;
    }

    private void styleCell(PdfPCell cell) {
        cell.setPadding(9);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.8f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    }

    private String propertyTitle(Lease lease) {
        return lease.getProperty() != null ? lease.getProperty().getTitle() : "-";
    }

    private String userName(com.openroof.openroof.model.user.User user) {
        if (user == null) return "-";
        return user.getName() != null && !user.getName().isBlank() ? user.getName() : user.getEmail();
    }

    private String formatDate(java.time.LocalDate date) {
        return date != null ? date.format(DATE) : "-";
    }

    private String money(BigDecimal amount, String currency) {
        BigDecimal value = amount != null ? amount : BigDecimal.ZERO;
        return (currency != null ? currency : "PYG") + " " + value.setScale(2, RoundingMode.HALF_UP);
    }

    private String methodLabel(String method) {
        return switch (safe(method)) {
            case "CARD" -> "Tarjeta";
            case "CASH" -> "Efectivo";
            case "CHECK" -> "Cheque";
            case "ACH" -> "ACH";
            case "BANK_TRANSFER", "TRANSFER" -> "Transferencia bancaria";
            default -> method;
        };
    }

    private String statusLabel(String status) {
        return switch (safe(status)) {
            case "PAID" -> "Pagado";
            case "PENDING" -> "Pendiente";
            case "OVERDUE" -> "Atrasado";
            case "PARTIAL" -> "Pago parcial";
            case "COMPLETED" -> "Completado";
            case "FAILED" -> "Fallido";
            case "REFUNDED" -> "Reembolsado";
            default -> status;
        };
    }

    private String safe(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }

    private Font font(String fontName, float size, Color color) {
        Font font = FontFactory.getFont(fontName, size);
        font.setColor(color);
        return font;
    }

    @FunctionalInterface
    private interface DocumentWriter {
        void write(Document document) throws Exception;
    }
}
