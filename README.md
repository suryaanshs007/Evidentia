# Evidentia: Secure Digital Document Management System 

(Prototype)

Two services, run independently:

```
spring-boot-service/   System of record: auth, RBAC, document storage, audit logging (designed by Suryaansh)
django-service/        Reporting/oversight dashboard, reads the Spring Boot API only (designed by Parth)
```

See each service's own README for setup and details. This file only covers how they fit together.

## How the two services talk to each other

Communication is one directional. The Django dashboard calls the Spring Boot REST API over HTTP. Django has no direct database access to Spring Boot's data, and Spring Boot has no dependency on Django at all, it can run and be fully tested on its own.

```
Django views.py
      |
      v
Django dashboard/api_client.py  (single point of contact with Spring Boot)
      |
      v
Spring Boot REST API (localhost:8080)
      |
      v
PostgreSQL
```

## Contract between the two services

Django's `api_client.py` was originally built against a stubbed version of this contract before the real Spring Boot endpoints existed, so it could be developed in parallel. The two are now aligned on:

| Endpoint | Purpose | Access |
|---|---|---|
| `POST /api/auth/register` | Create a user | Open |
| `POST /api/auth/login` | Verify credentials, return a token | Open |
| `GET /api/documents` | List documents (role filtered) | Authenticated |
| `POST /api/documents` | Upload a document | Authenticated |
| `GET /api/documents/{id}` | View one document | Authenticated, owner or ADMIN |
| `GET /api/cases` | List case summaries (derived, not a real entity) | ADMIN |
| `GET /api/cases/{caseId}` | Single case detail | ADMIN |
| `GET /api/audit-log` | Full or filtered audit trail | ADMIN |

Field names in every response are chosen to match what `dashboard/api_client.py` and the Django templates already expect (`documentId`, `title`, `uploadedBy`, `auditId`, `performedBy`, `performedAt`, etc), not Spring Boot's internal naming, so no translation layer is needed on the Django side.

## Running both together locally

Terminal 1:
```
cd spring-boot-service
mvn spring-boot:run
```

Terminal 2:
```
cd django-service
source venv/bin/activate
export DJANGO_USE_STUB_API=false
export SPRING_BOOT_API_BASE_URL=http://localhost:8080
python manage.py runserver
```

Register at least one ADMIN user first (via `POST /api/auth/register`), since `/api/cases` and `/api/audit-log` will return 403 for an OFFICER.

## Known gaps, honestly listed

- No real `Case` entity, `title` and `status` on case summaries are placeholders
- No blockchain/hash-chain integrity layer yet (planned, not built)
- No AI document analysis layer yet (deferred to after the presentation round)
- No upload form in the Django dashboard, uploads are tested via the Spring Boot API directly
- The Django session stores the raw password for reuse as Basic Auth on later calls, acceptable for a localhost prototype, not something to carry into a real deployment
- `POST /api/auth/login`'s "token" is a Basic Auth string, not a real bearer token, a deliberate simplification given the timeline

## Implementation Tasks

### Spring Boot service
- [x] Core entities, RBAC, document upload/retrieval, audit logging
- [x] `/api/cases`, `/api/audit-log`, `/api/auth/login` to match the Django contract
- [ ] Hash-chain or on-chain (Ganache/Web3j) tamper-evidence layer
- [ ] Real `Case` entity if time allows

### Django service
- [x] Case overview, case detail, document list, audit log pages, built against stub data
- [x] Real login page, session-based auth, logout, per-session credentials used on every API call
- [x] Graceful handling when an OFFICER session hits an ADMIN-only endpoint (restricted notice instead of a crash)
- [ ] Switch to live Spring Boot data and verify all four pages render correctly
- [ ] Upload form calling `POST /api/documents`

### Later, after the presentation round
- [ ] LLM-based document analysis (summarization, detail extraction)
- [ ] Document version history
