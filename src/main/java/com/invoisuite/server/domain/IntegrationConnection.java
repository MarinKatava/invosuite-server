package com.invoisuite.server.domain;

import com.invoisuite.server.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A user's link to an external time-tracking system (e.g. a client's Odoo
 * instance). Imported hours are booked against {@link #company}; projects
 * created during import are assigned to {@link #client} when one is set.
 */
@Entity
@Table(name = "integration_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationProvider provider;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "db_name")
    private String dbName;

    @Column(nullable = false)
    private String username;

    /** API key (or password); encrypted at rest, never returned by the API. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "api_key_encrypted", nullable = false, length = 1000)
    private String apiKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "api_mode", nullable = false)
    private OdooApiMode apiMode;

    /** Only import bookings made by the connected user, not the whole team's. */
    @Column(name = "only_mine", nullable = false)
    private boolean onlyMine;

    @Column(name = "lookback_days", nullable = false)
    private int lookbackDays;

    @Column(name = "default_hourly_rate", precision = 12, scale = 2)
    private BigDecimal defaultHourlyRate;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "last_sync_status")
    private String lastSyncStatus;

    @Column(name = "last_sync_message", length = 1000)
    private String lastSyncMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
