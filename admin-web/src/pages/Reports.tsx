import { useEffect, useMemo, useState } from "react";
import { Button, Form, Input, Modal, Table, Tag, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import Icon from "../components/Icons";
import { request } from "../api";
import type { ItemPostView, ReportListData, ReportView } from "../types";

type QueueFilter = "ALL" | "PENDING" | "RISK";

export default function Reports() {
  const [items, setItems] = useState<ReportView[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(false);
  const [target, setTarget] = useState<ReportView | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [filter, setFilter] = useState<QueueFilter>("ALL");
  const [form] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const r = await request<ReportListData>(`/admin/reports?page=${page}&pageSize=${pageSize}`);
      setItems(r.items); setTotal(r.pagination.total);
    } catch (e: any) { message.error(e.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [page, pageSize]);

  const isRisk = (r: ReportView) => /骚扰|冒用|诈骗|敏感|威胁/i.test(`${r.reason} ${r.description || ""}`);
  const isPending = (r: ReportView) => !/RESOLVED|CLOSED|REJECTED/i.test(r.status);
  const visibleItems = useMemo(() => items.filter((item) => {
    const hit = !keyword || `${item.targetId} ${item.reporterId} ${item.reason} ${item.description || ""}`.toLowerCase().includes(keyword.toLowerCase());
    return hit && (filter === "ALL" || (filter === "PENDING" && isPending(item)) || (filter === "RISK" && isRisk(item)));
  }), [items, keyword, filter]);

  const pending = items.filter(isPending).length;
  const risk = items.filter(isRisk).length;
  const completed = items.filter((r) => !isPending(r)).length;
  const postReports = items.filter((r) => r.targetType === "POST").length;

  const onRemove = async (values: { reason: string }) => {
    if (!target) return;
    setSubmitting(true);
    try {
      await request<ItemPostView>(`/admin/posts/${target.targetId}/remove`, { method: "POST", body: { reason: values.reason } });
      message.success("已下架该信息"); setTarget(null); form.resetFields(); load();
    } catch (e: any) { message.error(e.message); }
    finally { setSubmitting(false); }
  };

  const columns: ColumnsType<ReportView> = [
    { title: "对象", dataIndex: "targetType", width: 135, render: (v) => <span className="object-cell">{v === "POST" ? "信息" : "用户"} · <code>{v}</code></span> },
    { title: "举报分类", dataIndex: "reason", width: 150 },
    { title: "说明 / 证据摘要", dataIndex: "description", ellipsis: true, render: (v) => v || "—" },
    { title: "状态", dataIndex: "status", width: 112, render: (v, r) => <Tag className={`status-tag ${isRisk(r) ? "risk" : isPending(r) ? "pending" : "done"}`}>{isRisk(r) ? "高风险" : isPending(r) ? "待处理" : v}</Tag> },
    { title: "举报人", dataIndex: "reporterId", width: 150, ellipsis: true, render: (v) => <code>{v}</code> },
    { title: "时间", dataIndex: "createdAt", width: 170, render: (v) => <code>{new Date(v).toLocaleString()}</code> },
    { title: "操作", key: "action", width: 110, render: (_, record) => record.targetType === "POST" ? <Button className="table-action" size="small" onClick={() => setTarget(record)}>审查</Button> : <span className="table-dash">—</span> },
  ];

  return (
    <div className="reports-page page-content">
      <div className="stat-grid">
        <Stat label="全部举报" value={total} detail={`PAGE ${page}`} tone="amber" />
        <Stat label="当前待处理" value={pending} detail="PENDING" tone="red" />
        <Stat label="当前高风险" value={risk} detail="PRIORITY" tone="green" />
        <Stat label="信息类举报" value={postReports} detail={`${completed} CLOSED`} tone="slate" />
      </div>
      <div className="queue-toolbar surface">
        <div className="search-field"><Icon name="search"/><Input bordered={false} placeholder="搜索对象 ID / 举报人" value={keyword} onChange={(e) => setKeyword(e.target.value)} /></div>
        <div className="queue-filters">
          <button className={filter === "ALL" ? "active" : ""} onClick={() => setFilter("ALL")}>全部对象</button>
          <button className={`pending ${filter === "PENDING" ? "active" : ""}`} onClick={() => setFilter("PENDING")}><i/>待处理</button>
          <button className={`risk ${filter === "RISK" ? "active" : ""}`} onClick={() => setFilter("RISK")}><i/>高风险</button>
        </div>
        <Button className="refresh-button" icon={<Icon name="refresh"/>} onClick={load} loading={loading}>刷新队列</Button>
      </div>
      <div className="ledger-table surface">
        <Table rowKey="id" columns={columns} dataSource={visibleItems} loading={loading} pagination={{ current: page, pageSize, total, showSizeChanger: true, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />
      </div>
      <Modal className="ledger-modal" title={<><span className="modal-code">CONTROL / REMOVE</span><strong>下架违规信息</strong></>} open={!!target} onCancel={() => setTarget(null)} onOk={() => form.submit()} confirmLoading={submitting} okText="确认下架" cancelText="取消">
        <div className="modal-target">目标信息 <code>{target?.targetId}</code></div>
        <Form form={form} layout="vertical" onFinish={onRemove}><Form.Item name="reason" label="处理原因" rules={[{ required: true, message: "请填写原因" }]}><Input.TextArea rows={4} maxLength={200} showCount placeholder="记录事实、影响范围与处理依据…" /></Form.Item></Form>
      </Modal>
    </div>
  );
}

function Stat({ label, value, detail, tone }: { label: string; value: number; detail: string; tone: string }) {
  return <div className={`stat-card surface ${tone}`}><div className="stat-label"><i/>{label}</div><div className="stat-row"><strong>{String(value).padStart(2,"0")}</strong><span>{detail}</span></div></div>;
}
