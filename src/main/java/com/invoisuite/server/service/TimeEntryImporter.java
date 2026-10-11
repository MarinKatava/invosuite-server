package com.invoisuite.server.service;

import com.invoisuite.server.domain.IntegrationConnection;
import com.invoisuite.server.domain.Project;
import com.invoisuite.server.domain.TimeEntry;
import com.invoisuite.server.dto.integration.SyncResult;
import com.invoisuite.server.integration.ExternalTimeEntry;
import com.invoisuite.server.repository.IntegrationConnectionRepository;
import com.invoisuite.server.repository.ProjectRepository;
import com.invoisuite.server.repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applies a fetched set of external bookings to the local time entries in one short transaction.
 * Kept separate from the network fetch so no database connection is held during HTTP calls.
 */
@Service
@RequiredArgsConstructor
public class TimeEntryImporter {

    private final IntegrationConnectionRepository connectionRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public SyncResult apply(Long connectionId, LocalDate from, LocalDate to, List<ExternalTimeEntry> allFetched) {
        // Zero-hour bookings (e.g. placeholder lines Odoo creates for new tasks) carry nothing to bill.
        List<ExternalTimeEntry> fetched = allFetched.stream().filter(e -> e.hours().signum() != 0).toList();
        IntegrationConnection connection = connectionRepository.findById(connectionId).orElseThrow();

        Map<String, TimeEntry> existing = timeEntryRepository
                .findByConnectionIdAndExternalIdIn(connectionId, fetched.stream().map(ExternalTimeEntry::externalId).toList())
                .stream().collect(Collectors.toMap(TimeEntry::getExternalId, e -> e));
        Map<String, Project> projectCache = new HashMap<>();

        int imported = 0, updated = 0, skippedBilled = 0;
        BigDecimal totalHours = BigDecimal.ZERO;

        for (ExternalTimeEntry ext : fetched) {
            totalHours = totalHours.add(ext.hours());
            TimeEntry entry = existing.get(ext.externalId());

            if (entry == null) {
                timeEntryRepository.save(TimeEntry.builder()
                        .company(connection.getCompany())
                        .connection(connection)
                        .project(resolveProject(connection, ext.projectName(), projectCache))
                        .projectName(ext.projectName())
                        .externalId(ext.externalId())
                        .workDate(ext.date())
                        .hours(ext.hours())
                        .description(ext.description())
                        .build());
                imported++;
            } else if (changed(entry, ext)) {
                if (entry.getInvoiceItem() != null) {
                    skippedBilled++;   // already billed: the invoice stays authoritative
                } else {
                    entry.setWorkDate(ext.date());
                    entry.setHours(ext.hours());
                    entry.setDescription(ext.description());
                    entry.setProjectName(ext.projectName());
                    entry.setProject(resolveProject(connection, ext.projectName(), projectCache));
                    updated++;
                }
            }
        }

        // Bookings deleted in the source system disappear here too, unless they were already billed.
        Set<String> fetchedIds = fetched.stream().map(ExternalTimeEntry::externalId).collect(Collectors.toSet());
        int removed = 0;
        for (TimeEntry local : timeEntryRepository.findByConnectionAndPeriod(connectionId, from, to)) {
            if (!fetchedIds.contains(local.getExternalId()) && local.getInvoiceItem() == null) {
                timeEntryRepository.delete(local);
                removed++;
            }
        }

        String message = "Imported " + imported + " new, updated " + updated + ", removed " + removed
                + (skippedBilled > 0 ? ", left " + skippedBilled + " already-billed entries unchanged" : "");
        connection.setLastSyncAt(LocalDateTime.now());
        connection.setLastSyncStatus("SUCCESS");
        connection.setLastSyncMessage(message);

        return new SyncResult(connectionId, connection.getName(), true, message, from, to,
                imported, updated, removed, skippedBilled, totalHours);
    }

    @Transactional
    public void recordFailure(Long connectionId, String message) {
        connectionRepository.findById(connectionId).ifPresent(c -> {
            c.setLastSyncAt(LocalDateTime.now());
            c.setLastSyncStatus("FAILED");
            c.setLastSyncMessage(message.length() > 1000 ? message.substring(0, 1000) : message);
        });
    }

    private boolean changed(TimeEntry e, ExternalTimeEntry ext) {
        return e.getHours().compareTo(ext.hours()) != 0
                || !e.getWorkDate().equals(ext.date())
                || !e.getProjectName().equals(ext.projectName())
                || !Objects.equals(e.getDescription(), ext.description());
    }

    /** Matches by name within the company, creating the project (assigned to the connection's client) if new. */
    private Project resolveProject(IntegrationConnection connection, String name, Map<String, Project> cache) {
        return cache.computeIfAbsent(name.toLowerCase(), key ->
                projectRepository.findFirstByCompanyIdAndNameIgnoreCase(connection.getCompany().getId(), name)
                        .orElseGet(() -> projectRepository.save(Project.builder()
                                .company(connection.getCompany())
                                .client(connection.getClient())
                                .name(name)
                                .defaultUnit("Hour")
                                .defaultRate(connection.getDefaultHourlyRate())
                                .currency(connection.getCompany().getDefaultCurrency())
                                .active(true)
                                .build())));
    }
}
