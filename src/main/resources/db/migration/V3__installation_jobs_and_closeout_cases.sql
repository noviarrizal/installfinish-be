-- Job sites, installation jobs received from clients, and the closeout case InstallFinish runs for each job.

CREATE TABLE service_location (
    id                uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_account_id uuid         NOT NULL REFERENCES client_account (id),
    address_line1     varchar(200) NOT NULL,
    address_line2     varchar(200),
    city              varchar(100) NOT NULL,
    state_code        varchar(2)   NOT NULL,
    postal_code       varchar(10)  NOT NULL,
    county            varchar(100),
    -- Normalized jurisdiction; set by an operator, never inferred automatically.
    municipality_id   uuid         REFERENCES municipality (id),
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    version           bigint       NOT NULL DEFAULT 0,
    CONSTRAINT service_location_state_code_chk CHECK (state_code ~ '^[A-Z]{2}$'),
    CONSTRAINT service_location_postal_code_chk CHECK (postal_code ~ '^[0-9]{5}(-[0-9]{4})?$'),
    CONSTRAINT service_location_client_id_uk UNIQUE (client_account_id, id)
);

CREATE INDEX service_location_client_idx ON service_location (client_account_id);
CREATE INDEX service_location_municipality_idx ON service_location (municipality_id);

CREATE TABLE installation_job (
    id                  uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_account_id   uuid         NOT NULL REFERENCES client_account (id),
    service_location_id uuid         NOT NULL,
    -- The client's own job identifier (CRM/FSM job number), when one exists.
    external_job_id     varchar(100),
    job_type            varchar(50)  NOT NULL,
    intake_channel      varchar(20)  NOT NULL,
    source_system       varchar(50),
    homeowner_name      varchar(200),
    homeowner_email     varchar(320),
    homeowner_phone     varchar(30),
    sold_date           date,
    installed_date      date,
    contractor_team     varchar(200),
    -- [{ "equipmentType", "manufacturer", "modelNumber", "serialNumber" }]. Serial numbers are sensitive.
    equipment           jsonb        NOT NULL DEFAULT '[]'::jsonb,
    created_by          varchar(255) NOT NULL,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    updated_at          timestamptz  NOT NULL DEFAULT now(),
    version             bigint       NOT NULL DEFAULT 0,
    CONSTRAINT installation_job_service_location_fk FOREIGN KEY (client_account_id, service_location_id)
        REFERENCES service_location (client_account_id, id),
    CONSTRAINT installation_job_intake_channel_chk CHECK (intake_channel IN ('MANUAL', 'CSV_IMPORT', 'EMAIL')),
    CONSTRAINT installation_job_equipment_chk CHECK (jsonb_typeof(equipment) = 'array'),
    CONSTRAINT installation_job_external_job_id_uk UNIQUE (client_account_id, external_job_id),
    CONSTRAINT installation_job_client_id_uk UNIQUE (client_account_id, id)
);

CREATE INDEX installation_job_client_created_idx ON installation_job (client_account_id, created_at DESC);
CREATE INDEX installation_job_service_location_idx ON installation_job (service_location_id);

-- Human-readable case references: IF-100001, IF-100002, ...
CREATE SEQUENCE closeout_case_reference_seq START WITH 100001;

CREATE TABLE closeout_case (
    id                  uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_account_id   uuid         NOT NULL REFERENCES client_account (id),
    installation_job_id uuid         NOT NULL,
    case_reference      varchar(30)  NOT NULL DEFAULT ('IF-' || nextval('closeout_case_reference_seq')),
    status              varchar(40)  NOT NULL DEFAULT 'RECEIVED',
    status_reason       text,
    status_changed_at   timestamptz  NOT NULL DEFAULT now(),
    priority            varchar(10)  NOT NULL DEFAULT 'NORMAL',
    -- The playbook version this case is processed against; later playbook versions never change it.
    playbook_id         uuid,
    assigned_operator   varchar(255),
    due_at              timestamptz,
    closed_at           timestamptz,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    updated_at          timestamptz  NOT NULL DEFAULT now(),
    version             bigint       NOT NULL DEFAULT 0,
    CONSTRAINT closeout_case_installation_job_fk FOREIGN KEY (client_account_id, installation_job_id)
        REFERENCES installation_job (client_account_id, id),
    CONSTRAINT closeout_case_playbook_fk FOREIGN KEY (client_account_id, playbook_id)
        REFERENCES playbook (client_account_id, id),
    CONSTRAINT closeout_case_status_chk CHECK (status IN (
        'RECEIVED', 'DATA_INCOMPLETE', 'READY_FOR_REVIEW', 'CUSTOMER_APPROVAL_PENDING', 'APPROVED_TO_SUBMIT',
        'SUBMITTED', 'AUTHORITY_REVIEW', 'CORRECTION_REQUESTED', 'PERMIT_ISSUED', 'NO_PERMIT_REQUIRED',
        'FIELD_READY', 'INSPECTION_SCHEDULED', 'INSPECTION_RESULT_RECORDED', 'INSPECTION_FAILED',
        'WARRANTY_PENDING', 'WARRANTY_REGISTERED', 'ON_HOLD_CUSTOMER', 'CLOSED', 'CLOSED_WITH_EXCEPTION',
        'CANCELLED')),
    CONSTRAINT closeout_case_priority_chk CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT closeout_case_installation_job_uk UNIQUE (installation_job_id),
    CONSTRAINT closeout_case_reference_uk UNIQUE (case_reference),
    CONSTRAINT closeout_case_client_id_uk UNIQUE (client_account_id, id)
);

CREATE INDEX closeout_case_status_due_idx ON closeout_case (status, due_at);
CREATE INDEX closeout_case_client_status_idx ON closeout_case (client_account_id, status);
CREATE INDEX closeout_case_assigned_operator_idx ON closeout_case (assigned_operator) WHERE assigned_operator IS NOT NULL;
CREATE INDEX closeout_case_playbook_idx ON closeout_case (playbook_id) WHERE playbook_id IS NOT NULL;
