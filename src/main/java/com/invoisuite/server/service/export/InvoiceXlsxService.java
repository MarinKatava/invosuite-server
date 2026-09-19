package com.invoisuite.server.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoisuite.server.domain.Invoice;
import com.invoisuite.server.domain.InvoiceItem;
import com.invoisuite.server.service.ClientSnapshot;
import com.invoisuite.server.service.IssuerSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/** Exports an {@link Invoice} to an .xlsx file laid out like the original invoice template. */
@Service
@RequiredArgsConstructor
public class InvoiceXlsxService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public byte[] render(Invoice invoice) {
        IssuerSnapshot issuer = objectMapper.readValue(invoice.getIssuerSnapshot(), IssuerSnapshot.class);
        ClientSnapshot client = objectMapper.readValue(invoice.getClientSnapshot(), ClientSnapshot.class);
        String symbol = invoice.getCurrency().symbol();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Invoice");
            CellStyle bold = boldStyle(workbook);
            CellStyle header = headerStyle(workbook);

            int r = 0;
            r = writeLine(sheet, r, issuer.name(), bold);
            r = writeLine(sheet, r, issuer.addressLine(), null);
            r = writeLine(sheet, r, issuer.postalCode() + " " + issuer.city(), null);
            r = writeLine(sheet, r, issuer.country(), null);
            if (notBlank(issuer.registrationNumber())) r = writeLine(sheet, r, "ID number: " + issuer.registrationNumber(), null);
            if (notBlank(issuer.vatId())) r = writeLine(sheet, r, "VAT ID: " + issuer.vatId(), null);
            if (notBlank(issuer.bankName())) r = writeLine(sheet, r, "BANK: " + issuer.bankName(), null);
            if (notBlank(issuer.iban())) r = writeLine(sheet, r, "IBAN: " + issuer.iban(), null);
            if (notBlank(issuer.bic())) r = writeLine(sheet, r, "BIC: " + issuer.bic(), null);

            r++;
            r = writeLabeled(sheet, r, "Invoice", invoice.getInvoiceNumber(), bold);
            r = writeLabeled(sheet, r, "Invoicing date:", invoice.getIssueDate().format(DATE_FORMAT), null);
            r = writeLabeled(sheet, r, "Invoicing period:",
                    invoice.getPeriodStart().format(DATE_FORMAT) + " - " + invoice.getPeriodEnd().format(DATE_FORMAT), null);
            r = writeLabeled(sheet, r, "Delivery date:",
                    invoice.getDeliveryDate() != null ? invoice.getDeliveryDate().format(DATE_FORMAT) : "", null);
            r = writeLabeled(sheet, r, "Invoicing place:", nullToEmpty(invoice.getInvoicingPlace()), null);
            r = writeLabeled(sheet, r, "Customer:", client.name(), null);
            r = writeLabeled(sheet, r, "Address:", client.addressLine(), null);
            if (notBlank(client.vatId())) r = writeLabeled(sheet, r, "UID:", client.vatId(), null);

            r++;
            Row headRow = sheet.createRow(r++);
            String[] headers = {"Project", "Unit", "Quantity", "Unit price", "Total", "Currency"};
            for (int c = 0; c < headers.length; c++) {
                Cell cell = headRow.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(header);
            }

            for (InvoiceItem item : invoice.getItems()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(item.getProjectName());
                row.createCell(1).setCellValue(item.getUnit());
                row.createCell(2).setCellValue(item.getQuantity().doubleValue());
                row.createCell(3).setCellValue(item.getUnitPrice().doubleValue());
                row.createCell(4).setCellValue(item.getLineTotal().doubleValue());
                row.createCell(5).setCellValue(symbol);
            }

            Row totalRow = sheet.createRow(r++);
            totalRow.createCell(1).setCellValue("Total");
            double totalQty = invoice.getItems().stream().mapToDouble(i -> i.getQuantity().doubleValue()).sum();
            totalRow.createCell(2).setCellValue(totalQty);
            totalRow.createCell(4).setCellValue(invoice.getNetTotal().doubleValue());
            totalRow.createCell(5).setCellValue(symbol);

            r++;
            r = writeTotalsLine(sheet, r, "Net total:", invoice.getNetTotal().doubleValue(), symbol, bold);
            r = writeTotalsLine(sheet, r, "VAT:", invoice.getVatPercent().doubleValue(), "%", null);
            r = writeTotalsLine(sheet, r, "TOTAL:", invoice.getTotal().doubleValue(), symbol, bold);

            if (notBlank(invoice.getVatExemptText())) {
                r++;
                writeLine(sheet, r, "●  " + invoice.getVatExemptText(), null);
            }

            for (int c = 0; c < 6; c++) {
                sheet.autoSizeColumn(c);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private int writeLine(Sheet sheet, int rowIndex, String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(value);
        if (style != null) cell.setCellStyle(style);
        return rowIndex + 1;
    }

    private int writeLabeled(Sheet sheet, int rowIndex, String label, String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        if (style != null) labelCell.setCellStyle(style);
        row.createCell(1).setCellValue(value);
        return rowIndex + 1;
    }

    private int writeTotalsLine(Sheet sheet, int rowIndex, String label, double value, String unit, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(3);
        labelCell.setCellValue(label);
        if (style != null) labelCell.setCellStyle(style);
        row.createCell(4).setCellValue(value);
        row.createCell(5).setCellValue(unit);
        return rowIndex + 1;
    }

    private CellStyle boldStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
