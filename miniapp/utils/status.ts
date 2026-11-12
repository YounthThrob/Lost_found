// 状态枚举的中文文案映射，供列表与详情展示。

export const POST_STATUS_LABEL: Record<string, string> = {
  DRAFT: "草稿",
  PENDING_REVIEW: "审核中",
  PUBLISHED: "展示中",
  REJECTED: "未通过",
  CLOSED: "已关闭",
  VERIFYING: "核验中",
  PENDING_HANDOVER: "待交接",
  COMPLETED: "已完成",
  REMOVED: "已下架",
};

export const CLAIM_STATUS_LABEL: Record<string, string> = {
  SUBMITTED: "已提交",
  NEEDS_MORE_INFO: "待补充",
  ACCEPTED: "已通过",
  REJECTED: "已拒绝",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
};

export const CLUE_STATUS_LABEL: Record<string, string> = {
  SUBMITTED: "待处理",
  ACCEPTED: "有效",
  CLOSED: "已关闭",
  REJECTED: "无效",
  EXPIRED: "已过期",
};

export const HANDOVER_STATUS_LABEL: Record<string, string> = {
  PENDING: "待确认",
  COMPLETED: "双方确认完成",
  CANCELLED: "已终止",
};

export const MESSAGE_TYPE_LABEL: Record<string, string> = {
  TEXT: "文本",
  SUGGEST_TIME: "建议时间",
  SUGGEST_PLACE: "建议地点",
  LATE_REMINDER: "迟到提醒",
};

export function postStatus(status: string): string {
  return POST_STATUS_LABEL[status] || status;
}

export function claimStatus(status: string): string {
  return CLAIM_STATUS_LABEL[status] || status;
}

export function clueStatus(status: string): string {
  return CLUE_STATUS_LABEL[status] || status;
}

export function handoverStatus(status: string): string {
  return HANDOVER_STATUS_LABEL[status] || status;
}
