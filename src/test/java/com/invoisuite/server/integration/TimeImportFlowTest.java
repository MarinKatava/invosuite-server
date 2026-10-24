package com.invoisuite.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoisuite.server.support.FakeOdooServer;
import com.invoisuite.server.support.FakeOdooServer.Line;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TimeImportFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    FakeOdooServer odoo;
    String token;
    long companyId;
    long clientId;

    @BeforeEach
    void setUp() throws Exception {
        odoo = new FakeOdooServer();
        odoo.lines.add(new Line(1, "2026-08-03", 8, "Spec work", 10, "SO1629", 100L, "Analysis", FakeOdooServer.MY_UID));
        odoo.lines.add(new Line(2, "2026-08-04", 6.5, null, 10, "SO1629", null, null, FakeOdooServer.MY_UID));
        odoo.lines.add(new Line(3, "2026-08-05", 4, "MES module", 11, "OneBase MES", null, null, FakeOdooServer.MY_UID));
        odoo.lines.add(new Line(4, "2026-08-05", 5, "colleague", 10, "SO1629", null, null, 99));
        odoo.lines.add(new Line(5, "2026-08-06", 0, "Analysis", 12, "Internal", null, null, FakeOdooServer.MY_UID));

        token = register("user-" + UUID.randomUUID() + "@example.com");
        companyId = post("/api/companies", Map.of("name", "Acme", "addressLine", "S 1", "city", "C",
                "postalCode", "1", "country", "X", "defaultCurrency", "EUR", "defaultVatPercent", 0)).get("id").asLong();
        clientId = post("/api/companies/" + companyId + "/clients", Map.of("name", "ABF GmbH", "addressLine", "Linz")).get("id").asLong();
    }

    @AfterEach
    void tearDown() {
        odoo.close();
    }

    @Test
    void importsOnlyMyBookingsAndCreatesProjects() throws Exception {
        long id = createConnection("JSONRPC", FakeOdooServer.SECRET);

        JsonNode result = post("/api/integrations/" + id + "/sync", null);
        assertTrue(result.get("success").asBoolean(), result.toString());
        assertEquals(3, result.get("imported").asInt());
        assertEquals(18.5, result.get("totalHours").asDouble(), 0.001);   // the 0 h placeholder line is ignored

        JsonNode entries = get("/api/companies/" + companyId + "/time-entries?from=2026-08-01&to=2026-08-31");
        assertEquals(3, entries.size());

        // Projects were created automatically and assigned to the connection's client.
        JsonNode projects = get("/api/companies/" + companyId + "/projects");
        assertEquals(2, projects.size());
        for (JsonNode p : projects) {
            assertEquals(clientId, p.get("clientId").asLong());
            assertEquals(61.0, p.get("defaultRate").asDouble(), 0.001);
        }
    }

    @Test
    void importingTwiceDoesNotDuplicate_andEditsAndDeletionsPropagate() throws Exception {
        long id = createConnection("JSONRPC", FakeOdooServer.SECRET);
        post("/api/integrations/" + id + "/sync", null);

        JsonNode again = post("/api/integrations/" + id + "/sync", null);
        assertEquals(0, again.get("imported").asInt());
        assertEquals(0, again.get("updated").asInt());

        odoo.lines.set(0, new Line(1, "2026-08-03", 9, "Spec work", 10, "SO1629", 100L, "Analysis", FakeOdooServer.MY_UID));
        odoo.lines.remove(2);
        JsonNode changed = post("/api/integrations/" + id + "/sync", null);
        assertEquals(1, changed.get("updated").asInt());
        assertEquals(1, changed.get("removed").asInt());
        assertEquals(2, get("/api/companies/" + companyId + "/time-entries?from=2026-08-01&to=2026-08-31").size());
    }

    @Test
    void jsonRpcAndJson2BothWork() throws Exception {
        long id = createConnection("JSON2", FakeOdooServer.SECRET);
        JsonNode result = post("/api/integrations/" + id + "/sync", null);
        assertTrue(result.get("success").asBoolean(), result.toString());
        assertEquals(3, result.get("imported").asInt());
    }

    @Test
    void loginAndPasswordWorkWithoutApiKey() throws Exception {
        long id = createConnection("SESSION", FakeOdooServer.PASSWORD);
        JsonNode result = post("/api/integrations/" + id + "/sync", null);
        assertTrue(result.get("success").asBoolean(), result.toString());
        assertEquals(3, result.get("imported").asInt());   // only my bookings, like the other modes

        JsonNode ok = post("/api/integrations/test", connectionBody("SESSION", FakeOdooServer.PASSWORD));
        assertTrue(ok.get("ok").asBoolean(), ok.toString());
    }

    @Test
    void wrongPasswordIsReportedForSessionLogin() throws Exception {
        JsonNode bad = post("/api/integrations/test", connectionBody("SESSION", "not-my-password"));
        assertFalse(bad.get("ok").asBoolean());
        assertTrue(bad.get("message").asText().toLowerCase().contains("password"), bad.toString());
    }

    @Test
    void badCredentialsAreReportedNotThrown() throws Exception {
        long id = createConnection("JSONRPC", "wrong-password");
        JsonNode result = post("/api/integrations/" + id + "/sync", null);
        assertFalse(result.get("success").asBoolean());
        assertTrue(result.get("message").asText().toLowerCase().contains("login"), result.toString());

        JsonNode saved = get("/api/integrations/" + id);
        assertEquals("FAILED", saved.get("lastSyncStatus").asText());
    }

    @Test
    void testEndpointValidatesUnsavedSettings() throws Exception {
        JsonNode ok = post("/api/integrations/test", connectionBody("JSON2", FakeOdooServer.SECRET));
        assertTrue(ok.get("ok").asBoolean(), ok.toString());
        JsonNode bad = post("/api/integrations/test", connectionBody("JSON2", "nope"));
        assertFalse(bad.get("ok").asBoolean());
        assertTrue(bad.get("message").asText().contains("401"), bad.toString());
    }

    @Test
    void apiKeyIsEncryptedAtRestAndNeverReturned() throws Exception {
        MvcResult res = mvc.perform(authedBuilder(MockMvcRequestBuilders.post("/api/integrations"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(connectionBody("JSONRPC", FakeOdooServer.SECRET))))
                .andReturn();
        String body = res.getResponse().getContentAsString();
        assertFalse(body.contains(FakeOdooServer.SECRET), body);
        assertTrue(json.readTree(body).get("hasApiKey").asBoolean());

        long createdId = json.readTree(body).get("id").asLong();
        String stored = jdbc.queryForObject(
                "select api_key_encrypted from integration_connections where id = ?", String.class, createdId);
        assertNotNull(stored);
        assertFalse(stored.contains(FakeOdooServer.SECRET));
    }

    @Test
    void updateWithBlankKeyKeepsStoredKey() throws Exception {
        long id = createConnection("JSONRPC", FakeOdooServer.SECRET);
        Map<String, Object> edit = connectionBody("JSONRPC", "");
        edit.put("name", "Renamed");
        JsonNode updated = put("/api/integrations/" + id, edit);
        assertEquals("Renamed", updated.get("name").asText());
        assertTrue(post("/api/integrations/" + id + "/sync", null).get("success").asBoolean());
    }

    @Test
    void billedHoursAreLockedAndReleasedWhenInvoiceIsRemoved() throws Exception {
        long id = createConnection("JSONRPC", FakeOdooServer.SECRET);
        post("/api/integrations/" + id + "/sync", null);

        JsonNode summary = get("/api/companies/" + companyId + "/time-entries/summary?clientId=" + clientId
                + "&from=2026-08-01&to=2026-08-31");
        assertEquals(2, summary.size());
        JsonNode so = summary.get(0).get("projectName").asText().equals("SO1629") ? summary.get(0) : summary.get(1);
        assertEquals(14.5, so.get("hours").asDouble(), 0.001);
        assertEquals(2, so.get("timeEntryIds").size());

        Map<String, Object> invoice = invoiceBody(so);
        JsonNode created = post("/api/companies/" + companyId + "/invoices", invoice);
        long invoiceId = created.get("id").asLong();
        assertEquals(2, created.get("items").get(0).get("timeEntryIds").size());

        // Billed hours no longer show up as unbilled, and are flagged in the list.
        JsonNode after = get("/api/companies/" + companyId + "/time-entries/summary?clientId=" + clientId
                + "&from=2026-08-01&to=2026-08-31");
        assertEquals(1, after.size());
        JsonNode billed = get("/api/companies/" + companyId + "/time-entries?from=2026-08-01&to=2026-08-31&invoiced=true");
        assertEquals(2, billed.size());
        assertTrue(billed.get(0).get("invoiced").asBoolean());

        // The same hours can't be billed on a second invoice.
        mvcPostExpect(400, "/api/companies/" + companyId + "/invoices", invoice);

        // Editing a billed entry in Odoo must not change what was invoiced.
        odoo.lines.set(0, new Line(1, "2026-08-03", 20, "Spec work", 10, "SO1629", 100L, "Analysis", FakeOdooServer.MY_UID));
        JsonNode resync = post("/api/integrations/" + id + "/sync", null);
        assertEquals(1, resync.get("skippedBilled").asInt());

        // Updating the invoice keeps the link (the client round-trips the ids)...
        Map<String, Object> edit = invoiceBody(so);
        edit.put("name", "Renamed invoice");
        JsonNode updated = put("/api/companies/" + companyId + "/invoices/" + invoiceId, edit);
        assertEquals(2, updated.get("items").get(0).get("timeEntryIds").size());
        assertEquals(1, get("/api/companies/" + companyId + "/time-entries/summary?clientId=" + clientId
                + "&from=2026-08-01&to=2026-08-31").size());

        // ...and deleting it frees the hours again.
        mvc.perform(authedDelete("/api/companies/" + companyId + "/invoices/" + invoiceId)).andExpect(status().isNoContent());
        assertEquals(2, get("/api/companies/" + companyId + "/time-entries/summary?clientId=" + clientId
                + "&from=2026-08-01&to=2026-08-31").size());
    }

    @Test
    void deletingConnectionKeepsBilledHoursAndDropsTheRest() throws Exception {
        long id = createConnection("JSONRPC", FakeOdooServer.SECRET);
        post("/api/integrations/" + id + "/sync", null);
        JsonNode summary = get("/api/companies/" + companyId + "/time-entries/summary?clientId=" + clientId
                + "&from=2026-08-01&to=2026-08-31");
        JsonNode so = summary.get(0).get("projectName").asText().equals("SO1629") ? summary.get(0) : summary.get(1);
        post("/api/companies/" + companyId + "/invoices", invoiceBody(so));

        mvc.perform(authedDelete("/api/integrations/" + id)).andExpect(status().isNoContent());

        JsonNode remaining = get("/api/companies/" + companyId + "/time-entries?from=2026-08-01&to=2026-08-31");
        assertEquals(2, remaining.size());                       // only the billed SO1629 entries survive
        for (JsonNode e : remaining) assertTrue(e.get("invoiced").asBoolean());
    }

    @Test
    void otherUsersCannotSeeOrUseMyConnections() throws Exception {
        long id = createConnection("JSONRPC", FakeOdooServer.SECRET);

        String otherToken = register("other-" + UUID.randomUUID() + "@example.com");
        String saved = token;
        token = otherToken;
        try {
            assertEquals(0, get("/api/integrations").size());
            mvc.perform(authedBuilder(MockMvcRequestBuilders.get("/api/integrations/" + id))).andExpect(status().isNotFound());
            mvc.perform(authedBuilder(MockMvcRequestBuilders.post("/api/integrations/" + id + "/sync"))).andExpect(status().isNotFound());
            mvc.perform(authedBuilder(MockMvcRequestBuilders.get("/api/companies/" + companyId + "/time-entries"))).andExpect(status().isNotFound());
        } finally {
            token = saved;
        }
    }

    @Test
    void syncAllImportsFromEveryActiveConnection() throws Exception {
        createConnection("JSONRPC", FakeOdooServer.SECRET);
        JsonNode results = post("/api/integrations/sync?companyId=" + companyId, null);
        assertEquals(1, results.size());
        assertEquals(3, results.get(0).get("imported").asInt());
    }

    // ---------------------------------------------------------------- helpers

    private Map<String, Object> invoiceBody(JsonNode summaryLine) {
        Map<String, Object> item = new java.util.HashMap<>();
        item.put("projectId", summaryLine.get("projectId").asLong());
        item.put("projectName", summaryLine.get("projectName").asText());
        item.put("unit", "Hour");
        item.put("quantity", summaryLine.get("hours").asDouble());
        item.put("unitPrice", 61);
        item.put("timeEntryIds", json.convertValue(summaryLine.get("timeEntryIds"), java.util.List.class));
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("clientId", clientId);
        body.put("name", "August");
        body.put("issueDate", "2026-08-31");
        body.put("periodStart", "2026-08-01");
        body.put("periodEnd", "2026-08-31");
        body.put("currency", "EUR");
        body.put("vatPercent", 0);
        body.put("items", java.util.List.of(item));
        return body;
    }

    private Map<String, Object> connectionBody(String mode, String key) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("provider", "ODOO");
        body.put("name", "ABF Odoo");
        body.put("companyId", companyId);
        body.put("clientId", clientId);
        body.put("baseUrl", odoo.url());
        body.put("dbName", "prod");
        body.put("username", FakeOdooServer.LOGIN);
        body.put("apiKey", key);
        body.put("apiMode", mode);
        body.put("onlyMine", true);
        body.put("lookbackDays", 700);
        body.put("defaultHourlyRate", 61);
        return body;
    }

    private long createConnection(String mode, String key) throws Exception {
        return post("/api/integrations", connectionBody(mode, key)).get("id").asLong();
    }

    private String register(String email) throws Exception {
        MvcResult r = mvc.perform(MockMvcRequestBuilders.post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "password123", "fullName", "T"))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private MockHttpServletRequestBuilder authedBuilder(MockHttpServletRequestBuilder b) {
        return b.header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder authedDelete(String url) {
        return authedBuilder(MockMvcRequestBuilders.delete(url));
    }

    private JsonNode get(String url) throws Exception {
        return json.readTree(mvc.perform(authedBuilder(MockMvcRequestBuilders.get(url)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode post(String url, Object body) throws Exception {
        MockHttpServletRequestBuilder b = authedBuilder(MockMvcRequestBuilders.post(url))
                .contentType(MediaType.APPLICATION_JSON);
        if (body != null) b.content(json.writeValueAsString(body));
        return json.readTree(mvc.perform(b).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode put(String url, Object body) throws Exception {
        return json.readTree(mvc.perform(authedBuilder(MockMvcRequestBuilders.put(url))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private void mvcPostExpect(int expected, String url, Object body) throws Exception {
        mvc.perform(authedBuilder(MockMvcRequestBuilders.post(url))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().is(expected));
    }
}
