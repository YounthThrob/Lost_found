package com.lostfound.shared;

/** 业务错误码，必须与 contracts/error-codes.yaml 保持一致。 */
public enum ErrorCode {
    SESSION_EXPIRED(401, "会话已过期，请重新登录"),
    WECHAT_CODE_INVALID(401, "微信登录凭证无效或已过期"),
    AGREEMENT_NOT_ACCEPTED(403, "未确认用户协议，禁止发布/留言/认领"),
    VALIDATION_FAILED(422, "参数校验失败"),
    NOT_FOUND(404, "资源不存在或无权访问"),
    INVALID_STATE_TRANSITION(409, "非法状态迁移"),
    CLAIM_DUPLICATE(409, "同一物品已存在进行中的申请"),
    POST_NOT_CLAIMABLE(409, "信息当前不可申请认领"),
    HANDOVER_CONFLICT(409, "已有申请进入待交接"),
    BLOCKED(403, "已被对方拉黑，无法继续发送消息或申请"),
    INTERNAL_ERROR(500, "服务内部错误");

    private final int httpStatus;
    private final String message;

    ErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}
