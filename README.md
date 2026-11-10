# InstallFinish Ops — Backend (`installfinish-be`)

Backend for **InstallFinish Ops**, the internal operations system InstallFinish staff use to deliver managed
residential HVAC installation closeout (permit packet preparation, approvals, submission tracking, inspection
coordination, warranty registration and closeout reporting) for US contractors.

This is not a public SaaS. Customers buy the closeout result; this service is how InstallFinish delivers it with
every step tracked and evidenced. Product scope and business rules are in
[`docs/product/raas_installfinish_launch_blueprint.md`](docs/product/raas_installfinish_launch_blueprint.md);
technical decisions are in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md). The operator UI lives in the separate
`installfinish-fe` (Next.js) repository and consumes this API's OpenAPI document.

## Technology stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 21 (LTS) |
| Framework | Spring Boot 4.1.2 — Spring Framework 7.0, Spring Security 7.1, Hibernate 7.4, Jackson 3.1 (Web MVC, Data JPA, Validation, Security, Actuator) |
| Build | Maven 3.9+ via the included wrapper (`./mvnw`) |
| Database | PostgreSQL 17 (Neon in hosted environments), Flyway 12 migrations |
| Auth | OAuth2 resource server — JWT bearer tokens from an OIDC identity provider |
| API docs | SpringDoc OpenAPI 3.2.0 (`/v3/api-docs`, Swagger UI locally) |
| Tests | JUnit 6, Mockito, Spring Test/MockMvc, Testcontainers 2 PostgreSQL |

## Prerequisites

- **JDK 21 or newer** (the build targets Java 21 via `--release 21`; the enforcer plugin rejects older JDKs). Java 21
  is the supported runtime; the build and tests are verified on Temurin 21.
- **Docker** (recommended) for the local database and for Testcontainers-based integration tests.
  Without Docker, use any disposable PostgreSQL 13+ instead (see *Running tests*).
- `bash` and `openssl` for `scripts/dev-jwt.sh` (Git Bash works on Windows).

## Environment variables

All configuration comes from environment variables; nothing secret is stored in the repository.
[`.env.example`](.env.example) lists every variable with placeholder/local values.

| Variable | Required in | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | all | `local` for development, `prod` for deployed environments (`test` is used by the test suite) |
| `DATABASE_URL` | prod | JDBC URL. Neon: `jdbc:postgresql://<endpoint>/<db>?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | prod | Database credentials |
| `FLYWAY_URL` | optional | Separate URL for migrations (e.g. Neon direct endpoint when `DATABASE_URL` uses the `-pooler` endpoint). Defaults to `DATABASE_URL` |
| `DATABASE_POOL_MAX_SIZE` | optional | Hikari pool size (default 5) |
| `JWT_ISSUER_URI` | prod | OIDC issuer of the identity provider; signing keys are discovered from it |
| `JWT_AUDIENCE` | prod | Required `aud` claim (e.g. `installfinish-api`) |
| `JWT_STAFF_ROLES_CLAIM` | optional | Claim carrying staff roles (default `installfinish_roles`) |
| `JWT_PUBLIC_KEY_LOCATION` | local | Dev token public key (default `file:.local/jwt/dev-public.pem`) |
| `CORS_ALLOWED_ORIGINS` | prod | Comma-separated exact origins of `installfinish-fe`; must be `https://` in prod |
| `API_DOCS_ENABLED` | optional | Prod only: serve `/v3/api-docs` to authenticated staff (default `false`) |
| `PORT` | optional | HTTP port (default 8080) |

The `local` profile also reads a git-ignored `.env` file from the working directory:
`cp .env.example .env`.

## Local setup

```bash
# 1. Start PostgreSQL 17 (data persists in a Docker volume)
docker compose up -d

# 2. Optional: local overrides
cp .env.example .env

# 3. Create the local token-signing key pair (written to .local/jwt/, git-ignored)
scripts/dev-jwt.sh keys

# 4. Run the application (migrations are applied on startup)
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

On Windows PowerShell use `$env:SPRING_PROFILES_ACTIVE="local"; ./mvnw spring-boot:run`, and run
`scripts/dev-jwt.sh` from Git Bash.

### Calling the API locally

Every endpoint except health requires a bearer token. Mint local tokens with:

```bash
ADMIN=$(scripts/dev-jwt.sh token admin-dev SYSTEM_ADMIN)
OPERATOR=$(scripts/dev-jwt.sh token operator-dev OPERATOR)

curl -s localhost:8080/api/v1/health

curl -s -X POST localhost:8080/api/v1/client-accounts \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"legalName":"Example HVAC LLC","displayName":"Example HVAC","timezone":"America/New_York"}'
```

Client-side users (approvers/viewers) get their access from the `client_user` table, not from token claims: link
one with `POST /api/v1/client-accounts/{id}/users` (`identitySubject` = the token `sub`), then use
`scripts/dev-jwt.sh token <that subject>` with no roles.

## Database and migrations

- Flyway SQL migrations in [`src/main/resources/db/migration`](src/main/resources/db/migration) are the only source
  of schema changes. They run automatically on startup; Hibernate only **validates** the schema (`ddl-auto=validate`).
- Add a change as a new file `V<next>__<description>.sql`. Never edit a migration that has been applied anywhere —
  Flyway validates checksums and will refuse to start. Prefer additive, non-destructive changes.
- `flyway clean` is disabled.
- Migrations currently run only as part of application startup; there is no separate migrate-only command yet. With
  more than one instance, Flyway's database lock serializes concurrent startups.

Neon specifics: use `sslmode=verify-full` with `sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory` (validates the
server certificate against the JVM trust store). If the app connects through Neon's pooled (`-pooler`) endpoint, set
`FLYWAY_URL` to the direct endpoint so migrations do not run through PgBouncer.

## Running the application

| Command | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run` | Run from source |
| `./mvnw package` | Build `target/installfinish-be-<version>.jar` (runs tests) |
| `java -jar target/installfinish-be-*.jar` | Run the packaged jar (set `SPRING_PROFILES_ACTIVE`) |

## Running tests

```bash
./mvnw test        # unit, web-slice and PostgreSQL integration tests
./mvnw verify      # full build
```

Integration tests (`*IntegrationTest`) run against real PostgreSQL, applying all migrations:

1. If `TEST_DATABASE_URL` is set, they use that database (plus `TEST_DATABASE_USERNAME` / `TEST_DATABASE_PASSWORD`).
   **It must be disposable** — tests insert data, and the audit table cannot be cleaned.
2. Otherwise, if Docker is available, Testcontainers starts `postgres:17-alpine`.
3. Otherwise they are **skipped** (shown as skipped in the Maven summary, never as passed).

Setting `INSTALLFINISH_REQUIRE_TESTCONTAINERS=true` allows only option 2: a set `TEST_DATABASE_URL` or a missing
Docker daemon then fails the run instead of being used or skipped.

### Continuous integration

[`.github/workflows/backend-ci.yml`](.github/workflows/backend-ci.yml) runs on pull requests to `main` and on pushes
to `main`, `chore/**`, `feature/**` and `fix/**`. It uses an Ubuntu 24.04 runner, Temurin 21 and a cached Maven
repository, and runs `./mvnw --batch-mode --no-transfer-progress clean verify` with
`INSTALLFINISH_REQUIRE_TESTCONTAINERS=true`, so integration tests always run on a Testcontainers `postgres:17-alpine`.
The job then fails if:

- the run does not report that Testcontainers was the PostgreSQL source;
- any test failed, errored or was skipped;
- fewer than `MIN_TEST_COUNT` tests ran;
- any of the integration or security compatibility test classes did not run.

It needs no secrets and only `contents: read`. Raise `MIN_TEST_COUNT` when you add tests.

## API documentation

- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html` (use **Authorize** with a dev token)

Both are public under the `local` profile. In `prod`, Swagger UI is off and `/v3/api-docs` is off unless
`API_DOCS_ENABLED=true`, in which case it requires a staff token. `installfinish-fe` should generate its client
types from the OpenAPI document rather than duplicating them by hand.

Conventions: base path `/api/v1`; JSON; timestamps are ISO-8601 UTC instants; calendar dates are ISO-8601 dates;
collections are paginated (`page`, `size` ≤ 100, `sort`) and return `{items, page, size, totalItems, totalPages}`;
errors are RFC 9457 `application/problem+json` with a stable `code`, a `requestId` (also in the `X-Request-Id`
header) and field-level `errors` for validation failures.

## Deployment notes

- Run with `SPRING_PROFILES_ACTIVE=prod`. The prod profile **refuses to start** if the database URL lacks TLS
  (`sslmode`), if any CORS origin is not an exact `https://` origin, or if API docs are public. It also requires
  `JWT_ISSUER_URI` and `JWT_AUDIENCE`.
- Logs are ECS-format JSON on stdout in prod, including `requestId` and `clientIp` from the MDC.
- Health: `GET /api/v1/health` (aggregated, includes the database; 503 when not UP),
  `GET /actuator/health/liveness` and `/actuator/health/readiness` for orchestrator probes. Other actuator endpoints
  require `SYSTEM_ADMIN`.
- The app expects to run behind a TLS-terminating proxy (`server.forward-headers-strategy=native` in prod); do not
  expose it directly to the internet.
- Recommended database hardening: run migrations with an owner role and the app with a role that cannot `ALTER`
  tables or disable triggers (see `docs/ARCHITECTURE.md`, *Auditability*).
