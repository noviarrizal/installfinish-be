package com.installfinish.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** An HVAC contractor that InstallFinish delivers closeout services to. Root of client data ownership. */
@Entity
@Table(name = "client_account")
public class ClientAccount {

    public enum Status { ACTIVE, SUSPENDED, TERMINATED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    @Column(name = "operating_area")
    private String operatingArea;

    @Column(name = "contract_tier")
    private String contractTier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ClientAccount() {
    }

    public ClientAccount(String legalName, String displayName, String timezone, String operatingArea,
                         String contractTier) {
        this.legalName = legalName;
        this.displayName = displayName;
        this.timezone = timezone;
        this.operatingArea = operatingArea;
        this.contractTier = contractTier;
        this.status = Status.ACTIVE;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getOperatingArea() {
        return operatingArea;
    }

    public String getContractTier() {
        return contractTier;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
