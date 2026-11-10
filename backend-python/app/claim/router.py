from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel

from app.account.deps import require_agreement
from app.claim import service
from app.security.deps import get_current_user
from app.shared.errors import envelope

router = APIRouter()


class ClaimCreate(BaseModel):
    statement: str


def _view(claim) -> dict:
    return {
        "id": claim.id,
        "postId": claim.post_id,
        "applicantId": claim.applicant_id,
        "statement": claim.statement,
        "status": claim.status,
        "version": claim.version,
        "createdAt": claim.created_at,
        "updatedAt": claim.updated_at,
    }


@router.post("/posts/{post_id}/claims")
def create_claim(post_id: str, request: ClaimCreate, user_id: str = Depends(require_agreement)):
    claim = service.submit(user_id, post_id, statement=request.statement)
    return envelope(_view(claim))


@router.get("/posts/{post_id}/claims")
def list_post_claims(
    post_id: str,
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100, alias="pageSize"),
    user_id: str = Depends(get_current_user),
):
    result = service.list_by_post(user_id, post_id, page, page_size)
    return envelope({
        "items": [_view(c) for c in result["items"]],
        "pagination": result["pagination"],
    })


@router.post("/claims/{claim_id}/request-info")
def request_info(claim_id: str, user_id: str = Depends(get_current_user)):
    return envelope(_view(service.request_info(user_id, claim_id)))


@router.post("/claims/{claim_id}/accept")
def accept(claim_id: str, user_id: str = Depends(get_current_user)):
    return envelope(_view(service.accept(user_id, claim_id)))


@router.post("/claims/{claim_id}/reject")
def reject(claim_id: str, user_id: str = Depends(get_current_user)):
    return envelope(_view(service.reject(user_id, claim_id)))
