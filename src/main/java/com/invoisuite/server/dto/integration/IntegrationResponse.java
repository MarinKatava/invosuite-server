package com.invoisuite.server.dto.integration;

import com.invoisuite.server.domain.IntegrationConnection;
import com.invoisuite.server.domain.IntegrationProvider;
import com.invoisuite.server.domain.OdooApiMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Never carries the API key; {@code hasApiKey} only says whether one is stored. */
public record IntegrationResponse(
        Long id,
        IntegrationProvider provider,
        String name,
        Long companyId,
        String companyName,
        Long clientId,
        String clientName,
        String baseUrl,
        String dbName,
        String username,
        boolean hasApiKey,
        OdooApiMode apiMode,
        boolean onlyMine,
        int lookbackDays,
        BigDecimal defaultHourlyRate,
        boolean active,
        LocalDateTime lastSyncAt,
        String lastSyncStatus,
        String lastSyncMessage
) {
    public static IntegrationResponse from(IntegrationConnection c) {
        return new IntegrationResponse(
                c.getId(), c.getProvider(), c.getName(),
                c.getCompany().getId(), c.getCompany().getName(),
                c.getClient() != null ? c.getClient().getId() : null,
                c.getClient() != null ? c.getClient().getName() : null,
                c.getBaseUrl(), c.getDbName(), c.getUsername(),
                c.getApiKey() != null && !c.getApiKey().isBlank(),
                c.getApiMode(), c.isOnlyMine(), c.getLookbackDays(), c.getDefaultHourlyRate(),
                c.isActive(), c.getLastSyncAt(), c.getLastSyncStatus(), c.getLastSyncMessage());
    }
}
