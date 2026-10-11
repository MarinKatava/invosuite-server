package com.invoisuite.server.integration.odoo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoisuite.server.integration.IntegrationException;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Shared HTTP plumbing for the Odoo transports. */
final class OdooRpcSupport {

    private static final ObjectMapper JSON = new ObjectMapper();

    private OdooRpcSupport() {
    }

    static JsonNode post(RestClient client, String url, Map<String, String> headers, Object body) {
        // Serialised up front so a Content-Length is sent. Streaming the object would use chunked
        // transfer encoding, which Odoo does not read: it sees an empty body and runs the call with
        // no arguments (e.g. search_read over every record, ignoring the domain).
        byte[] payload;
        try {
            payload = JSON.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            throw new IntegrationException("Could not build the request for Odoo", e);
        }
        try {
            JsonNode response = client.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(h -> headers.forEach(h::set))
                    .body(payload)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, res) -> {
                        throw new IntegrationException(describeHttpError(res.getStatusCode().value(),
                                new String(res.getBody().readAllBytes())));
                    })
                    .body(JsonNode.class);
            if (response == null) {
                throw new IntegrationException("Odoo returned an empty response");
            }
            return response;
        } catch (ResourceAccessException e) {
            throw new IntegrationException("Could not reach the Odoo server: " + rootMessage(e), e);
        }
    }

    private static String describeHttpError(int status, String body) {
        String message = extractMessage(body);
        if (status == 401 || status == 403) {
            return "Odoo rejected the credentials (HTTP " + status + ")" + suffix(message);
        }
        if (status == 404) {
            return "Odoo endpoint not found (HTTP 404) - check the URL and the API mode" + suffix(message);
        }
        return "Odoo returned HTTP " + status + suffix(message);
    }

    /** Pulls the human-readable message out of Odoo's JSON-2 / JSON-RPC error bodies. */
    static String extractMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode node = JSON.readTree(body);
            if (node.hasNonNull("message")) {
                return node.get("message").asText();
            }
            JsonNode error = node.path("error");
            if (error.has("data") && error.get("data").hasNonNull("message")) {
                return error.get("data").get("message").asText();
            }
            if (error.hasNonNull("message")) {
                return error.get("message").asText();
            }
        } catch (Exception ignored) {
            // not JSON (e.g. an HTML error page) - no useful message to extract
        }
        return null;
    }

    private static String suffix(String message) {
        return message == null ? "" : ": " + message;
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }
}
