package com.installfinish.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Append-only audit record. Immutable in Hibernate and protected by triggers in the database. */
@Entity
@Immutable
@Table(name = "audit_event")
public class AuditEvent {

    public enum ActorType { STAFF, CLIENT_USER, SYSTEM }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "client_account_id")
    private UUID clientAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    private ActorType actorType;

    @Column(name = "actor_subject")
    private String actorSubject;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes")
    private Map<String, Object> changes;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "source_system", nullable = false)
    private String sourceSystem;

    protected AuditEvent() {
    }

    AuditEvent(UUID clientAccountId, ActorType actorType, String actorSubject, AuditAction action, UUID entityId,
               Map<String, Object> changes, String requestId, String sourceIp, String sourceSystem) {
        this.clientAccountId = clientAccountId;
        this.actorType = actorType;
        this.actorSubject = actorSubject;
        this.action = action.code();
        this.entityType = action.entityType();
        this.entityId = entityId;
        this.changes = changes;
        this.requestId = requestId;
        this.sourceIp = sourceIp;
        this.sourceSystem = sourceSystem;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getClientAccountId() {
        return clientAccountId;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public String getActorSubject() {
        return actorSubject;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public Map<String, Object> getChanges() {
        return changes;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }
}
