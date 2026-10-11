package com.invoisuite.server.dto.integration;

import java.math.BigDecimal;
import java.util.List;

/** Unbilled hours for one project, shaped like an invoice line. */
public record TimeSummaryLine(
        Long projectId,
        String projectName,
        String unit,
        BigDecimal hours,
        BigDecimal unitPrice,
        int entryCount,
        List<Long> timeEntryIds
) {
}
