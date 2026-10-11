package com.invoisuite.server.integration.odoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.invoisuite.server.integration.IntegrationException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Legacy Odoo external API: JSON-RPC at {@code /jsonrpc}, authenticating with login + password/API key. */
class OdooJsonRpcTransport implements OdooTransport {

    private static final Map<String, String> HEADERS = Map.of("User-Agent", "InvoiSuite");

    private final RestClient client;
    private final String baseUrl;
    private final String database;
    private final String login;
    private final String secret;
    private Long uid;

    OdooJsonRpcTransport(RestClient client, String baseUrl, String database, String login, String secret) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.database = database;
        this.login = login;
        this.secret = secret;
    }

    @Override
    public long currentUserId() {
        if (uid == null) {
            if (database == null || database.isBlank()) {
                throw new IntegrationException("The Odoo database name is required for the legacy JSON-RPC API");
            }
            JsonNode result = call("common", "authenticate", List.of(database, login, secret, Map.of()));
            if (result.isBoolean() || result.isNull() || result.asLong() <= 0) {
                throw new IntegrationException("Odoo rejected the login or password/API key");
            }
            uid = result.asLong();
        }
        return uid;
    }

    @Override
    public JsonNode searchRead(String model, List<Object> domain, List<String> fields, int limit, int offset, String order) {
        Map<String, Object> kwargs = new LinkedHashMap<>();
        kwargs.put("fields", fields);
        kwargs.put("limit", limit);
        kwargs.put("offset", offset);
        if (order != null) {
            kwargs.put("order", order);
        }
        return call("object", "execute_kw", List.of(database, currentUserId(), secret, model, "search_read",
                List.of(domain), kwargs));
    }

    private JsonNode call(String service, String method, List<Object> args) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("service", service);
        params.put("method", method);
        params.put("args", args);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("method", "call");
        body.put("params", params);
        body.put("id", 1);

        JsonNode response = OdooRpcSupport.post(client, baseUrl + "/jsonrpc", HEADERS, body);
        if (response.has("error")) {
            String message = OdooRpcSupport.extractMessage(response.toString());
            throw new IntegrationException("Odoo error: " + (message != null ? message : "request failed"));
        }
        return response.path("result");
    }
}
