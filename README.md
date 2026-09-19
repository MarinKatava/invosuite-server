# InvoiSuite Server

Spring Boot (Java 21) backend for InvoiSuite: multi-company invoicing for
freelancers and small businesses. Users sign in with email/password or
Google, create one or more companies, manage clients and projects, and
generate invoices as PDF or XLSX.

## Stack

- Java 21, Spring Boot 3.3 (Web, Data JPA, Security, Validation)
- Oracle Database (Oracle JDBC thin driver, `ojdbc11`)
- Liquibase migrations (`src/main/resources/db/changelog`)
- JWT auth (email/password) + Google Sign-In (ID token verification)
- PDF via openhtmltopdf, XLSX via Apache POI

## Running everything with Docker (recommended)

```bash
docker compose up --build
```

This starts two containers:

- `db` — [gvenzl/oracle-free](https://github.com/gvenzl/oci-oracle-free), a
  free, non-Oracle-account-gated Oracle Database Free 23ai image commonly
  used for local dev/CI (the official Oracle images require an Oracle
  Container Registry login, which is awkward for a plain `docker compose up`)
- `backend` — built from the `Dockerfile` in this directory, waits for the
  database's healthcheck before starting, then runs the Liquibase changelog
  automatically on boot

The API is then available at `http://localhost:8080`. First boot takes a
minute or two while Oracle initializes its data files; `docker compose logs -f db`
shows `DATABASE IS READY TO USE!` when it's done.

Override any of `ORACLE_PASSWORD`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
`JWT_EXPIRATION_MINUTES`, `GOOGLE_CLIENT_ID` via a `.env` file or exported
environment variables before running `docker compose up`.

## Running against an external Oracle instance (no Docker)

```bash
export DB_URL="jdbc:oracle:thin:@your-host:1521/YOUR_SERVICE"
export DB_USERNAME=invoisuite
export DB_PASSWORD=invoisuite
./mvnw spring-boot:run
```

## Running the test suite (no Oracle required)

```bash
./mvnw test
```

Tests run under the `test` classpath, which overrides the datasource to an
in-memory H2 instance in Oracle-compatibility mode (`src/test/resources/application.yml`).
The Liquibase changelog uses Liquibase's portable column types (`BIGINT`,
`VARCHAR`, `DECIMAL`, `BOOLEAN`, `CLOB`, ...), so the exact same changesets
that create the real Oracle schema also run cleanly against H2 — no
Oracle-only SQL, no separate test schema to maintain.

## Configuration

Set these environment variables in production:

- `JWT_SECRET` — a long random string (HMAC key for signing tokens)
- `JWT_EXPIRATION_MINUTES` — token lifetime (default 1440 = 24h)
- `GOOGLE_CLIENT_ID` — the OAuth 2.0 **Web** client ID from Google Cloud
  Console. The Android app requests an ID token using this same client ID
  (see the Android project's README), and the backend validates that the
  token's `aud` claim matches it.
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — Oracle JDBC URL and credentials,
  e.g. `jdbc:oracle:thin:@db:1521/FREEPDB1` (this is set automatically by
  `docker-compose.yml`)

## API overview

- `POST /api/auth/register`, `/login`, `/google` — returns `{ token, user }`
- `GET /api/auth/me`
- `GET/POST/PUT/DELETE /api/companies[/{id}]`
- `GET/POST/PUT/DELETE /api/companies/{companyId}/clients[/{id}]`
- `GET/POST/PUT/DELETE /api/companies/{companyId}/projects[/{id}]`
- `GET/POST/PUT/DELETE /api/companies/{companyId}/invoices[/{id}]`
- `GET /api/companies/{companyId}/invoices/next-number?issueDate=YYYY-MM-DD`
- `PATCH /api/companies/{companyId}/invoices/{id}/status` — body `{ "status": "SENT" }`
- `GET /api/companies/{companyId}/invoices/{id}/pdf`
- `GET /api/companies/{companyId}/invoices/{id}/xlsx`
- `GET /api/meta/currencies`, `/api/meta/units` — dropdown data for the app

All endpoints except `/api/auth/**` and `/api/meta/**` require
`Authorization: Bearer <token>`. Companies, clients, projects and invoices
are always scoped to the authenticated user; a user can never see another
user's data.

## Design notes

- An invoice snapshots the issuing company's and client's details as JSON
  at creation/edit time (`issuer_snapshot` / `client_snapshot` columns), so
  a previously generated PDF/XLSX stays accurate even if the company profile
  or client details are edited later.
- Google ID tokens are verified via Google's `tokeninfo` endpoint rather than
  a local JWKS check, trading one extra HTTPS round trip for not needing the
  full `google-api-client` dependency. Fine for this app's login volume.
