# DMS Spring Boot Service

Core backend for the Secure Digital Document Management System prototype (SIH26190). Handles authentication, role based access control, document upload/retrieval, and audit logging.

## Requirements

- Java 17
- Maven
- PostgreSQL running locally

## Setup

1. Create the database, Postgres does not create it automatically:
   ```
   createdb dms_db
   ```
   or from inside `psql`:
   ```
   CREATE DATABASE dms_db;
   ```
2. Update `spring.datasource.username` and `spring.datasource.password` in `src/main/resources/application.properties` to match your local PostgreSQL setup. The default connection points to `dms_db` on `localhost:5432`.
3. Run the app:

```
mvn spring-boot:run
```

The service starts on `http://localhost:8080`.

## Authentication

This prototype uses HTTP Basic authentication rather than JWT, to keep the first version simple. Every request to a protected endpoint needs an `Authorization: Basic <base64(username:password)>` header, or you can use your HTTP client's built in basic auth support.

## Endpoints

### Register a user

```
POST /api/auth/register
Content-Type: application/json

{
  "username": "officer1",
  "password": "password123",
  "role": "OFFICER"
}
```

`role` must be `OFFICER` or `ADMIN`. This endpoint is open, no authentication required, since it is how the first users get created.

### Upload a document

```
POST /api/documents
Authorization: Basic <credentials>
Content-Type: multipart/form-data

file: <the file>
caseId: CASE-001
documentType: FIR
```

Saves the file to the folder configured in `dms.storage.location`, saves the metadata, and writes an audit log entry, in a single transaction.

### List documents

```
GET /api/documents
Authorization: Basic <credentials>
```

An OFFICER sees only documents they uploaded. An ADMIN sees all documents.

### View a single document

```
GET /api/documents/{id}
Authorization: Basic <credentials>
```

Returns the document if the requester is the uploader or an ADMIN, otherwise responds with 403. Every view, whether granted or denied, is recorded in the audit log.

## What is intentionally not in this version

- No file versioning
- No hash based tamper evidence
- No JWT, sessions use HTTP Basic only
- No AI analysis layer

These are planned as later additions once the core flow above is working end to end.
