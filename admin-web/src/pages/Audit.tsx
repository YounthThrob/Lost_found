import { useEffect, useState } from "react";
import { Table, Tag, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { request } from "../api";
import type { AuditEventListData, AuditEventView } from "../types";

const EVENT_LABEL: Record<string, { label: string; color: string }> = {
  POST_REMOVED: { label: "下架信息", color: "red" },
  USER_RESTRICTED: { label: "限制账户", color: "orange" },
  USER_UNRESTRICTED: { label: "解除限制", color: "green" },
};

export default function Audit() {
  const [items, setItems] = useState<AuditEventView[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const r = await request<AuditEventListData>(`/admin/audit-events?page=${page}&pageSize=${pageSize}`);
        setItems(r.items);
        setTotal(r.pagination.total);
      } catch (e: any) {
        message.error(e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, [page, pageSize]);

  const columns: ColumnsType<AuditEventView> = [
    {
      title: "事件类型",
      dataIndex: "eventType",
      width: 120,
      render: (v) => {
        const m = EVENT_LABEL[v] || { label: v, color: "default" };
        return <Tag color={m.color}>{m.label}</Tag>;
      },
    },
    { title: "目标类型", dataIndex: "targetType", width: 100 },
    { title: "目标 ID", dataIndex: "targetId", ellipsis: true },
    { title: "原因", dataIndex: ["metadata", "reason"], ellipsis: true, render: (v) => v || "—" },
    { title: "操作者", dataIndex: "actorId", ellipsis: true },
    { title: "时间", dataIndex: "createdAt", width: 180, render: (v) => new Date(v).toLocaleString() },
  ];

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={items}
      loading={loading}
      pagination={{
        current: page,
        pageSize,
        total,
        showSizeChanger: true,
        onChange: (p, ps) => {
          setPage(p);
          setPageSize(ps);
        },
      }}
    />
  );
}
