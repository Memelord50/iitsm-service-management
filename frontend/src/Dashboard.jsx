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
  refresh,
  notify,
}) {
  const [data, setData] = useState(null),
    [recent, setRecent] = useState([]);
  useEffect(() => {
    let live = true;
    Promise.all([api("/reports"), api("/incidents?size=6")])
      .then(([d, r]) => {
        if (live) {
          setData(d);
          setRecent(r.items);
        }
      })
      .catch((e) => notify(e.message, true));
    return () => {
      live = false;
    };
  }, [refresh]);
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
