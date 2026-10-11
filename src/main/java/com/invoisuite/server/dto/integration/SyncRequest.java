package com.invoisuite.server.dto.integration;

import java.time.LocalDate;

/** Optional explicit import window; defaults to "the connection's look-back period up to today". */
public record SyncRequest(LocalDate from, LocalDate to) {
}
