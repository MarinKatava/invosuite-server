package com.invoisuite.server.web;

import com.invoisuite.server.dto.integration.IntegrationRequest;
import com.invoisuite.server.dto.integration.IntegrationResponse;
import com.invoisuite.server.dto.integration.SyncRequest;
import com.invoisuite.server.dto.integration.SyncResult;
import com.invoisuite.server.integration.ConnectionTestResult;
import com.invoisuite.server.service.IntegrationService;
import com.invoisuite.server.service.IntegrationSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;
    private final IntegrationSyncService syncService;

    @GetMapping
    public List<IntegrationResponse> list() {
        return integrationService.list();
    }

    @GetMapping("/{id}")
    public IntegrationResponse get(@PathVariable Long id) {
        return integrationService.get(id);
    }

    @PostMapping
    public ResponseEntity<IntegrationResponse> create(@Valid @RequestBody IntegrationRequest request) {
        return ResponseEntity.ok(integrationService.create(request));
    }

    @PutMapping("/{id}")
    public IntegrationResponse update(@PathVariable Long id, @Valid @RequestBody IntegrationRequest request) {
        return integrationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        integrationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Tests unsaved form values; pass connectionId to reuse the stored API key when the form leaves it blank. */
    @PostMapping("/test")
    public ConnectionTestResult test(@Valid @RequestBody IntegrationRequest request,
                                     @RequestParam(required = false) Long connectionId) {
        return integrationService.test(request, connectionId);
    }

    @PostMapping("/{id}/sync")
    public SyncResult sync(@PathVariable Long id, @RequestBody(required = false) SyncRequest request) {
        return syncService.sync(id, request);
    }

    /** The "import time bookings" button: imports from all active connections (optionally for one company). */
    @PostMapping("/sync")
    public List<SyncResult> syncAll(@RequestParam(required = false) Long companyId) {
        return syncService.syncAll(companyId);
    }
}
