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
    token = service.login(request.code)
    return envelope({"token": token})


@router.get("/me")
def me(user_id: str = Depends(get_current_user)):
    user = service.current_user(user_id)
    return envelope({"id": user.id, "status": user.status})
