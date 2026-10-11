package com.invoisuite.server.service;

import com.invoisuite.server.common.BadRequestException;
import com.invoisuite.server.common.ResourceNotFoundException;
import com.invoisuite.server.domain.Client;
import com.invoisuite.server.domain.Company;
import com.invoisuite.server.domain.IntegrationConnection;
import com.invoisuite.server.dto.integration.IntegrationRequest;
import com.invoisuite.server.dto.integration.IntegrationResponse;
import com.invoisuite.server.integration.ConnectionTestResult;
import com.invoisuite.server.integration.OutboundUrlValidator;
import com.invoisuite.server.integration.TimeTrackingProvider;
import com.invoisuite.server.repository.IntegrationConnectionRepository;
import com.invoisuite.server.repository.TimeEntryRepository;
import com.invoisuite.server.repository.UserRepository;
import com.invoisuite.server.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Per-user management of connections to external time-tracking platforms. */
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private static final int DEFAULT_LOOKBACK_DAYS = 45;

    private final IntegrationConnectionRepository connectionRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final ClientService clientService;
    private final OutboundUrlValidator urlValidator;
    private final List<TimeTrackingProvider> providers;

    @Transactional(readOnly = true)
    public List<IntegrationResponse> list() {
        return connectionRepository.findByOwnerIdOrderByNameAsc(CurrentUser.id()).stream()
                .map(IntegrationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public IntegrationResponse get(Long id) {
        return IntegrationResponse.from(getOwned(id));
    }

    @Transactional(readOnly = true)
    public IntegrationConnection getOwned(Long id) {
        return connectionRepository.findByIdAndOwnerId(id, CurrentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Integration " + id + " not found"));
    }

    @Transactional
    public IntegrationResponse create(IntegrationRequest request) {
        if (request.apiKey() == null || request.apiKey().isBlank()) {
            throw new BadRequestException("An API key (or password) is required");
        }
        IntegrationConnection connection = IntegrationConnection.builder()
                .owner(userRepository.getReferenceById(CurrentUser.id()))
                .build();
        apply(connection, request);
        return IntegrationResponse.from(connectionRepository.save(connection));
    }

    @Transactional
    public IntegrationResponse update(Long id, IntegrationRequest request) {
        IntegrationConnection connection = getOwned(id);
        apply(connection, request);
        return IntegrationResponse.from(connection);
    }

    @Transactional
    public void delete(Long id) {
        IntegrationConnection connection = getOwned(id);
        // Unbilled hours vanish with the connection; billed ones stay as invoice history.
        timeEntryRepository.deleteUnbilledByConnection(id);
        timeEntryRepository.detachFromConnection(id);
        connectionRepository.delete(connection);
    }

    /**
     * Tests the given settings without saving them. A blank API key falls back to the one stored
     * on {@code existingId}, so the edit form can re-test without the user retyping the secret.
     */
    @Transactional(readOnly = true)
    public ConnectionTestResult test(IntegrationRequest request, Long existingId) {
        IntegrationConnection transientConnection = IntegrationConnection.builder().build();
        transientConnection.setProvider(request.provider());
        transientConnection.setBaseUrl(request.baseUrl());
        transientConnection.setDbName(blankToNull(request.dbName()));
        transientConnection.setUsername(request.username());
        transientConnection.setApiMode(request.apiMode());
        transientConnection.setOnlyMine(request.onlyMine() == null || request.onlyMine());

        String key = request.apiKey();
        if ((key == null || key.isBlank()) && existingId != null) {
            key = getOwned(existingId).getApiKey();
        }
        if (key == null || key.isBlank()) {
            return new ConnectionTestResult(false, "Enter the API key (or password) to test the connection");
        }
        transientConnection.setApiKey(key);

        return providerFor(transientConnection).test(transientConnection);
    }

    public TimeTrackingProvider providerFor(IntegrationConnection connection) {
        return providers.stream()
                .filter(p -> p.type() == connection.getProvider())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported provider " + connection.getProvider()));
    }

    private void apply(IntegrationConnection c, IntegrationRequest r) {
        Company company = companyService.getOwned(r.companyId());
        Client client = r.clientId() != null ? clientService.getOwned(r.companyId(), r.clientId()) : null;

        c.setProvider(r.provider());
        c.setName(r.name().trim());
        c.setCompany(company);
        c.setClient(client);
        c.setBaseUrl(urlValidator.validate(r.baseUrl()));
        c.setDbName(blankToNull(r.dbName()));
        c.setUsername(r.username().trim());
        if (r.apiKey() != null && !r.apiKey().isBlank()) {
            c.setApiKey(r.apiKey().trim());
        }
        c.setApiMode(r.apiMode());
        c.setOnlyMine(r.onlyMine() == null || r.onlyMine());
        c.setLookbackDays(r.lookbackDays() != null ? r.lookbackDays() : DEFAULT_LOOKBACK_DAYS);
        c.setDefaultHourlyRate(r.defaultHourlyRate());
        c.setActive(r.active() == null || r.active());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
