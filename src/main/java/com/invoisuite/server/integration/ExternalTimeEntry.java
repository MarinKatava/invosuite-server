package com.invoisuite.server.integration;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A time booking as reported by an external platform, before it is stored locally. */
public record ExternalTimeEntry(
        String externalId,
        LocalDate date,
        BigDecimal hours,
        String projectName,
        String description
) {
}
