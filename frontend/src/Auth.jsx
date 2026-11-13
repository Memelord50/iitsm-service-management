import React, { useState } from "react";
import { ArrowUpRight, ShieldCheck, Activity, Layers } from "lucide-react";
import { api, setToken } from "./api";
import { Field } from "./components";
export default function Auth({ onLogin }) {
  const query = new URLSearchParams(window.location.search);
  const initial = query.get("action");
  const [mode, setMode] = useState(
    initial === "verify" ? "verify" : initial === "reset" ? "reset" : "login",
  );
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setMessage("");
    const d = Object.fromEntries(new FormData(e.target));
    try {
      if (mode === "login") {
        const r = await api("/auth/login", "POST", d);
        setToken(r.token);
        onLogin(r.user);
      } else {
        const path = {
          register: "register",
          forgot: "forgot-password",
          verify: "verify",
          reset: "reset-password",
          resend: "resend-verification",
        }[mode];
        const r = await api(
          "/auth/" + path,
          "POST",
          mode === "verify"
            ? { token: query.get("token") }
            : mode === "reset"
              ? { token: query.get("token"), password: d.password }
              : d,
        );
        setMessage(r.message);
        if (mode === "verify" || mode === "reset")
          window.history.replaceState({}, "", window.location.pathname);
      }
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  const titles = {
    login: "Welcome back.",
    register: "Your support starts here.",
    forgot: "Recover your account.",
    verify: "Verify your email.",
    reset: "Choose a new password.",
    resend: "Verify your account.",
  };
  return (
    <div className="auth">
      <aside className="auth-story">
        <div className="brand">
          <span className="brand-icon">
            <Layers size={23} />
          </span>
          I-ITSM<span className="brand-dot">/</span>
        </div>
        <div>
          <span className="eyebrow">INTELLIGENT SERVICE OPERATIONS</span>
          <h1>
            Less friction.
            <br />
            More resolution.
          </h1>
          <p>
            A connected workspace for the people who keep your business moving.
          </p>
          <div className="auth-feature">
            <Activity /> Incidents, expertise, and service levels.
            <br />
            Working together.
          </div>
        </div>
        <footer>
          <ShieldCheck size={16} /> Built around accountability.
        </footer>
      </aside>
      <main className="auth-form">
        <span className="eyebrow">YOUR SERVICE WORKSPACE</span>
        <h2>{titles[mode]}</h2>
        <p className="muted">
          {mode === "login"
            ? "Sign in to manage your IT support."
            : "Secure access for your team."}
        </p>
        <form onSubmit={submit}>
          {mode === "register" && (
            <Field label="Full name">
              <input name="name" required maxLength={100} autoComplete="name" />
            </Field>
          )}
          {!["verify", "reset"].includes(mode) && (
            <Field label="Work email">
              <input
                name="email"
                type="email"
                required
                autoComplete="email"
                placeholder="you@company.com"
              />
            </Field>
          )}
          {["login", "register", "reset"].includes(mode) && (
            <Field label="Password">
              <input
                name="password"
                type="password"
                required
                minLength={mode === "login" ? 1 : 12}
                maxLength={72}
                autoComplete={
                  mode === "login" ? "current-password" : "new-password"
                }
              />
            </Field>
          )}
          {error && (
            <div role="alert" className="error">
              {error}
            </div>
          )}
          {message && (
            <div role="status" className="success">
              {message}
            </div>
          )}
          <button className="primary wide" disabled={busy}>
            {busy ? "Please wait…" : humanMode(mode)}
            <ArrowUpRight size={18} />
          </button>
        </form>
        <div className="auth-links">
          {mode !== "login" && (
            <button
              onClick={() => {
                setMode("login");
                setMessage("");
                setError("");
              }}
            >
              Back to sign in
            </button>
          )}
          {mode === "login" && (
            <>
              <button onClick={() => setMode("forgot")}>
                Forgot password?
              </button>
              <button onClick={() => setMode("register")}>
                Create an account
              </button>
              <button onClick={() => setMode("resend")}>
                Resend verification
              </button>
            </>
          )}
        </div>
        <p className="auth-note">
          Your session stays in memory and expires after one hour.
          <br />
          Reloading this page signs you out.
        </p>
      </main>
    </div>
  );
}
function humanMode(m) {
  return {
    login: "Sign in",
    register: "Create account",
    forgot: "Send recovery link",
    verify: "Verify email",
    reset: "Update password",
    resend: "Send verification link",
  }[m];
}
