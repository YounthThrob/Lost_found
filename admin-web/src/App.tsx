import { useState } from "react";
import Login from "./pages/Login";
import Reports from "./pages/Reports";
import Actions from "./pages/Actions";
import Audit from "./pages/Audit";
import Icon from "./components/Icons";
import { clearToken, getToken } from "./api";

type TabKey = "reports" | "actions" | "audit";

const MENU: Array<{ key: TabKey; label: string; caption: string; icon: "report" | "action" | "audit" }> = [
  { key: "reports", label: "举报处理", caption: "REPORT QUEUE", icon: "report" },
  { key: "actions", label: "用户与信息处理", caption: "CONTROL DESK", icon: "action" },
  { key: "audit", label: "审计日志", caption: "AUDIT TRACE", icon: "audit" },
];

const PAGE_META: Record<TabKey, { title: string; subtitle: string; index: string }> = {
  reports: { title: "举报处理", subtitle: "REPORT INTAKE / TRIAGE / RESOLUTION", index: "QUEUE / 01" },
  actions: { title: "用户与信息处理", subtitle: "CONTROLLED ACTIONS / TWO-PART CONFIRMATION", index: "CONTROL DESK / 02" },
  audit: { title: "审计日志", subtitle: "IMMUTABLE OPERATOR TRACE / READ ONLY", index: "AUDIT TRACE / 03" },
};

export default function App() {
  const [token, setToken] = useState<string>(getToken());
  const [tab, setTab] = useState<TabKey>("reports");

  if (!token) return <Login onLogin={setToken} />;

  const logout = () => {
    clearToken();
    setToken("");
  };

  const meta = PAGE_META[tab];
  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="sidebar-accent" />
        <div className="brand-block">
          <div className="brand-cn">拾光</div>
          <div className="brand-en">OPERATIONS LEDGER</div>
        </div>
        <div className="sidebar-rule" />
        <nav className="sidebar-nav" aria-label="运营后台导航">
          {MENU.map((item) => (
            <button key={item.key} className={`nav-item ${tab === item.key ? "active" : ""}`} onClick={() => setTab(item.key)}>
              <Icon name={item.icon} className="nav-icon" />
              <span><strong>{item.label}</strong><small>{item.caption}</small></span>
            </button>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="micro-label">PRIVATE / INTERNAL</div>
          <div className="sidebar-note">非官方平台运营工具</div>
          <div className="system-status"><i /> SYSTEM ONLINE</div>
        </div>
      </aside>

      <main className="admin-main">
        <header className="admin-header">
          <div><h1>{meta.title}</h1><div className="header-subtitle">{meta.subtitle}</div></div>
          <div className="operator-area">
            <span className="operator-code">OPERATOR / 01</span>
            <span className="operator-avatar">OP</span>
            <button className="logout-button" onClick={logout} title="退出登录"><Icon name="logout" /></button>
          </div>
        </header>
        <section className="workspace">
          <div className="page-index">{meta.index}</div>
          {tab === "reports" && <Reports />}
          {tab === "actions" && <Actions />}
          {tab === "audit" && <Audit />}
        </section>
      </main>
    </div>
  );
}
