import React from "react";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  cleanup,
} from "@testing-library/react";
import "@testing-library/jest-dom/vitest";
import { vi, afterEach, test, expect } from "vitest";
import Auth from "./Auth";
import IncidentDetail from "./IncidentDetail";
import { CreateIncident, IncidentList } from "./Incidents";
import { Badge } from "./components";
import { api, setToken, human, duration } from "./api";
afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  setToken(null);
});
test("login submits credentials and returns authenticated role", async () => {
  const onLogin = vi.fn();
  vi.spyOn(globalThis, "fetch").mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => ({ token: "jwt", user: { id: 1, role: "EMPLOYEE" } }),
  });
  render(<Auth onLogin={onLogin} />);
  fireEvent.change(screen.getByLabelText("Work email"), {
    target: { value: "employee@example.com" },
  });
  fireEvent.change(screen.getByLabelText("Password"), {
    target: { value: "password" },
  });
  fireEvent.submit(screen.getByText("Sign in").closest("form"));
  await waitFor(() =>
    expect(onLogin).toHaveBeenCalledWith({ id: 1, role: "EMPLOYEE" }),
  );
});
test("invalid login displays server error", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValue({
    ok: false,
    status: 401,
    json: async () => ({ message: "Invalid email or password" }),
  });
  render(<Auth onLogin={vi.fn()} />);
  fireEvent.submit(screen.getByText("Sign in").closest("form"));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Invalid email or password",
  );
});
test("registration presents a full name and secure password minimum", () => {
  render(<Auth onLogin={vi.fn()} />);
  fireEvent.click(screen.getByText("Create an employee account"));
  expect(screen.getByLabelText("Full name")).toBeRequired();
  expect(screen.getByLabelText("Password")).toHaveAttribute("minlength", "12");
});
test("creation sends numeric incident factors to actual API", async () => {
  const created = vi.fn();
  const fetch = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValue({
      ok: true,
      status: 201,
      json: async () => ({ id: 5 }),
    });
  render(
    <CreateIncident
      catalog={[
        { id: 1, kind: "SERVICE", name: "Network", active: true },
        { id: 2, kind: "CATEGORY", name: "Fault", active: true },
      ]}
      onCreated={created}
      onClose={vi.fn()}
      notify={vi.fn()}
    />,
  );
  fireEvent.change(screen.getByLabelText("Short title"), {
    target: { value: "VPN fails" },
  });
  fireEvent.change(screen.getByLabelText("Description"), {
    target: { value: "Cannot connect" },
  });
  fireEvent.change(screen.getByLabelText("Service"), {
    target: { value: "1" },
  });
  fireEvent.change(screen.getByLabelText("Category"), {
    target: { value: "2" },
  });
  fireEvent.submit(screen.getByText("Submit incident").closest("form"));
  await waitFor(() => expect(created).toHaveBeenCalledWith({ id: 5 }));
  expect(JSON.parse(fetch.mock.calls[0][1].body)).toMatchObject({
    serviceId: 1,
    categoryId: 2,
    impact: 2,
    affectedUsers: 1,
  });
});
test("attachment failure is reported alongside the created incident", async () => {
  const created = vi.fn();
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce({ ok: true, status: 201, json: async () => ({ id: 5 }) })
    .mockResolvedValueOnce({ ok: false, status: 422, json: async () => ({ message: "Invalid file" }) });
  const { container } = render(<CreateIncident
    catalog={[
      { id: 1, kind: "SERVICE", name: "Network", active: true },
      { id: 2, kind: "CATEGORY", name: "Fault", active: true },
    ]}
    onCreated={created} onClose={vi.fn()} notify={vi.fn()}
  />);
  const file = new File(["invalid"], "invalid.pdf", { type: "application/pdf" });
  const input = container.querySelector('input[type="file"]');
  fireEvent.change(input, { target: { files: [file] } });
  fireEvent.submit(screen.getByText("Submit incident").closest("form"));
  await waitFor(() => expect(created).toHaveBeenCalledWith(
    { id: 5 }, { uploaded: false, attachmentError: "Invalid file" }
  ));
});
test("technician list does not expose employee creation action", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => ({ items: [], total: 0 }),
  });
  render(
    <IncidentList
      user={{ role: "TECHNICIAN" }}
      catalog={[]}
      notify={vi.fn()}
    />,
  );
  expect(screen.queryByText("New incident")).not.toBeInTheDocument();
  await screen.findByText("Nothing here yet.");
});
test("API attaches bearer token and propagates authorization error", async () => {
  setToken("session-token");
  const fetch = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValue({
      ok: false,
      status: 403,
      json: async () => ({ message: "Incident access denied" }),
    });
  await expect(api("/incidents/99")).rejects.toThrow("Incident access denied");
  expect(fetch.mock.calls[0][1].headers.Authorization).toBe(
    "Bearer session-token",
  );
});
test("badge uses readable lifecycle labels", () => {
  render(<Badge value="WAITING_FOR_EMPLOYEE" />);
  expect(screen.getByText("Waiting for employee")).toBeInTheDocument();
});
test("duration and labels retain meaning", () => {
  expect(duration(4)).toBe("4s");
  expect(duration(120)).toBe("2m");
  expect(duration(7200)).toBe("2.0h");
  expect(human("IN_PROGRESS")).toBe("In progress");
});

test("staff tabs hide registration and submit the selected role", async () => {
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue({ ok: true, status: 200, json: async () => ({token: "jwt", user: {role: "ADMIN"}}) });
  render(<Auth onLogin={vi.fn()} />);
  fireEvent.click(screen.getByRole("tab", {name: "Admin"}));
  expect(screen.queryByText("Create an employee account")).not.toBeInTheDocument();
  fireEvent.submit(screen.getByText("Sign in").closest("form"));
  await waitFor(() => expect(fetch).toHaveBeenCalled());
  expect(JSON.parse(fetch.mock.calls[0][1].body).role).toBe("ADMIN");
  fireEvent.click(screen.getByRole("tab", {name: "Employee"}));
  expect(screen.getByText("Create an employee account")).toBeInTheDocument();
});

const chatIncident = (status = "WAITING_FOR_EMPLOYEE") => ({ id: 4, reference: "INC-4", title: "VPN", status, reporterId: 1, technicianId: 2, assignments: [], comments: [], history: [], resolutions: [], feedback: [], sla: [], priorityHistory: [] });
test("chat sends a message separately from information completion", async () => {
  const fetch = vi.spyOn(globalThis, "fetch").mockImplementation(async (path) => ({ ok: true, status: 200, json: async () => path.endsWith("/attachments") ? [] : chatIncident() }));
  render(<IncidentDetail id={4} user={{id: 1, role: "EMPLOYEE"}} notify={vi.fn()} onChange={vi.fn()} />);
  fireEvent.change(await screen.findByLabelText("Message"), {target: {value: "More detail"}});
  fireEvent.submit(screen.getByText("Send message").closest("form"));
  await waitFor(() => expect(fetch.mock.calls.some(([path, opts]) => path.endsWith("/comments") && JSON.parse(opts.body).body === "More detail")).toBe(true));
  expect(fetch.mock.calls.some(([path]) => path.endsWith("/actions"))).toBe(false);
  await waitFor(() => expect(screen.getByText("Send message")).not.toBeDisabled());
  fireEvent.click(screen.getByText("Information provided"));
  fireEvent.click(screen.getByText("Confirm information provided"));
  await waitFor(() => expect(fetch.mock.calls.some(([path, opts]) => path.endsWith("/actions") && JSON.parse(opts.body).action === "RESPOND")).toBe(true));
});
test("closed chat preserves messages and removes composer", async () => {
  vi.spyOn(globalThis, "fetch").mockImplementation(async (path) => ({ok: true, status: 200, json: async () => path.endsWith("/attachments") ? [] : {...chatIncident("CLOSED"), comments: [{id: 7, author: "Employee", body: "Prior message"}]} }));
  render(<IncidentDetail id={4} user={{id: 1, role: "EMPLOYEE"}} notify={vi.fn()} onChange={vi.fn()} />);
  expect(await screen.findByText("Prior message")).toBeInTheDocument();
  expect(screen.getByText("Chat is read-only after closure.")).toBeInTheDocument();
  expect(screen.queryByLabelText("Message")).not.toBeInTheDocument();
});
