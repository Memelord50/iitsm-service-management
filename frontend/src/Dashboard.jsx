import React, { useEffect, useState } from "react";
import {
  ArrowUpRight,
  Activity,
  CheckCircle2,
  Clock3,
  TriangleAlert,
} from "lucide-react";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import { api, date, duration, human } from "./api";
import { Badge, Empty, Loading, Table } from "./components";
export default function Dashboard({
  user,
  onOpen,
  onCreate,
  onIncidents,
  onNavigate,
  refresh,
  notify,
}) {
  const [data, setData] = useState(null),
    [recent, setRecent] = useState([]);
  const [loadError, setLoadError] = useState("");
  const [retry, setRetry] = useState(0);
  const retryLoad = () => { setLoadError(""); setRetry(n => n + 1); };
  useEffect(() => {
    let live = true;
    Promise.all([api("/reports"), api("/incidents?size=6")])
      .then(([d, r]) => {
        if (live) {
          setData(d);
          setRecent(r.items);
        }
      })
      .catch((e) => { if (live) setLoadError(e.message); });
    return () => {
      live = false;
    };
  }, [refresh, retry]);
  if (loadError) return <section className="panel padded" role="alert">
    <h2>Could not load this view</h2><p>{loadError}</p>
    <button className="secondary" onClick={retryLoad}>Try again</button>
  </section>;
  if (!data) return <Loading />;
  const stats = [
    [
      "Active incidents",
      data.active,
      Activity,
      "Awaiting a verified resolution",
    ],
    [
      "Resolved incidents",
      data.resolved,
      CheckCircle2,
      "Resolved or successfully closed",
    ],
    [
      "SLA compliance",
      data.slaCompliance === null ? "—" : data.slaCompliance + "%",
      Clock3,
      "Across completed SLA cycles",
    ],
    [
      "SLA breaches",
      data.breaches,
      TriangleAlert,
      "Historical breached cycles",
    ],
  ];
  return (
    <>
      <div className="page-heading">
        <div>
          <div className="eyebrow">
            {user.role === "ADMIN"
              ? "OPERATIONS OVERVIEW"
              : user.role === "TECHNICIAN"
                ? "YOUR SERVICE QUEUE"
                : "YOUR SUPPORT AT A GLANCE"}
          </div>
          <h1>Good to see you, {user.name.split(" ")[0]}.</h1>
          <p className="muted">Here’s where your service operations stand.</p>
        </div>
        {user.role === "EMPLOYEE" && (
          <button className="primary" onClick={onCreate}>
            Report an incident <ArrowUpRight size={17} />
          </button>
        )}
      </div>
      <div className="metrics">
        {stats.map(([label, value, Icon, sub]) => (
          <article className="metric" key={label}>
            <div>
              <span>{label}</span>
              <Icon size={18} />
            </div>
            <strong>{value}</strong>
            <small>{sub}</small>
          </article>
        ))}
      </div>
      {user.role !== "EMPLOYEE" && (
        <section className="panel padded dashboard-queue">
          <h2>{user.role === "TECHNICIAN" ? "Assigned queue" : "Operations queue"}</h2>
          <div className="queue-statuses">
            {["NEW", "ASSIGNED", "IN_PROGRESS", "WAITING_FOR_EMPLOYEE", "RESOLVED"]
              .filter(status => user.role === "ADMIN" || status !== "NEW")
              .map(status => (
                <div key={status}>
                  <span>{human(status)}</span>
                  <strong>{data.byStatus.find(row => row.name === status)?.value ?? 0}</strong>
                </div>
              ))}
          </div>
          <div className="inline queue-links">
            <button className="secondary" onClick={() => onNavigate("Technicians")}>Technician workload</button>
            {user.role === "ADMIN" && <>
              <button className="secondary" onClick={() => onNavigate("SLA monitor")}>SLA monitor</button>
              <button className="secondary" onClick={() => onNavigate("Reports")}>Reports</button>
            </>}
          </div>
        </section>
      )}
      <div className="dashboard-grid">
        <section className="panel">
          <div className="panel-title">
            <div>
              <h2>Incident activity</h2>
              <p>Reported incidents by day · UTC</p>
            </div>
            <span className="pill">{data.total} total</span>
          </div>
          {data.trends.length ? (
            <div className="chart">
              <ResponsiveContainer width="100%" height={230}>
                <BarChart data={data.trends}>
                  <CartesianGrid
                    strokeDasharray="3 5"
                    vertical={false}
                    stroke="#edf0f2"
                  />
                  <XAxis
                    dataKey="name"
                    tick={{ fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    allowDecimals={false}
                    tick={{ fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <Tooltip />
                  <Bar
                    dataKey="value"
                    name="Incidents"
                    fill="#087d6d"
                    radius={[5, 5, 0, 0]}
                    maxBarSize={46}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <Empty>
              Activity appears when your first incident is reported.
            </Empty>
          )}
        </section>
        <section className="panel">
          <div className="panel-title">
            <div>
              <h2>Priority distribution</h2>
              <p>What needs attention</p>
            </div>
          </div>
          <div className="distribution">
            {["CRITICAL", "HIGH", "MEDIUM", "LOW"].map((p) => {
              const n = data.byPriority.find((x) => x.name === p)?.value || 0;
              return (
                <div key={p}>
                  <div>
                    <Badge value={p} />
                    <strong>{n}</strong>
                  </div>
                  <div className="track">
                    <i
                      className={p.toLowerCase()}
                      style={{
                        width: data.total ? `${(n / data.total) * 100}%` : "0%",
                      }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      </div>
      <section className="panel">
        <div className="panel-title">
          <div>
            <h2>Recent incidents</h2>
            <p>Your latest service requests and updates</p>
          </div>
          <button className="text-button" onClick={onIncidents}>
            View all incidents <ArrowUpRight size={16} />
          </button>
        </div>
        <Table
          rows={recent}
          onRow={onOpen}
          columns={[
            {
              key: "reference",
              label: "INCIDENT",
              render: (r) => (
                <>
                  <span className="reference">{r.reference}</span>
                  <strong className="cell-title">{r.title}</strong>
                </>
              ),
            },
            {
              key: "priority",
              label: "PRIORITY",
              render: (r) => <Badge value={r.priority} />,
            },
            {
              key: "status",
              label: "STATUS",
              render: (r) => <Badge value={r.status} />,
            },
            { key: "technician", label: "ASSIGNED TO" },
            {
              key: "createdAt",
              label: "REPORTED",
              render: (r) => date(r.createdAt),
            },
          ]}
        />
      </section>
      <div className="sub-metrics">
        <span>
          Average resolution{" "}
          <strong>{duration(data.averageResolutionSeconds)}</strong>
        </span>
        <span>
          Employee feedback{" "}
          <strong>
            {data.averageFeedback === null
              ? "No feedback yet"
              : data.averageFeedback.toFixed(1) + " / 5"}
          </strong>
        </span>
        <span>All metrics reflect your permitted incident scope.</span>
      </div>
    </>
  );
}
