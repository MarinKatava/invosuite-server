package com.invoisuite.server.domain;

/**
 * Which Odoo external API to talk to. JSON2 is the bearer-token API introduced in
 * Odoo 19 (API key only); JSONRPC is the legacy /jsonrpc endpoint (password or API key);
 * SESSION signs in like the web client does ({@code /web/session/authenticate}) and so works
 * with the user's ordinary login and password on any recent Odoo version.
 */
public enum OdooApiMode {
    JSON2,
    JSONRPC,
    SESSION
}
