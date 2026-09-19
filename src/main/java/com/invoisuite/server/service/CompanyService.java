package com.invoisuite.server.service;

import com.invoisuite.server.common.ResourceNotFoundException;
import com.invoisuite.server.domain.Company;
import com.invoisuite.server.domain.User;
import com.invoisuite.server.dto.company.CompanyRequest;
import com.invoisuite.server.dto.company.CompanyResponse;
import com.invoisuite.server.repository.CompanyRepository;
import com.invoisuite.server.repository.UserRepository;
import com.invoisuite.server.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CompanyResponse> listMine() {
        return companyRepository.findByOwnerIdOrderByNameAsc(CurrentUser.id()).stream()
                .map(CompanyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(Long id) {
        return CompanyResponse.from(getOwned(id));
    }

    /** Fetches the company and verifies it belongs to the current user; used by other services too. */
    @Transactional(readOnly = true)
    public Company getOwned(Long id) {
        return companyRepository.findByIdAndOwnerId(id, CurrentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Company " + id + " not found"));
    }

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        User owner = userRepository.getReferenceById(CurrentUser.id());
        Company company = Company.builder()
                .owner(owner)
                .name(request.name())
                .addressLine(request.addressLine())
                .city(request.city())
                .postalCode(request.postalCode())
                .country(request.country())
                .registrationNumber(request.registrationNumber())
                .vatId(request.vatId())
                .bankName(request.bankName())
                .iban(request.iban())
                .bic(request.bic())
                .defaultCurrency(request.defaultCurrency())
                .defaultVatPercent(request.defaultVatPercent() != null ? request.defaultVatPercent() : BigDecimal.ZERO)
                .vatExemptText(request.vatExemptText())
                .invoicingPlace(request.invoicingPlace())
                .build();
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request) {
        Company company = getOwned(id);
        company.setName(request.name());
        company.setAddressLine(request.addressLine());
        company.setCity(request.city());
        company.setPostalCode(request.postalCode());
        company.setCountry(request.country());
        company.setRegistrationNumber(request.registrationNumber());
        company.setVatId(request.vatId());
        company.setBankName(request.bankName());
        company.setIban(request.iban());
        company.setBic(request.bic());
        company.setDefaultCurrency(request.defaultCurrency());
        company.setDefaultVatPercent(request.defaultVatPercent() != null ? request.defaultVatPercent() : BigDecimal.ZERO);
        company.setVatExemptText(request.vatExemptText());
        company.setInvoicingPlace(request.invoicingPlace());
        return CompanyResponse.from(company);
    }

    @Transactional
    public void delete(Long id) {
        Company company = getOwned(id);
        companyRepository.delete(company);
    }
}
