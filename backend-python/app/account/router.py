from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.account import service
from app.security.deps import get_current_user
from app.shared.errors import envelope

router = APIRouter()


class LoginRequest(BaseModel):
    code: str


@router.post("/session/wechat")
def login(request: LoginRequest):
    token, agreement_accepted = service.login(request.code)
    return envelope({"token": token, "agreementAccepted": agreement_accepted})


@router.get("/me")
def me(user_id: str = Depends(get_current_user)):
    user = service.current_user(user_id)
    return envelope({"id": user.id, "status": user.status, "agreementAccepted": user.agreement_accepted})


@router.post("/me/agreement")
def confirm_agreement(user_id: str = Depends(get_current_user)):
    service.confirm_agreement(user_id)
    return envelope({"agreementAccepted": True})
