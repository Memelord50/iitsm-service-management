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
  fireEvent.click(screen.getByText("Create an account"));
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
