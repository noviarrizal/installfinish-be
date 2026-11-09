package com.installfinish.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** An HVAC installation job a client handed to InstallFinish for administrative closeout. */
@Entity
@Table(name = "installation_job")
public class InstallationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_account_id", nullable = false, updatable = false)
    private UUID clientAccountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_location_id", nullable = false)
    private ServiceLocation serviceLocation;

    @Column(name = "external_job_id")
    private String externalJobId;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "intake_channel", nullable = false, updatable = false)
    private IntakeChannel intakeChannel;

    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "homeowner_name")
    private String homeownerName;

    @Column(name = "homeowner_email")
    private String homeownerEmail;

    @Column(name = "homeowner_phone")
    private String homeownerPhone;

    @Column(name = "sold_date")
    private LocalDate soldDate;

    @Column(name = "installed_date")
    private LocalDate installedDate;

    @Column(name = "contractor_team")
    private String contractorTeam;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "equipment", nullable = false)
    private List<EquipmentItem> equipment;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected InstallationJob() {
    }

    InstallationJob(UUID clientAccountId, ServiceLocation serviceLocation, String externalJobId, String jobType,
                    IntakeChannel intakeChannel, String sourceSystem, String createdBy) {
        if (!serviceLocation.getClientAccountId().equals(clientAccountId)) {
            throw new IllegalArgumentException("Service location belongs to a different client account");
        }
        this.clientAccountId = clientAccountId;
        this.serviceLocation = serviceLocation;
        this.externalJobId = externalJobId;
        this.jobType = jobType;
        this.intakeChannel = intakeChannel;
        this.sourceSystem = sourceSystem;
        this.createdBy = createdBy;
        this.equipment = List.of();
    }

    void recordHomeowner(String name, String email, String phone) {
        this.homeownerName = name;
        this.homeownerEmail = email;
        this.homeownerPhone = phone;
    }

    void recordInstallation(LocalDate soldDate, LocalDate installedDate, String contractorTeam,
                            List<EquipmentItem> equipment) {
        this.soldDate = soldDate;
        this.installedDate = installedDate;
        this.contractorTeam = contractorTeam;
        this.equipment = List.copyOf(equipment);
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientAccountId() {
        return clientAccountId;
    }

    public ServiceLocation getServiceLocation() {
        return serviceLocation;
    }

    public String getExternalJobId() {
        return externalJobId;
    }

    public String getJobType() {
        return jobType;
    }

    public IntakeChannel getIntakeChannel() {
        return intakeChannel;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getHomeownerName() {
        return homeownerName;
    }

    public String getHomeownerEmail() {
        return homeownerEmail;
    }

    public String getHomeownerPhone() {
        return homeownerPhone;
    }

    public LocalDate getSoldDate() {
        return soldDate;
    }

    public LocalDate getInstalledDate() {
        return installedDate;
    }

    public String getContractorTeam() {
        return contractorTeam;
    }

    public List<EquipmentItem> getEquipment() {
        return equipment;
    }

    public String getCreatedBy() {
        return createdBy;
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
