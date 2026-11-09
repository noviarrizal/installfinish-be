-- Permitting authorities and customer-approved, versioned playbooks.
-- InstallFinish does not determine permit requirements; a playbook records the checklist the client approved,
-- with its sources. Approved versions are frozen so historical cases keep the checklist they were processed with.

-- Shared reference data (not client-owned).
CREATE TABLE municipality (
    id             uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
    authority_name varchar(200)  NOT NULL,
    state_code     varchar(2)    NOT NULL,
    county         varchar(100),
    timezone       varchar(64)   NOT NULL,
    portal_url     varchar(2048),
    contact_email  varchar(320),
    contact_phone  varchar(30),
    contact_notes  text,
    active         boolean       NOT NULL DEFAULT true,
    created_at     timestamptz   NOT NULL DEFAULT now(),
    updated_at     timestamptz   NOT NULL DEFAULT now(),
    version        bigint        NOT NULL DEFAULT 0,
    CONSTRAINT municipality_state_code_chk CHECK (state_code ~ '^[A-Z]{2}$')
);

CREATE UNIQUE INDEX municipality_authority_uk
    ON municipality (state_code, coalesce(lower(county), ''), lower(authority_name));

CREATE TABLE playbook (
    id                uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    client_account_id uuid        NOT NULL REFERENCES client_account (id),
    municipality_id   uuid        NOT NULL REFERENCES municipality (id),
    job_type          varchar(50) NOT NULL,
    version_number    integer     NOT NULL,
    status            varchar(20) NOT NULL DEFAULT 'DRAFT',
    effective_date    date,
    approved_by       varchar(200),
    approved_at       timestamptz,
    -- [{ "title": ..., "url": ..., "retrievedOn": ... }] — the official sources the client approved.
    source_references jsonb       NOT NULL DEFAULT '[]'::jsonb,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    version           bigint      NOT NULL DEFAULT 0,
    CONSTRAINT playbook_version_number_chk CHECK (version_number > 0),
    CONSTRAINT playbook_status_chk CHECK (status IN ('DRAFT', 'APPROVED', 'RETIRED')),
    CONSTRAINT playbook_approval_recorded_chk CHECK (
        status = 'DRAFT' OR (approved_by IS NOT NULL AND approved_at IS NOT NULL AND effective_date IS NOT NULL)),
    CONSTRAINT playbook_source_references_chk CHECK (jsonb_typeof(source_references) = 'array'),
    CONSTRAINT playbook_version_uk UNIQUE (client_account_id, municipality_id, job_type, version_number),
    CONSTRAINT playbook_client_id_uk UNIQUE (client_account_id, id)
);

-- At most one approved (in-force) version per client, authority and job type.
CREATE UNIQUE INDEX playbook_single_approved_uk
    ON playbook (client_account_id, municipality_id, job_type)
    WHERE status = 'APPROVED';

CREATE INDEX playbook_municipality_idx ON playbook (municipality_id);

CREATE TABLE playbook_checklist_item (
    id                uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_account_id uuid         NOT NULL,
    playbook_id       uuid         NOT NULL,
    item_key          varchar(100) NOT NULL,
    label             varchar(300) NOT NULL,
    item_kind         varchar(20)  NOT NULL,
    required          boolean      NOT NULL DEFAULT true,
    owner_party       varchar(20)  NOT NULL,
    validation_rule   jsonb,
    source_reference  text,
    sort_order        integer      NOT NULL,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    version           bigint       NOT NULL DEFAULT 0,
    CONSTRAINT playbook_checklist_item_playbook_fk FOREIGN KEY (client_account_id, playbook_id)
        REFERENCES playbook (client_account_id, id) ON DELETE CASCADE,
    CONSTRAINT playbook_checklist_item_kind_chk CHECK (item_kind IN ('DATA_FIELD', 'DOCUMENT')),
    CONSTRAINT playbook_checklist_item_owner_chk CHECK (owner_party IN ('CUSTOMER', 'INSTALL_TEAM', 'INSTALLFINISH')),
    CONSTRAINT playbook_checklist_item_key_uk UNIQUE (playbook_id, item_key)
);

CREATE INDEX playbook_checklist_item_order_idx ON playbook_checklist_item (playbook_id, sort_order);

-- Approved playbooks are immutable apart from retirement; retired playbooks are immutable; only drafts may be deleted.
-- Errors use SQLSTATE 23000 so the application reports them as integrity violations.
CREATE FUNCTION playbook_guard_published() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.status <> 'DRAFT' THEN
            RAISE EXCEPTION 'playbook % is % and cannot be deleted', OLD.id, OLD.status
                USING ERRCODE = 'integrity_constraint_violation';
        END IF;
        RETURN OLD;
    END IF;

    IF OLD.status = 'DRAFT' THEN
        IF NEW.status = 'RETIRED' THEN
            RAISE EXCEPTION 'draft playbook % cannot be retired; delete it instead', OLD.id
                USING ERRCODE = 'integrity_constraint_violation';
        END IF;
        RETURN NEW;
    END IF;

    IF OLD.status = 'APPROVED' AND NEW.status = 'RETIRED'
        AND (NEW.client_account_id, NEW.municipality_id, NEW.job_type, NEW.version_number, NEW.effective_date,
             NEW.approved_by, NEW.approved_at, NEW.source_references)
            IS NOT DISTINCT FROM
            (OLD.client_account_id, OLD.municipality_id, OLD.job_type, OLD.version_number, OLD.effective_date,
             OLD.approved_by, OLD.approved_at, OLD.source_references) THEN
        RETURN NEW;
    END IF;

    RAISE EXCEPTION 'playbook % is % and cannot be modified; create a new version', OLD.id, OLD.status
        USING ERRCODE = 'integrity_constraint_violation';
END;
$$;

CREATE TRIGGER playbook_guard_published
    BEFORE UPDATE OR DELETE ON playbook
    FOR EACH ROW EXECUTE FUNCTION playbook_guard_published();

-- Checklist items can only change while their playbook is a draft.
-- A missing parent means the draft playbook itself is being deleted (cascade), which is allowed.
CREATE FUNCTION playbook_checklist_item_guard() RETURNS trigger
    LANGUAGE plpgsql AS
$$
DECLARE
    parent_status varchar(20);
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        SELECT status INTO parent_status FROM playbook WHERE id = OLD.playbook_id;
        IF parent_status IS NOT NULL AND parent_status <> 'DRAFT' THEN
            RAISE EXCEPTION 'checklist of playbook % is frozen (playbook is %)', OLD.playbook_id, parent_status
                USING ERRCODE = 'integrity_constraint_violation';
        END IF;
    END IF;

    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        SELECT status INTO parent_status FROM playbook WHERE id = NEW.playbook_id;
        IF parent_status IS NOT NULL AND parent_status <> 'DRAFT' THEN
            RAISE EXCEPTION 'checklist of playbook % is frozen (playbook is %)', NEW.playbook_id, parent_status
                USING ERRCODE = 'integrity_constraint_violation';
        END IF;
        RETURN NEW;
    END IF;

    RETURN OLD;
END;
$$;

CREATE TRIGGER playbook_checklist_item_guard
    BEFORE INSERT OR UPDATE OR DELETE ON playbook_checklist_item
    FOR EACH ROW EXECUTE FUNCTION playbook_checklist_item_guard();
