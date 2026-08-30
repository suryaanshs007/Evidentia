# Evidentia: Secure Digital Document Management System (26190)

(Prototype)

Two services, run independently: 

See each service's own README for setup and details. This file covers the problem being solved, how the two services fit together, and how to build and run the whole thing.

## Problem Statement

**PS ID:** SIH26190
**Title:** Secure Digital Document Management System for Legal and Investigation Documents
**Organization:** Ministry of Home Affairs
**Department:** National Crime Records Bureau (NCRB), Women Safety Division
**Category:** Software
**Theme:** Blockchain & Cybersecurity

Law enforcement agencies, courts, legal departments, and investigative organizations handle large volumes of sensitive documents across a case lifecycle, including FIRs, investigation records, witness statements, charge sheets, court filings, evidence records, and forensic reports. Many organizations still rely on paper based or fragmented digital storage, which causes slow document retrieval, unauthorized access risk, tampering risk, no version control, poor inter department collaboration, and weak auditability.

## Proposed Solution

We are building a Secure Digital Document Management System (DMS) that lets authorized personnel securely store, organize, retrieve, and share legal and investigation documents tied to a case.

Core capabilities for the prototype:

- Role based access control, so only authorized roles can view or act on a given document
- Centralized document upload and storage with case level metadata
- A full audit trail that records who viewed, uploaded, or modified a document, and when
- Search and filter of documents by case ID, document type, and date

How it addresses the problem:

- Replaces fragmented, paper based storage with a single system of record per case
- Enforces access control at the API layer instead of relying on manual, physical restriction
- Makes every document interaction traceable, which directly targets the auditability gap named in the problem statement
- Lays the groundwork for tamper evidence and version history as the system matures beyond the prototype

Innovation and uniqueness:

- The audit log is a first class part of the data model, not an afterthought bolted on later
- The architecture separates the system of record from the reporting and oversight layer, so investigative staff and oversight/admin staff interact with purpose built interfaces instead of one generic dashboard

## Planned: local Ethereum integration for tamper evidence (possible future inclusion)

The official theme includes Blockchain, and the current prototype has no blockchain involvement yet, this is the next planned layer, not yet built.

The plan, inspired by the B-CoC (Blockchain-based Chain of Custody) research architecture (Bonomi et al.), scaled down for a single-department deployment instead of a multi-institution validator network:

- Run **Ganache**, a local single-node Ethereum simulator, no real network or gas cost involved
- Write a small **Solidity** smart contract that stores a document's hash, case ID, and timestamp on-chain, with a `getDocumentRecord` view function to read it back
- Use **Web3j** from the Spring Boot service to deploy the contract and call it whenever a document is uploaded
- Add a verification endpoint that recomputes a document's hash on demand and compares it against the on-chain value, so tampering is detectable, not just logged after the fact

This is a deliberate simplification of full blockchain (single node, no consensus protocol) chosen because a distributed multi-validator network is unrealistic to deploy or demo within the prototype's timeline, and because this problem statement is scoped to a single department (NCRB Women Safety Division) rather than the multi-institution setting the original research assumes.

## How the two services talk to each other

Communication is one directional. The Django dashboard calls the Spring Boot REST API over HTTP. Django has no direct database access to Spring Boot's data, and Spring Boot has no dependency on Django at all, it can run and be fully tested on its own. 


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

## How to build

### 1. Spring Boot service (system of record)

**Requirements:** Java 17, Maven, PostgreSQL running locally.

1. Create the database:
```bash
   createdb dms_db
```
   or from inside `psql`:
```sql
   CREATE DATABASE dms_db;
```
2. Copy the properties template and fill in your real local credentials:
```bash
   cd spring-boot-service/src/main/resources
   cp application.properties.example application.properties
```
   Then edit `application.properties` and set `spring.datasource.username` / `spring.datasource.password` to your local Postgres user.
3. Build and run:
```bash
   cd spring-boot-service
   mvn clean install
   mvn spring-boot:run
```
   Runs on `http://localhost:8080`.

### 2. Django dashboard (reporting/oversight layer)

**Requirements:** Python 3.11+, pip.

```bash
cd django-service
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt
python manage.py migrate
```

Point it at the real Spring Boot API instead of stub data:
```bash
export DJANGO_USE_STUB_API=false
export SPRING_BOOT_API_BASE_URL=http://localhost:8080
```

Run it:
```bash
python manage.py runserver
```
Runs on `http://localhost:8000`.

### 3. Register your first users

The dashboard has no signup page, users are created directly against the Spring Boot API. Register at least one ADMIN, since `/api/cases` and `/api/audit-log` return 403 for an OFFICER, and you'll want both roles to test the dashboard's restricted-view handling:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"adminpass","role":"ADMIN"}'

curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"officer1","password":"officerpass","role":"OFFICER"}'
```

Then log in through the dashboard at `http://localhost:8000` with either account.

## CRUD operations via curl

Reading data (cases, documents, audit log) is easiest through the dashboard once you're logged in, that's what it's for. The commands below cover Create, since that's what currently exists in the API.

### Create

**Register a user** (shown above), and **log in** to verify credentials directly:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"officer1","password":"officerpass"}'
```

**Upload a document:**
```bash
curl -X POST http://localhost:8080/api/documents \
  -u officer1:officerpass \
  -F "file=@/path/to/some/file.pdf" \
  -F "caseId=CASE-001" \
  -F "documentType=FIR"
```

### Read (also available via curl, if you want to bypass the dashboard)

```bash
# List documents (role filtered)
curl -u officer1:officerpass http://localhost:8080/api/documents

# View one document
curl -u officer1:officerpass http://localhost:8080/api/documents/1

# Cases and audit log require an ADMIN account
curl -u admin1:adminpass http://localhost:8080/api/cases
curl -u admin1:adminpass http://localhost:8080/api/audit-log
```

### Update and Delete

**Not implemented yet.** There is no `PUT`, `PATCH`, or `DELETE` endpoint anywhere in the current API, documents can be created and read, but not edited or removed once uploaded. This is an honest gap, not an oversight to gloss over, adding these (with corresponding audit log entries for `UPDATE`/`DELETE` actions) is on the task list below.

## Known gaps, honestly listed

- No real `Case` entity, `title` and `status` on case summaries are placeholders
- No blockchain/hash-chain integrity layer yet (planned, see above, not built)
- No AI document analysis layer yet (deferred to after the presentation round)
- No upload form in the Django dashboard, uploads are tested via the Spring Boot API directly
- No Update or Delete endpoints for documents, only Create and Read exist
- The Django session stores the raw password for reuse as Basic Auth on later calls, acceptable for a localhost prototype, not something to carry into a real deployment
- `POST /api/auth/login`'s "token" is a Basic Auth string, not a real bearer token, a deliberate simplification given the timeline

## Implementation Tasks

### Spring Boot service
- [x] Core entities, RBAC, document upload/retrieval, audit logging
- [x] `/api/cases`, `/api/audit-log`, `/api/auth/login` to match the Django contract
- [ ] Update and Delete endpoints for documents, with matching audit log entries
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


