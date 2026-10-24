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
- `ENCRYPTION_KEY` — encrypts stored third-party credentials (Odoo API keys) with
  AES-256-GCM. Use a long random value and **keep it stable**: changing it makes
  already-stored credentials unreadable (users would have to re-enter them).
- `ALLOW_PRIVATE_HOSTS`, `ALLOW_INSECURE_HTTP` — both default to `false`. Set to `true`
  **for local development only** so the server may call an Odoo on `localhost`/a LAN
  address over plain http (see "Time tracking imports" below).
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — Oracle JDBC URL and credentials,
  e.g. `jdbc:oracle:thin:@db:1521/FREEPDB1` (this is set automatically by
  `docker-compose.yml`)

## Time tracking imports (Odoo and similar platforms)

Each user can connect the platforms they log hours in and import those bookings.
The credentials live on the server (the Android app only configures them and triggers
imports), encrypted at rest and never returned by the API.

- A **connection** (`/api/integrations`) belongs to the user and says: which platform and
  server, which credentials, which of the user's **companies** the hours are booked to,
  optionally which **client** newly discovered projects belong to, whether to import only
  the user's own bookings, how many days back to import, and a default hourly rate.
- **Import** (`POST /api/integrations/sync`, or `/{id}/sync` for one connection) fetches the
  bookings and stores them as *time entries*. It is idempotent: re-importing never duplicates,
  edits in the source system update unbilled entries, bookings deleted there disappear here
  (unless already billed), and zero-hour placeholder lines are skipped. Projects are matched
  by name and created automatically if new.
- **Billing**: `GET /api/companies/{id}/time-entries/summary?clientId=&from=&to=` returns the
  unbilled hours per project as ready-made invoice lines. Saving an invoice with those lines'
  `timeEntryIds` marks the hours as billed, so they cannot be billed twice; editing or deleting
  the invoice releases/keeps them consistently. Billed entries are never changed by later imports.
- **Providers** implement `TimeTrackingProvider` (`integration/` package). Only **Odoo** ships
  today: timesheet lines (`account.analytic.line`) with three login methods (`apiMode`):
  **`SESSION`** — the user's ordinary Odoo login + password, via `/web/session/authenticate`
  (any recent version; accounts with two-factor auth need an API key instead);
  **`JSON2`** — API key as bearer token (Odoo 19+); **`JSONRPC`** — legacy `/jsonrpc`
  (login + password or API key). JSON-2 and JSON-RPC were verified against Odoo 19.0; `SESSION`
  is covered by tests against a fake Odoo server but not yet run against a real instance.

Because users supply the URLs the server calls, outbound requests are restricted: https only,
no credentials/query in the URL, no loopback/private/link-local/metadata addresses, redirects
not followed, short timeouts. (Known limit: the host name is resolved once for the check and again
by the HTTP client, so a DNS-rebinding attacker controlling a domain could still race it.)

To try it locally against an Odoo on your machine, start the stack with the relaxed flags and
point the connection at `http://host.docker.internal:8069`:

```bash
ALLOW_PRIVATE_HOSTS=true ALLOW_INSECURE_HTTP=true docker compose up --build
```

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
- `GET/POST/PUT/DELETE /api/integrations[/{id}]` — the signed-in user's time-tracking connections
- `POST /api/integrations/test[?connectionId=]` — test unsaved settings (a blank API key reuses the stored one)
- `POST /api/integrations/{id}/sync`, `POST /api/integrations/sync[?companyId=]` — import time bookings
- `GET /api/companies/{companyId}/time-entries[?from=&to=&invoiced=]`
- `GET /api/companies/{companyId}/time-entries/summary?clientId=&from=&to=` — unbilled hours per project
- `GET /api/meta/currencies`, `/api/meta/units`, `/api/meta/integration-providers` — dropdown data for the app

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
