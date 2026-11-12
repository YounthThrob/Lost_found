import { useState } from "react";
import { Button, Card, Form, Input, Typography, message } from "antd";
import { request, setToken } from "../api";
import { OPERATOR_CODE } from "../config";

interface LoginResult {
  token: string;
  agreementAccepted: boolean;
}

export default function Login({ onLogin }: { onLogin: (t: string) => void }) {
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: { code: string }) => {
    setLoading(true);
    try {
      const data = await request<LoginResult>("/session/wechat", {
        method: "POST",
        body: { code: values.code },
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
          本后台仅限运营人员使用（非官方平台内部工具）。开发环境使用 mock 凭证 operator-code。
        </Typography.Paragraph>
        <Form layout="vertical" onFinish={onFinish} initialValues={{ code: OPERATOR_CODE }}>
          <Form.Item name="code" label="登录凭证 code" rules={[{ required: true, message: "请输入 code" }]}>
            <Input placeholder="operator-code" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            登录
          </Button>
        </Form>
      </Card>
    </div>
  );
}
