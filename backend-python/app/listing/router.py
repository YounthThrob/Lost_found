from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel

from app.account.deps import require_agreement
from app.listing import service
from app.security.deps import get_current_user
from app.shared.errors import envelope

router = APIRouter()


class ItemPostCreate(BaseModel):
    type: str
    category: str
    title: str
    publicDescription: str
    school: str
    campus: str | None = None
    area: str | None = None


class ItemPostUpdate(BaseModel):
    type: str
    category: str
    title: str
    publicDescription: str
    school: str
    campus: str | None = None
    area: str | None = None


def _view(post) -> dict:
    return {
        "id": post.id,
        "type": post.type,
        "category": post.category,
        "title": post.title,
        "publicDescription": post.public_description,
        "school": post.school,
        "campus": post.campus,
        "area": post.area,
        "status": post.status,
        "authorId": post.author_id,
        "createdAt": post.created_at,
        "updatedAt": post.updated_at,
    }


@router.post("/posts")
def create_post(request: ItemPostCreate, user_id: str = Depends(require_agreement)):
    post = service.create_draft(
        user_id,
        post_type=request.type,
        category=request.category,
        title=request.title,
        public_description=request.publicDescription,
        school=request.school,
        campus=request.campus,
        area=request.area,
    )
    return envelope(_view(post))


@router.patch("/posts/{post_id}")
def update_post(post_id: str, request: ItemPostUpdate, user_id: str = Depends(require_agreement)):
    post = service.update_draft(
        user_id,
        post_id,
        post_type=request.type,
        category=request.category,
        title=request.title,
        public_description=request.publicDescription,
        school=request.school,
        campus=request.campus,
        area=request.area,
    )
    return envelope(_view(post))


@router.post("/posts/{post_id}/publish")
def publish_post(post_id: str, user_id: str = Depends(require_agreement)):
    return envelope(_view(service.publish(user_id, post_id)))


@router.post("/posts/{post_id}/close")
def close_post(post_id: str, user_id: str = Depends(require_agreement)):
    return envelope(_view(service.close(user_id, post_id)))


@router.get("/posts/{post_id}")
def get_post(post_id: str, user_id: str = Depends(get_current_user)):
    return envelope(_view(service.get_detail(user_id, post_id)))


@router.get("/posts")
def search_posts(
    keyword: str | None = Query(None),
    post_type: str | None = Query(None, alias="type"),
    category: str | None = Query(None),
    school: str | None = Query(None),
    campus: str | None = Query(None),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100, alias="pageSize"),
    user_id: str = Depends(get_current_user),
):
    result = service.search(keyword, post_type, category, school, campus, page, page_size)
    return envelope({
        "items": [_view(p) for p in result["items"]],
        "pagination": result["pagination"],
    })
