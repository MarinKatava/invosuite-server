package com.invoisuite.server.web;

import com.invoisuite.server.domain.Invoice;
import com.invoisuite.server.domain.InvoiceStatus;
import com.invoisuite.server.dto.invoice.InvoiceRequest;
import com.invoisuite.server.dto.invoice.InvoiceResponse;
import com.invoisuite.server.dto.invoice.InvoiceSummaryResponse;
import com.invoisuite.server.service.InvoiceService;
import com.invoisuite.server.service.export.InvoiceXlsxService;
import com.invoisuite.server.service.pdf.InvoicePdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companies/{companyId}/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final InvoiceXlsxService invoiceXlsxService;

    @GetMapping
    public List<InvoiceSummaryResponse> list(@PathVariable Long companyId) {
        return invoiceService.list(companyId);
    }

    @GetMapping("/{invoiceId}")
    public InvoiceResponse get(@PathVariable Long companyId, @PathVariable Long invoiceId) {
        return invoiceService.get(companyId, invoiceId);
    }

    @GetMapping("/next-number")
    public Map<String, String> nextNumber(@PathVariable Long companyId,
                                           @RequestParam(required = false) LocalDate issueDate) {
        LocalDate date = issueDate != null ? issueDate : LocalDate.now();
        return Map.of("invoiceNumber", invoiceService.suggestNextNumber(companyId, date));
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@PathVariable Long companyId, @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.create(companyId, request));
    }

    @PutMapping("/{invoiceId}")
    public InvoiceResponse update(@PathVariable Long companyId, @PathVariable Long invoiceId,
                                   @Valid @RequestBody InvoiceRequest request) {
        return invoiceService.update(companyId, invoiceId, request);
    }

    @PatchMapping("/{invoiceId}/status")
    public InvoiceResponse updateStatus(@PathVariable Long companyId, @PathVariable Long invoiceId,
                                         @RequestBody Map<String, String> body) {
        return invoiceService.updateStatus(companyId, invoiceId, InvoiceStatus.valueOf(body.get("status")));
    }

    @DeleteMapping("/{invoiceId}")
    public ResponseEntity<Void> delete(@PathVariable Long companyId, @PathVariable Long invoiceId) {
        invoiceService.delete(companyId, invoiceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{invoiceId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long companyId, @PathVariable Long invoiceId) {
        Invoice invoice = invoiceService.getOwned(companyId, invoiceId);
        byte[] pdf = invoicePdfService.render(invoice);
        return fileResponse(pdf, MediaType.APPLICATION_PDF, fileName(invoice, "pdf"));
    }

    @GetMapping("/{invoiceId}/xlsx")
    public ResponseEntity<byte[]> xlsx(@PathVariable Long companyId, @PathVariable Long invoiceId) {
        Invoice invoice = invoiceService.getOwned(companyId, invoiceId);
        byte[] xlsx = invoiceXlsxService.render(invoice);
        MediaType type = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return fileResponse(xlsx, type, fileName(invoice, "xlsx"));
    }

    private ResponseEntity<byte[]> fileResponse(byte[] content, MediaType type, String fileName) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(content);
    }

    private String fileName(Invoice invoice, String extension) {
        String safeNumber = invoice.getInvoiceNumber().replaceAll("[^a-zA-Z0-9]", "-");
        return "invoice-" + safeNumber + "." + extension;
    }
}
