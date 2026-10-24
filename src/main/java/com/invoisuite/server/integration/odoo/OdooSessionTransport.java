package com.invoisuite.server.integration.odoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.invoisuite.server.integration.IntegrationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Signs in with the Odoo user's login and password through {@code /web/session/authenticate} (what the
 * web client does) and reads data with the resulting session cookie via {@code /web/dataset/call_kw}.
 * Unlike JSON-2 this needs no API key; unlike {@code /jsonrpc} it is not slated for removal.
 * Accounts with two-factor authentication must use an API key instead.
 */
class OdooSessionTransport implements OdooTransport {

    private static final String COOKIE_NAME = "session_id";

    private final RestClient client;
    private final String baseUrl;
    private final String database;
    private final String login;
    private final String password;
    private Long uid;
    private String sessionCookie;

    OdooSessionTransport(RestClient client, String baseUrl, String database, String login, String password) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.database = database;
        this.login = login;
        this.password = password;
    }

    @Override
    public long currentUserId() {
        if (uid == null) {
            Map<String, Object> params = new LinkedHashMap<>();
            if (database != null && !database.isBlank()) {
                params.put("db", database);
            }
            params.put("login", login);
            params.put("password", password);

            ResponseEntity<JsonNode> response = OdooRpcSupport.postForEntity(client,
                    baseUrl + "/web/session/authenticate", Map.of("User-Agent", "InvoiSuite"), envelope(params));
            JsonNode body = response.getBody();
            failOnError(body);
            JsonNode id = body.path("result").path("uid");
            if (!id.canConvertToLong() || id.asLong() <= 0) {
                throw new IntegrationException("Odoo rejected the login or password");
            }
            sessionCookie = sessionCookie(response.getHeaders().get(HttpHeaders.SET_COOKIE));
            if (sessionCookie == null) {
                throw new IntegrationException("Odoo did not start a session - check the URL and database name");
            }
            uid = id.asLong();
        }
        return uid;
    }

    @Override
    public JsonNode searchRead(String model, List<Object> domain, List<String> fields, int limit, int offset, String order) {
        currentUserId();
        Map<String, Object> kwargs = new LinkedHashMap<>();
        kwargs.put("fields", fields);
        kwargs.put("limit", limit);
        kwargs.put("offset", offset);
        if (order != null) {
            kwargs.put("order", order);
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("model", model);
        params.put("method", "search_read");
        params.put("args", List.of(domain));
        params.put("kwargs", kwargs);

        JsonNode body = OdooRpcSupport.post(client, baseUrl + "/web/dataset/call_kw/" + model + "/search_read",
                Map.of("User-Agent", "InvoiSuite", "Cookie", COOKIE_NAME + "=" + sessionCookie), envelope(params));
        failOnError(body);
        return body.path("result");
    }

    private static Map<String, Object> envelope(Map<String, Object> params) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("method", "call");
        body.put("params", params);
        body.put("id", 1);
        return body;
    }

    private static void failOnError(JsonNode body) {
        if (body.has("error")) {
            String message = OdooRpcSupport.extractMessage(body.toString());
            throw new IntegrationException("Odoo error: " + (message != null ? message : "request failed"));
        }
    }

    private static String sessionCookie(List<String> setCookieHeaders) {
        if (setCookieHeaders == null) {
            return null;
        }
        for (String header : setCookieHeaders) {
            if (header.startsWith(COOKIE_NAME + "=")) {
                int end = header.indexOf(';');
                String value = header.substring(COOKIE_NAME.length() + 1, end < 0 ? header.length() : end);
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }
}
