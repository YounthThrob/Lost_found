from fastapi import APIRouter, Depends, Query

from app.bookmark import service
from app.listing import service as listing_service
from app.security.deps import get_current_user
from app.shared.errors import envelope

router = APIRouter()


def _post_view(post) -> dict:
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
        "images": [
            {"id": i.id, "url": "/media/" + i.storage_key, "sortOrder": i.sort_order}
            for i in listing_service.list_images(post.id)
        ],
    }


def _view(entry: dict) -> dict:
    return {
        "postId": entry["postId"],
        "createdAt": entry["createdAt"],
        "post": _post_view(entry["post"]),
    }


@router.post("/posts/{post_id}/bookmark")
def bookmark(post_id: str, user_id: str = Depends(get_current_user)):
    return envelope(_view(service.bookmark(user_id, post_id)))


@router.delete("/posts/{post_id}/bookmark")
def unbookmark(post_id: str, user_id: str = Depends(get_current_user)):
    service.unbookmark(user_id, post_id)
    return envelope({})


@router.get("/me/bookmarks")
def list_bookmarks(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100, alias="pageSize"),
    user_id: str = Depends(get_current_user),
):
    result = service.list_bookmarks(user_id, page, page_size)
    return envelope({
        "items": [_view(e) for e in result["items"]],
        "pagination": result["pagination"],
    })
