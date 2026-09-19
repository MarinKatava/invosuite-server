package com.invoisuite.server.dto.invoice;

import com.invoisuite.server.domain.Currency;
import com.invoisuite.server.domain.Invoice;
import com.invoisuite.server.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lightweight projection used for invoice list screens. */
public record InvoiceSummaryResponse(
        Long id,
        String name,
        String invoiceNumber,
        String clientName,
        LocalDate issueDate,
        Currency currency,
        BigDecimal total,
        InvoiceStatus status
) {
    public static InvoiceSummaryResponse from(Invoice inv) {
        return new InvoiceSummaryResponse(
                inv.getId(), inv.getName(), inv.getInvoiceNumber(), inv.getClient().getName(),
                inv.getIssueDate(), inv.getCurrency(), inv.getTotal(), inv.getStatus());
    }
}
