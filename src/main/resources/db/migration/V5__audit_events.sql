-- Append-only audit trail for meaningful actions (state changes, uploads, approvals, submissions, messages).

CREATE TABLE audit_event (
    id                uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at       timestamptz  NOT NULL DEFAULT now(),
    client_account_id uuid         REFERENCES client_account (id),
    actor_type        varchar(20)  NOT NULL,
    actor_subject     varchar(255),
    action            varchar(100) NOT NULL,
    entity_type       varchar(60)  NOT NULL,
    entity_id         uuid,
    -- { "field": { "before": ..., "after": ... } } or a creation snapshot. Never contains secrets.
    changes           jsonb,
    request_id        varchar(64),
    source_ip         varchar(45),
    source_system     varchar(50)  NOT NULL,
    CONSTRAINT audit_event_actor_type_chk CHECK (actor_type IN ('STAFF', 'CLIENT_USER', 'SYSTEM')),
    CONSTRAINT audit_event_actor_subject_chk CHECK (actor_type = 'SYSTEM' OR actor_subject IS NOT NULL)
);

CREATE INDEX audit_event_client_occurred_idx ON audit_event (client_account_id, occurred_at DESC);
CREATE INDEX audit_event_entity_idx ON audit_event (entity_type, entity_id, occurred_at DESC);

CREATE FUNCTION audit_event_reject_change() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'audit_event is append-only' USING ERRCODE = 'integrity_constraint_violation';
END;
$$;

CREATE TRIGGER audit_event_append_only
    BEFORE UPDATE OR DELETE ON audit_event
    FOR EACH ROW EXECUTE FUNCTION audit_event_reject_change();

CREATE TRIGGER audit_event_no_truncate
    BEFORE TRUNCATE ON audit_event
    FOR EACH STATEMENT EXECUTE FUNCTION audit_event_reject_change();
