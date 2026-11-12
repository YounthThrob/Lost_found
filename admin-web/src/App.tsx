import { useState } from "react";
import { Layout, Menu, Typography } from "antd";
import Login from "./pages/Login";
import Reports from "./pages/Reports";
import Actions from "./pages/Actions";
import Audit from "./pages/Audit";
import { clearToken, getToken } from "./api";

const { Header, Sider, Content } = Layout;

const MENU = [
  { key: "reports", label: "举报处理" },
  { key: "actions", label: "用户与信息处理" },
  { key: "audit", label: "审计日志" },
];

export default function App() {
  const [token, setToken] = useState<string>(getToken());
  const [tab, setTab] = useState("reports");

  if (!token) {
    return <Login onLogin={setToken} />;
  }

  const logout = () => {
    clearToken();
    setToken("");
  };

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider breakpoint="lg" collapsedWidth="0">
        <div style={{ color: "#fff", padding: "16px", fontWeight: 600, fontSize: 16 }}>
          运营后台
        </div>
        <div style={{ color: "rgba(255,255,255,0.6)", padding: "0 16px 16px", fontSize: 12 }}>
          非官方平台内部工具
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[tab]}
          items={MENU}
          onClick={(e) => setTab(e.key)}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: "#fff",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            paddingInline: 24,
          }}
        >
          <Typography.Title level={4} style={{ margin: 0 }}>
            校园失物招领 · 运营管理
          </Typography.Title>
          <a onClick={logout}>退出登录</a>
        </Header>
        <Content style={{ margin: 24 }}>
          {tab === "reports" && <Reports />}
          {tab === "actions" && <Actions />}
          {tab === "audit" && <Audit />}
        </Content>
      </Layout>
    </Layout>
  );
}
