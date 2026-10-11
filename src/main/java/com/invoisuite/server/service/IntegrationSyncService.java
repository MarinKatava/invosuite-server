package com.invoisuite.server.service;

import com.invoisuite.server.dto.integration.SyncRequest;
import com.invoisuite.server.dto.integration.SyncResult;
import com.invoisuite.server.common.BadRequestException;
import com.invoisuite.server.domain.IntegrationConnection;
import com.invoisuite.server.integration.ExternalTimeEntry;
import com.invoisuite.server.integration.IntegrationException;
import com.invoisuite.server.repository.IntegrationConnectionRepository;
import com.invoisuite.server.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Runs imports: fetches from the external platform (no DB transaction open), then stores the result. */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationSyncService {

    private final IntegrationService integrationService;
    private final IntegrationConnectionRepository connectionRepository;
    private final TimeEntryImporter importer;

    private final Set<Long> running = ConcurrentHashMap.newKeySet();

    public SyncResult sync(Long connectionId, SyncRequest request) {
        // getOwned enforces that the connection belongs to the signed-in user.
        IntegrationConnection connection = integrationService.getOwned(connectionId);

        LocalDate to = request != null && request.to() != null ? request.to() : LocalDate.now();
        LocalDate from = request != null && request.from() != null
                ? request.from() : to.minusDays(connection.getLookbackDays());
        if (from.isAfter(to)) {
            throw new BadRequestException("The start date must not be after the end date");
        }

        if (!running.add(connectionId)) {
            return SyncResult.failure(connectionId, connection.getName(), "An import is already running for this connection");
        }
        try {
            List<ExternalTimeEntry> fetched = integrationService.providerFor(connection).fetch(connection, from, to);
            return importer.apply(connectionId, from, to, fetched);
        } catch (IntegrationException | BadRequestException e) {
            importer.recordFailure(connectionId, e.getMessage());
            return SyncResult.failure(connectionId, connection.getName(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected failure importing from connection {}", connectionId, e);
            String message = "Unexpected error during import";
            importer.recordFailure(connectionId, message);
            return SyncResult.failure(connectionId, connection.getName(), message);
        } finally {
            running.remove(connectionId);
        }
    }

    /** Imports from every active connection of the signed-in user, optionally limited to one company. */
    public List<SyncResult> syncAll(Long companyId) {
        Long userId = CurrentUser.id();
        List<IntegrationConnection> connections = companyId != null
                ? connectionRepository.findByOwnerIdAndCompanyIdAndActiveTrueOrderByNameAsc(userId, companyId)
                : connectionRepository.findByOwnerIdAndActiveTrueOrderByNameAsc(userId);
        return connections.stream().map(c -> sync(c.getId(), null)).toList();
    }
}
