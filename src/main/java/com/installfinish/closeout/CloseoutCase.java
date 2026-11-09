package com.installfinish.closeout;

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
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * The closeout work InstallFinish performs for one installation job. The job is referenced by id only, keeping the
 * closeout module independent of the job module.
 */
@Entity
@Table(name = "closeout_case")
public class CloseoutCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_account_id", nullable = false, updatable = false)
    private UUID clientAccountId;

    @Column(name = "installation_job_id", nullable = false, updatable = false)
    private UUID installationJobId;

    /** Assigned by the database sequence (IF-100001, ...). */
    @Generated
    @Column(name = "case_reference", insertable = false, updatable = false)
    private String caseReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CloseoutCaseStatus status;

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private CasePriority priority;

    @Column(name = "playbook_id")
    private UUID playbookId;

    @Column(name = "assigned_operator")
    private String assignedOperator;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CloseoutCase() {
    }

    static CloseoutCase received(UUID clientAccountId, UUID installationJobId, Instant now) {
        CloseoutCase closeoutCase = new CloseoutCase();
        closeoutCase.clientAccountId = clientAccountId;
        closeoutCase.installationJobId = installationJobId;
        closeoutCase.status = CloseoutCaseStatus.RECEIVED;
        closeoutCase.statusChangedAt = now;
        closeoutCase.priority = CasePriority.NORMAL;
        return closeoutCase;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientAccountId() {
        return clientAccountId;
    }

    public UUID getInstallationJobId() {
        return installationJobId;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public CloseoutCaseStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public CasePriority getPriority() {
        return priority;
    }

    public UUID getPlaybookId() {
        return playbookId;
    }

    public String getAssignedOperator() {
        return assignedOperator;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getClosedAt() {
        return closedAt;
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
