package com.invoisuite.server.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Small helper to pull the authenticated user's id out of the JWT-backed security context. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getId();
    }
}
