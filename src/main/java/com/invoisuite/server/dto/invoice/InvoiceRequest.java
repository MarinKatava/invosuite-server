package com.invoisuite.server.dto.invoice;

import com.invoisuite.server.domain.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
        @NotNull Long clientId,
        @NotBlank String name,
        String invoiceNumber,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        LocalDate deliveryDate,
        String invoicingPlace,
        @NotNull Currency currency,
        BigDecimal vatPercent,
        String vatExemptText,
        String notes,
        @NotEmpty @Valid List<InvoiceItemRequest> items
) {
}
