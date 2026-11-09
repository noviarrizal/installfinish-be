# InstallFinish BE — Architecture and Technical Decisions

Status: initial foundation (Sprint 1 groundwork). Product source of truth:
[`product/raas_installfinish_launch_blueprint.md`](product/raas_installfinish_launch_blueprint.md).

## 1. Shape of the system

A **modular monolith**: one Spring Boot application and one PostgreSQL database, organised by business feature.
The software is the delivery engine for a managed service (blueprint §3.1), so the priorities are correctness,
auditability and client data isolation, not multi-tenant SaaS scale.

```
com.installfinish
├── common        cross-cutting: error model (ApiProblems, ApiExceptionHandler), pagination, request-id filter,
│                 validation, health endpoint
├── config        OpenAPI, clock, production safety checks
├── security      roles, Actor, JWT → Actor conversion, filter chain, CORS
├── audit         append-only audit events (AuditService, AuditAction)
├── client        client accounts and client-side users
├── job           installation jobs and service locations (job intake)
└── closeout      closeout cases (the central workflow record)
```

Planned packages (`municipality`/`playbook`, `document`, `task`, `approval`, `permit`, `inspection`, `warranty`,
`reporting`) are created when their features are implemented; their tables already exist where the blueprint lists
them as initial. `case` is a Java keyword, so the case feature lives in `closeout`.

Within a feature: controller (HTTP only, `@PreAuthorize`) → service (business rules, transactions, client scoping,
audit) → Spring Data repository → JPA entity. Requests/responses are records; entities never leave the service
layer. No interfaces exist without a second implementation.

**Dependency direction** is one-way: `job → closeout → audit`, `job → client`, `security → client`. The closeout
case refers to its job by id only, so the case module does not depend on the job module.

## 2. Key decisions

### 2.1 Spring Boot 3.5 (not 4.x)
The brief mandates Spring Boot 3. 3.5.16 is the newest 3.x release. Note that the 3.5 line's open-source support
window ended in mid-2026; plan a Boot 4 migration (Spring Framework 7, Jakarta EE 11, Hibernate 7, Jackson 3) as a
dedicated piece of work rather than mixing it into feature work.

### 2.2 Authentication: provider-agnostic OAuth2 resource server — **IdP choice still open**
The blueprint does not name an identity provider, so the backend validates standard OIDC JWTs (`issuer-uri` +
audience) and does not depend on any vendor SDK. Candidate providers (Auth0, Microsoft Entra ID, AWS Cognito,
Keycloak, Clerk, WorkOS) all fit if they can:
- put staff roles in a custom claim (`installfinish_roles` by default),
- enforce MFA for staff (blueprint §3.8),
- issue tokens with a dedicated API audience to the Next.js app.

**Decision needed:** pick the IdP and agree the role-claim mapping with `installfinish-fe`. Until then, local
development uses a throwaway RSA key (`scripts/dev-jwt.sh`) and production refuses to start without
`JWT_ISSUER_URI`/`JWT_AUDIENCE`. The current design assumes a **single issuer** (`client_user.identity_subject` is
unique without an issuer column).

### 2.3 Authorization model
Roles follow blueprint §2.6:

| Role | Source | Can do now |
|---|---|---|
| `SYSTEM_ADMIN` | token claim | onboard client accounts, link client users, actuator |
| `OPERATOR` | token claim | receive jobs; read jobs, cases, client accounts |
| `QUALITY_REVIEWER` | token claim | read jobs, cases, client accounts |
| `CUSTOMER_APPROVER` | `client_user` row | read own account, jobs, cases |
| `CUSTOMER_VIEWER` | `client_user` row | read own account, jobs, cases |

- **Deny by default.** Public: `GET /api/v1/health`, actuator health probes, and API docs only where configured.
  `/api/**` requires a recognised role; anything else is denied. Each endpoint narrows further with `@PreAuthorize`.
  `SYSTEM_ADMIN` deliberately does not operate cases (least privilege); pilot staff can hold several roles.
- **Client membership never comes from the token or the request.** If the token subject is linked to a
  `client_user`, the caller is that client user, their role and account come from the database, and token role
  claims are ignored. Inactive users and users of non-`ACTIVE` accounts get no roles (403 on everything).
- Staff currently have access to all client accounts. **Gap:** per-staff client assignment, if required by customer
  contracts, is a follow-up.

### 2.4 Client data isolation (defence in depth)
1. **Service layer:** every service method takes the `Actor`. Reads by id use `findByIdAndClientAccountId` for
   client users; collection queries use `Actor.resolveClientFilter`, which pins client users to their own account
   and rejects requests for another account (403). Records outside scope are reported as **404**, so the API never
   reveals whether another client's record exists.
2. **Schema:** every client-owned table has `client_account_id`, and parents expose `UNIQUE (client_account_id, id)`.
   Children reference parents through **composite foreign keys** (e.g. `closeout_case (client_account_id,
   installation_job_id) → installation_job (client_account_id, id)`), so the database rejects any row linking
   records of different clients, whatever the application does.
3. **Tests:** `TenantIsolationIntegrationTest` exercises this end to end with real tokens and PostgreSQL;
   `DatabaseSchemaIntegrationTest` proves the composite keys reject cross-client rows.

PostgreSQL row-level security was considered and deferred: with an internal staff-operated system and the two
layers above, RLS would add per-request session state and pooling complexity for limited extra benefit now.

### 2.5 Auditability
- `audit_event` is **append-only**: Hibernate maps it `@Immutable`, the repository exposes no delete, and database
  triggers reject `UPDATE`, `DELETE` and `TRUNCATE`.
- `AuditService.record` requires an existing transaction (`Propagation.MANDATORY`), so an audit event commits or
  rolls back with the change it describes.
- Events carry actor, action code, entity, a change snapshot, the request id and the client IP.
- **Audit snapshots exclude homeowner contact details and equipment serial numbers.** Audit rows are never deleted,
  but the blueprint requires a correction/deletion process for personal data; keeping PII out of the audit trail
  keeps both obligations compatible.
- Approvals are evidence-grade: the database computes `payload_sha256` from the stored payload, the request side
  is immutable, and a decided approval can never change or be deleted.
- Approved playbooks are frozen by trigger (only `APPROVED → RETIRED` is allowed), and checklist items of a non-draft
  playbook cannot change, so a case processed against playbook version N keeps that checklist forever.
- Hardening for production (not yet automated): run Flyway as the schema owner and the application as a separate
  role without `ALTER`/`TRIGGER` privileges, so triggers cannot be disabled by the application credentials.

### 2.6 Workflow statuses and evidence
`closeout_case.status` holds the full lifecycle from blueprint §2.7, enforced by a CHECK constraint. In this phase
every case is opened as `RECEIVED`; **transitions are not implemented yet**. When they are, the rules are:
transitions are explicit (no generic "set status" endpoint), each needs a reason, and statuses asserting external
facts (`SUBMITTED`, `PERMIT_ISSUED`, `WARRANTY_REGISTERED`, …) require recorded evidence. `NO_PERMIT_REQUIRED`
requires a client-approved playbook or written confirmation, never an inference. All mutable workflow rows have a
`version` column for optimistic locking; conflicts return `409 CONCURRENT_MODIFICATION`.

### 2.7 Data model notes
- UUID primary keys (generated by the application; `gen_random_uuid()` defaults for manual inserts).
- `timestamptz` everywhere; JDBC, Hibernate and Jackson are pinned to UTC. Authority/client time zones are stored as
  IANA ids for later scheduling logic.
- Enumerations are `varchar` + CHECK constraints (simple to extend with an additive migration), mapped as
  `EnumType.STRING`.
- Job equipment is a JSON array on `installation_job` for now; it should become a table when warranty registration
  needs per-unit records.
- `document` stores metadata only (storage provider + key, checksum, size, category, access policy). Binaries go to
  object storage; `extracted_fields` is reserved for AI extraction output and must be treated as untrusted draft data.
- Tables from the blueprint not yet created: `permit_application`, `inspection`, `warranty_registration`,
  `communication`, `billable_event`, `integration_connection`, import batches. Each arrives with its feature.

### 2.8 API conventions
`/api/v1`, JSON, DTO records, Bean Validation, `fail-on-unknown-properties` (typos in client payloads fail loudly),
pagination via `PageResponse` (max page size 100), RFC 9457 problem details with a stable `code` and `requestId`,
`201 Created` + `Location` for creations. The OpenAPI document is generated from the code and is the contract for
`installfinish-fe`. Not yet implemented: idempotency keys for retryable writes (blueprint §3.5). They are required
before CSV import and before any integration webhooks.

### 2.9 Configuration and secrets
Profiles: `local` (compose PostgreSQL, dev JWT key, public docs, optional `.env`), `test` (Testcontainers or
`TEST_DATABASE_URL`, per-run in-memory JWT key), `prod` (all secrets from the environment; startup fails on
non-TLS database URLs, wildcard or `http://` CORS origins, or public docs). No credentials are committed;
`.gitignore` excludes `.env`, `.local/` and key files.

### 2.10 File storage
Not implemented in this phase. The document feature will introduce a small `DocumentStorage` abstraction with a
local-filesystem implementation for development and an S3-compatible (Cloudflare R2) implementation for production,
serving downloads through short-lived signed URLs. Provider configuration stays in `config`/properties, not in
business logic.

### 2.11 Observability
A request id (`X-Request-Id`, reused if well-formed, else generated) is put in the MDC, response headers, error
bodies and audit events. Production logs are ECS JSON. Sentry (blueprint §3.2) is not wired yet.

## 3. Out of scope for this foundation
AI extraction/LLM features, n8n, email automation, municipal portal and CRM integrations, payments, public
registration, subscription billing and reporting dashboards. These belong to later phases (blueprint §3.10).
