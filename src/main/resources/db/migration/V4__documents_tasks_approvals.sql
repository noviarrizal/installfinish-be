-- Document metadata, operator tasks, and approvals for external actions.

-- Metadata only: binaries live in object storage (local filesystem in development, S3-compatible/R2 in production).
CREATE TABLE document (
    id                  uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    client_account_id   uuid          NOT NULL REFERENCES client_account (id),
    installation_job_id uuid,
    closeout_case_id    uuid,
    category            varchar(40)   NOT NULL,
    original_filename   varchar(255)  NOT NULL,
    content_type        varchar(150)  NOT NULL,
    size_bytes          bigint        NOT NULL,
    checksum_sha256     varchar(64)   NOT NULL,
    storage_provider    varchar(20)   NOT NULL,
    storage_key         varchar(1024) NOT NULL,
    access_policy       varchar(20)   NOT NULL DEFAULT 'INTERNAL_ONLY',
    -- Machine-extracted fields are untrusted draft data until a human confirms them.
    extracted_fields    jsonb,
    uploaded_by         varchar(255)  NOT NULL,
    archived_at         timestamptz,
    created_at          timestamptz   NOT NULL DEFAULT now(),
    updated_at          timestamptz   NOT NULL DEFAULT now(),
    version             bigint        NOT NULL DEFAULT 0,
    CONSTRAINT document_installation_job_fk FOREIGN KEY (client_account_id, installation_job_id)
        REFERENCES installation_job (client_account_id, id),
    CONSTRAINT document_closeout_case_fk FOREIGN KEY (client_account_id, closeout_case_id)
        REFERENCES closeout_case (client_account_id, id),
    CONSTRAINT document_owner_chk CHECK (installation_job_id IS NOT NULL OR closeout_case_id IS NOT NULL),
    CONSTRAINT document_size_chk CHECK (size_bytes > 0),
    CONSTRAINT document_checksum_chk CHECK (checksum_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT document_category_chk CHECK (category IN (
        'PERMIT_APPLICATION', 'PERMIT_RECEIPT', 'PERMIT_ISSUED', 'AUTHORITY_CORRESPONDENCE', 'INVOICE',
        'EQUIPMENT_LABEL', 'SITE_PHOTO', 'INSPECTION_RECORD', 'WARRANTY_CONFIRMATION', 'CUSTOMER_AUTHORIZATION',
        'CLOSEOUT_REPORT', 'OTHER')),
    CONSTRAINT document_storage_provider_chk CHECK (storage_provider IN ('LOCAL_FILESYSTEM', 'S3_COMPATIBLE')),
    CONSTRAINT document_access_policy_chk CHECK (access_policy IN ('INTERNAL_ONLY', 'CLIENT_VISIBLE')),
    CONSTRAINT document_storage_key_uk UNIQUE (storage_provider, storage_key),
    CONSTRAINT document_client_id_uk UNIQUE (client_account_id, id)
);

CREATE INDEX document_case_idx ON document (client_account_id, closeout_case_id);
CREATE INDEX document_job_idx ON document (client_account_id, installation_job_id);
CREATE INDEX document_checksum_idx ON document (client_account_id, checksum_sha256);

CREATE TABLE task (
    id                uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_account_id uuid         NOT NULL REFERENCES client_account (id),
    closeout_case_id  uuid         NOT NULL,
    title             varchar(300) NOT NULL,
    description       text,
    status            varchar(20)  NOT NULL DEFAULT 'OPEN',
    priority          varchar(10)  NOT NULL DEFAULT 'NORMAL',
    assignee          varchar(255),
    due_at            timestamptz,
    escalation_reason text,
    escalated_at      timestamptz,
    completed_at      timestamptz,
    created_by        varchar(255) NOT NULL,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    version           bigint       NOT NULL DEFAULT 0,
    CONSTRAINT task_closeout_case_fk FOREIGN KEY (client_account_id, closeout_case_id)
        REFERENCES closeout_case (client_account_id, id),
    CONSTRAINT task_status_chk CHECK (status IN ('OPEN', 'IN_PROGRESS', 'BLOCKED', 'DONE', 'CANCELLED')),
    CONSTRAINT task_priority_chk CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT task_completion_chk CHECK ((status = 'DONE') = (completed_at IS NOT NULL)),
    CONSTRAINT task_escalation_chk CHECK (escalated_at IS NULL OR escalation_reason IS NOT NULL)
);

CREATE INDEX task_case_idx ON task (client_account_id, closeout_case_id);
CREATE INDEX task_open_due_idx ON task (due_at) WHERE status IN ('OPEN', 'IN_PROGRESS', 'BLOCKED');
CREATE INDEX task_assignee_status_idx ON task (assignee, status) WHERE assignee IS NOT NULL;

-- A request for, and the recorded decision on, an external action that needs customer authorization.
-- payload_snapshot is exactly what was put in front of the approver; payload_sha256 is computed by the database.
CREATE TABLE approval (
    id                      uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_account_id       uuid         NOT NULL REFERENCES client_account (id),
    closeout_case_id        uuid         NOT NULL,
    action_type             varchar(40)  NOT NULL,
    status                  varchar(20)  NOT NULL DEFAULT 'PENDING',
    payload_snapshot        jsonb        NOT NULL,
    payload_sha256          varchar(64)  NOT NULL,
    requested_by            varchar(255) NOT NULL,
    requested_at            timestamptz  NOT NULL DEFAULT now(),
    approver_client_user_id uuid,
    approver_name           varchar(200),
    approver_email          varchar(320),
    decision_channel        varchar(20),
    decided_at              timestamptz,
    decision_comment        text,
    evidence_document_id    uuid,
    created_at              timestamptz  NOT NULL DEFAULT now(),
    updated_at              timestamptz  NOT NULL DEFAULT now(),
    version                 bigint       NOT NULL DEFAULT 0,
    CONSTRAINT approval_closeout_case_fk FOREIGN KEY (client_account_id, closeout_case_id)
        REFERENCES closeout_case (client_account_id, id),
    CONSTRAINT approval_approver_fk FOREIGN KEY (client_account_id, approver_client_user_id)
        REFERENCES client_user (client_account_id, id),
    CONSTRAINT approval_evidence_document_fk FOREIGN KEY (client_account_id, evidence_document_id)
        REFERENCES document (client_account_id, id),
    CONSTRAINT approval_action_type_chk CHECK (action_type IN (
        'PERMIT_SUBMISSION', 'GOVERNMENT_FEE_PAYMENT', 'WARRANTY_REGISTRATION', 'NO_PERMIT_REQUIRED',
        'CLOSEOUT_EXCEPTION')),
    CONSTRAINT approval_status_chk CHECK (status IN ('PENDING', 'APPROVED', 'DECLINED', 'WITHDRAWN')),
    CONSTRAINT approval_decision_channel_chk CHECK (decision_channel IN ('PORTAL', 'EMAIL', 'PHONE', 'IN_PERSON')),
    CONSTRAINT approval_payload_sha256_chk CHECK (payload_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT approval_decision_recorded_chk CHECK (
        status NOT IN ('APPROVED', 'DECLINED')
        OR (decided_at IS NOT NULL AND approver_name IS NOT NULL AND decision_channel IS NOT NULL)),
    CONSTRAINT approval_pending_undecided_chk CHECK (status <> 'PENDING' OR decided_at IS NULL)
);

CREATE INDEX approval_case_idx ON approval (client_account_id, closeout_case_id);
CREATE INDEX approval_pending_idx ON approval (requested_at) WHERE status = 'PENDING';

-- The hash always reflects the stored payload; the request side of an approval never changes; a decided or
-- withdrawn approval never changes; approvals are never deleted.
CREATE FUNCTION approval_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.payload_sha256 := encode(sha256(convert_to(NEW.payload_snapshot::text, 'UTF8')), 'hex');
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'approval % cannot be deleted', OLD.id USING ERRCODE = 'integrity_constraint_violation';
    END IF;

    IF OLD.status <> 'PENDING' THEN
        RAISE EXCEPTION 'approval % is % and cannot be modified', OLD.id, OLD.status
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;

    IF (NEW.client_account_id, NEW.closeout_case_id, NEW.action_type, NEW.payload_snapshot, NEW.payload_sha256,
        NEW.requested_by, NEW.requested_at)
        IS DISTINCT FROM
        (OLD.client_account_id, OLD.closeout_case_id, OLD.action_type, OLD.payload_snapshot, OLD.payload_sha256,
         OLD.requested_by, OLD.requested_at) THEN
        RAISE EXCEPTION 'the request side of approval % is immutable', OLD.id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER approval_guard
    BEFORE INSERT OR UPDATE OR DELETE ON approval
    FOR EACH ROW EXECUTE FUNCTION approval_guard();
