import { useEffect, useState } from "react";
import { Button, Form, Input, Modal, Table, Tag, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { request } from "../api";
import type { ItemPostView, ReportListData, ReportView } from "../types";

export default function Reports() {
  const [items, setItems] = useState<ReportView[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(false);
  const [target, setTarget] = useState<ReportView | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const r = await request<ReportListData>(`/admin/reports?page=${page}&pageSize=${pageSize}`);
      setItems(r.items);
      setTotal(r.pagination.total);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, pageSize]);

  const onRemove = async (values: { reason: string }) => {
    if (!target) return;
    setSubmitting(true);
    try {
      await request<ItemPostView>(`/admin/posts/${target.targetId}/remove`, {
        method: "POST",
        body: { reason: values.reason },
      });
      message.success("已下架该信息");
      setTarget(null);
      form.resetFields();
      load();
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<ReportView> = [
    { title: "对象类型", dataIndex: "targetType", width: 100, render: (v) => (v === "POST" ? <Tag color="blue">信息</Tag> : <Tag color="orange">用户</Tag>) },
    { title: "对象 ID", dataIndex: "targetId", ellipsis: true },
    { title: "举报分类", dataIndex: "reason", width: 120 },
    { title: "说明", dataIndex: "description", ellipsis: true, render: (v) => v || "—" },
    { title: "状态", dataIndex: "status", width: 100, render: (v) => <Tag>{v}</Tag> },
    { title: "举报人", dataIndex: "reporterId", ellipsis: true },
    { title: "时间", dataIndex: "createdAt", width: 180, render: (v) => new Date(v).toLocaleString() },
    {
      title: "操作",
      key: "action",
      width: 140,
      render: (_, record) =>
        record.targetType === "POST" ? (
          <Button size="small" danger onClick={() => setTarget(record)}>
            下架该信息
          </Button>
        ) : (
          "—"
        ),
    },
  ];

  return (
    <>
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
      <Modal
        title="下架违规信息"
        open={!!target}
        onCancel={() => setTarget(null)}
        onOk={() => form.submit()}
        confirmLoading={submitting}
        okText="确认下架"
      >
        <p>
          目标信息：<b>{target?.targetId}</b>
        </p>
        <Form form={form} layout="vertical" onFinish={onRemove}>
          <Form.Item name="reason" label="处理原因" rules={[{ required: true, message: "请填写原因" }]}>
            <Input.TextArea rows={3} maxLength={200} placeholder="下架原因（≤200 字）" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
