// 与 openapi/product-api.yaml 的 schema 一一对应，仅作为客户端类型约束。

export type PostType = "LOST" | "FOUND";

export type PostStatus =
  | "DRAFT"
  | "PENDING_REVIEW"
  | "PUBLISHED"
  | "REJECTED"
  | "CLOSED"
  | "VERIFYING"
  | "PENDING_HANDOVER"
  | "COMPLETED"
  | "REMOVED";

export type ClaimStatus =
  | "SUBMITTED"
  | "NEEDS_MORE_INFO"
  | "ACCEPTED"
  | "REJECTED"
  | "COMPLETED"
  | "CANCELLED";

export type ClueStatus = "SUBMITTED" | "ACCEPTED" | "CLOSED" | "REJECTED" | "EXPIRED";

export type HandoverStatus = "PENDING" | "COMPLETED" | "CANCELLED";

export type MessageType = "TEXT" | "SUGGEST_TIME" | "SUGGEST_PLACE" | "LATE_REMINDER";

export interface Pagination {
  page: number;
  pageSize: number;
  total: number;
  hasMore: boolean;
}

export interface ItemPostView {
  id: string;
  type: PostType;
  category: string;
  title: string;
  publicDescription: string;
  school: string;
  campus?: string | null;
  area?: string | null;
  status: PostStatus;
  authorId: string;
  createdAt: string;
  updatedAt: string;
}

export interface ItemPostListData {
  items: ItemPostView[];
  pagination: Pagination;
}

export interface MeView {
  id: string;
  status: string;
  agreementAccepted: boolean;
}

export interface CampusProfileView {
  school: string | null;
  campus: string | null;
  verificationStatus: string;
}

export interface ClaimView {
  id: string;
  postId: string;
  applicantId: string;
  statement: string;
  status: ClaimStatus;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface ClaimListData {
  items: ClaimView[];
  pagination: Pagination;
}

export interface ClueView {
  id: string;
  postId: string;
  providerId: string;
  content: string;
  status: ClueStatus;
  createdAt: string;
}

export interface ClueListData {
  items: ClueView[];
  pagination: Pagination;
}

export interface ReportView {
  id: string;
  targetType: "POST" | "USER";
  targetId: string;
  reason: string;
  description?: string | null;
  status: string;
  reporterId: string;
  createdAt: string;
}

export interface ReportListData {
  items: ReportView[];
  pagination: Pagination;
}

export interface BookmarkView {
  postId: string;
  createdAt: string;
  post: ItemPostView;
}

export interface BookmarkListData {
  items: BookmarkView[];
  pagination: Pagination;
}

export interface BlockView {
  blockedUserId: string;
  createdAt: string;
}

export interface BlockListData {
  items: BlockView[];
  pagination: Pagination;
}

export interface MessageView {
  id: string;
  conversationId: string;
  senderId: string;
  type: MessageType;
  content: string;
  createdAt: string;
}

export interface MessageListData {
  items: MessageView[];
  pagination: Pagination;
}

export interface HandoverView {
  id: string;
  claimId: string;
  proposerId: string;
  timeWindow: string;
  publicPlace: string;
  ownerConfirmedAt?: string | null;
  claimantConfirmedAt?: string | null;
  status: HandoverStatus;
  createdAt: string;
}

export interface AuditEventView {
  id: string;
  actorId: string;
  eventType: "POST_REMOVED" | "USER_RESTRICTED" | "USER_UNRESTRICTED";
  targetType: "POST" | "USER";
  targetId: string;
  metadata: { reason?: string };
  createdAt: string;
}

export interface AuditEventListData {
  items: AuditEventView[];
  pagination: Pagination;
}

export interface UserStatusView {
  id: string;
  status: "ACTIVE" | "RESTRICTED";
}
