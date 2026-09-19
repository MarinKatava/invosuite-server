package com.invoisuite.server.dto.client;

import com.invoisuite.server.domain.Client;

public record ClientResponse(
        Long id,
        String name,
        String addressLine,
        String vatId,
        String email
) {
    public static ClientResponse from(Client c) {
        return new ClientResponse(c.getId(), c.getName(), c.getAddressLine(), c.getVatId(), c.getEmail());
    }
}
