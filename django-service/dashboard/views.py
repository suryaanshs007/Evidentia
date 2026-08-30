from django.shortcuts import render, redirect
from django.http import Http404

from . import api_client
from .decorators import spring_login_required


def login_view(request):
    """
    Logs the dashboard user in by verifying their credentials against
    Spring Boot's /api/auth/login, then stores the (username, password)
    tuple in the session for use as Basic Auth on every later API call,
    and the returned role/username for display and access decisions.
    """
    error = None

    if request.method == "POST":
        username = request.POST.get("username", "").strip()
        password = request.POST.get("password", "")

        try:
            result = api_client.login(username, password)
            request.session["sb_auth"] = (username, password)
            request.session["sb_role"] = result.get("role")
            request.session["sb_username"] = result.get("username", username)
            return redirect("dashboard:index")
        except api_client.SpringBootAuthError:
            error = "Invalid username or password."
        except api_client.SpringBootAPIError:
            error = "Could not reach the document service. Is Spring Boot running?"

    return render(request, "dashboard/login.html", {"error": error})


def logout_view(request):
    request.session.flush()
    return redirect("dashboard:login")


@spring_login_required
def index(request):
    """
    Case overview page: list of cases with document counts, plus the most
    recent activity across all cases pulled from the audit log.

    /api/cases and /api/audit-log are ADMIN-only on the Spring Boot side,
    so an OFFICER session will get rejected here, that is handled below
    rather than left to crash the page.
    """
    auth = request.session.get("sb_auth")
    restricted = False

    try:
        cases = api_client.get_cases(auth=auth)
        recent_activity = api_client.get_audit_log(auth=auth)[:5]
    except api_client.SpringBootAPIError:
        cases = []
        recent_activity = []
        restricted = True

    context = {
        "cases": cases,
        "recent_activity": recent_activity,
        "restricted": restricted,
        "using_stub": api_client.USE_STUB,
        "current_username": request.session.get("sb_username"),
        "current_role": request.session.get("sb_role"),
    }
    return render(request, "dashboard/index.html", context)


@spring_login_required
def case_detail(request, case_id):
    """
    Single case view: case metadata, its documents, and its audit trail.

    get_case_detail and the audit log call are ADMIN-only on the Spring
    Boot side, an OFFICER can still see the case's documents (that
    endpoint is authenticated-only) but not its case summary or audit
    trail, handled below rather than crashing.
    """
    auth = request.session.get("sb_auth")
    restricted = False

    try:
        case = api_client.get_case_detail(case_id, auth=auth)
    except api_client.SpringBootAPIError:
        case = None
        restricted = True

    if case is None and not restricted:
        raise Http404(f"No case found with ID {case_id}")

    documents = api_client.get_documents(case_id=case_id, auth=auth)

    try:
        audit_log = api_client.get_audit_log(case_id=case_id, auth=auth)
    except api_client.SpringBootAPIError:
        audit_log = []
        restricted = True

    context = {
        "case": case or {"caseId": case_id, "title": case_id, "status": ""},
        "documents": documents,
        "audit_log": audit_log,
        "restricted": restricted,
        "using_stub": api_client.USE_STUB,
        "current_username": request.session.get("sb_username"),
        "current_role": request.session.get("sb_role"),
    }
    return render(request, "dashboard/case_detail.html", context)


@spring_login_required
def document_list(request):
    """
    Filterable document listing. Reads optional query params:
    ?case_id=&type=&from=&to=
    """
    auth = request.session.get("sb_auth")
    case_id = request.GET.get("case_id") or None
    document_type = request.GET.get("type") or None
    date_from = request.GET.get("from") or None
    date_to = request.GET.get("to") or None

    documents = api_client.get_documents(
        case_id=case_id,
        document_type=document_type,
        date_from=date_from,
        date_to=date_to,
        auth=auth,
    )

    context = {
        "documents": documents,
        "filters": {
            "case_id": case_id or "",
            "type": document_type or "",
            "from": date_from or "",
            "to": date_to or "",
        },
        "using_stub": api_client.USE_STUB,
        "current_username": request.session.get("sb_username"),
        "current_role": request.session.get("sb_role"),
    }
    return render(request, "dashboard/document_list.html", context)


@spring_login_required
def audit_log(request):
    """
    Full activity/audit trail view, optionally scoped by ?case_id=.
    ADMIN-only on the Spring Boot side, handled gracefully for an
    OFFICER session rather than crashing.
    """
    auth = request.session.get("sb_auth")
    case_id = request.GET.get("case_id") or None
    restricted = False

    try:
        entries = api_client.get_audit_log(case_id=case_id, auth=auth)
    except api_client.SpringBootAPIError:
        entries = []
        restricted = True

    context = {
        "entries": entries,
        "case_id": case_id or "",
        "restricted": restricted,
        "using_stub": api_client.USE_STUB,
        "current_username": request.session.get("sb_username"),
        "current_role": request.session.get("sb_role"),
    }
    return render(request, "dashboard/audit_log.html", context)
