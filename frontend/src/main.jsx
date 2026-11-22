import React, { useState, useEffect } from "react";
import { createRoot } from "react-dom/client";
import {
  LayoutDashboard,
  Ticket,
  Users,
  Layers,
  Settings2,
  Clock3,
  ShieldCheck,
  BarChart3,
  Bell,
  LogOut,
  ChevronRight,
  Menu,
  Mail,
  CheckCircle2,
  X,
} from "lucide-react";
import Auth from "./Auth";
import ThemeToggle, { useTheme } from "./ThemeToggle";
import Dashboard from "./Dashboard";
import { IncidentList, CreateIncident } from "./Incidents";
import IncidentDetail from "./IncidentDetail";
import Admin from "./Admin";
import { api, setToken, human } from "./api";
import "./style.css";
function App() {
  const theme = useTheme();
  const [user, setUser] = useState(null),
    [page, setPage] = useState("Overview"),
    [selected, setSelected] = useState(null),
    [create, setCreate] = useState(false),
    [catalog, setCatalog] = useState([]),
    [toast, setToast] = useState(null),
    [refresh, setRefresh] = useState(0),
    [mobile, setMobile] = useState(false);
  useEffect(() => {
    const expire = () => {
      setUser(null);
      setSelected(null);
      setCreate(false);
    };
    window.addEventListener("iitsm-session-expired", expire);
    return () => window.removeEventListener("iitsm-session-expired", expire);
  }, []);
  function notify(message, error = false) {
    setToast({ message, error });
  }
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 8000);
      return () => clearTimeout(timer);
    }
  }, [toast]);
  function loadCatalog() {
    api("/catalog")
      .then(setCatalog)
      .catch((e) => notify(e.message, true));
  }
  useEffect(() => {
    if (user) loadCatalog();
  }, [user]);
  if (!user)
    return (
      <Auth themeToggle={<ThemeToggle {...theme} />}
        onLogin={(u) => {
          setUser(u);
          setPage("Overview");
          setSelected(null);
        }}
      />
    );
  function go(p) {
    setPage(p);
    setSelected(null);
    setMobile(false);
  }
  function open(i) {
    setSelected(i.id);
    setPage("Incidents");
  }
  let nav = [
    ["Overview", LayoutDashboard],
    ["Incidents", Ticket],
    ["Notifications", Bell],
  ];
  if (user.role !== "EMPLOYEE") nav.push(["Technicians", Users]);
  let admin = [
    ["Users", Users],
    ["Catalog", Layers],
    ["SLA monitor", Clock3],
    ["SLA policies", ShieldCheck],
    ["Configuration", Settings2],
    ["Reports", BarChart3],
    ["Audit log", ShieldCheck],
    ["Email delivery", Mail],
  ];
  return (
    <div className="app">
      <aside className={"sidebar " + (mobile ? "open" : "")}>
        <div className="brand">
          <span className="brand-icon">
            <Layers size={22} />
          </span>
          I-ITSM<span className="brand-dot">/</span>
        </div>
        <div className="workspace-label">
          <span className="workspace-avatar">IT</span>
          <div>
            <strong>Service workspace</strong>
            <small>{human(user.role)} portal</small>
          </div>
        </div>
        <span className="nav-label">WORKSPACE</span>
        <nav>
          {nav.map(([p, Icon]) => (
            <button
              key={p}
              className={page === p ? "selected" : ""}
              onClick={() => go(p)}
            >
              <Icon size={18} />
              {p}
              <ChevronRight size={13} />
            </button>
          ))}
        </nav>
        {user.role === "ADMIN" && (
          <>
            <span className="nav-label">MANAGEMENT</span>
            <nav>
              {admin.map(([p, Icon]) => (
                <button
                  key={p}
                  className={page === p ? "selected" : ""}
                  onClick={() => go(p)}
                >
                  <Icon size={17} />
                  {p}
                </button>
              ))}
            </nav>
          </>
        )}
        <button className="secondary" onClick={async () => {
          try { const result = await api("/auth/change-password", "POST", {}); notify(result.message); }
          catch (e) { notify(e.message, true); }
        }}>Change password by email</button>
        <div className="sidebar-bottom">
          <div className="avatar">
            {user.name
              .split(" ")
              .map((x) => x[0])
              .join("")}
          </div>
          <div>
            <strong>{user.name}</strong>
            <small>{human(user.role)}</small>
          </div>
          <button
            aria-label="Sign out"
            onClick={() => {
              setToken(null);
              setUser(null);
            }}
          >
            <LogOut size={17} />
          </button>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <div className="inline">
            <button
              aria-label="Toggle navigation"
              className="mobile-menu icon-button"
              onClick={() => setMobile(!mobile)}
            >
              <Menu />
            </button>
            <span>Workspace</span>
            <ChevronRight size={14} />
            <strong>{page}</strong>
          </div>
          <div className="inline">
            <span className="live-indicator">Connected workspace</span>
            <ThemeToggle {...theme} />
            <button
              aria-label="Notifications"
              className="icon-button"
              onClick={() => go("Notifications")}
            >
              <Bell size={19} />
            </button>
            <span className="avatar small">{user.name[0]}</span>
          </div>
        </header>
        <main className="content">
          {selected ? (
            <IncidentDetail
              id={selected}
              user={user}
              onBack={() => setSelected(null)}
              notify={notify}
              onChange={() => setRefresh((x) => x + 1)}
            />
          ) : page === "Overview" ? (
            <Dashboard
              user={user}
              onOpen={open}
              onCreate={() => setCreate(true)}
              onIncidents={() => go("Incidents")}
              onNavigate={go}
              refresh={refresh}
              notify={notify}
            />
          ) : page === "Incidents" ? (
            <IncidentList
              user={user}
              catalog={catalog}
              onOpen={open}
              onCreate={() => setCreate(true)}
              notify={notify}
              refresh={refresh}
            />
          ) : (
            <Admin
              key={page}
              page={page}
              user={user}
              catalog={catalog}
              notify={notify}
              onCatalog={loadCatalog}
              onOpen={open}
            />
          )}
        </main>
        <footer className="app-footer">
          I-ITSM <span>Clarity at every step.</span>
          <small>
            Times shown in {Intl.DateTimeFormat().resolvedOptions().timeZone}
          </small>
        </footer>
      </div>
      {create && (
        <CreateIncident
          catalog={catalog}
          onClose={() => setCreate(false)}
          notify={notify}
          onCreated={(i, { uploaded, attachmentError } = {}) => {
            setCreate(false);
            setRefresh((x) => x + 1);
            open(i);
            if (attachmentError) notify("Incident reported. Attachment upload failed: " + attachmentError, true);
            else notify(uploaded ? "Incident reported and attachment uploaded" : "Incident reported successfully");
          }}
        />
      )}
      {toast && (
        <div
          role={toast.error ? "alert" : "status"}
          className={"toast " + (toast.error ? "error" : "")}
        >
          <CheckCircle2 size={18} />
          {toast.message}
          <button aria-label="Dismiss message" onClick={() => setToast(null)}>
            <X size={16} />
          </button>
        </div>
      )}
    </div>
  );
}
createRoot(document.getElementById("root")).render(<App />);
