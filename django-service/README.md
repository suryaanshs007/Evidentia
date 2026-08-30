# DMS Django Dashboard (Designed by Parth)

Reporting and oversight layer for the Secure Digital Document Management System prototype (SIH26190). Reads from the Spring Boot service, has no database access of its own, and no direct write access to case or document data.

## Requirements

- Python 3.11+
- pip

## Setup

1. Create a virtual environment and install dependencies:
```
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```
2. Run migrations (only needed for Django's own admin/session tables, this app stores no document or case data of its own):
```
python manage.py migrate
```
3. Run the server:
```
python manage.py runserver
```
The dashboard starts on `http://localhost:8000`.

## Stub mode vs real API

By default this runs in stub mode, `dashboard/api_client.py` returns hardcoded sample data shaped exactly like the real Spring Boot responses, so the dashboard can be built and demoed before the Spring Boot service is ready or reachable.

To point at the real Spring Boot service instead:
```
export DJANGO_USE_STUB_API=false
export SPRING_BOOT_API_BASE_URL=http://localhost:8080
python manage.py runserver
```

Nothing in `views.py` or the templates needs to change either way, `api_client.py` is the only file that knows whether it's talking to stub data or the real service.

## Login

The dashboard has no Django User model of its own, identity lives entirely in Spring Boot's `users` table. Visiting any page redirects to `/login/` if not authenticated. Logging in calls Spring Boot's `POST /api/auth/login` to verify credentials, then stores them in the Django session for use as Basic Auth on every subsequent API call. Log out via the link in the top nav, which clears the session.

An OFFICER can log in and see documents, but `/api/cases` and `/api/audit-log` are ADMIN-only on the Spring Boot side, an OFFICER session sees those pages with a "restricted" notice instead of the page crashing.

## Pages

- `/` — case overview: list of cases with document counts, plus recent activity
- `/cases/<case_id>/` — single case detail: documents and audit trail for that case
- `/documents/` — filterable document listing (`?case_id=&type=&from=&to=`)
- `/audit-log/` — full audit trail, optionally scoped with `?case_id=`

## Spring Boot contract this depends on

- `GET /api/cases`
- `GET /api/cases/{caseId}`
- `GET /api/documents?caseId=&type=&from=&to=`
- `GET /api/audit-log?caseId=&documentId=`
- `POST /api/auth/login` -> `{token, role, username}`

`/api/cases` and `/api/audit-log` require an ADMIN user on the Spring Boot side. The `token` returned by `/api/auth/login` is a Base64 Basic Auth string, not a real bearer token, see `LoginResponse.java` on the Spring Boot side for why.

## What is intentionally not built yet

- No real Case entity on the Spring Boot side, case summaries are derived by grouping documents by `caseId`, `title` and `status` are placeholders
- No document upload form in the dashboard yet, uploads currently only work by calling the Spring Boot API directly
- Session stores the raw password (as part of the Basic Auth tuple), acceptable for a hackathon prototype on localhost, not something to carry into a real deployment without moving to real tokens
