from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.account import service
from app.security.deps import get_current_user
from app.shared.errors import envelope

router = APIRouter()


class LoginRequest(BaseModel):
    code: str


class CampusProfileUpdate(BaseModel):
    school: str
    campus: str | None = None


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


@router.get("/me/campus-profile")
def get_campus_profile(user_id: str = Depends(get_current_user)):
    user = service.current_user(user_id)
    return envelope(
        {"school": user.school, "campus": user.campus, "verificationStatus": "SELF_DECLARED"}
    )


@router.put("/me/campus-profile")
def put_campus_profile(request: CampusProfileUpdate, user_id: str = Depends(get_current_user)):
    user = service.update_campus_profile(user_id, request.school, request.campus)
    return envelope(
        {"school": user.school, "campus": user.campus, "verificationStatus": "SELF_DECLARED"}
    )
