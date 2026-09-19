package com.invoisuite.server.dto.company;

import com.invoisuite.server.domain.Company;
import com.invoisuite.server.domain.Currency;

import java.math.BigDecimal;

public record CompanyResponse(
        Long id,
        String name,
        String addressLine,
        String city,
        String postalCode,
        String country,
        String registrationNumber,
        String vatId,
        String bankName,
        String iban,
        String bic,
        Currency defaultCurrency,
        BigDecimal defaultVatPercent,
        String vatExemptText,
        String invoicingPlace
) {
    public static CompanyResponse from(Company c) {
        return new CompanyResponse(
                c.getId(), c.getName(), c.getAddressLine(), c.getCity(), c.getPostalCode(), c.getCountry(),
                c.getRegistrationNumber(), c.getVatId(), c.getBankName(), c.getIban(), c.getBic(),
                c.getDefaultCurrency(), c.getDefaultVatPercent(), c.getVatExemptText(), c.getInvoicingPlace());
    }
}
