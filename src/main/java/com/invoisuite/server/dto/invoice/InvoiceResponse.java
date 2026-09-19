package com.invoisuite.server.dto.invoice;

import com.invoisuite.server.domain.Currency;
import com.invoisuite.server.domain.Invoice;
import com.invoisuite.server.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Long id,
        Long companyId,
        Long clientId,
        String clientName,
        String name,
        String invoiceNumber,
        LocalDate issueDate,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate deliveryDate,
        String invoicingPlace,
        Currency currency,
        BigDecimal vatPercent,
        String vatExemptText,
        String notes,
        InvoiceStatus status,
        BigDecimal netTotal,
        BigDecimal vatAmount,
        BigDecimal total,
        List<InvoiceItemResponse> items
) {
    public static InvoiceResponse from(Invoice inv) {
        return new InvoiceResponse(
                inv.getId(),
                inv.getCompany().getId(),
                inv.getClient().getId(),
                inv.getClient().getName(),
                inv.getName(),
                inv.getInvoiceNumber(),
                inv.getIssueDate(),
                inv.getPeriodStart(),
                inv.getPeriodEnd(),
                inv.getDeliveryDate(),
                inv.getInvoicingPlace(),
                inv.getCurrency(),
                inv.getVatPercent(),
                inv.getVatExemptText(),
                inv.getNotes(),
                inv.getStatus(),
                inv.getNetTotal(),
                inv.getVatAmount(),
                inv.getTotal(),
                inv.getItems().stream().map(InvoiceItemResponse::from).toList());
    }
}
