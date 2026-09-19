package com.invoisuite.server.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Free-text name/title chosen by the user, e.g. "August 2026 Invoice". */
    @Column(nullable = false)
    private String name;

    /** Human-editable invoice number/reference, e.g. "1/8/2026". */
    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "invoicing_place")
    private String invoicingPlace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(name = "vat_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatPercent;

    @Column(name = "vat_exempt_text")
    private String vatExemptText;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    /**
     * Issuer (company) details serialized to JSON at the time the invoice was
     * finalized/generated, so historical PDFs stay accurate even if the
     * company's profile is edited afterwards.
     */
    @Column(name = "issuer_snapshot", columnDefinition = "TEXT")
    private String issuerSnapshot;

    /** Same idea as {@link #issuerSnapshot}, but for the client/customer. */
    @Column(name = "client_snapshot", columnDefinition = "TEXT")
    private String clientSnapshot;

    @Column(name = "net_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal netTotal;

    @Column(name = "vat_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal vatAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    // EAGER is deliberate: invoices only ever carry a handful of line items,
    // every call site needs them, and with open-in-view disabled a LAZY
    // collection would throw outside the originating @Transactional method
    // (e.g. PDF/XLSX rendering, which runs after the service call returns).
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addItem(InvoiceItem item) {
        item.setInvoice(this);
        item.setSortOrder(items.size());
        items.add(item);
    }

    public void clearItems() {
        items.clear();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = InvoiceStatus.DRAFT;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
