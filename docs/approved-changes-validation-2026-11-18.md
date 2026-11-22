# Approved account/chat changes — validation, 18 November 2026

## Delivered behavior

Employee/Technician/Admin tabs share one sign-in page. The backend validates an optional selected role against the stored account role; it never grants permissions from the tab. Public registration always creates an employee. Administrators can create technicians/admins with an initial password, and duplicate email returns “This email already exists.”

Flyway V3 adds `app_user.admin_created`, default false. An active, admin-created staff account can authenticate and be assigned while its actual email verification remains false. JWT requests recheck eligibility. Existing accounts are not automatically flagged or reset. Staff passwords are BCrypt hashes; API views omit password hashes.

Every signed-in role has Change password by email. The endpoint derives the recipient from the authenticated account. Unverified users receive verification first and must request recovery again after verifying. A reset token cannot change an unverified user's password. A successful reset invalidates previous JWTs using tokenVersion. SMTP delivery itself remains unconfigured: queued is not delivered.

Chat uses persisted incident comments, with author identity/time and existing notifications/audit. It opens after assignment, is accessible to the reporter/current technician/admin, and preserves history across reassignment. Former assignees lose access. Employee views filter internal notes. Closed chat rejects new posts, including direct API calls. Foreground forms retain text on request failure. Chat polls every five seconds while the incident is open and document visible; cleanup prevents stale poll results after leaving/changing status or starting a mutation.

Messages never change SLA status. Information provided is a separate reporter-only confirmation, reusing the RESPOND API action. It can confirm information already supplied via chat/attachments without reposting it. The server's lifecycle/transaction locks prevent repeated completion. The request-information action still requires text. Mobile grid sizing was corrected after an observed horizontal overflow.

## Executed evidence

| Check | Result | Evidence |
|---|---|---|
| Backend regression against H2 | PASS — 77 tests, 0 failures/errors/skips | work/approved-changes-tests.log |
| Backend regression against MySQL 8.4, separate iitsm_test schema | PASS — 77 tests, 0 failures/errors/skips | work/approved-changes-mysql.log; Flyway V3 validated/applied |
| Frontend component regression | PASS — 11 tests | Vitest src/app.test.jsx |
| Frontend production build | PASS | Vite build, including subsequent mobile CSS fix |
| Packaged executable JAR | PASS | work/approved-changes-package.log; backend/target/i-itsm-1.0.0.jar |
| Admin creates unverified technician via browser | PASS | staff-account-preview.jpg; QA Staff Preview row, verified No |
| New technician signs in immediately via browser/JWT | PASS | Technician portal reached; new JWT/filter regression test |
| Assignment of unverified admin-created technician | PASS | MySQL-backed API fixture and regression |
| Public employee cannot create staff; wrong-role tab rejected | PASS | WorkflowTest and frontend regression |
| Reset requests for unverified account issue verification; old reset cannot bypass | PASS | WorkflowTest token and consumption cases |
| Employee posts from assignment | PASS | Isolated API fixture, preserved browser history |
| Browser chat message leaves WAITING_FOR_EMPLOYEE and paused SLA | PASS | chat-paused-preview.jpg; DOM inspection |
| Browser Information provided confirmation resumes SLA | PASS | chat-resumed-preview.jpg and persisted API state |
| Replacement technician history / former technician denied | PASS | WorkflowTest.reassignmentTransfersChatAccessAndPreservesHistory |
| Closed chat rejects new API message | PASS — HTTP 422 | approved-live-results.json |
| Browser automatically notices closure, retains messages, removes composer | PASS | chat-closed-preview.jpg; no reload/manual navigation |
| Narrow responsive detail | PASS at 390px | DOM clientWidth=scrollWidth=390; chat-mobile-preview.jpg |
| Browser console on final test tab | No error entries observed | qaTab.dev.logs(error) returned [] |
| Real verification/reset inbox delivery | BLOCKED | Provider/sender/credentials not configured |
| Entire supplied A–T checklist | NOT COMPLETED | See checklist-gap-review-2026-11-18.md; no blanket pass claim |

The backend suites include seven new workflow tests beyond the preceding 70-test baseline. They cover immediate staff login and the JWT filter, staff creation permission, role mismatch, verified-email reset guards, chat/SLA separation, closure, and reassignment access/history. Three frontend cases were added beyond the preceding eight-test baseline.

The first backend attempt failed to compile due to a test call with the wrong assignment signature; corrected before validation. The next attempt was blocked by sandbox socket permissions; rerun after local-network permission passed. Preview restart after a new user turn needed renewed local-network permission. A browser error-page tab was replaced after its reload could not recover. These are not application test passes.

## Environment and remaining work

Browser/API smoke checks used port 8082 and **iitsm_test**, with scheduling disabled, no AI key, and separate attachment storage. No real emails were sent. A clearly named QA technician and one closed QA incident remain in that isolated schema. No main-database user credentials or incidents were changed by this validation.

The updated packaged JAR exists; the separately running main application on port 8081 was not restarted. Restart it using the existing run-packaged.sh workflow to load the new JAR and apply Flyway V3. The temporary 8082 preview uses test accounts, not the seven main-database accounts.

Real mail requires provider/sender identification followed by credentials entered privately into local configuration and an agreed test inbox. Full desktop/mobile journeys, every checklist edge case, load/fault/concurrency campaign and classification quality evaluation are not claimed complete. Structural checklist changes remain pending review/approval.
