from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel

from app.directory import service
from app.operations.deps import require_operator
from app.security.deps import get_current_user
from app.shared.errors import envelope

router = APIRouter()


class CampusCreate(BaseModel):
    name: str


class CampusReview(BaseModel):
    decision: str


def _school_view(school) -> dict:
    return {"id": school.id, "name": school.name, "aliases": list(school.aliases)}


def _campus_view(campus) -> dict:
    return {"id": campus.id, "schoolId": campus.school_id, "name": campus.name, "status": campus.status}


@router.get("/directory/schools")
def search_schools(
    keyword: str | None = Query(None),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100, alias="pageSize"),
    user_id: str = Depends(get_current_user),
):
    result = service.search_schools(keyword, page, page_size)
    return envelope({
        "items": [_school_view(s) for s in result["items"]],
        "pagination": result["pagination"],
    })


@router.get("/directory/schools/{school_id}/campuses")
def list_campuses(school_id: str, user_id: str = Depends(get_current_user)):
    campuses = service.list_campuses(school_id)
    return envelope({
        "items": [_campus_view(c) for c in campuses],
        "pagination": {"page": 1, "pageSize": len(campuses), "total": len(campuses), "hasMore": False},
    })


@router.post("/directory/schools/{school_id}/campuses")
def request_campus(school_id: str, request: CampusCreate, user_id: str = Depends(get_current_user)):
    campus = service.request_campus(school_id, request.name)
    return envelope(_campus_view(campus))


@router.post("/admin/campuses/{campus_id}/review")
def review_campus(campus_id: str, request: CampusReview, operator_id: str = Depends(require_operator)):
    campus = service.review_campus(campus_id, request.decision)
    return envelope(_campus_view(campus))
