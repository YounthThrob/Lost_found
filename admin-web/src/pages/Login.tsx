import { useState } from "react";
import { Button, Form, Input, message } from "antd";
import Icon from "../components/Icons";
import { request, setToken } from "../api";

interface LoginResult { token: string; }

interface LoginValues {
  username: string;
  password: string;
}

export default function Login({ onLogin }: { onLogin: (t: string) => void }) {
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: LoginValues) => {
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
    <main className="login-page">
      <section className="login-manifesto">
        <div className="brand-block login-brand"><div className="brand-cn">拾光</div><div className="brand-en">OPERATIONS LEDGER</div></div>
        <div className="manifesto-copy">
          <h1>每一次处理，<br />都应留下清晰边界。</h1>
          <div className="manifesto-code">TRACEABLE / CAREFUL / HUMAN</div>
          <div className="manifesto-rule" />
          <p>非官方校园互助平台</p><small>内部运营工具 · 仅限授权人员</small>
        </div>
        <div className="route-graphic" aria-hidden="true">
          <i className="route-line one"/><i className="route-line two"/>
          {[1,2,3,4,5,6].map((n) => <span key={n} className={`route-point point-${n}`}>{`0${n}`}</span>)}
        </div>
        <div className="login-private">PRIVATE SYSTEM / 2026</div>
      </section>

      <section className="login-stage">
        <div className="paper-orbit" />
        <div className="login-card">
          <div className="page-index">ACCESS / 01</div>
          <h2>运营后台登录</h2>
          <p className="login-subtitle">身份校验后进入内部工作台</p>
          <div className="form-rule" />
          <Form className="login-form" layout="vertical" onFinish={onFinish} initialValues={{ username: "admin", password: "admin123" }}>
            <Form.Item name="username" label="运营账号" rules={[{ required: true, message: "请输入运营账号" }]}>
              <Input autoComplete="username" prefix={<span className="credential-dot" />} placeholder="admin" />
            </Form.Item>
            <Form.Item name="password" label="登录密码" rules={[{ required: true, message: "请输入登录密码" }]}>
              <Input.Password autoComplete="current-password" prefix={<span className="credential-dot" />} placeholder="admin123" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>
              进入运营工作台 <Icon name="arrow" />
            </Button>
          </Form>
          <div className="access-note">
            <Icon name="shield" /><div><strong>权限与边界</strong><p>操作将被记录；请勿共享凭证或代替他人执行。</p></div>
          </div>
          <div className="secure-caption">SECURE SESSION / INTERNAL ONLY</div>
        </div>
        <div className="login-product">LOST / FOUND COMMONS</div>
      </section>
    </main>
  );
}
