"""
api_client.py

Single point of contact between the Django reporting/oversight service and the
Spring Boot system-of-record service.

Phase 2 goal: let the dashboard views be built and demoed *before* the real
Spring Boot endpoints exist, by returning data shaped exactly like the real
API contract will be. When Spring Boot is ready, flip USE_STUB to False (or
set DJANGO_USE_STUB_API=false in the environment) and nothing in views.py
has to change, because the return shapes are identical either way.

Expected real endpoints (Spring Boot side, for reference):
    GET  /api/cases                      -> list of case summaries
    GET  /api/cases/{caseId}              -> single case detail
    GET  /api/documents?caseId=&type=&from=&to=  -> filtered document list
    GET  /api/audit-log?caseId=&documentId=      -> audit trail entries
    POST /api/auth/login                  -> {token, role, username}
"""

import os
from datetime import datetime, timedelta

import requests
from django.conf import settings

# Toggle: True while Spring Boot isn't ready / isn't reachable from here.
# Read from settings.py first, fall back to an env var, default to stub mode.
USE_STUB = getattr(
    settings,
    "USE_STUB_API",
    os.environ.get("DJANGO_USE_STUB_API", "true").lower() == "true",
)

BASE_URL = getattr(settings, "SPRING_BOOT_API_BASE_URL", "http://localhost:8080")
REQUEST_TIMEOUT_SECONDS = 5


class SpringBootAPIError(Exception):
    """Raised when the real Spring Boot API call fails or returns a non-2xx status."""
    pass


# ---------------------------------------------------------------------------
# Stub data
# ---------------------------------------------------------------------------
# Shaped to mirror the real DocumentRecord / AuditLog / User entities so that
# swapping stub -> real is a data-source change only, not a shape change.

_NOW = datetime(2026, 8, 27, 10, 0, 0)

_STUB_CASES = [
    {"caseId": "CASE-2026-0142", "title": "Missing Persons - Sector 12", "status": "OPEN", "documentCount": 7},
    {"caseId": "CASE-2026-0157", "title": "Assault Investigation - MG Road", "status": "OPEN", "documentCount": 4},
    {"caseId": "CASE-2026-0099", "title": "Cyber Harassment Complaint", "status": "CLOSED", "documentCount": 11},
]

_STUB_DOCUMENTS = [
    {
        "documentId": "DOC-1001", "caseId": "CASE-2026-0142", "documentType": "FIR",
        "title": "FIR Copy - Sector 12", "uploadedBy": "officer.rao",
        "uploadedAt": (_NOW - timedelta(days=5)).isoformat(),
    },
    {
        "documentId": "DOC-1002", "caseId": "CASE-2026-0142", "documentType": "WITNESS_STATEMENT",
        "title": "Witness Statement - S. Iyer", "uploadedBy": "officer.rao",
        "uploadedAt": (_NOW - timedelta(days=4)).isoformat(),
    },
    {
        "documentId": "DOC-1003", "caseId": "CASE-2026-0157", "documentType": "FORENSIC_REPORT",
        "title": "Forensic Report - MG Road Incident", "uploadedBy": "officer.singh",
        "uploadedAt": (_NOW - timedelta(days=2)).isoformat(),
    },
    {
        "documentId": "DOC-1004", "caseId": "CASE-2026-0099", "documentType": "CHARGE_SHEET",
        "title": "Charge Sheet - Cyber Harassment", "uploadedBy": "officer.menon",
        "uploadedAt": (_NOW - timedelta(days=30)).isoformat(),
    },
]

_STUB_AUDIT_LOG = [
    {
        "auditId": "AUD-9001", "documentId": "DOC-1001", "caseId": "CASE-2026-0142",
        "action": "UPLOAD", "performedBy": "officer.rao",
        "performedAt": (_NOW - timedelta(days=5)).isoformat(),
    },
    {
        "auditId": "AUD-9002", "documentId": "DOC-1001", "caseId": "CASE-2026-0142",
        "action": "VIEW", "performedBy": "admin.kapoor",
        "performedAt": (_NOW - timedelta(days=3)).isoformat(),
    },
    {
        "auditId": "AUD-9003", "documentId": "DOC-1003", "caseId": "CASE-2026-0157",
        "action": "VIEW", "performedBy": "officer.singh",
        "performedAt": (_NOW - timedelta(hours=6)).isoformat(),
    },
    {
        "auditId": "AUD-9004", "documentId": "DOC-1004", "caseId": "CASE-2026-0099",
        "action": "EDIT", "performedBy": "officer.menon",
        "performedAt": (_NOW - timedelta(days=1)).isoformat(),
    },
]


# ---------------------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------------------

def _get(path, params=None):
    """Perform a real GET call against the Spring Boot service."""
    url = f"{BASE_URL}{path}"
    try:
        response = requests.get(url, params=params, timeout=REQUEST_TIMEOUT_SECONDS)
        response.raise_for_status()
        return response.json()
    except requests.RequestException as exc:
        raise SpringBootAPIError(f"Failed calling {url}: {exc}") from exc


def _filter_documents(case_id=None, document_type=None, date_from=None, date_to=None):
    results = _STUB_DOCUMENTS
    if case_id:
        results = [d for d in results if d["caseId"] == case_id]
    if document_type:
        results = [d for d in results if d["documentType"] == document_type]
    if date_from:
        results = [d for d in results if d["uploadedAt"] >= date_from]
    if date_to:
        results = [d for d in results if d["uploadedAt"] <= date_to]
    return results


def _filter_audit_log(case_id=None, document_id=None):
    results = _STUB_AUDIT_LOG
    if case_id:
        results = [a for a in results if a["caseId"] == case_id]
    if document_id:
        results = [a for a in results if a["documentId"] == document_id]
    return sorted(results, key=lambda a: a["performedAt"], reverse=True)


# ---------------------------------------------------------------------------
# Public API - this is what views.py calls. Shape is identical stub or real.
# ---------------------------------------------------------------------------

def get_cases():
    """Return a list of case summaries for the overview page."""
    if USE_STUB:
        return _STUB_CASES
    return _get("/api/cases")


def get_case_detail(case_id):
    """Return a single case's detail, or None if not found."""
    if USE_STUB:
        return next((c for c in _STUB_CASES if c["caseId"] == case_id), None)
    return _get(f"/api/cases/{case_id}")


def get_documents(case_id=None, document_type=None, date_from=None, date_to=None):
    """Return documents, optionally filtered by case, type, and date range."""
    if USE_STUB:
        return _filter_documents(case_id, document_type, date_from, date_to)
    params = {
        "caseId": case_id,
        "type": document_type,
        "from": date_from,
        "to": date_to,
    }
    params = {k: v for k, v in params.items() if v}
    return _get("/api/documents", params=params)


def get_audit_log(case_id=None, document_id=None):
    """Return audit trail entries, most recent first, optionally scoped to a case or document."""
    if USE_STUB:
        return _filter_audit_log(case_id, document_id)
    params = {k: v for k, v in {"caseId": case_id, "documentId": document_id}.items() if v}
    return _get("/api/audit-log", params=params)