package com.invoisuite.server.integration.odoo;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** The two calls the importer needs from Odoo, independent of which wire API carries them. */
interface OdooTransport {

    /** Id of the Odoo user the credentials belong to (verifies the credentials as a side effect). */
    long currentUserId();

    /** Runs {@code search_read}; the result is a JSON array of records. */
    JsonNode searchRead(String model, List<Object> domain, List<String> fields, int limit, int offset, String order);
}
