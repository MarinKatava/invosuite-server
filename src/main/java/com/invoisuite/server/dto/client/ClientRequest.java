package com.invoisuite.server.dto.client;

import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank String name,
        @NotBlank String addressLine,
        String vatId,
        String email
) {
}
