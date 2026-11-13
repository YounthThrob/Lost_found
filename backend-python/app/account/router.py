from fastapi import APIRouter, Depends, File, UploadFile
from pydantic import BaseModel

from app.account import service
from app.security.deps import get_current_user
from app.shared.errors import envelope

router = APIRouter()


class LoginRequest(BaseModel):
    code: str


class ProfileUpdate(BaseModel):
    nickname: str


class CampusProfileUpdate(BaseModel):
    schoolId: str
    campusId: str | None = None


class EmailVerificationSend(BaseModel):
    email: str


class EmailVerificationConfirm(BaseModel):
    email: str
    code: str


def _profile_view(user) -> dict:
    return service.campus_profile_view(user)


@router.post("/session/wechat")
def login(request: LoginRequest):
    token, agreement_accepted = service.login(request.code)
    return envelope({"token": token, "agreementAccepted": agreement_accepted})


@router.get("/me")
def me(user_id: str = Depends(get_current_user)):
    user = service.current_user(user_id)
    return envelope({
        "id": user.id,
        "status": user.status,
        "agreementAccepted": user.agreement_accepted,
        "nickname": user.nickname,
        "avatarUrl": service.avatar_url(user.avatar_key),
    })


@router.put("/me/profile")
def put_profile(request: ProfileUpdate, user_id: str = Depends(get_current_user)):
    user = service.update_profile(user_id, request.nickname)
    return envelope(service.profile_view(user))


@router.post("/me/avatar")
def upload_avatar(file: UploadFile = File(...), user_id: str = Depends(get_current_user)):
    url = service.set_avatar(user_id, file.file.read(), file.content_type)
    return envelope({"avatarUrl": url})


@router.post("/me/agreement")
def confirm_agreement(user_id: str = Depends(get_current_user)):
    service.confirm_agreement(user_id)
    return envelope({"agreementAccepted": True})


@router.get("/me/campus-profile")
def get_campus_profile(user_id: str = Depends(get_current_user)):
    user = service.current_user(user_id)
    return envelope(_profile_view(user))


@router.put("/me/campus-profile")
def put_campus_profile(request: CampusProfileUpdate, user_id: str = Depends(get_current_user)):
    user = service.update_campus_profile(user_id, request.schoolId, request.campusId)
    return envelope(_profile_view(user))


@router.post("/me/campus-verification/email")
def send_campus_email_verification(request: EmailVerificationSend, user_id: str = Depends(get_current_user)):
    expires_in_seconds = service.send_campus_email_verification(user_id, request.email)
    return envelope({"expiresInSeconds": expires_in_seconds})


@router.post("/me/campus-verification/email/confirm")
def confirm_campus_email_verification(
    request: EmailVerificationConfirm, user_id: str = Depends(get_current_user)
):
    user = service.confirm_campus_email_verification(user_id, request.email, request.code)
    return envelope(_profile_view(user))
