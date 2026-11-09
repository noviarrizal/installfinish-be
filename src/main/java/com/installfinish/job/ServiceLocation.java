package com.installfinish.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** The job site address. Contains homeowner property data, so it is client-scoped like every other record. */
@Entity
@Table(name = "service_location")
public class ServiceLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_account_id", nullable = false, updatable = false)
    private UUID clientAccountId;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state_code", nullable = false)
    private String stateCode;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "county")
    private String county;

    /** Normalized jurisdiction, assigned by an operator. */
    @Column(name = "municipality_id")
    private UUID municipalityId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ServiceLocation() {
    }

    ServiceLocation(UUID clientAccountId, String addressLine1, String addressLine2, String city, String stateCode,
                    String postalCode, String county) {
        this.clientAccountId = clientAccountId;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.stateCode = stateCode;
        this.postalCode = postalCode;
        this.county = county;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientAccountId() {
        return clientAccountId;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getCity() {
        return city;
    }

    public String getStateCode() {
        return stateCode;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCounty() {
        return county;
    }

    public UUID getMunicipalityId() {
        return municipalityId;
    }
}
