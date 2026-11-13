from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel

from app.listing import service as listing_service
from app.operations import service
from app.operations.deps import require_operator
from app.shared.errors import envelope

router = APIRouter()


class AdminActionRequest(BaseModel):
    reason: str


def _post_view(post) -> dict:
    return {
        "id": post.id,
        "type": post.type,
        "category": post.category,
        "title": post.title,
        "publicDescription": post.public_description,
        "schoolId": post.school_id,
        "schoolName": post.school_name,
        "campusId": post.campus_id,
        "campusName": post.campus_name,
        "area": post.area,
        "status": post.status,
        "authorId": post.author_id,
        "createdAt": post.created_at,
        "updatedAt": post.updated_at,
        "images": [
            {"id": i.id, "url": "/media/" + i.storage_key, "sortOrder": i.sort_order}
            for i in listing_service.list_images(post.id)
        ],
    }


def _report_view(report) -> dict:
    return {
        "id": report.id,
        "targetType": report.target_type,
        "targetId": report.target_id,
        "reason": report.reason,
        "description": report.description,
        "status": report.status,
        "reporterId": report.reporter_id,
        "createdAt": report.created_at,
    }


def _user_status_view(user) -> dict:
    return {"id": user.id, "status": user.status}


def _audit_event_view(event) -> dict:
    return {
        "id": event.id,
        "actorId": event.actor_id,
        "eventType": event.event_type,
        "targetType": event.target_type,
        "targetId": event.target_id,
        "metadata": {"reason": event.reason},
        "createdAt": event.created_at,
    }


@router.post("/admin/posts/{post_id}/remove")
def remove_post(post_id: str, request: AdminActionRequest, operator_id: str = Depends(require_operator)):
    post = service.remove_post(operator_id, post_id, request.reason)
    return envelope(_post_view(post))


@router.post("/admin/users/{user_id}/restrict")
def restrict_user(user_id: str, request: AdminActionRequest, operator_id: str = Depends(require_operator)):
    user = service.restrict_user(operator_id, user_id, request.reason)
    return envelope(_user_status_view(user))


@router.post("/admin/users/{user_id}/unrestrict")
def unrestrict_user(user_id: str, request: AdminActionRequest, operator_id: str = Depends(require_operator)):
    user = service.unrestrict_user(operator_id, user_id, request.reason)
    return envelope(_user_status_view(user))


@router.get("/admin/reports")
def list_reports(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100, alias="pageSize"),
    operator_id: str = Depends(require_operator),
):
    result = service.list_reports(page, page_size)
    return envelope({
        "items": [_report_view(r) for r in result["items"]],
        "pagination": result["pagination"],
    })


@router.get("/admin/audit-events")
def list_audit_events(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100, alias="pageSize"),
    operator_id: str = Depends(require_operator),
):
    result = service.list_audit_events(page, page_size)
    return envelope({
        "items": [_audit_event_view(e) for e in result["items"]],
        "pagination": result["pagination"],
    })
