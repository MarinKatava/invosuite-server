package com.invoisuite.server.integration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "invoisuite.integrations")
@Getter
@Setter
public class IntegrationProperties {

    /** Allow connections to loopback/private/link-local addresses. Only for local development. */
    private boolean allowPrivateHosts = false;

    /** Allow plain http:// URLs (API keys would travel unencrypted). Only for local development. */
    private boolean allowInsecureHttp = false;

    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 60;
}
