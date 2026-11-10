from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel

from app.moderation import service
from app.security.deps import get_current_user
from app.shared.errors import envelope

router = APIRouter()


class ReportCreate(BaseModel):
    targetType: str
    targetId: str
    reason: str
    description: str | None = None


def _view(report) -> dict:
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


@router.post("/reports")
def create_report(request: ReportCreate, user_id: str = Depends(get_current_user)):
    report = service.create_report(
        user_id,
        target_type=request.targetType,
        target_id=request.targetId,
        reason=request.reason,
        description=request.description,
    )
    return envelope(_view(report))


@router.get("/reports/me")
def list_my_reports(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100, alias="pageSize"),
    user_id: str = Depends(get_current_user),
):
    result = service.list_reports(user_id, page, page_size)
    return envelope({
        "items": [_view(r) for r in result["items"]],
        "pagination": result["pagination"],
    })
