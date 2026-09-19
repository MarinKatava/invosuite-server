package com.invoisuite.server.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A business entity owned by a {@link User} that issues invoices.
 * A single user can own several companies (e.g. a sole proprietorship
 * and a separate consultancy), each with its own issuer details.
 */
@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(nullable = false)
    private String city;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String country;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "vat_id")
    private String vatId;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "iban")
    private String iban;

    @Column(name = "bic")
    private String bic;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_currency", nullable = false)
    private Currency defaultCurrency;

    @Column(name = "default_vat_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultVatPercent;

    @Column(name = "vat_exempt_text")
    private String vatExemptText;

    /** City/place printed on the invoice as the place of issue, e.g. "Zagreb, Croatia". */
    @Column(name = "invoicing_place")
    private String invoicingPlace;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (defaultVatPercent == null) {
            defaultVatPercent = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
