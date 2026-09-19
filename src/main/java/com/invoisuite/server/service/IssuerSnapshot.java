package com.invoisuite.server.service;

import com.invoisuite.server.domain.Company;

/** Point-in-time copy of a company's issuer details, frozen onto an invoice when it is created. */
public record IssuerSnapshot(
        String name,
        String addressLine,
        String city,
        String postalCode,
        String country,
        String registrationNumber,
        String vatId,
        String bankName,
        String iban,
        String bic
) {
    public static IssuerSnapshot from(Company c) {
        return new IssuerSnapshot(
                c.getName(), c.getAddressLine(), c.getCity(), c.getPostalCode(), c.getCountry(),
                c.getRegistrationNumber(), c.getVatId(), c.getBankName(), c.getIban(), c.getBic());
    }
}
