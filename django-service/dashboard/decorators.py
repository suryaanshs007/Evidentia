"""
decorators.py

This dashboard has no Django User model of its own, identity lives in
Spring Boot's users table. So instead of django.contrib.auth's
@login_required (which checks request.user against Django's own auth
system), this checks for the session keys login_view sets after a
successful call to Spring Boot's /api/auth/login.
"""

from functools import wraps

from django.shortcuts import redirect


def spring_login_required(view_func):
    @wraps(view_func)
    def wrapper(request, *args, **kwargs):
        if "sb_auth" not in request.session:
            return redirect("dashboard:login")
        return view_func(request, *args, **kwargs)
    return wrapper
