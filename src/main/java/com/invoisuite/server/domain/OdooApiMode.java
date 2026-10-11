package com.invoisuite.server.domain;

/**
 * Which Odoo external API to talk to. JSON2 is the bearer-token API introduced in
 * Odoo 19; JSONRPC is the legacy /jsonrpc endpoint that older versions expose.
 */
public enum OdooApiMode {
    JSON2,
    JSONRPC
}
