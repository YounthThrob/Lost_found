import { useState } from "react";
import type { ReactNode } from "react";
import { Button, Form, Input, message } from "antd";
import { request } from "../api";
import type { ItemPostView, UserStatusView } from "../types";

interface FormValues { id: string; reason: string; }

export default function Actions() {
  const [postLoading, setPostLoading] = useState(false);
  const [userLoading, setUserLoading] = useState(false);
  const [postForm] = Form.useForm();
  const [userForm] = Form.useForm();

  const onRemovePost = async (values: FormValues) => {
    setPostLoading(true);
    try {
      await request<ItemPostView>(`/admin/posts/${values.id}/remove`, { method: "POST", body: { reason: values.reason } });
      message.success("已下架该信息"); postForm.resetFields();
    } catch (e: any) { message.error(e.message); }
    finally { setPostLoading(false); }
  };

  const onUserAction = async (action: "restrict" | "unrestrict", values: FormValues) => {
    setUserLoading(true);
    try {
      await request<UserStatusView>(`/admin/users/${values.id}/${action}`, { method: "POST", body: { reason: values.reason } });
      message.success(action === "restrict" ? "已限制账户" : "已解除限制"); userForm.resetFields();
    } catch (e: any) { message.error(e.message); }
    finally { setUserLoading(false); }
  };

  return (
    <div className="actions-page page-content">
      <div className="trace-banner"><i/><span>所有处理都会写入审计日志。高风险动作必须填写理由并复核目标 ID。</span><code>TRACE ENABLED</code></div>
      <div className="action-grid">
        <ActionPanel code="POST / REMOVE" title="下架信息" note="输入信息 ID 与处理依据，下架后不再公开展示。">
          <Form form={postForm} layout="vertical" onFinish={onRemovePost}>
            <Form.Item name="id" label="目标标识" rules={[{ required: true, message: "请输入信息 ID" }]}><Input placeholder="输入 UUID / 精确匹配" /></Form.Item>
            <Form.Item name="reason" label="处理原因" rules={[{ required: true, message: "请填写原因" }]}><Input.TextArea rows={4} maxLength={200} showCount placeholder="记录事实、影响范围与处理依据…" /></Form.Item>
            <div className="action-buttons"><Button type="primary" danger htmlType="submit" loading={postLoading}>确认下架</Button><Button onClick={() => postForm.resetFields()}>清空</Button></div>
          </Form>
        </ActionPanel>
        <ActionPanel code="USER / ACCESS" title="用户限制" note="限制发布、认领与留言；解除操作同样留痕。">
          <Form form={userForm} layout="vertical" onFinish={(v) => onUserAction("restrict", v)}>
            <Form.Item name="id" label="目标标识" rules={[{ required: true, message: "请输入用户 ID" }]}><Input placeholder="输入 UUID / 精确匹配" /></Form.Item>
            <Form.Item name="reason" label="处理原因" rules={[{ required: true, message: "请填写原因" }]}><Input.TextArea rows={4} maxLength={200} showCount placeholder="记录事实、影响范围与处理依据…" /></Form.Item>
            <div className="action-buttons"><Button type="primary" danger htmlType="submit" loading={userLoading}>限制账户</Button><Button className="outline-green" loading={userLoading} onClick={() => userForm.validateFields().then((v) => onUserAction("unrestrict", v)).catch(() => {})}>解除限制</Button></div>
          </Form>
        </ActionPanel>
      </div>
      <div className="operator-note"><code>OPERATOR SAFETY NOTE</code><p>先核对对象，再执行动作；系统记录结果，不替代人工判断。</p></div>
    </div>
  );
}

function ActionPanel({ code, title, note, children }: { code: string; title: string; note: string; children: ReactNode }) {
  return <section className="action-panel surface"><div className="action-rail">{[0,1,2,3,4,5,6,7,8,9,10].map((n) => <i key={n}/>)}</div><code className="panel-code">{code}</code><h2>{title}</h2><p className="panel-note">{note}</p><div className="panel-rule"/>{children}<div className="panel-requirement">REQUIRES / REASON + TARGET</div></section>;
}
