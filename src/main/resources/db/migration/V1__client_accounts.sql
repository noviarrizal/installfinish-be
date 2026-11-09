-- Client accounts (HVAC contractors served by InstallFinish) and their customer-side users.
-- Conventions for all migrations:
--   * UUID primary keys (gen_random_uuid() is built in since PostgreSQL 13).
--   * timestamptz for instants, stored and compared in UTC.
--   * "version" is the JPA optimistic-locking counter on mutable rows.
--   * Every client-owned table carries client_account_id. Parents expose UNIQUE (client_account_id, id) so children
--     reference them through composite foreign keys, which makes cross-client references impossible.

CREATE TABLE client_account (
    id             uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name     varchar(200) NOT NULL,
    display_name   varchar(200) NOT NULL,
    timezone       varchar(64)  NOT NULL,
    operating_area varchar(500),
    contract_tier  varchar(50),
    status         varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    version        bigint       NOT NULL DEFAULT 0,
    CONSTRAINT client_account_status_chk CHECK (status IN ('ACTIVE', 'SUSPENDED', 'TERMINATED'))
);

CREATE INDEX client_account_status_idx ON client_account (status);

-- Customer-side users. identity_subject is the "sub" claim issued by the configured identity provider.
-- Client membership is resolved from this table only; tokens never assert which client a user belongs to.
CREATE TABLE client_user (
    id                uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_account_id uuid         NOT NULL REFERENCES client_account (id),
    identity_subject  varchar(255) NOT NULL,
    email             varchar(320) NOT NULL,
    full_name         varchar(200) NOT NULL,
    role              varchar(30)  NOT NULL,
    active            boolean      NOT NULL DEFAULT true,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    version           bigint       NOT NULL DEFAULT 0,
    CONSTRAINT client_user_role_chk CHECK (role IN ('CUSTOMER_APPROVER', 'CUSTOMER_VIEWER')),
    CONSTRAINT client_user_identity_subject_uk UNIQUE (identity_subject),
    CONSTRAINT client_user_client_id_uk UNIQUE (client_account_id, id)
);

CREATE UNIQUE INDEX client_user_client_email_uk ON client_user (client_account_id, lower(email));
