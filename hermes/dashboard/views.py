from django.shortcuts import render

# Create your views here.
from django.shortcuts import render
from django.http import HttpResponse
from django.http import Http404
from . import api_client

def dash(request):
    return HttpResponse("This is my dashboard")

def home(request):
    return HttpResponse("This is my default page")

def index(request):
    """
    Case overview page: list of cases with document counts, plus the most
    recent activity across all cases pulled from the audit log.
    """
    cases = api_client.get_cases()
    recent_activity = api_client.get_audit_log()[:5]  # most recent 5 events

    context = {
        "cases": cases,
        "recent_activity": recent_activity,
        "using_stub": api_client.USE_STUB,
    }
    return render(request, "dashboard/index.html", context)


def case_detail(request, case_id):
    """
    Single case view: case metadata, its documents, and its audit trail.
    """
    case = api_client.get_case_detail(case_id)
    if case is None:
        raise Http404(f"No case found with ID {case_id}")

    documents = api_client.get_documents(case_id=case_id)
    audit_log = api_client.get_audit_log(case_id=case_id)

    context = {
        "case": case,
        "documents": documents,
        "audit_log": audit_log,
        "using_stub": api_client.USE_STUB,
    }
    return render(request, "dashboard/case_detail.html", context)


def document_list(request):
    """
    Filterable document listing. Reads optional query params:
    ?case_id=&type=&from=&to=
    """
    case_id = request.GET.get("case_id") or None
    document_type = request.GET.get("type") or None
    date_from = request.GET.get("from") or None
    date_to = request.GET.get("to") or None

    documents = api_client.get_documents(
        case_id=case_id,
        document_type=document_type,
        date_from=date_from,
        date_to=date_to,
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
    }
    return render(request, "dashboard/document_list.html", context)


def audit_log(request):
    """
    Full activity/audit trail view, optionally scoped by ?case_id=.
    """
    case_id = request.GET.get("case_id") or None
    entries = api_client.get_audit_log(case_id=case_id)

    context = {
        "entries": entries,
        "case_id": case_id or "",
        "using_stub": api_client.USE_STUB,
    }
    return render(request, "dashboard/audit_log.html", context)