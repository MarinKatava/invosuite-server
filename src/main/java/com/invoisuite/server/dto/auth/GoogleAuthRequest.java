package com.invoisuite.server.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** The ID token obtained on-device from Google Sign-In. */
public record GoogleAuthRequest(
        @NotBlank String idToken
) {
}
