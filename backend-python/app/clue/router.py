from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel

from app.account.deps import require_agreement
from app.clue import service
from app.security.deps import get_current_user
from app.shared.errors import envelope

router = APIRouter()


class ClueCreate(BaseModel):
    content: str


class ClueDecide(BaseModel):
    decision: str


def _view(clue) -> dict:
    return {
        "id": clue.id,
        "postId": clue.post_id,
        "providerId": clue.provider_id,
        "content": clue.content,
        "status": clue.status,
        "createdAt": clue.created_at,
    }


@router.post("/posts/{post_id}/clues")
def create_clue(post_id: str, request: ClueCreate, user_id: str = Depends(require_agreement)):
    clue = service.submit(user_id, post_id, content=request.content)
    return envelope(_view(clue))


@router.get("/posts/{post_id}/clues")
def list_post_clues(
    post_id: str,
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100, alias="pageSize"),
    user_id: str = Depends(get_current_user),
):
    result = service.list_pending_by_post(user_id, post_id, page, page_size)
    return envelope({
        "items": [_view(c) for c in result["items"]],
        "pagination": result["pagination"],
    })


@router.post("/clues/{clue_id}/decide")
def decide_clue(clue_id: str, request: ClueDecide, user_id: str = Depends(get_current_user)):
    return envelope(_view(service.decide(user_id, clue_id, decision=request.decision)))
