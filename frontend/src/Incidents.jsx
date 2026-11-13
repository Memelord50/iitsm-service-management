import React, { useState, useEffect } from "react";
import { Search, Plus, SlidersHorizontal } from "lucide-react";
import { api, date, human } from "./api";
import { Badge, Table, Pager, Loading, Field, Modal } from "./components";
export function IncidentList({
  user,
  catalog,
  onOpen,
  onCreate,
  notify,
  refresh,
}) {
  const [filters, setFilters] = useState({
      q: "",
      status: "",
      priority: "",
      serviceId: "",
      categoryId: "",
      slaStatus: "",
      sort: "createdAt",
      direction: "desc",
    }),
    [more, setMore] = useState(false),
    [page, setPage] = useState(0),
    [data, setData] = useState(null);
  useEffect(() => {
    let live = true;
    setData(null);
    const query = new URLSearchParams({ ...filters, page, size: 20 });
    const timer = setTimeout(
      () =>
        api("/incidents?" + query)
          .then((r) => live && setData(r))
          .catch((e) => notify(e.message, true)),
      200,
    );
    return () => {
      live = false;
      clearTimeout(timer);
    };
  }, [filters, page, refresh]);
  function change(k, v) {
    setFilters({ ...filters, [k]: v });
    setPage(0);
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <span className="eyebrow">SERVICE DESK</span>
          <h1>
            {user.role === "EMPLOYEE"
              ? "My incidents"
              : user.role === "TECHNICIAN"
                ? "Assigned incidents"
                : "Incident management"}
          </h1>
          <p className="muted">
            Track every request from first report to resolution.
          </p>
        </div>
        {user.role === "EMPLOYEE" && (
          <button className="primary" onClick={onCreate}>
            <Plus size={17} />
            New incident
          </button>
        )}
      </div>
      <section className="panel">
        <div className="filters">
          <div className="search">
            <Search size={17} />
            <input
              aria-label="Search incidents"
              placeholder="Search reference, title, description…"
              value={filters.q}
              onChange={(e) => change("q", e.target.value)}
            />
          </div>
          <select
            aria-label="Filter status"
            value={filters.status}
            onChange={(e) => change("status", e.target.value)}
          >
            <option value="">All statuses</option>
            {[
              "NEW",
              "ASSIGNED",
              "IN_PROGRESS",
              "WAITING_FOR_EMPLOYEE",
              "RESOLVED",
              "CLOSED",
              "REOPENED",
            ].map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
          <select
            aria-label="Filter priority"
            value={filters.priority}
            onChange={(e) => change("priority", e.target.value)}
          >
            <option value="">All priorities</option>
            {["LOW", "MEDIUM", "HIGH", "CRITICAL"].map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
          <button className="secondary" onClick={() => setMore(!more)}>
            <SlidersHorizontal size={16} />
            Filters
          </button>
        </div>
        {more && (
          <div className="form-grid filter-extra">
            {["SERVICE", "CATEGORY"].map((kind) => (
              <Field key={kind} label={human(kind)}>
                <select
                  value={
                    filters[kind === "SERVICE" ? "serviceId" : "categoryId"]
                  }
                  onChange={(e) =>
                    change(
                      kind === "SERVICE" ? "serviceId" : "categoryId",
                      e.target.value,
                    )
                  }
                >
                  <option value="">All</option>
                  {catalog
                    .filter((c) => c.kind === kind)
                    .map((c) => (
                      <option value={c.id} key={c.id}>
                        {c.name}
                      </option>
                    ))}
                </select>
              </Field>
            ))}
            <Field label="SLA status">
              <select
                value={filters.slaStatus}
                onChange={(e) => change("slaStatus", e.target.value)}
              >
                <option value="">All</option>
                {["ACTIVE", "WARNING", "BREACHED"].map((x) => (
                  <option key={x}>{x}</option>
                ))}
              </select>
            </Field>
            {["from", "to"].map((k) => (
              <Field label={human(k) + " date"} key={k}>
                <input
                  type="date"
                  onChange={(e) =>
                    change(
                      k,
                      e.target.value
                        ? e.target.value +
                            (k === "from" ? "T00:00:00Z" : "T23:59:59Z")
                        : "",
                    )
                  }
                />
              </Field>
            ))}
            {user.role === "ADMIN" &&
              ["reporterId", "technicianId"].map((k) => (
                <Field key={k} label={k}>
                  <input
                    type="number"
                    min="1"
                    onChange={(e) => change(k, e.target.value)}
                  />
                </Field>
              ))}
            <Field label="Sort">
              <select
                value={filters.sort}
                onChange={(e) => change("sort", e.target.value)}
              >
                {["createdAt", "reference", "priority", "status", "title"].map(
                  (s) => (
                    <option key={s}>{s}</option>
                  ),
                )}
              </select>
            </Field>
            <Field label="Direction">
              <select
                value={filters.direction}
                onChange={(e) => change("direction", e.target.value)}
              >
                <option>desc</option>
                <option>asc</option>
              </select>
            </Field>
          </div>
        )}
        {!data ? (
          <Loading />
        ) : (
          <>
            <Table
              rows={data.items}
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
                { key: "service", label: "SERVICE" },
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
                { key: "technician", label: "TECHNICIAN" },
                {
                  key: "createdAt",
                  label: "CREATED",
                  render: (r) => date(r.createdAt),
                },
              ]}
            />
            <div className="table-footer">
              <span>{data.total} incidents</span>
              <Pager
                page={page}
                setPage={setPage}
                hasNext={(page + 1) * 20 < data.total}
              />
            </div>
          </>
        )}
      </section>
    </>
  );
}
export function CreateIncident({ catalog, onClose, onCreated, notify }) {
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    const f = e.target;
    const d = Object.fromEntries(new FormData(f));
    const file = d.file;
    delete d.file;
    for (const k of [
      "serviceId",
      "categoryId",
      "impact",
      "urgency",
      "criticality",
      "affectedUsers",
    ])
      d[k] = Number(d[k]);
    try {
      const r = await api("/incidents", "POST", d);
      if (file?.size) {
        try {
          const form = new FormData();
          form.append("file", file);
          await api("/incidents/" + r.id + "/attachments", "POST", form);
        } catch (e) {
          notify("Incident created; attachment failed: " + e.message, true);
        }
      }
      onCreated(r);
    } catch (e) {
      notify(e.message, true);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title="Report an incident" onClose={onClose}>
      <p className="muted">
        Tell us what happened. We’ll calculate priority and find the right
        technician.
      </p>
      <form onSubmit={submit}>
        <Field label="Short title">
          <input
            name="title"
            required
            maxLength={160}
            placeholder="What isn’t working?"
          />
        </Field>
        <Field label="Description">
          <textarea
            name="description"
            required
            maxLength={20000}
            rows="4"
            placeholder="What were you trying to do? Include error messages and steps to reproduce."
          />
        </Field>
        <div className="form-grid">
          {["SERVICE", "CATEGORY"].map((k) => (
            <Field key={k} label={human(k)}>
              <select
                name={k === "SERVICE" ? "serviceId" : "categoryId"}
                required
              >
                <option value="">Select {k.toLowerCase()}</option>
                {catalog
                  .filter((c) => c.kind === k && c.active)
                  .map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
              </select>
            </Field>
          ))}
          {["impact", "urgency", "criticality"].map((k) => (
            <Field key={k} label={human(k)}>
              <select name={k} defaultValue="2">
                <option value="1">1 · Low</option>
                <option value="2">2 · Medium</option>
                <option value="3">3 · High</option>
              </select>
            </Field>
          ))}
          <Field label="Affected users">
            <input
              type="number"
              name="affectedUsers"
              min="1"
              max="10000000"
              defaultValue="1"
              required
            />
          </Field>
        </div>
        <Field label="Attachment · optional · up to 5 MB">
          <input type="file" name="file" accept=".pdf,.png,.jpg,.jpeg,.txt" />
        </Field>
        <div className="form-actions">
          <button type="button" className="secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="primary" disabled={busy}>
            {busy ? "Submitting…" : "Submit incident"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
