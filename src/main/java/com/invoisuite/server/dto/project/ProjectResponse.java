package com.invoisuite.server.dto.project;

import com.invoisuite.server.domain.Currency;
import com.invoisuite.server.domain.Project;

import java.math.BigDecimal;

public record ProjectResponse(
        Long id,
        String name,
        Long clientId,
        String clientName,
        String defaultUnit,
        BigDecimal defaultRate,
        Currency currency,
        boolean active
) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getName(),
                p.getClient() != null ? p.getClient().getId() : null,
                p.getClient() != null ? p.getClient().getName() : null,
                p.getDefaultUnit(),
                p.getDefaultRate(),
                p.getCurrency(),
                p.isActive());
    }
}
