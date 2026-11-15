import { useState } from "react";
import { Button, Card, Form, Input, Typography, message } from "antd";
import { request, setToken } from "../api";

interface LoginResult {
  token: string;
}

export default function Login({ onLogin }: { onLogin: (t: string) => void }) {
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const data = await request<LoginResult>("/session/operator", {
        method: "POST",
        body: { username: values.username, password: values.password },
      });
      setToken(data.token);
      message.success("登录成功");
      onLogin(data.token);
    } catch (e: any) {
      message.error(e.message || "登录失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        height: "100vh",
        background: "#f0f2f5",
      }}
    >
      <Card title="运营后台登录" style={{ width: 380 }}>
        <Typography.Paragraph type="secondary">
          本后台仅限运营人员使用（非官方平台内部工具）。开发环境默认账号 admin / admin123，生产请通过环境变量覆盖。
        </Typography.Paragraph>
        <Form layout="vertical" onFinish={onFinish}>
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: "请输入用户名" }]}>
            <Input placeholder="admin" autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: "请输入密码" }]}>
            <Input.Password placeholder="admin123" autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            登录
          </Button>
        </Form>
      </Card>
    </div>
  );
}
