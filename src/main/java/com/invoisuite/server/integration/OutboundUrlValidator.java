package com.invoisuite.server.integration;

import com.invoisuite.server.common.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Users supply the URLs this server calls, which would otherwise let them probe
 * the server's internal network (SSRF). Only public https endpoints are allowed
 * unless explicitly relaxed for local development.
 *
 * <p>Known limit: the hostname is resolved here and again by the HTTP client, so a
 * DNS-rebinding attacker controlling a domain could still race the check. Redirects
 * are disabled in the client to at least keep the validated host authoritative.
 */
@Component
@RequiredArgsConstructor
public class OutboundUrlValidator {

    private final IntegrationProperties properties;

    /** Validates the URL and returns it normalised without a trailing slash. */
    public String validate(String rawUrl) {
        URI uri;
        try {
            uri = new URI(rawUrl == null ? "" : rawUrl.trim());
        } catch (java.net.URISyntaxException e) {
            throw new BadRequestException("The server URL is not valid");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        boolean https = scheme.equals("https");
        boolean http = scheme.equals("http");
        if (!https && !(http && properties.isAllowInsecureHttp())) {
            throw new BadRequestException("The server URL must start with https://");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new BadRequestException("The server URL has no host");
        }
        if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new BadRequestException("The server URL must not contain credentials, a query or a fragment");
        }

        if (!properties.isAllowPrivateHosts()) {
            InetAddress[] addresses;
            try {
                addresses = InetAddress.getAllByName(uri.getHost());
            } catch (UnknownHostException e) {
                throw new BadRequestException("Cannot resolve host " + uri.getHost());
            }
            for (InetAddress address : addresses) {
                if (isInternal(address)) {
                    throw new BadRequestException("The server URL points to a private or internal address");
                }
            }
        }

        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        int port = uri.getPort();
        return scheme + "://" + uri.getHost() + (port > 0 ? ":" + port : "") + path;
    }

    static boolean isInternal(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return true;
        }
        byte[] b = a.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            return first == 0                                   // 0.0.0.0/8
                    || (first == 100 && second >= 64 && second <= 127)   // CGNAT 100.64.0.0/10
                    || (first == 198 && (second == 18 || second == 19))  // benchmarking
                    || first >= 240;                            // reserved / broadcast
        }
        return (b[0] & 0xFE) == 0xFC;                           // IPv6 unique local fc00::/7
    }
}
