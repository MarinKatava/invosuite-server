package com.invoisuite.server.domain;

/** External time-tracking platforms time bookings can be imported from. */
public enum IntegrationProvider {
    ODOO("Odoo");

    private final String label;

    IntegrationProvider(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
