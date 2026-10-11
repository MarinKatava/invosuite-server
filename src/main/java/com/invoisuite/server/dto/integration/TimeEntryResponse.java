package com.invoisuite.server.dto.integration;

import com.invoisuite.server.domain.TimeEntry;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeEntryResponse(
        Long id,
        LocalDate workDate,
        Long projectId,
        String projectName,
        BigDecimal hours,
        String description,
        boolean invoiced,
        String invoiceNumber
) {
    public static TimeEntryResponse from(TimeEntry e) {
        boolean invoiced = e.getInvoiceItem() != null;
        return new TimeEntryResponse(
                e.getId(), e.getWorkDate(),
                e.getProject() != null ? e.getProject().getId() : null,
                e.getProjectName(), e.getHours(), e.getDescription(), invoiced,
                invoiced ? e.getInvoiceItem().getInvoice().getInvoiceNumber() : null);
    }
}
