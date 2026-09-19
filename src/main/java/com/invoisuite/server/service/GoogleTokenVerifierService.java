package com.invoisuite.server.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.invoisuite.server.common.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Verifies Google Sign-In ID tokens issued to the Android app.
 *
 * <p>This calls Google's tokeninfo endpoint rather than validating the JWT
 * signature locally against Google's JWKS. That keeps the backend free of an
 * extra Google client-library dependency; the tradeoff is one extra outbound
 * HTTPS call per login/registration via Google, which is an acceptable cost
 * for an auth flow that already happens over the network.
 */
@Service
@Slf4j
public class GoogleTokenVerifierService {

    private final RestClient restClient = RestClient.create("https://oauth2.googleapis.com");
    private final String expectedAudience;

    public GoogleTokenVerifierService(@Value("${invoisuite.google.client-id}") String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    public GoogleUserInfo verify(String idToken) {
        TokenInfo info;
        try {
            info = restClient.get()
                    .uri("/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(TokenInfo.class);
        } catch (Exception ex) {
            log.warn("Google token verification failed", ex);
            throw new BadRequestException("Invalid Google ID token");
        }

        if (info == null || info.sub == null) {
            throw new BadRequestException("Invalid Google ID token");
        }
        if (!expectedAudience.equals(info.aud)) {
            throw new BadRequestException("Google ID token was not issued for this application");
        }
        if (info.email == null || !"true".equalsIgnoreCase(info.emailVerified)) {
            throw new BadRequestException("Google account email is not verified");
        }

        String name = (info.name != null && !info.name.isBlank()) ? info.name : info.email;
        return new GoogleUserInfo(info.sub, info.email, name);
    }

    public record GoogleUserInfo(String sub, String email, String fullName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenInfo {
        public String sub;
        public String aud;
        public String email;
        public String name;
        @JsonProperty("email_verified")
        public String emailVerified;
    }
}
