from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel

from app.account.deps import require_agreement
from app.communication import service
from app.security.deps import get_current_user
from app.shared.errors import envelope

router = APIRouter()


class BlockCreate(BaseModel):
    blockedUserId: str


class MessageCreate(BaseModel):
    type: str
    content: str


def _block_view(relation) -> dict:
    return {
        "blockedUserId": relation.blocked_id,
        "createdAt": relation.created_at,
    }


def _message_view(message) -> dict:
    return {
        "id": message.id,
        "conversationId": message.conversation_id,
        "senderId": message.sender_id,
        "type": message.type,
        "content": message.content,
        "createdAt": message.created_at,
    }


@router.post("/blocks")
def block(request: BlockCreate, user_id: str = Depends(get_current_user)):
    relation = service.block(user_id, request.blockedUserId)
    return envelope(_block_view(relation))


@router.delete("/blocks/{blocked_user_id}")
def unblock(blocked_user_id: str, user_id: str = Depends(get_current_user)):
    service.unblock(user_id, blocked_user_id)
    return envelope({})


@router.get("/me/blocks")
def list_blocks(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100, alias="pageSize"),
    user_id: str = Depends(get_current_user),
):
    result = service.list_blocks(user_id, page, page_size)
    return envelope({
        "items": [_block_view(r) for r in result["items"]],
        "pagination": result["pagination"],
    })


@router.get("/conversations/{conversation_id}/messages")
def list_messages(
    conversation_id: str,
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100, alias="pageSize"),
    user_id: str = Depends(get_current_user),
):
    result = service.list_messages(user_id, conversation_id, page, page_size)
    return envelope({
        "items": [_message_view(m) for m in result["items"]],
        "pagination": result["pagination"],
    })


@router.post("/conversations/{conversation_id}/messages")
def send_message(
    conversation_id: str,
    request: MessageCreate,
    user_id: str = Depends(require_agreement),
):
    message = service.send(user_id, conversation_id, type_=request.type, content=request.content)
    return envelope(_message_view(message))
