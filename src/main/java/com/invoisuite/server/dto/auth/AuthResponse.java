package com.invoisuite.server.dto.auth;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
