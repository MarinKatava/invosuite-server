package com.invoisuite.server.dto.integration;

import com.invoisuite.server.domain.IntegrationProvider;
import com.invoisuite.server.domain.OdooApiMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record IntegrationRequest(
        @NotNull IntegrationProvider provider,
        @NotBlank String name,
        @NotNull Long companyId,
        Long clientId,
        @NotBlank String baseUrl,
        String dbName,
        @NotBlank String username,
        /** Blank on update means "keep the stored key". */
        String apiKey,
        @NotNull OdooApiMode apiMode,
        Boolean onlyMine,
        @Min(1) @Max(730) Integer lookbackDays,
        BigDecimal defaultHourlyRate,
        Boolean active
) {
}
