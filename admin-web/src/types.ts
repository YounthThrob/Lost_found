// 与 openapi/product-api.yaml 中运营相关 schema 对应。

export interface Pagination {
  page: number;
  pageSize: number;
  total: number;
  hasMore: boolean;
}

export interface ItemPostView {
  id: string;
  type: "LOST" | "FOUND";
  category: string;
  title: string;
  publicDescription: string;
  school: string;
  campus?: string | null;
  area?: string | null;
  status: string;
  authorId: string;
  createdAt: string;
  updatedAt: string;
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
