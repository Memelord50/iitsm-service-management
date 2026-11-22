import React, { useState, useEffect } from "react";
import { Plus, Settings2, ArrowUpRight } from "lucide-react";
import { api, date, human, duration } from "./api";
import {
  Field,
  Table,
  Loading,
  Modal,
  Badge,
  Pager,
  Empty,
} from "./components";
export default function Admin({
  page,
  user,
  catalog,
  notify,
  onCatalog,
  onOpen,
}) {
  const [data, setData] = useState(null),
    [modal, setModal] = useState(null),
    [index, setIndex] = useState(0),
    [busy, setBusy] = useState(false);
  const routes = {
    Users: "/admin/users",
    Catalog: "/catalog",
    Configuration: "/admin/settings",
    "SLA policies": "/admin/sla-policies",
    "SLA monitor": "/admin/sla",
    "Audit log": "/admin/audit",
    Notifications: "/notifications",
    Technicians: "/technicians",
    "Email delivery": "/admin/delivery",
    Reports: "/reports",
  };
  async function load() {
    try {
      setData(await api(routes[page] + "?page=" + index));
    } catch (e) {
      setLoadError(e.message);
    }
  }
  const [loadError, setLoadError] = useState("");
  const [retry, setRetry] = useState(0);
  const retryLoad = () => { setLoadError(""); setRetry(n => n + 1); };
  useEffect(() => {
    setData(null);
    setIndex(0);
  }, [page]);
  useEffect(() => {
    load();
  }, [page, index, retry]);
  async function save(path, method, body) {
    setBusy(true);
    try {
      await api(path, method, body);
      setModal(null);
      await load();
      if (page === "Catalog") onCatalog();
      notify("Changes saved");
    } catch (e) {
      notify(e.message, true);
    } finally {
      setBusy(false);
    }
  }
  if (loadError) return <section className="panel padded" role="alert">
    <h2>Could not load this view</h2><p>{loadError}</p>
    <button className="secondary" onClick={retryLoad}>Try again</button>
  </section>;
  if (!data) return <Loading />;
  const titles = {
    Users: "People & permissions",
    Catalog: "Services & categories",
    Configuration: "Service desk configuration",
    "SLA policies": "Service level policies",
    "SLA monitor": "SLA monitoring",
    "Audit log": "Audit trail",
    Notifications: "Your notifications",
    Technicians:
      user.role === "TECHNICIAN"
        ? "Your workload & expertise"
        : "Technician workspace",
    "Email delivery": "Notification delivery",
    Reports: "Reports & analytics",
  };
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">
            {user.role === "ADMIN" ? "ADMINISTRATION" : "WORKSPACE"}
          </span>
          <h1>{titles[page]}</h1>
          <p className="muted">
            {page === "Configuration"
              ? "Versioned rules for priority, assignment, and service levels."
              : page === "Audit log"
                ? "A permanent record of actions and decisions."
                : "Live information from your service desk."}
          </p>
        </div>
        {["Users", "Catalog", "SLA policies"].includes(page) && (
          <button className="primary" onClick={() => setModal(page === "Users" ? { role: "TECHNICIAN", active: true, available: true } : {})}>
            <Plus size={17} />
            Add {page === "Users" ? "staff account" : page === "Catalog" ? "entry" : "policy"}
          </button>
        )}
      </div>
      {page === "Configuration" ? (
        <section className="panel padded">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const d = Object.fromEntries(new FormData(e.target));
              for (const k of [
                "impactWeight",
                "urgencyWeight",
                "criticalityWeight",
                "affectedWeight",
                "mediumCutoff",
                "highCutoff",
                "criticalCutoff",
                "skillWeight",
                "loadWeight",
                "skillThreshold",
                "reopenDays",
              ])
                d[k] = Number(d[k]);
              d.emailEnabled = d.emailEnabled === "on";
              d.enabledStrategies = [
                "LEAST_LOADED",
                "SKILL_FIRST",
                "PRIORITY_WEIGHTED",
                "ROUND_ROBIN",
              ]
                .filter((k) => d[k] === "on")
                .join(",");
              for (const k of [
                "LEAST_LOADED",
                "SKILL_FIRST",
                "PRIORITY_WEIGHTED",
                "ROUND_ROBIN",
              ])
                delete d[k];
              if (
                confirm(
                  "Apply configuration version " + (data.revision + 1) + "?",
                )
              )
                save("/admin/settings", "PUT", d);
            }}
          >
            <div className="section-heading">
              <h2>Priority calculation</h2>
              <span className="pill">Version {data.revision}</span>
            </div>
            <div className="form-grid">
              <Field label="Active strategy">
                <select
                  name="priorityStrategy"
                  defaultValue={data.priorityStrategy}
                >
                  <option>FORMULA</option>
                  <option>MATRIX</option>
                </select>
              </Field>
              {[
                "impactWeight",
                "urgencyWeight",
                "criticalityWeight",
                "affectedWeight",
                "mediumCutoff",
                "highCutoff",
                "criticalCutoff",
              ].map((k) => (
                <Field key={k} label={k.replace(/([A-Z])/g, " $1")}>
                  <input
                    required
                    type="number"
                    min={k.includes("Weight") ? 0 : 1}
                    max={k.includes("Weight") ? 100 : 10000}
                    name={k}
                    defaultValue={data[k]}
                  />
                </Field>
              ))}
            </div>
            <Field label="Matrix · nine comma-separated priorities · impact rows × urgency columns">
              <input name="matrix" defaultValue={data.matrix} required />
            </Field>
            <h2>Assignment strategy selection</h2>
            <p className="muted">
              Enabled strategies may be recommended by the AI provider.
              Least-loaded remains available as the system fallback regardless
              of these choices.
            </p>
            <div className="strategy-options">
              {[
                "LEAST_LOADED",
                "SKILL_FIRST",
                "PRIORITY_WEIGHTED",
                "ROUND_ROBIN",
              ].map((s) => (
                <label key={s} className="check">
                  <input
                    type="checkbox"
                    name={s}
                    defaultChecked={data.enabledStrategies
                      .split(",")
                      .includes(s)}
                  />
                  {human(s)}
                </label>
              ))}
            </div>
            <div className="form-grid">
              {[
                "skillWeight",
                "loadWeight",
                "skillThreshold",
                "reopenDays",
              ].map((k) => (
                <Field key={k} label={k.replace(/([A-Z])/g, " $1")}>
                  <input
                    name={k}
                    type="number"
                    required
                    min="1"
                    max={k === "reopenDays" ? 365 : 10000}
                    defaultValue={data[k]}
                  />
                </Field>
              ))}
              <Field label="SLA warning thresholds · percentages">
                <input
                  name="warningThresholds"
                  defaultValue={data.warningThresholds}
                  required
                />
              </Field>
              <label className="check">
                <input
                  type="checkbox"
                  name="emailEnabled"
                  defaultChecked={data.emailEnabled}
                />
                Enable incident email notifications
              </label>
            </div>
            <div className="form-actions">
              <button className="primary" disabled={busy}>
                Save configuration
              </button>
            </div>
          </form>
        </section>
      ) : page === "Users" ? (
        <section className="panel">
          <Table
            rows={data}
            columns={[
              { key: "name", label: "NAME" },
              { key: "email", label: "EMAIL" },
              {
                key: "role",
                label: "ROLE",
                render: (r) => <Badge value={r.role} />,
              },
              {
                key: "active",
                label: "ACCOUNT",
                render: (r) => (r.active ? "Active" : "Inactive"),
              },
              {
                key: "verified",
                label: "EMAIL VERIFIED",
                render: (r) => (r.verified ? "Yes" : "No"),
              },
              {
                key: "edit",
                label: "",
                render: (r) => (
                  <button className="text-button" onClick={() => setModal(r)}>
                    Manage
                  </button>
                ),
              },
            ]}
          />
          <Pager page={index} setPage={setIndex} hasNext={data.length === 50} />
        </section>
      ) : page === "Catalog" ? (
        <section className="panel">
          <Table
            rows={data}
            columns={[
              { key: "name", label: "NAME" },
              { key: "kind", label: "TYPE" },
              {
                key: "active",
                label: "STATUS",
                render: (r) => (r.active ? "Active" : "Inactive"),
              },
              {
                key: "edit",
                label: "",
                render: (r) => (
                  <button className="text-button" onClick={() => setModal(r)}>
                    Edit
                  </button>
                ),
              },
            ]}
          />
        </section>
      ) : page === "SLA policies" ? (
        <section className="panel">
          <Table
            rows={data}
            columns={[
              {
                key: "serviceId",
                label: "SERVICE",
                render: (r) => catalog.find((c) => c.id === r.serviceId)?.name,
              },
              {
                key: "priority",
                label: "PRIORITY",
                render: (r) => <Badge value={r.priority} />,
              },
              {
                key: "responseSeconds",
                label: "RESPONSE",
                render: (r) => duration(r.responseSeconds),
              },
              {
                key: "resolutionSeconds",
                label: "RESOLUTION",
                render: (r) => duration(r.resolutionSeconds),
              },
              { key: "revision", label: "VERSION" },
              {
                key: "active",
                label: "ACTIVE",
                render: (r) => (r.active ? "Yes" : "No"),
              },
              {
                key: "edit",
                label: "",
                render: (r) => (
                  <button className="text-button" onClick={() => setModal(r)}>
                    New revision
                  </button>
                ),
              },
            ]}
          />
        </section>
      ) : page === "SLA monitor" ? (
        <section className="panel">
          <Table
            rows={data}
            onRow={(r) => onOpen({ id: r.incidentId })}
            columns={[
              { key: "reference", label: "INCIDENT" },
              { key: "kind", label: "CLOCK" },
              { key: "service", label: "SERVICE" },
              {
                key: "priority",
                label: "SNAPSHOT PRIORITY",
                render: (r) => <Badge value={r.priority} />,
              },
              {
                key: "deadline",
                label: "DEADLINE",
                render: (r) => date(r.deadline),
              },
              {
                key: "elapsedSeconds",
                label: "ELAPSED / TARGET",
                render: (r) =>
                  duration(r.elapsedSeconds) +
                  " / " +
                  duration(r.targetSeconds),
              },
              {
                key: "state",
                label: "STATE",
                render: (r) =>
                  r.signals.some((s) => s.threshold === 100) ? (
                    <Badge value="BREACHED" />
                  ) : r.stoppedAt ? (
                    "Stopped"
                  ) : r.pausedAt ? (
                    "Waiting for employee"
                  ) : r.signals.length ? (
                    "Warning"
                  ) : (
                    "Running"
                  ),
              },
            ]}
          />
          <Pager page={index} setPage={setIndex} hasNext={data.length === 50} />
        </section>
      ) : page === "Audit log" ? (
        <section className="panel">
          <Table
            rows={data}
            columns={[
              {
                key: "createdAt",
                label: "TIMESTAMP",
                render: (r) => date(r.createdAt),
              },
              { key: "actorId", label: "ACTOR ID" },
              { key: "action", label: "ACTION" },
              {
                key: "entityType",
                label: "ENTITY",
                render: (r) => r.entityType + " #" + r.entityId,
              },
              { key: "details", label: "DETAILS" },
            ]}
          />
          <Pager page={index} setPage={setIndex} hasNext={data.length === 50} />
        </section>
      ) : page === "Notifications" ? (
        <section className="panel padded">
          {data.length ? (
            data.map((n) => (
              <article
                key={n.id}
                className={"notification " + (!n.readFlag ? "unread" : "")}
              >
                <div>
                  <p>{n.message}</p>
                  <small>{date(n.createdAt)}</small>
                </div>
                <div className="inline">
                  {n.incidentId && (
                    <button
                      className="text-button"
                      onClick={() => onOpen({ id: n.incidentId })}
                    >
                      Open
                    </button>
                  )}
                  {!n.readFlag && (
                    <button
                      className="secondary"
                      onClick={() =>
                        save("/notifications/" + n.id + "/read", "POST", {})
                      }
                    >
                      Mark read
                    </button>
                  )}
                </div>
              </article>
            ))
          ) : (
            <Empty>No notifications yet.</Empty>
          )}
          <Pager page={index} setPage={setIndex} hasNext={data.length === 50} />
        </section>
      ) : page === "Technicians" ? (
        <div className="technician-grid">
          {data.map((t) => (
            <section className="panel padded" key={t.id}>
              <div className="tech-header">
                <span className="avatar">
                  {t.name
                    .split(" ")
                    .map((x) => x[0])
                    .join("")}
                </span>
                <div>
                  <h2>{t.name}</h2>
                  <small>{t.email}</small>
                </div>
              </div>
              <div className="tech-numbers">
                <div>
                  <strong>{t.workload}</strong>
                  <small>Active incidents</small>
                </div>
                <div>
                  <strong>{t.verifiedResolutions}</strong>
                  <small>Verified resolutions</small>
                </div>
              </div>
              <p className="muted">
                {t.available && t.active
                  ? "Available for assignment"
                  : "Not available for assignment"}
              </p>
              <h3>Recognized skills</h3>
              {t.skills.length ? (
                t.skills.map((s) => (
                  <p key={s.id}>
                    <span className="pill">{s.category}</span>
                    <small className="block">
                      Recognized {date(s.recognizedAt)} · threshold{" "}
                      {s.thresholdAtRecognition}
                    </small>
                  </p>
                ))
              ) : (
                <p className="muted">
                  Skills are recognized automatically after verified resolutions
                  meet the configured threshold.
                </p>
              )}
            </section>
          ))}
        </div>
      ) : page === "Email delivery" ? (
        <>
          <section className="panel padded">
            <h2>Incident notification delivery</h2>
            <Table
              rows={data.notifications}
              columns={[
                { key: "id", label: "ID" },
                { key: "userId", label: "USER" },
                { key: "deliveryStatus", label: "STATUS" },
                { key: "attempts", label: "ATTEMPTS" },
                { key: "deliveryError", label: "ERROR" },
              ]}
            />
          </section>
          <section className="panel padded">
            <h2>Account email delivery</h2>
            <Table
              rows={data.accountEmails}
              columns={[
                { key: "recipient", label: "RECIPIENT" },
                { key: "subject", label: "SUBJECT" },
                { key: "status", label: "STATUS" },
                { key: "attempts", label: "ATTEMPTS" },
                { key: "error", label: "ERROR" },
              ]}
            />
          </section>
        </>
      ) : page === "Reports" ? (
        <Reports data={data} notify={notify} />
      ) : null}
      {modal && (
        <Modal
          title={
            page === "Users"
              ? modal.id ? "Manage " + modal.name : "Create staff account"
              : page === "Catalog"
                ? "Catalog entry"
                : "SLA policy revision"
          }
          onClose={() => setModal(null)}
        >
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const d = Object.fromEntries(new FormData(e.target));
              d.active = d.active === "on";
              if (page === "Users") {
                if (!modal.id) { save("/admin/users", "POST", { name: d.name, email: d.email, password: d.password, role: d.role }); return; }
                d.available = d.available === "on";
                if (
                  confirm(
                    "Update account access? Existing sessions will be invalidated.",
                  )
                )
                  save("/admin/users/" + modal.id, "PUT", d);
              } else if (page === "Catalog")
                save(
                  "/admin/catalog" + (modal.id ? "/" + modal.id : ""),
                  modal.id ? "PUT" : "POST",
                  d,
                );
              else {
                d.serviceId = Number(d.serviceId);
                d.responseSeconds = Number(d.responseSeconds);
                d.resolutionSeconds = Number(d.resolutionSeconds);
                save("/admin/sla-policies", "POST", d);
              }
            }}
          >
            {page === "Users" ? (
              <>
                {!modal.id && <>
                  <Field label="Full name"><input name="name" required maxLength={100} /></Field>
                  <Field label="Email"><input name="email" type="email" required maxLength={200} /></Field>
                  <Field label="Initial password"><input name="password" type="password" autoComplete="new-password" required minLength={12} maxLength={72} /></Field>
                  <p className="muted">Share these credentials securely with the user. They can sign in immediately; email verification is required before password changes.</p>
                </>}
                <Field label="Role">
                  <select name="role" defaultValue={modal.role}>
                    {(modal.id ? ["EMPLOYEE", "TECHNICIAN", "ADMIN"] : ["TECHNICIAN", "ADMIN"]).map((r) => (
                      <option key={r}>{r}</option>
                    ))}
                  </select>
                </Field>
                {modal.id && <label className="check">
                  <input
                    type="checkbox"
                    name="available"
                    defaultChecked={modal.available}
                  />
                  Available for assignments
                </label>}
              </>
            ) : page === "Catalog" ? (
              <>
                <Field label="Type">
                  <select name="kind" defaultValue={modal.kind || "SERVICE"}>
                    {modal.id ? (
                      <option>{modal.kind}</option>
                    ) : (
                      <>
                        <option>SERVICE</option>
                        <option>CATEGORY</option>
                      </>
                    )}
                  </select>
                </Field>
                <Field label="Name">
                  <input
                    name="name"
                    required
                    maxLength={100}
                    defaultValue={modal.name}
                  />
                </Field>
              </>
            ) : (
              <>
                <p className="muted">
                  Saving creates a new policy version. Existing incident
                  snapshots remain intact.
                </p>
                <Field label="Service">
                  <select name="serviceId" defaultValue={modal.serviceId}>
                    {catalog
                      .filter((c) => c.kind === "SERVICE")
                      .map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                  </select>
                </Field>
                <Field label="Priority">
                  <select
                    name="priority"
                    defaultValue={modal.priority || "MEDIUM"}
                  >
                    {["LOW", "MEDIUM", "HIGH", "CRITICAL"].map((p) => (
                      <option key={p}>{p}</option>
                    ))}
                  </select>
                </Field>
                {["responseSeconds", "resolutionSeconds"].map((k) => (
                  <Field
                    key={k}
                    label={k.replace("Seconds", " target (seconds)")}
                  >
                    <input
                      name={k}
                      type="number"
                      min="1"
                      max="31536000"
                      required
                      defaultValue={modal[k] || 3600}
                    />
                  </Field>
                ))}
              </>
            )}
            {(page !== "Users" || modal.id) && <label className="check">
              <input
                type="checkbox"
                name="active"
                defaultChecked={modal.active ?? true}
              />
              Active
            </label>}
            <div className="form-actions">
              <button
                type="button"
                className="secondary"
                onClick={() => setModal(null)}
              >
                Cancel
              </button>
              <button className="primary" disabled={busy}>
                Save changes
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
function Reports({ data, notify }) {
  const [compliance, setCompliance] = useState([]);
  useEffect(() => {
    api("/admin/sla-compliance")
      .then(setCompliance)
      .catch((e) => notify(e.message, true));
  }, []);
  return (
    <>
      <div className="metrics">
        {[
          ["Total incidents", data.total],
          ["Average resolution", duration(data.averageResolutionSeconds)],
          ["SLA breaches", data.breaches],
          ["Average feedback", data.averageFeedback?.toFixed(1) || "—"],
        ].map(([k, v]) => (
          <div className="metric" key={k}>
            <span>{k}</span>
            <strong>{v}</strong>
          </div>
        ))}
      </div>
      <div className="dashboard-grid">
        {["byStatus", "byPriority", "byCategory", "byService", "trends"].map(
          (k) => (
            <section className="panel padded" key={k}>
              <h2>
                {k.replace("by", "Incidents by ").replace(/([A-Z])/g, " $1")}
              </h2>
              <Table
                rows={data[k]}
                columns={[
                  { key: "name", label: "GROUP" },
                  { key: "value", label: "INCIDENTS" },
                ]}
              />
            </section>
          ),
        )}
      </div>
      <section className="panel padded">
        <h2>SLA compliance by service and snapshot priority</h2>
        <Table
          rows={compliance}
          columns={[
            { key: "name", label: "SERVICE / PRIORITY" },
            { key: "cycles", label: "CYCLES" },
            { key: "completed", label: "COMPLETED" },
            {
              key: "compliance",
              label: "COMPLIANCE",
              render: (r) =>
                r.compliance === null
                  ? "No completed cycles"
                  : r.compliance.toFixed(1) + "%",
            },
          ]}
        />
      </section>
    </>
  );
}
