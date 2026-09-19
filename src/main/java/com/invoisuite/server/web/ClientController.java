package com.invoisuite.server.web;

import com.invoisuite.server.dto.client.ClientRequest;
import com.invoisuite.server.dto.client.ClientResponse;
import com.invoisuite.server.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies/{companyId}/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public List<ClientResponse> list(@PathVariable Long companyId) {
        return clientService.list(companyId);
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@PathVariable Long companyId, @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(clientService.create(companyId, request));
    }

    @PutMapping("/{clientId}")
    public ClientResponse update(@PathVariable Long companyId, @PathVariable Long clientId,
                                  @Valid @RequestBody ClientRequest request) {
        return clientService.update(companyId, clientId, request);
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> delete(@PathVariable Long companyId, @PathVariable Long clientId) {
        clientService.delete(companyId, clientId);
        return ResponseEntity.noContent().build();
    }
}
