package com.invoisuite.server.dto.project;

import com.invoisuite.server.domain.Currency;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ProjectRequest(
        @NotBlank String name,
        Long clientId,
        @NotBlank String defaultUnit,
        BigDecimal defaultRate,
        Currency currency,
        Boolean active
) {
}
