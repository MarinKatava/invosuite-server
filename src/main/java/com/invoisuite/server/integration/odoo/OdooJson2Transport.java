package com.invoisuite.server.integration.odoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.invoisuite.server.integration.IntegrationException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Odoo 19+ "JSON-2" API: {@code POST /json/2/<model>/<method>} with a bearer API key. */
class OdooJson2Transport implements OdooTransport {

    private final RestClient client;
    private final String baseUrl;
    private final Map<String, String> headers = new HashMap<>();

    OdooJson2Transport(RestClient client, String baseUrl, String database, String apiKey) {
        this.client = client;
        this.baseUrl = baseUrl;
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("User-Agent", "InvoiSuite");
        if (database != null && !database.isBlank()) {
            headers.put("X-Odoo-Database", database);
        }
    }

    @Override
    public long currentUserId() {
        JsonNode context = OdooRpcSupport.post(client, baseUrl + "/json/2/res.users/context_get", headers, Map.of());
        if (!context.hasNonNull("uid")) {
            throw new IntegrationException("Odoo did not report which user the API key belongs to");
        }
        return context.get("uid").asLong();
    }

    @Override
    public JsonNode searchRead(String model, List<Object> domain, List<String> fields, int limit, int offset, String order) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("domain", domain);
        body.put("fields", fields);
        body.put("limit", limit);
        body.put("offset", offset);
        if (order != null) {
            body.put("order", order);
        }
        return OdooRpcSupport.post(client, baseUrl + "/json/2/" + model + "/search_read", headers, body);
    }
}
