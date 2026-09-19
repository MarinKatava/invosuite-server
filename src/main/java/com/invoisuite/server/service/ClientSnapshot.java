package com.invoisuite.server.service;

import com.invoisuite.server.domain.Client;

/** Point-in-time copy of a customer's details, frozen onto an invoice when it is created. */
public record ClientSnapshot(
        String name,
        String addressLine,
        String vatId,
        String email
) {
    public static ClientSnapshot from(Client c) {
        return new ClientSnapshot(c.getName(), c.getAddressLine(), c.getVatId(), c.getEmail());
    }
}
