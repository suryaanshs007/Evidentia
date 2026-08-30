from django.urls import path, include
from . import views

app_name='dashboard'


urlpatterns = [
    # path('members/', views.members, name='members'),
    #path('dashboard/', views.dash, name='dashboard'),
    path('', views.index, name='index'),
    path("cases/<str:case_id>/", views.case_detail, name="case_detail"),
    path("documents/", views.document_list, name="document_list"),
    path("audit-log/", views.audit_log, name="audit_log"),
    #path('', include('dashboard.urls'))
]