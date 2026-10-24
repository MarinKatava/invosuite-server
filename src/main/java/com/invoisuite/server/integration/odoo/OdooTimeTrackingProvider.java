package com.invoisuite.server.integration.odoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.invoisuite.server.domain.IntegrationConnection;
import com.invoisuite.server.domain.IntegrationProvider;
import com.invoisuite.server.integration.ConnectionTestResult;
import com.invoisuite.server.integration.ExternalTimeEntry;
import com.invoisuite.server.integration.IntegrationException;
import com.invoisuite.server.integration.OutboundUrlValidator;
import com.invoisuite.server.integration.TimeTrackingProvider;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

/** Imports Odoo timesheet lines ({@code account.analytic.line} with a project) as time entries. */
@Component
@RequiredArgsConstructor
public class OdooTimeTrackingProvider implements TimeTrackingProvider {

    private static final String MODEL = "account.analytic.line";
    private static final int PAGE_SIZE = 500;
    private static final List<String> FIELDS = List.of("id", "date", "unit_amount", "name", "project_id", "task_id");

    private final RestClient integrationRestClient;
    private final OutboundUrlValidator urlValidator;

    @Override
    public IntegrationProvider type() {
        return IntegrationProvider.ODOO;
    }

    @Override
    public ConnectionTestResult test(IntegrationConnection connection) {
        try {
            OdooTransport transport = transport(connection);
            long uid = transport.currentUserId();
            transport.searchRead(MODEL, List.of(List.of("project_id", "!=", false)), List.of("id"), 1, 0, null);
            return new ConnectionTestResult(true, "Connected to Odoo as user " + uid + ", timesheets are readable");
        } catch (IntegrationException e) {
            return new ConnectionTestResult(false, hint(e.getMessage()));
        } catch (com.invoisuite.server.common.BadRequestException e) {
            return new ConnectionTestResult(false, e.getMessage());
        }
    }

    @Override
    public List<ExternalTimeEntry> fetch(IntegrationConnection connection, LocalDate from, LocalDate to) {
        try {
            OdooTransport transport = transport(connection);

            List<Object> domain = new ArrayList<>();
            domain.add(List.of("project_id", "!=", false));
            domain.add(List.of("date", ">=", from.toString()));
            domain.add(List.of("date", "<=", to.toString()));
            if (connection.isOnlyMine()) {
                domain.add(List.of("user_id", "=", transport.currentUserId()));
            }

            List<ExternalTimeEntry> entries = new ArrayList<>();
            int offset = 0;
            while (true) {
                JsonNode page = transport.searchRead(MODEL, domain, FIELDS, PAGE_SIZE, offset, "date asc, id asc");
                for (JsonNode row : page) {
                    entries.add(toEntry(row));
                }
                if (page.size() < PAGE_SIZE) {
                    return entries;
                }
                offset += PAGE_SIZE;
            }
        } catch (IntegrationException e) {
            throw new IntegrationException(hint(e.getMessage()), e);
        }
    }

    private OdooTransport transport(IntegrationConnection c) {
        String baseUrl = urlValidator.validate(c.getBaseUrl());
        return switch (c.getApiMode()) {
            case JSON2 -> new OdooJson2Transport(integrationRestClient, baseUrl, c.getDbName(), c.getApiKey());
            case JSONRPC -> new OdooJsonRpcTransport(integrationRestClient, baseUrl, c.getDbName(), c.getUsername(), c.getApiKey());
            case SESSION -> new OdooSessionTransport(integrationRestClient, baseUrl, c.getDbName(), c.getUsername(), c.getApiKey());
        };
    }

    private ExternalTimeEntry toEntry(JsonNode row) {
        JsonNode project = row.path("project_id");
        String projectName = project.isArray() && project.size() > 1 ? project.get(1).asText() : "Unknown project";

        String description = row.hasNonNull("name") && row.get("name").isTextual() ? row.get("name").asText().trim() : "";
        JsonNode task = row.path("task_id");
        if (task.isArray() && task.size() > 1) {
            description = ("[" + task.get(1).asText() + "] " + description).trim();
        }

        return new ExternalTimeEntry(
                row.get("id").asText(),
                LocalDate.parse(row.get("date").asText()),
                new BigDecimal(row.path("unit_amount").asText("0")),
                projectName,
                description.isEmpty() ? null : description);
    }

    private String hint(String message) {
        if (message != null && message.contains("project_id")) {
            return message + " (is the Odoo Timesheets app installed?)";
        }
        return message;
    }
}
