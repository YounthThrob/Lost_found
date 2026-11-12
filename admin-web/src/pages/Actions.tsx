import { useState } from "react";
import { Button, Card, Form, Input, Space, Typography, message } from "antd";
import { request } from "../api";
import type { ItemPostView, UserStatusView } from "../types";

interface FormValues {
  id: string;
  reason: string;
}

export default function Actions() {
  const [postLoading, setPostLoading] = useState(false);
  const [userLoading, setUserLoading] = useState(false);
  const [postForm] = Form.useForm();
  const [userForm] = Form.useForm();

  const onRemovePost = async (values: FormValues) => {
    setPostLoading(true);
    try {
      await request<ItemPostView>(`/admin/posts/${values.id}/remove`, {
        method: "POST",
        body: { reason: values.reason },
      });
      message.success("已下架该信息");
      postForm.resetFields();
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setPostLoading(false);
    }
  };

  const onUserAction = async (action: "restrict" | "unrestrict", values: FormValues) => {
    setUserLoading(true);
    try {
      const r = await request<UserStatusView>(`/admin/users/${values.id}/${action}`, {
        method: "POST",
        body: { reason: values.reason },
      });
      message.success(action === "restrict" ? "已限制账户" : "已解除限制");
      userForm.resetFields();
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      void r;
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setUserLoading(false);
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: "100%", maxWidth: 720 }}>
      <Card title="下架信息">
        <Form form={postForm} layout="vertical" onFinish={onRemovePost}>
          <Form.Item name="id" label="信息 ID" rules={[{ required: true, message: "请输入信息 ID" }]}>
            <Input placeholder="帖子 ID（UUID）" />
          </Form.Item>
          <Form.Item name="reason" label="处理原因" rules={[{ required: true, message: "请填写原因" }]}>
            <Input.TextArea rows={2} maxLength={200} placeholder="下架原因（≤200 字）" />
          </Form.Item>
          <Button type="primary" danger htmlType="submit" loading={postLoading}>
            确认下架
          </Button>
        </Form>
      </Card>

      <Card title="用户限制 / 解除">
        <Typography.Paragraph type="secondary">
          限制后用户无法执行发布、认领、留言等受限操作；解除后恢复正常。
        </Typography.Paragraph>
        <Form form={userForm} layout="vertical" onFinish={(v) => onUserAction("restrict", v)}>
          <Form.Item name="id" label="用户 ID" rules={[{ required: true, message: "请输入用户 ID" }]}>
            <Input placeholder="用户 ID（UUID）" />
          </Form.Item>
          <Form.Item name="reason" label="处理原因" rules={[{ required: true, message: "请填写原因" }]}>
            <Input.TextArea rows={2} maxLength={200} placeholder="处理原因（≤200 字）" />
          </Form.Item>
          <Space>
            <Button type="primary" danger htmlType="submit" loading={userLoading}>
              限制账户
            </Button>
            <Button
              loading={userLoading}
              onClick={() => {
                userForm.validateFields().then((v) => onUserAction("unrestrict", v)).catch(() => {});
              }}
            >
              解除限制
            </Button>
          </Space>
        </Form>
      </Card>
    </Space>
  );
}
