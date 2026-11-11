from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.account.deps import require_agreement
from app.handover import service
from app.security.deps import get_current_user
from app.shared.errors import envelope

router = APIRouter()


class HandoverCreate(BaseModel):
    claimId: str
    timeWindow: str
    publicPlace: str


def _view(handover) -> dict:
    return {
        "id": handover.id,
        "claimId": handover.claim_id,
        "proposerId": handover.proposer_id,
        "timeWindow": handover.time_window,
        "publicPlace": handover.public_place,
        "ownerConfirmedAt": handover.owner_confirmed_at,
        "claimantConfirmedAt": handover.claimant_confirmed_at,
        "status": handover.status,
        "createdAt": handover.created_at,
    }


@router.post("/handovers")
def create_handover(request: HandoverCreate, user_id: str = Depends(require_agreement)):
    handover = service.create(
        user_id,
        claim_id=request.claimId,
        time_window=request.timeWindow,
        public_place=request.publicPlace,
    )
    return envelope(_view(handover))


@router.post("/handovers/{handover_id}/confirm")
def confirm_handover(handover_id: str, user_id: str = Depends(get_current_user)):
    return envelope(_view(service.confirm(user_id, handover_id)))


@router.post("/handovers/{handover_id}/cancel")
def cancel_handover(handover_id: str, user_id: str = Depends(get_current_user)):
    return envelope(_view(service.cancel(user_id, handover_id)))
