package com.invoisuite.server.dto.auth;

import com.invoisuite.server.domain.AuthProvider;
import com.invoisuite.server.domain.User;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        AuthProvider authProvider
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getAuthProvider());
    }
}
