# I-ITSM Phase 3 final report

The validation corrections, SLA history correction, per-job mail delivery transactions, and frontend improvements are implemented. The core browser demonstration passed. Submission sign-off remains open because SMTP interruption and load behavior, detailed report reconciliation, and mobile visual acceptance were not verified.

## Phase 1 and Phase 2 findings

| Finding | Disposition | Evidence or remaining decision |
|---|---|---|
| H04 blank passwords | Fixed and regression-tested | Registration, reset and staff creation each return HTTP 400 for a blank password. |
| H05 fractional affected-user estimates | Fixed and regression-tested | HTTP rejects 1.7. Jackson rejects float-to-integer coercion for integral request fields. Existing integer limits remain in place. |
| M01 malformed search timestamp | Fixed and regression-tested | HTTP returns 400. Blank and valid ranges still return 200. |
| M01b reversed search dates | Fixed and regression-tested | HTTP returns 400 with a start/end explanation. |
| M02 calendar boundaries and priority order | Implemented | The UI uses local midnight and the next local midnight as an exclusive upper bound. The legacy API to filter stays inclusive. Severity ordering uses an explicit numeric rank. The sort query ran on both databases. Full boundary and rank fixtures were not executed. |
| M03 attachment failure hidden by success | Fixed and regression-tested | A component regression verifies that incident creation returns the attachment failure to the parent. The parent displays that failure in the final toast. |
| M04 indefinite loading and expired sessions | Implemented | Major views show explicit retry states after failed loads. Authenticated HTTP 401 returns the user to sign-in. Failed-network browser verification remains open. |
| H01 settings lock held during SMTP | Corrected by inspection | Selection and each job attempt use separate transactions. Only the relevant delivery row is locked during SMTP. Each job rechecks its status after locking. Delayed-SMTP latency measurements were not performed. |
| H02 repeat send after interruption | Remaining delivery limitation | Per-job commits narrow the interruption window. SMTP acceptance and database commit are still separate operations. Retrying after interruption may send a duplicate. No process crash experiment was run. |
| H03 elapsed SLA breach before revision | Fixed and regression-tested | The old target is evaluated before inserting a revision. Tests cover one second before, exactly at and one second after the original deadline. The recorded breach retains the original deadline and detection time. |
| M05 documentation drift | Partially corrected | README, technical design and limitations now describe staff creation, V3 and mail delivery. Historical QA reports remain historical evidence. |
| M06 Vite port | Existing setup constraint | Vite proxies to 8080. Use the documented development port or the packaged same-origin application. The packaged app started successfully on 8083. |
| M07 client-side logout | Existing session policy | Reset and account changes invalidate prior sessions. Immediate JWT revocation on logout remains a product decision. |
| M08 auth throttling | Remaining deployment limitation | No application rate limiter was implemented or tested. |
| M09 independent outstanding reset links | Remaining policy decision | The existing single-use policy applies per issued token. Parallel outstanding token invalidation remains open. |
| M10 report scalability | Remaining performance limitation | Current aggregates use Java collections. Large-dataset performance was not measured. |
| L01 verification/reset completion UX | Open | A completed form can still be displayed until the user returns to sign-in. |
| L02 SLA/skill environment initialization | Existing configuration rule | Existing database settings control operation. Seed environment values initialize demo data. |
| L03 staff edit/search and delivery operations | Scope limitation | Current role and availability controls remain available. Broader editing, search and manual retry were not added. |

## Changes and recovery

Backend changes are in AuthController, AdminController, application.properties, SearchService, SlaService and NotificationService. WorkflowTest now includes HTTP input regressions and SLA revision boundary coverage. No migration files changed. The existing application directory has no Git metadata. A source and documentation checkpoint was saved at work/phase3-checkpoint.tar before edits.

Frontend changes are in style.css, Dashboard.jsx, Incidents.jsx, IncidentDetail.jsx, Admin.jsx, main.jsx, api.js and app.test.jsx. The design uses a dark navigation rail, denser metric strips, a consistent panel and form treatment, clearer keyboard focus, and queue summaries for technician and administrator dashboards. Incident creation distinguishes attachment failure from incident success. The administrator queue links to workload, SLA monitoring and reports.

## Executed verification

| Check | Result |
|---|---|
| H2 Maven suite | PASS, 83 tests |
| Fresh MySQL Maven suite | PASS, 83 tests, V1 through V3 applied on iitsm_qa_phase3 |
| Frontend Vitest | PASS, 12 tests |
| Vite production build and Maven package | PASS |
| Packaged startup | PASS, isolated loopback at 8083 with scheduling disabled and no external integrations |
| Browser employee reporting | PASS, synthetic incident displayed as Assigned and Medium |
| Browser technician investigation | PASS, assigned queue, acknowledgement, start and information request |
| Browser SLA and resolution | PASS, waiting state and paused resolution clock, explicit Information provided returned incident to In progress and resumed the clock, technician resolved and employee verified |
| Browser reopen and administrator review | PASS, closed chat displayed read-only, reopened incident retained stopped response and original resolution cycles and added a running resolution cycle |
| Browser configuration and reports | PASS for navigation and observation. One fixture yielded total 1, reopened 1, medium 1, average resolution 43 seconds and zero breaches. The completed resolution cycle displayed 43 active seconds. |
| Failed tests during implementation | The initial frontend run caught the no-attachment callback contract change. The callback was corrected and the final suite passed. |

The browser run used all three seeded roles. Desktop Reports was visually inspected. Complete screenshot coverage, mobile inspection, feedback and rejection browser flows, ordinary chat during an SLA pause, live attachment failure, and the full authorization matrix were not executed. Existing integration tests provide coverage for some of those behaviors at the service or HTTP level.

SMTP retry timing, crash recovery, concurrent assignment experiments and full reporting reconciliation were not executed. The tests and browser demonstration used isolated databases and synthetic identities. No actual SMTP or external AI calls were made. No schema upgrade test was necessary for this patch because migrations did not change.

Evidence and commands are in work/qa-phase3. The temporary application server was stopped. Final sign-off needs mobile checks and the remaining controlled reliability tests, with explicit acceptance of the SMTP duplicate-delivery limitation.
