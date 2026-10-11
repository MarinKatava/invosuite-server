package com.invoisuite.server.dto.invoice;

import com.invoisuite.server.domain.InvoiceItem;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceItemResponse(
        Long id,
        Long projectId,
        String projectName,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        List<Long> timeEntryIds
) {
    public static InvoiceItemResponse from(InvoiceItem item, List<Long> timeEntryIds) {
        return new InvoiceItemResponse(
                item.getId(),
                item.getProject() != null ? item.getProject().getId() : null,
                item.getProjectName(),
                item.getUnit(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal(),
                timeEntryIds != null ? timeEntryIds : List.of());
    }
}
