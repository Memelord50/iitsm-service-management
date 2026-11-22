import React, { useState, useEffect } from "react";
import { ArrowLeft, Paperclip, Send, CheckCircle2, Clock3 } from "lucide-react";
import { api, date, human, duration, download } from "./api";
import { Badge, Loading, Field, Modal, Empty, Table } from "./components";
export default function IncidentDetail({ id, user, onBack, notify, onChange }) {
  const [item, setItem] = useState(null),
    [files, setFiles] = useState([]),
    [tab, setTab] = useState("Chat"),
    [action, setAction] = useState(null),
    [busy, setBusy] = useState(false),
    [techs, setTechs] = useState([]);
  async function load() {
    try {
      const [i, f] = await Promise.all([
        api("/incidents/" + id),
        api("/incidents/" + id + "/attachments"),
      ]);
      setItem(i);
      setFiles(f);
    } catch (e) {
      setLoadError(e.message);
    }
  }
  const [loadError, setLoadError] = useState("");
  const [retry, setRetry] = useState(0);
  const retryLoad = () => { setLoadError(""); setRetry(n => n + 1); };
  useEffect(() => {
    load();
    if (user.role === "ADMIN")
      api("/technicians")
        .then(setTechs)
        .catch((e) => notify(e.message, true));
  }, [id, retry]);
  async function mutate(path, body) {
    setBusy(true);
    try {
      const r = await api("/incidents/" + id + path, "POST", body);
      setItem(r);
      setAction(null);
      notify("Incident updated");
      onChange();
      return true;
    } catch (e) {
      notify(e.message, true);
    } finally {
      setBusy(false);
    }
  }
  useEffect(() => {
    if (!item || item.status === "CLOSED") return;
    let active = true, pending = false;
    const timer = setInterval(async () => {
      if (document.hidden || pending || busy) return;
      pending = true;
      try {
        const fresh = await api("/incidents/" + id);
        if (active) setItem(fresh);
      } catch { /* A foreground action reports errors; background refresh retries. */ }
      finally { pending = false; }
    }, 5000);
    return () => { active = false; clearInterval(timer); };
  }, [id, item?.status, busy]);
  if (loadError) return <section className="panel padded" role="alert">
    <h2>Could not load this view</h2><p>{loadError}</p>
    <button className="secondary" onClick={retryLoad}>Try again</button>
  </section>;
  if (!item) return <Loading />;
  const employee = user.id === item.reporterId,
    tech = user.role === "TECHNICIAN" && user.id === item.technicianId;
  const current = item.assignments.at(-1);
  let actions = [];
  if (tech) {
    if (
      current &&
      !current.acknowledgedAt &&
      ["ASSIGNED", "REOPENED", "IN_PROGRESS", "WAITING_FOR_EMPLOYEE"].includes(
        item.status,
      )
    )
      actions.push("ACKNOWLEDGE");
    if (
      ["ASSIGNED", "REOPENED"].includes(item.status) &&
      current?.acknowledgedAt
    )
      actions.push("START");
    if (item.status === "IN_PROGRESS") actions.push("REQUEST_INFO", "RESOLVE");
  }
  if (employee) {
    if (item.status === "WAITING_FOR_EMPLOYEE") actions.push("RESPOND");
    if (item.status === "RESOLVED") actions.push("VERIFY", "REJECT");
    if (item.status === "CLOSED") actions.push("REOPEN");
    if (item.status === "CLOSED" && !item.feedback.length)
      actions.push("FEEDBACK");
  }
  return (
    <>
      <button className="back" onClick={onBack}>
        <ArrowLeft size={16} />
        Back to incidents
      </button>
      <div className="page-heading detail-heading">
        <div>
          <span className="eyebrow">{item.reference}</span>
          <h1>{item.title}</h1>
          <div className="inline">
            <Badge value={item.status} />
            <Badge value={item.priority} />
            <span className="muted">Reported {date(item.createdAt)}</span>
          </div>
        </div>
      </div>
      <div className="detail-grid">
        <div>
          <section className="panel padded">
            <h2>Incident details</h2>
            <p className="prose">{item.description}</p>
            <div className="facts">
              {[
                ["Service", item.service],
                ["Category", item.category],
                ["Reported by", item.reporter],
                ["Assigned to", item.technician || "Awaiting assignment"],
                ["Impact / urgency", item.impact + " / " + item.urgency],
                ["Affected users", item.affectedUsers],
              ].map(([k, v]) => (
                <div key={k}>
                  <small>{k}</small>
                  <strong>{v}</strong>
                </div>
              ))}
            </div>
          </section>
          {item.resolutions.length > 0 && (
            <section className="panel padded resolution">
              <h2>
                <CheckCircle2 size={18} />
                Latest resolution
              </h2>
              <p className="prose">{item.resolutions.at(-1).description}</p>
              <small>
                {date(item.resolutions.at(-1).resolvedAt)}
                {item.resolutions.at(-1).verifiedAt
                  ? " · Verified by reporter"
                  : ""}
                {item.resolutions.at(-1).rejectedAt
                  ? " · Rejected: " + item.resolutions.at(-1).rejectionReason
                  : ""}
              </small>
            </section>
          )}
          <section className="panel">
            <div className="tabs">
              {[
                "Activity",
                "Chat",
                "SLA history",
                "Assignments",
                "Priority history",
                "Attachments",
              ].map((t) => (
                <button
                  key={t}
                  className={tab === t ? "active" : ""}
                  onClick={() => setTab(t)}
                >
                  {t}
                </button>
              ))}
            </div>
            <div className="tab-content">
              {tab === "Activity" && (
                <div className="timeline">
                  {[...item.history].reverse().map((h) => (
                    <div key={h.id}>
                      <i />
                      <span>
                        <strong>{human(h.action)}</strong>
                        <p>{h.details}</p>
                        <small>{date(h.createdAt)}</small>
                      </span>
                    </div>
                  ))}
                </div>
              )}
              {tab === "Chat" && (
                <>
                  {item.status === "WAITING_FOR_EMPLOYEE" && <p className="muted">Messages do not resume the SLA. The employee must select Information provided when finished.</p>}
                  <div className="comments">
                    {item.comments.length ? (
                      item.comments.map((c) => (
                        <article key={c.id}>
                          <div>
                            <strong>{c.author}</strong>
                            <small>
                              {date(c.createdAt)}
                              {c.internalNote ? " · Internal note" : ""}
                            </small>
                          </div>
                          <p className="prose">{c.body}</p>
                        </article>
                      ))
                    ) : (
                      <Empty>No comments yet. Start a conversation.</Empty>
                    )}
                  </div>
                  {item.status === "CLOSED" ? <p>Chat is read-only after closure.</p> : !item.technicianId ? <p>Chat opens when a technician is assigned.</p> : <form
                    onSubmit={async (e) => {
                      e.preventDefault();
                      const d = Object.fromEntries(new FormData(e.target));
                      const form = e.target;
                      const saved = await mutate("/comments", {
                        body: d.body,
                        internalNote: d.internal === "on",
                      });
                      if (saved) form.reset();
                    }}
                  >
                    <Field label="Message">
                      <textarea
                        name="body"
                        rows="3"
                        required
                        maxLength={20000}
                      />
                    </Field>
                    <div className="form-actions">
                      {user.role !== "EMPLOYEE" && (
                        <label className="check">
                          <input name="internal" type="checkbox" />
                          Internal investigation note
                        </label>
                      )}
                      <button className="primary" disabled={busy}>
                        <Send size={15} />
                        Send message
                      </button>
                    </div>
                  </form>}
                </>
              )}
              {tab === "SLA history" &&
                item.sla.map((c) => (
                  <article className="cycle" key={c.id}>
                    <div className="inline">
                      <h3>
                        {human(c.kind)} cycle #{c.id}
                      </h3>
                      <span className="pill">
                        {c.stoppedAt
                          ? "Stopped"
                          : c.pausedAt
                            ? "Paused for employee"
                            : "Running"}
                      </span>
                    </div>
                    <div className="facts">
                      <div>
                        <small>Started</small>
                        <strong>{date(c.startedAt)}</strong>
                      </div>
                      <div>
                        <small>Stopped</small>
                        <strong>{date(c.stoppedAt)}</strong>
                      </div>
                      <div>
                        <small>Original snapshot</small>
                        <strong>
                          {duration(c.originalTargetSeconds)} · policy{" "}
                          {c.policyId} v{c.policyVersion}
                        </strong>
                      </div>
                      <div>
                        <small>Current target / elapsed</small>
                        <strong>
                          {duration(c.targetSeconds)} /{" "}
                          {duration(c.elapsedSeconds)}
                        </strong>
                      </div>
                      <div>
                        <small>Deadline</small>
                        <strong>{date(c.deadline)}</strong>
                      </div>
                      <div>
                        <small>Accumulated pause</small>
                        <strong>{duration(c.pausedSeconds)}</strong>
                      </div>
                    </div>
                    {c.revisions.map((r) => (
                      <p key={r.id}>
                        Target revision: {duration(r.oldTargetSeconds)} →{" "}
                        {duration(r.newTargetSeconds)} · {r.oldPriority} →{" "}
                        {r.newPriority} · {date(r.createdAt)}
                      </p>
                    ))}
                    {c.pauses.map((p) => (
                      <p key={p.id}>
                        Waiting: {date(p.startedAt)} → {date(p.endedAt)}
                      </p>
                    ))}
                    {c.signals.map((s) => (
                      <p
                        className={s.threshold === 100 ? "error" : "muted"}
                        key={s.id}
                      >
                        {s.threshold === 100
                          ? "Breach"
                          : s.threshold + "% warning"}{" "}
                        · detected {date(s.detectedAt)} · deadline{" "}
                        {date(s.deadline)}
                      </p>
                    ))}
                  </article>
                ))}
              {tab === "Assignments" && (
                <Table
                  rows={item.assignments}
                  columns={[
                    { key: "technicianId", label: "TECHNICIAN ID" },
                    { key: "previousTechnicianId", label: "PREVIOUS" },
                    { key: "strategy", label: "STRATEGY" },
                    { key: "reason", label: "REASON" },
                    {
                      key: "acknowledgedAt",
                      label: "ACKNOWLEDGED",
                      render: (r) => date(r.acknowledgedAt),
                    },
                  ]}
                />
              )}
              {tab === "Priority history" && (
                <Table
                  rows={item.priorityHistory}
                  columns={[
                    { key: "oldPriority", label: "BEFORE" },
                    { key: "newPriority", label: "AFTER" },
                    { key: "configVersion", label: "VERSION" },
                    { key: "factors", label: "FACTORS" },
                    { key: "reason", label: "REASON" },
                  ]}
                />
              )}
              {tab === "Attachments" && (
                <>
                  <p className="muted">
                    PDF, PNG, JPEG or plain text · 5 MB maximum
                  </p>
                  <input
                    type="file"
                    aria-label="Upload attachment"
                    accept=".pdf,.png,.jpg,.jpeg,.txt"
                    onChange={async (e) => {
                      const file = e.target.files[0];
                      if (!file) return;
                      const f = new FormData();
                      f.append("file", file);
                      try {
                        await api(
                          "/incidents/" + id + "/attachments",
                          "POST",
                          f,
                        );
                        await load();
                        notify("Attachment uploaded");
                      } catch (e) {
                        notify(e.message, true);
                      }
                      e.target.value = "";
                    }}
                  />
                  {files.map((f) => (
                    <div className="attachment" key={f.id}>
                      <Paperclip size={17} />
                      <button
                        onClick={() =>
                          download(f.id, f.filename).catch((e) =>
                            notify(e.message, true),
                          )
                        }
                      >
                        {f.filename}
                      </button>
                      <span>{(f.sizeBytes / 1024).toFixed(1)} KB</span>
                      {(user.role === "ADMIN" || f.uploaderId === user.id) && (
                        <button
                          onClick={async () => {
                            if (
                              !confirm(
                                "Delete this attachment from the incident?",
                              )
                            )
                              return;
                            try {
                              await api("/attachments/" + f.id, "DELETE");
                              load();
                            } catch (e) {
                              notify(e.message, true);
                            }
                          }}
                        >
                          Delete
                        </button>
                      )}
                    </div>
                  ))}
                </>
              )}
            </div>
          </section>
        </div>
        <aside>
          <section className="panel padded">
            <h2>Next steps</h2>
            <p className="muted">
              {item.status === "CLOSED"
                ? "This incident has been verified and closed."
                : "Keep the resolution moving."}
            </p>
            <div className="action-stack">
              {actions.map((a) => (
                <button
                  key={a}
                  className={
                    ["RESOLVE", "VERIFY", "RESPOND"].includes(a)
                      ? "primary"
                      : "secondary"
                  }
                  onClick={() => setAction(a)}
                >
                  {a === "RESPOND" ? "Information provided" : human(a)}
                </button>
              ))}
              {user.role === "ADMIN" && (
                <>
                  <button
                    className="secondary"
                    onClick={() => setAction("REASSIGN")}
                  >
                    Reassign technician
                  </button>
                  <button
                    className="secondary"
                    onClick={() => setAction("PRIORITY")}
                  >
                    Override priority
                  </button>
                </>
              )}
              {!actions.length && user.role !== "ADMIN" && (
                <small className="muted">
                  No action needed from you right now.
                </small>
              )}
            </div>
          </section>
          <section className="panel padded">
            <h2>
              <Clock3 size={18} />
              Service level targets
            </h2>
            {item.sla
              .filter((c) => !c.stoppedAt)
              .map((c) => (
                <div className="sla-mini" key={c.id}>
                  <div>
                    <strong>{human(c.kind)}</strong>
                    <span>
                      {Math.min(
                        999,
                        Math.round((c.elapsedSeconds / c.targetSeconds) * 100),
                      )}
                      %
                    </span>
                  </div>
                  <div className="track">
                    <i
                      style={{
                        width: `${Math.min(100, (c.elapsedSeconds / c.targetSeconds) * 100)}%`,
                        background:
                          c.elapsedSeconds > c.targetSeconds
                            ? "#c8553b"
                            : "#168271",
                      }}
                    />
                  </div>
                  <small>
                    {c.pausedAt
                      ? "Paused while waiting for your response"
                      : "Due " + date(c.deadline)}
                  </small>
                </div>
              ))}
            {item.sla.every((c) => c.stoppedAt) && (
              <p className="muted">
                All SLA clocks stopped. History is preserved.
              </p>
            )}
          </section>
        </aside>
      </div>
      {action && (
        <Modal title={action === "RESPOND" ? "Information provided" : human(action)} onClose={() => setAction(null)}>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const d = Object.fromEntries(new FormData(e.target));
              if (action === "REASSIGN")
                mutate("/reassign", {
                  technicianId: Number(d.technicianId),
                  reason: d.text,
                });
              else if (action === "PRIORITY")
                mutate("/priority", { priority: d.priority, reason: d.text });
              else if (action === "FEEDBACK")
                mutate("/feedback", {
                  rating: Number(d.rating),
                  comment: d.text,
                });
              else mutate("/actions", { action, text: d.text || "" });
            }}
          >
            {action === "REASSIGN" && (
              <Field label="Technician">
                <select name="technicianId" required>
                  {techs
                    .filter(
                      (t) =>
                        t.active && t.available && t.id !== item.technicianId,
                    )
                    .map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name} · {t.workload} active
                      </option>
                    ))}
                </select>
              </Field>
            )}
            {action === "PRIORITY" && (
              <Field label="New priority">
                <select name="priority" defaultValue={item.priority}>
                  {["LOW", "MEDIUM", "HIGH", "CRITICAL"].map((p) => (
                    <option key={p}>{p}</option>
                  ))}
                </select>
              </Field>
            )}
            {action === "FEEDBACK" && (
              <Field label="Rating">
                <select name="rating" defaultValue="5">
                  {[5, 4, 3, 2, 1].map((n) => (
                    <option key={n} value={n}>
                      {n} / 5
                    </option>
                  ))}
                </select>
              </Field>
            )}
            {["ACKNOWLEDGE", "START", "VERIFY", "RESPOND"].includes(action) ? (
              <p>
                {action === "RESPOND" ? "Confirm that you have provided the requested information in chat or attachments. This resumes the resolution SLA." : `Confirm ${human(action).toLowerCase()} for ${item.reference}?`}
                {action === "VERIFY"
                  ? " This closes the incident and credits the verified resolution."
                  : ""}
              </p>
            ) : (
              <Field
                label={
                  action === "RESOLVE" ? "Resolution details" : "Explanation"
                }
              >
                <textarea
                  name="text"
                  required={action !== "FEEDBACK"}
                  maxLength={20000}
                  rows="5"
                />
              </Field>
            )}
            <div className="form-actions">
              <button
                type="button"
                className="secondary"
                onClick={() => setAction(null)}
              >
                Cancel
              </button>
              <button className="primary" disabled={busy}>
                {busy ? "Saving…" : "Confirm " + (action === "RESPOND" ? "information provided" : human(action).toLowerCase())}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
