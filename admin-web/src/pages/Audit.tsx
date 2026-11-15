import { useEffect, useMemo, useState } from "react";
import { Input, Table, Tag, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import Icon from "../components/Icons";
import { request } from "../api";
import type { AuditEventListData, AuditEventView } from "../types";

const EVENT_LABEL: Record<string, { label: string; tone: string }> = {
  POST_REMOVED: { label: "下架信息", tone: "red" },
  USER_RESTRICTED: { label: "限制账户", tone: "amber" },
  USER_UNRESTRICTED: { label: "解除限制", tone: "green" },
};

export default function Audit() {
  const [items, setItems] = useState<AuditEventView[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState("");

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const r = await request<AuditEventListData>(`/admin/audit-events?page=${page}&pageSize=${pageSize}`);
        setItems(r.items); setTotal(r.pagination.total);
      } catch (e: any) { message.error(e.message); }
      finally { setLoading(false); }
    })();
  }, [page, pageSize]);

  const visibleItems = useMemo(() => items.filter((item) => !keyword || `${item.eventType} ${item.targetId} ${item.actorId} ${item.metadata.reason || ""}`.toLowerCase().includes(keyword.toLowerCase())), [items, keyword]);
  const traceItems = items.slice(0, 5);

  const columns: ColumnsType<AuditEventView> = [
    { title: "事件", dataIndex: "eventType", width: 155, render: (v) => { const m = EVENT_LABEL[v] || { label: v, tone: "gray" }; return <Tag className={`event-tag ${m.tone}`}>{m.label}</Tag>; } },
    { title: "目标", key: "target", width: 190, render: (_, r) => <code>{r.targetType} · {r.targetId}</code> },
    { title: "原因", dataIndex: ["metadata", "reason"], ellipsis: true, render: (v) => v || "—" },
    { title: "操作者", dataIndex: "actorId", width: 160, ellipsis: true, render: (v) => <code>{v}</code> },
    { title: "时间戳", dataIndex: "createdAt", width: 210, render: (v) => <code>{new Date(v).toLocaleString()}</code> },
    { title: "校验", dataIndex: "id", width: 90, render: (v) => <code className="verify-code">{String(v).slice(0, 4).toUpperCase()}</code> },
  ];

  return (
    <div className="audit-page page-content">
      <section className="trace-card surface">
        <div className="trace-heading"><code>{new Date().toLocaleDateString("zh-CN", { month: "short", day: "2-digit" }).toUpperCase()}</code><h2>今日操作轨迹</h2></div>
        <div className="trace-line">
          {traceItems.length ? traceItems.map((item) => {
            const m = EVENT_LABEL[item.eventType] || { label: item.eventType, tone: "gray" };
            return <div className={`trace-node ${m.tone}`} key={item.id}><time>{new Date(item.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</time><i/><span>{m.label}</span></div>;
          }) : <div className="trace-empty">暂无操作轨迹</div>}
        </div>
      </section>
      <div className="audit-toolbar">
        <div className="search-field"><Icon name="search"/><Input bordered={false} placeholder="事件 / 对象 / 操作者" value={keyword} onChange={(e) => setKeyword(e.target.value)} /></div>
        <span className="filter-chip">全部事件</span><span className="filter-chip active"><i/>当前页</span><code>READ ONLY</code>
      </div>
      <div className="ledger-table audit-table surface">
        <Table rowKey="id" columns={columns} dataSource={visibleItems} loading={loading} pagination={{ current: page, pageSize, total, showSizeChanger: true, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />
        <div className="chain-status">CHAIN VERIFIED · {String(items.length).padStart(2,"0")} EVENTS</div>
      </div>
    </div>
  );
}
