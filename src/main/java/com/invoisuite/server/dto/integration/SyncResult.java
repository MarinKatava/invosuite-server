package com.invoisuite.server.dto.integration;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SyncResult(
        Long connectionId,
        String connectionName,
        boolean success,
        String message,
        LocalDate from,
        LocalDate to,
        int imported,
        int updated,
        int removed,
        int skippedBilled,
        BigDecimal totalHours
) {
    public static SyncResult failure(Long id, String name, String message) {
        return new SyncResult(id, name, false, message, null, null, 0, 0, 0, 0, BigDecimal.ZERO);
    }
}
