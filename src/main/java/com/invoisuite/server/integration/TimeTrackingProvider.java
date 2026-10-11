package com.invoisuite.server.integration;

import com.invoisuite.server.domain.IntegrationConnection;
import com.invoisuite.server.domain.IntegrationProvider;

import java.time.LocalDate;
import java.util.List;

/** A platform time bookings can be imported from. Add an implementation to support another one. */
public interface TimeTrackingProvider {

    IntegrationProvider type();

    /** Checks the credentials and that timesheets are readable. Must not throw for ordinary failures. */
    ConnectionTestResult test(IntegrationConnection connection);

    /** Returns every booking in [from, to] that this connection is configured to import. */
    List<ExternalTimeEntry> fetch(IntegrationConnection connection, LocalDate from, LocalDate to);
}
