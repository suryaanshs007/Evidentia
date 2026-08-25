# Evidentia: Secure Digital Document Management System (SIH26190)

Prototype for Smart India Hackathon 2026, Problem Statement 26190, sponsored by the Ministry of Home Affairs (National Crime Records Bureau, Women Safety Division).

## Problem Statement

**PS ID:** SIH26190
**Title:** Secure Digital Document Management System for Legal and Investigation Documents
**Organization:** Ministry of Home Affairs
**Department:** National Crime Records Bureau (NCRB), Women Safety Division
**Category:** Software
**Theme:** Miscellaneous

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

## Technical Approach

### Technologies

- **Backend / System of record:** Java, Spring Boot, Spring Security, Spring Data JPA (Hibernate), MySQL
- **Reporting / Admin layer:** Python, Django
- **Auth:** Spring Security with role based access control (Officer, Admin)
- **Storage:** Local filesystem for the prototype, with a metadata record per document in MySQL

### Architecture

The system is split into two independently run services rather than one combined codebase.

```
Spring Boot Service (system of record)
    - Owns the MySQL schema
    - Handles auth, document upload/retrieval, RBAC, audit logging
    - Exposes a REST API

Django Service (reporting and oversight)
    - Consumes the Spring Boot REST API only
    - No direct access to the Spring Boot database
    - Owns a case overview / activity dashboard
```

Communication is one directional: the Django service calls the Spring Boot API. It never writes to the Spring Boot database directly, and the Spring Boot service has no dependency on Django.

### Methodology

1. Define the core entities and relationships (User, DocumentRecord, AuditLog) in the Spring Boot service
2. Implement authentication and role based access control
3. Implement document upload, retrieval, and listing, filtered by role
4. Wire audit logging into every document view, upload, and edit as part of the same transaction
5. Stand up the Django service against the Spring Boot API and build the reporting/dashboard view
6. Iterate on tamper evidence (file hashing) and version history once the core flow is stable

## Feasibility and Viability

The core prototype uses a stack the team already has working knowledge of (Java, Spring Boot, MySQL, Django), so the main risk is scope, not unfamiliar technology.

Potential challenges:

- Limited timeline before the internal presentation round
- Coordinating a clean API contract between the two services
- Keeping the security and audit story credible without overbuilding it

Mitigation:

- Keep the prototype scope to auth, RBAC, document CRUD, and audit logging only, and defer version history, hashing, and dashboards to later iterations
- Have the Django side build against a stubbed version of the API contract early, so both services can be developed in parallel without blocking each other
- Treat the audit log and RBAC enforcement as the non negotiable core, since that is what differentiates this from a generic file upload app

## Impact and Benefits

- Faster, more reliable document retrieval for investigative and legal staff
- Reduced risk of unauthorized access or undetected tampering with sensitive case material
- A traceable audit history that supports compliance and internal oversight
- A foundation that can extend to version control, digital signatures, and tamper evidence without a redesign

## Project Structure

```
.
├── spring-boot-service/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/sih26190/dms/
│   │   │   │   ├── config/
│   │   │   │   │   └── SecurityConfig.java
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   └── DocumentController.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── DocumentRecord.java
│   │   │   │   │   ├── AuditLog.java
│   │   │   │   │   └── Role.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── DocumentRecordRepository.java
│   │   │   │   │   └── AuditLogRepository.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── DocumentService.java
│   │   │   │   │   └── AuditService.java
│   │   │   │   └── DmsApplication.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   ├── pom.xml
│   └── README.md
│
├── django-service/
│   ├── dashboard/
│   │   ├── views.py
│   │   ├── urls.py
│   │   ├── api_client.py
│   │   └── templates/
│   ├── manage.py
│   ├── requirements.txt
│   └── README.md
│
└── README.md
```

## Implementation Tasks

### Spring Boot service

- [ ] Set up project with Spring Web, Spring Security, Spring Data JPA, MySQL Driver, Lombok, Validation
- [ ] Define User, DocumentRecord, AuditLog entities and their relationships
- [ ] Configure Spring Security with a UserDetailsService and BCrypt password encoding
- [ ] Implement login endpoint
- [ ] Implement document upload endpoint, storing file on disk and metadata in MySQL
- [ ] Implement document listing endpoint, filtered by role in the service layer
- [ ] Implement document view endpoint
- [ ] Wire audit log writes into upload and view actions within the same transaction
- [ ] Add basic search/filter by case ID, document type, and date
- [ ] Write a stubbed API response contract for the Django team to build against early

### Django service

- [ ] Set up Django project and app structure
- [ ] Build an API client module to call the Spring Boot service
- [ ] Build a case overview / activity dashboard view against the stubbed API
- [ ] Switch the dashboard over to the real Spring Boot API once it is stable
- [ ] Basic styling for the dashboard views

### Later, after the prototype

- [ ] Document version history
- [ ] File hash based tamper evidence on upload and retrieval
- [ ] Expanded roles beyond Officer and Admin
- [ ] Deployment story for both services

## Team

Team Name:
Team ID:

## Setup

Setup instructions for each service are in their own README (`spring-boot-service/README.md`, `django-service/README.md`), to be filled in as each service takes shape.
