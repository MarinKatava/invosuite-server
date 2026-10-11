package com.invoisuite.server.dto.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceItemRequest(
        Long projectId,
        @NotBlank String projectName,
        @NotBlank String unit,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal unitPrice,
        /** Imported time entries this line bills; they are marked billed and can't be billed again. */
        List<Long> timeEntryIds
) {
}
