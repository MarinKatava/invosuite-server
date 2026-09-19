package com.invoisuite.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoisuite.server.common.ResourceNotFoundException;
import com.invoisuite.server.domain.Client;
import com.invoisuite.server.domain.Company;
import com.invoisuite.server.domain.Invoice;
import com.invoisuite.server.domain.InvoiceItem;
import com.invoisuite.server.domain.InvoiceStatus;
import com.invoisuite.server.domain.Project;
import com.invoisuite.server.dto.invoice.InvoiceItemRequest;
import com.invoisuite.server.dto.invoice.InvoiceRequest;
import com.invoisuite.server.dto.invoice.InvoiceResponse;
import com.invoisuite.server.dto.invoice.InvoiceSummaryResponse;
import com.invoisuite.server.repository.InvoiceRepository;
import com.invoisuite.server.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final int MONEY_SCALE = 2;

    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;
    private final CompanyService companyService;
    private final ClientService clientService;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<InvoiceSummaryResponse> list(Long companyId) {
        companyService.getOwned(companyId);
        return invoiceRepository.findByCompanyIdOrderByIssueDateDescIdDesc(companyId).stream()
                .map(InvoiceSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(Long companyId, Long invoiceId) {
        return InvoiceResponse.from(getOwned(companyId, invoiceId));
    }

    @Transactional(readOnly = true)
    public Invoice getOwned(Long companyId, Long invoiceId) {
        companyService.getOwned(companyId);
        return invoiceRepository.findByIdAndCompanyId(invoiceId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + invoiceId + " not found"));
    }

    @Transactional
    public InvoiceResponse create(Long companyId, InvoiceRequest request) {
        Company company = companyService.getOwned(companyId);
        Client client = clientService.getOwned(companyId, request.clientId());

        Invoice invoice = Invoice.builder()
                .company(company)
                .client(client)
                .status(InvoiceStatus.DRAFT)
                .build();

        applyFields(invoice, company, client, request, companyId);
        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse update(Long companyId, Long invoiceId, InvoiceRequest request) {
        Invoice invoice = getOwned(companyId, invoiceId);
        Company company = invoice.getCompany();
        Client client = clientService.getOwned(companyId, request.clientId());

        invoice.setClient(client);
        applyFields(invoice, company, client, request, companyId);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public InvoiceResponse updateStatus(Long companyId, Long invoiceId, InvoiceStatus status) {
        Invoice invoice = getOwned(companyId, invoiceId);
        invoice.setStatus(status);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public void delete(Long companyId, Long invoiceId) {
        invoiceRepository.delete(getOwned(companyId, invoiceId));
    }

    @Transactional(readOnly = true)
    public String suggestNextNumber(Long companyId, java.time.LocalDate issueDate) {
        companyService.getOwned(companyId);
        return invoiceNumberGenerator.suggestFor(companyId, issueDate);
    }

    @SneakyThrows
    private void applyFields(Invoice invoice, Company company, Client client, InvoiceRequest request, Long companyId) {
        invoice.setName(request.name());
        invoice.setInvoiceNumber(
                (request.invoiceNumber() == null || request.invoiceNumber().isBlank())
                        ? invoiceNumberGenerator.suggestFor(companyId, request.issueDate())
                        : request.invoiceNumber());
        invoice.setIssueDate(request.issueDate());
        invoice.setPeriodStart(request.periodStart());
        invoice.setPeriodEnd(request.periodEnd());
        invoice.setDeliveryDate(request.deliveryDate() != null ? request.deliveryDate() : request.issueDate());
        invoice.setInvoicingPlace(
                (request.invoicingPlace() == null || request.invoicingPlace().isBlank())
                        ? company.getInvoicingPlace()
                        : request.invoicingPlace());
        invoice.setCurrency(request.currency());
        BigDecimal vatPercent = request.vatPercent() != null ? request.vatPercent() : company.getDefaultVatPercent();
        invoice.setVatPercent(vatPercent);
        invoice.setVatExemptText(
                vatPercent.compareTo(BigDecimal.ZERO) == 0
                        ? (request.vatExemptText() != null ? request.vatExemptText() : company.getVatExemptText())
                        : null);
        invoice.setNotes(request.notes());

        // Refreshed on every save while the invoice is editable, so the
        // documents generated from it always reflect the latest edit.
        invoice.setIssuerSnapshot(objectMapper.writeValueAsString(IssuerSnapshot.from(company)));
        invoice.setClientSnapshot(objectMapper.writeValueAsString(ClientSnapshot.from(client)));

        invoice.clearItems();
        BigDecimal netTotal = BigDecimal.ZERO;
        for (InvoiceItemRequest itemRequest : request.items()) {
            Project project = itemRequest.projectId() != null
                    ? projectRepository.findByIdAndCompanyId(itemRequest.projectId(), companyId).orElse(null)
                    : null;

            BigDecimal lineTotal = itemRequest.quantity()
                    .multiply(itemRequest.unitPrice())
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            InvoiceItem item = InvoiceItem.builder()
                    .project(project)
                    .projectName(itemRequest.projectName())
                    .unit(itemRequest.unit())
                    .quantity(itemRequest.quantity())
                    .unitPrice(itemRequest.unitPrice())
                    .lineTotal(lineTotal)
                    .build();
            invoice.addItem(item);
            netTotal = netTotal.add(lineTotal);
        }

        BigDecimal vatAmount = netTotal.multiply(vatPercent)
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP);

        invoice.setNetTotal(netTotal);
        invoice.setVatAmount(vatAmount);
        invoice.setTotal(netTotal.add(vatAmount));
    }
}
