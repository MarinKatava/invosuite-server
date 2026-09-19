package com.invoisuite.server.service;

import com.invoisuite.server.common.ResourceNotFoundException;
import com.invoisuite.server.domain.Client;
import com.invoisuite.server.domain.Company;
import com.invoisuite.server.dto.client.ClientRequest;
import com.invoisuite.server.dto.client.ClientResponse;
import com.invoisuite.server.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final CompanyService companyService;

    @Transactional(readOnly = true)
    public List<ClientResponse> list(Long companyId) {
        companyService.getOwned(companyId);
        return clientRepository.findByCompanyIdOrderByNameAsc(companyId).stream()
                .map(ClientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Client getOwned(Long companyId, Long clientId) {
        companyService.getOwned(companyId);
        return clientRepository.findByIdAndCompanyId(clientId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Client " + clientId + " not found"));
    }

    @Transactional
    public ClientResponse create(Long companyId, ClientRequest request) {
        Company company = companyService.getOwned(companyId);
        Client client = Client.builder()
                .company(company)
                .name(request.name())
                .addressLine(request.addressLine())
                .vatId(request.vatId())
                .email(request.email())
                .build();
        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional
    public ClientResponse update(Long companyId, Long clientId, ClientRequest request) {
        Client client = getOwned(companyId, clientId);
        client.setName(request.name());
        client.setAddressLine(request.addressLine());
        client.setVatId(request.vatId());
        client.setEmail(request.email());
        return ClientResponse.from(client);
    }

    @Transactional
    public void delete(Long companyId, Long clientId) {
        clientRepository.delete(getOwned(companyId, clientId));
    }
}
