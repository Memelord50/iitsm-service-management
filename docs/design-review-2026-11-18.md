# Design review and requested changes — 18 November 2026

This is the original review checkpoint. The user has since supplied the checklist and approved account/chat decisions. See checklist-gap-review-2026-11-18.md for the current decisions and gaps, and approved-changes-validation-2026-11-18.md for new test results. The table below preserves the earlier baseline.

| Area | Current implementation | Requested change / decision needed |
|---|---|---|
| Sign-in | One shared form; backend returns the user's actual role | Three role tabs on the same page. Proposed: enforce tab/role match on the backend, never grant a role based on tab selection. |
| Registration | Public registration always creates an unverified employee | Keep public registration on employee tab only. Remove the need for technicians/admins to self-register and be promoted. |
| Staff account creation | Admins edit roles/status; no create-account form/API | Add admin-only creation of technicians and admins. User requested administrator sharing of initial credentials. Await choice of verification before first login versus immediate access. |
| Account verification | Hashed, single-use, one-hour tokens; mail queued | SMTP is unconfigured and defaults to localhost:1025, with no listener observed. Need real sender/provider configuration or explicit local-mail-catcher choice. |
| Password changes | Public forgotten-password/reset-link flow; token version invalidates sessions | Add an explicit authenticated change-password-by-email entry point for every role. Proposed: send to current account email, use existing single-use reset flow. |
| Existing demo emails | @iitsm.local records are verified manually | These cannot receive real inbox mail. Decide how to supply real addresses or use a local mail catcher for these test accounts. |
| Duplicate email | Generic uniqueness-conflict message | Source now returns “This email already exists.” for existing registration emails and concurrent email-uniqueness conflicts. Other constraint conflicts retain their own generic message. |
| Incident discussion | Comments plus request-information/respond actions; internal notes hidden from employees | Add dedicated employee/assigned-technician chat. Await availability timing and whether a reply or separate completion action resumes the SLA. |
| Chat delivery | Normal request/response fetching; no push transport | Decide periodic refresh versus live push, history visibility for replacement technicians, admin visibility and whether chat remains usable after resolution/closure. No transport or access policy chosen yet. |
| SLA | Response stops on first acknowledgement; resolution pauses for employee information; immutable original snapshots plus appended revisions | Chat must preserve these clocks. Do not equate a casual message with completed information unless explicitly chosen. |
| Mail failures | Generic error, automatic retry limited to three attempts | Need safe actionable failure categories and a decision on retry/resend controls. Old one-hour tokens may already have expired, so blindly replaying old mail is insufficient. |
| Notifications | SMTP worker holds global settings lock during external sends | Can block business mutations when SMTP is slow. Refactoring needs delivery-claim/idempotence design and concurrency tests. |
| Assignment | AI request occurs while global serialization lock is held | Correctness-first but slow under load. Future improvement requires advice outside lock and revalidation inside transaction. |
| Workload/reports | Matching records loaded and counted/aggregated in Java | SQL counts/aggregates and history pagination would reduce memory/query overhead. |
| UI session | JWT held in memory; refresh signs out; page selection is local state | Persistence, refresh-token and URL navigation choices are not being changed without discussion. |
| Connectivity label | Static “Connected workspace” label | Not a health check. Decide whether to remove it or show measured connection status. |

## Implementation/testing performed at this checkpoint

- Changed duplicate-email registration and uniqueness-race error messages.
- Added three backend regression cases: normalized duplicate registration, database email-conflict mapping and unrelated-constraint behavior.
- H2 backend regression: 70 passed, zero failures/errors/skips.
- MySQL backend regression against the dedicated iitsm_test schema: 70 passed, zero failures/errors/skips.
- Frontend regression: eight passed; Vite production build passed.
- Live HTTP/MySQL demonstrations rerun successfully with the extra technician credentials: all three main workflows plus administrator/access checks passed against the currently running server. These do not exercise the new duplicate-email source until a rebuilt server is launched.
- Initial test attempt was blocked by sandbox socket permissions; rerun used granted local network access and an explicit Mockito Java agent.
- Initial live demonstration attempt stopped after incident creation because the script knew only the two original technicians. It did not indicate a routing failure. The labeled development incident remains; no user incident was deleted.
- Updated demonstration script to accept E2E_EXTRA_TECHNICIANS as a private environment JSON array of email/password objects. No credentials are stored in that script or results.
- A running server does not automatically pick up these source changes. New account/chat UI is not yet implemented, packaged or deployed.

## Acceptance checks after decisions and implementation

1. Employee/technician/admin tabs authenticate only the matching role; tab selection cannot elevate access.
2. Public registration cannot supply a staff role, activation or verified status; duplicate email message is clear.
3. Only current active admins can create staff accounts; every creation is audited and passwords are hashed.
4. Account verification and password-change mail reach the agreed mail system; expired/reused tokens fail.
5. Password change invalidates previous sessions; another user cannot choose the recipient account.
6. Incident chat is restricted to the agreed participants, protects internal notes, and enforces the agreed lifecycle.
7. Chat messages do not incorrectly reset response time or resume resolution time; chat history survives reassignment/reopening as agreed.
8. SLA snapshots/revisions, workload transfers, skill credits and always-available fallback retain their existing invariants.
9. Full backend suites against H2 and MySQL, frontend tests/build, live workflows and desktop/mobile browser checks.
10. Map every requirement from the shared conversation to a specific test once its text is available.
