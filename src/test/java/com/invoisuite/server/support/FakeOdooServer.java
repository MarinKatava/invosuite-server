package com.invoisuite.server.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Minimal in-process stand-in for Odoo's timesheet API, speaking both JSON-RPC (/jsonrpc) and JSON-2 (/json/2). */
public class FakeOdooServer implements AutoCloseable {

    public static final String LOGIN = "freelancer@example.com";
    public static final String SECRET = "s3cret-key";
    public static final int MY_UID = 7;
    public static final String PASSWORD = "my-login-password";
    private static final String SESSION_ID = "sess-abc123";

    private static final ObjectMapper JSON = new ObjectMapper();

    public record Line(long id, String date, double hours, String description, long projectId,
                       String projectName, Long taskId, String taskName, long userId) {
    }

    public final List<Line> lines = new CopyOnWriteArrayList<>();
    private final HttpServer server;

    public FakeOdooServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jsonrpc", this::handleJsonRpc);
        server.createContext("/json/2/", this::handleJson2);
        server.createContext("/web/session/authenticate", this::handleSessionAuthenticate);
        server.createContext("/web/dataset/call_kw/", this::handleCallKw);
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    // ---- legacy JSON-RPC ----
    /** Like Odoo, ignores request bodies sent with chunked transfer encoding (no Content-Length). */
    private JsonNode readBody(HttpExchange ex) throws IOException {
        if (ex.getRequestHeaders().getFirst("Content-Length") == null) {
            return JSON.createObjectNode();   // what Odoo effectively sees: an empty body
        }
        return JSON.readTree(ex.getRequestBody());
    }

    private void handleJsonRpc(HttpExchange ex) throws IOException {
        JsonNode params = readBody(ex).path("params");
        String service = params.path("service").asText();
        String method = params.path("method").asText();
        JsonNode args = params.path("args");

        ObjectNode response = JSON.createObjectNode().put("jsonrpc", "2.0").put("id", 1);
        if (service.equals("common") && method.equals("authenticate")) {
            boolean ok = args.get(1).asText().equals(LOGIN) && args.get(2).asText().equals(SECRET);
            if (ok) response.put("result", MY_UID); else response.put("result", false);
        } else if (service.equals("object") && method.equals("execute_kw")) {
            if (args.get(1).asLong() != MY_UID || !args.get(2).asText().equals(SECRET)) {
                response.set("error", rpcError("Access Denied"));
            } else if (!args.get(3).asText().equals("account.analytic.line")) {
                response.set("error", rpcError("Unknown model " + args.get(3).asText()));
            } else {
                response.set("result", searchRead(args.get(5).path(0), args.get(6)));
            }
        }
        reply(ex, 200, response);
    }

    private ObjectNode rpcError(String message) {
        ObjectNode error = JSON.createObjectNode().put("code", 200).put("message", "Odoo Server Error");
        error.set("data", JSON.createObjectNode().put("name", "odoo.exceptions.AccessDenied").put("message", message));
        return error;
    }

    // ---- web session (login + password) ----
    private void handleSessionAuthenticate(HttpExchange ex) throws IOException {
        JsonNode params = readBody(ex).path("params");
        ObjectNode response = JSON.createObjectNode().put("jsonrpc", "2.0").put("id", 1);
        boolean ok = params.path("login").asText().equals(LOGIN) && params.path("password").asText().equals(PASSWORD);
        if (ok) {
            response.set("result", JSON.createObjectNode().put("uid", MY_UID));
            ex.getResponseHeaders().add("Set-Cookie", "session_id=" + SESSION_ID + "; Expires=Fri, 01-Jan-2100 00:00:00 GMT; Max-Age=604800; HttpOnly; Path=/");
        } else {
            // Odoo answers a failed login with 200 and uid = false
            response.set("result", JSON.createObjectNode().put("uid", false));
        }
        reply(ex, 200, response);
    }

    private void handleCallKw(HttpExchange ex) throws IOException {
        ObjectNode response = JSON.createObjectNode().put("jsonrpc", "2.0").put("id", 1);
        String cookie = ex.getRequestHeaders().getFirst("Cookie");
        JsonNode params = readBody(ex).path("params");
        if (cookie == null || !cookie.contains("session_id=" + SESSION_ID)) {
            ObjectNode error = JSON.createObjectNode().put("code", 100).put("message", "Odoo Session Expired");
            error.set("data", JSON.createObjectNode().put("message", "Session expired"));
            response.set("error", error);
        } else {
            response.set("result", searchRead(params.path("args").path(0), params.path("kwargs")));
        }
        reply(ex, 200, response);
    }

    // ---- JSON-2 ----
    private void handleJson2(HttpExchange ex) throws IOException {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (!("Bearer " + SECRET).equals(auth)) {
            reply(ex, 401, JSON.createObjectNode().put("name", "odoo.exceptions.AccessDenied").put("message", "Invalid apikey"));
            return;
        }
        String path = ex.getRequestURI().getPath();
        JsonNode body = readBody(ex);
        if (path.equals("/json/2/res.users/context_get")) {
            reply(ex, 200, JSON.createObjectNode().put("lang", "en_US").put("uid", MY_UID));
        } else if (path.equals("/json/2/account.analytic.line/search_read")) {
            reply(ex, 200, searchRead(body.path("domain"), body));
        } else {
            reply(ex, 404, JSON.createObjectNode().put("message", "no such endpoint"));
        }
    }

    // ---- shared query evaluation ----
    private ArrayNode searchRead(JsonNode domain, JsonNode options) {
        List<Line> matching = new ArrayList<>();
        for (Line line : lines) {
            boolean ok = true;
            for (JsonNode cond : domain) {
                String field = cond.get(0).asText();
                String op = cond.get(1).asText();
                JsonNode value = cond.get(2);
                ok &= switch (field) {
                    case "date" -> op.equals(">=") ? line.date().compareTo(value.asText()) >= 0
                            : line.date().compareTo(value.asText()) <= 0;
                    case "user_id" -> line.userId() == value.asLong();
                    default -> true;   // project_id != false: every fake line has a project
                };
            }
            if (ok) matching.add(line);
        }
        matching.sort((a, b) -> a.date().equals(b.date()) ? Long.compare(a.id(), b.id()) : a.date().compareTo(b.date()));

        int offset = options.path("offset").asInt(0);
        int limit = options.path("limit").asInt(Integer.MAX_VALUE);
        ArrayNode out = JSON.createArrayNode();
        for (int i = offset; i < Math.min(matching.size(), offset + limit); i++) {
            Line l = matching.get(i);
            ObjectNode row = out.addObject();
            row.put("id", l.id());
            row.put("date", l.date());
            row.put("unit_amount", l.hours());
            if (l.description() == null) row.put("name", false); else row.put("name", l.description());
            row.putArray("project_id").add(l.projectId()).add(l.projectName());
            if (l.taskId() == null) row.put("task_id", false); else row.putArray("task_id").add(l.taskId()).add(l.taskName());
        }
        return out;
    }

    private void reply(HttpExchange ex, int status, JsonNode body) throws IOException {
        byte[] bytes = JSON.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
