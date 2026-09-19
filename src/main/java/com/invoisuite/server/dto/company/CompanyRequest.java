package com.invoisuite.server.dto.company;

import com.invoisuite.server.domain.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CompanyRequest(
        @NotBlank String name,
        @NotBlank String addressLine,
        @NotBlank String city,
        @NotBlank String postalCode,
        @NotBlank String country,
        String registrationNumber,
        String vatId,
        String bankName,
        String iban,
        String bic,
        @NotNull Currency defaultCurrency,
        BigDecimal defaultVatPercent,
        String vatExemptText,
        String invoicingPlace
) {
}
