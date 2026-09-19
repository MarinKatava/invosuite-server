package com.invoisuite.server.dto.invoice;

import com.invoisuite.server.domain.InvoiceItem;

import java.math.BigDecimal;

public record InvoiceItemResponse(
        Long id,
        Long projectId,
        String projectName,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public static InvoiceItemResponse from(InvoiceItem item) {
        return new InvoiceItemResponse(
                item.getId(),
                item.getProject() != null ? item.getProject().getId() : null,
                item.getProjectName(),
                item.getUnit(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}
