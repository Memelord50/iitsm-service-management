# I-ITSM checklist gap review — 18 November 2026

The user supplied Parts A–T of the exhaustive checklist and approved **reporting gaps first, implementing only separately approved changes**. This document is a source inspection and execution-coverage review, not a claim that the entire checklist passed. The unrelated page export, account/session data and scripts included after the checklist are excluded.

## Approved changes

- Employee, Technician and Admin sign-in tabs; backend checks the selected role. Public registration remains employee-only.
- Admin-only creation of technician/admin accounts with an initial password. These accounts can sign in immediately, while their email remains unverified until a single-use verification link is consumed.
- Every role can request password changes by email. An unverified account receives verification first and must request password recovery again afterwards. Reset-token consumption also requires verification. Password changes invalidate existing JWTs.
- Chat opens at assignment. Reporter, current technician and admins can access it. Replacement technicians inherit history; previous technicians lose access. Internal notes stay hidden from employees.
- Chat refreshes every five seconds while the incident is open and the browser is visible. Closed chat is read-only. Ordinary messages never resume SLA; the explicit Information provided action resumes it.
- Real email delivery selected. Provider and sender address remain unspecified; real delivery is BLOCKED pending configuration and an agreed test inbox.

## Gaps against the supplied catalogue

NOT IMPLEMENTED means a required capability was not found in the inspected source. NEEDS CLARIFICATION means an existing deliberate behavior differs from the pasted checklist and needs a design decision. NOT RUN means the stated test has not been executed; it is never a pass. ID ranges below identify the relevant review scope; they do not assert identical status for every test in a range.

| Section / relevant IDs | Finding | Review status / next work |
|---|---|---|
| A: GEN-001–014 | Startup/migrations covered by regression suites; destructive database outage/restart and seed recovery campaign not executed in this change. | NOT RUN for remaining recovery cases |
| A/O: GEN-016–018,030; UI | Session is memory-only and refresh signs out; navigation is component state, without deep URLs/history routing. | NEEDS CLARIFICATION before session/router redesign |
| B: AUTH-001–050 | Registration, verification and hashed single-use tokens exist. Staff immediate-login exception is approved. No server-side logout revocation; suspended account state is absent. | NOT IMPLEMENTED for suspension/logout revocation; real inbox delivery BLOCKED |
| B: AUTH-051–067 | Staff creation added. User list paginated; no name/email editor or user-search form. Internal IDs are numeric, no UUID/display-ID pair. | NOT IMPLEMENTED / NEEDS CLARIFICATION |
| C: INC-001–025 | Deterministic priority, routing, SLA and audit creation exist. AI description classification and service-owned criticality snapshot are absent. Reporter supplies criticality. | NOT IMPLEMENTED / NEEDS CLARIFICATION |
| C: INC-026–043 | No pre-investigation incident-edit workflow. | NOT IMPLEMENTED |
| C: INC-044–067 | Current states: NEW, ASSIGNED, IN_PROGRESS, WAITING_FOR_EMPLOYEE, RESOLVED, CLOSED, REOPENED. No REPORTED/CLASSIFIED/PENDING_ASSIGNMENT/ACKNOWLEDGED/INVESTIGATING/CANCELLED state vocabulary. Explicit acknowledgement is a timestamp event. | NEEDS CLARIFICATION; do not rename states piecemeal |
| C: INC-068–106 | Reclassification, affected-user estimate and priority-review approval requests absent. | NOT IMPLEMENTED |
| C: INC-107–119 | Reopening creates a resolution cycle but can retain an eligible technician. Rejected resolution enters REOPENED and needs START. | NEEDS CLARIFICATION |
| C: INC-120–131 | Cancellation absent. | NOT IMPLEMENTED |
| D: WORK-001–014 | Explicit acknowledgement exists; opening a detail does not acknowledge. | Existing regression coverage; full UI sequence NOT RUN |
| D: WORK-015–026 | Missed-acknowledgement monitoring and automatic reassignment absent. | NOT IMPLEMENTED |
| D: WORK-027–070 | Scoped notes/chat exist. Explicit Information provided replaces any expectation that ordinary messages resume SLA. Non-response reminders/admin flagging absent. | Approved policy supersedes automatic-resume cases; reminders NOT IMPLEMENTED |
| D: WORK-071–104 | Resolution/verification/rejection history exists. Separate root-cause/prevention fields and admin verification for inactive reporter absent. | NOT IMPLEMENTED for those capabilities |
| E: CAT-001–025 | Catalog activation exists; service criticality field does not. | NOT IMPLEMENTED; historical snapshot design requires review |
| F: AI-001–070 | AI chooses routing strategy only; it does not classify description/category/impact/urgency. No independent labelled classification dataset or confusion matrix. | NOT IMPLEMENTED; classification accuracy cannot be claimed |
| G: PRI-001–027 | Existing formula uses integer weights 3/3/2/1, cutoffs 13/20/27 and three affected-user tiers. Checklist expects 30/30/25/15 percent, 30/55/80 thresholds and four tiers. | NEEDS CLARIFICATION before formula migration |
| G: PRI-028–045 | Existing matrix adds one level for criticality=3 OR affected users>=100. No checklist negative adjustments or summed +/-2 adjustments. | NEEDS CLARIFICATION |
| G: PRI-046–066 | Config revisions/audit exist, but no draft/compare/rollback UI or explicit bulk-recalculation workflow. | NOT IMPLEMENTED |
| G: PRI-067–097 | Override history exists; no separate current calculated/effective values or override-removal workflow. Override reasons exposed in incident detail to employees. | NOT IMPLEMENTED; privacy discrepancy requires review |
| H: ASN-001–028 | Strategies are LEAST_LOADED, SKILL_FIRST, ROUND_ROBIN, PRIORITY_WEIGHTED. No workload cap; names/ranking differ from checklist. | NEEDS CLARIFICATION |
| H: ASN-029–055 | AI strategy adapter and local fallback exist. Fallback is LEAST_LOADED even when all configured strategies disabled; no pending queue retry scheduler. | NEEDS CLARIFICATION for fallback; retries NOT IMPLEMENTED |
| H: ASN-056–068 | Admin direct reassignment exists; technician approval-request workflow absent. | NOT IMPLEMENTED |
| H: ASN-069–087 | Skill credit once per incident, threshold 60; no manual skill CRUD. Checklist wants per-resolution-cycle credit and experience. | NEEDS CLARIFICATION / NOT IMPLEMENTED |
| I: SLA-001–090 | Response starts at creation/stops at first acknowledgement; resolution cycles pause/resume, immutable snapshots and appended target revisions exist. Rejection creates a new resolution cycle. Boundary/concurrency/restart catalogue exceeds existing tests. | Partial regression coverage; full catalogue NOT RUN; cycle semantics need review |
| J: NOT-001–038 | In-app events and SMTP outbox exist. Events for missing workflows cannot fire. Real email unconfigured; worker retries at most three times. | BLOCKED for real inbox tests; missing-event notifications NOT IMPLEMENTED |
| J: AUD-001–030 | Audit records exist for implemented mutations; missing workflows have no corresponding events. Full actor/before-after/idempotence matrix not executed. | NOT RUN for full catalogue |
| K: ATT-001–027 | Scoped local-file upload/download, signature/size checks and database metadata exist. Not all storage/database fault windows and concurrency cases exercised. | NOT RUN for remaining fault campaign |
| K: FB-001–020 | Feedback is unique per incident, not resolution cycle. | NEEDS CLARIFICATION before schema/history migration |
| L: SRCH-001–030 | Scoped backend filtering/pagination exists; every combination/date boundary not executed. | NOT RUN for full matrix |
| L: REP-001–049 | Several metrics aggregate loaded records in Java. Full requested response averages, trends and report filters absent. Manual small-dataset reconciliation pending. | NOT IMPLEMENTED / NOT RUN |
| M: DB-001–044 | Foreign keys/uniqueness and transactions exist. Global settings lock serializes many changes. Full fault injection and simultaneous-worker campaign pending. | NOT RUN for full catalogue |
| N: SEC-001–040 | Server role/incident authorization and DTO validation exist; current tests cover selected restrictions, not every sensitive endpoint/role combination. | NOT RUN for full matrix; do not infer security from UI alone |
| O: UI-001–054 | New account/chat UI builds and has targeted component tests. Absent backend workflows have absent screens. Desktop/mobile visual checks and every form edge case pending. | NOT IMPLEMENTED / NOT RUN |
| P: OOP-001–030 | User is concrete with a role field, not abstract with Employee/Technician/Admin subclasses. Strategies/interfaces, service/repository separation and DTOs exist. Lifecycle uses controlled transitions, not a class per state. | NEEDS CLARIFICATION for inheritance requirement; explain actual design |
| Q: Six UI journeys | Existing API demo workflows are not equivalent to six UI journeys. New live-browser end-to-end campaign still needed. | NOT RUN |
| R: PERF-001–040 | No full 1,000-incident load, restart/fault-recovery or clean-machine installation campaign in this turn. | NOT RUN |
| S: 125 viva questions | Existing implementation-learning-report.md explains actual implementation; questions about missing classification/approval/UUID features must be answered as gaps. | Extend only after design decisions |
| T: Execution strategy | Use separate test schema, evidence per case and honest statuses. | Applied to automated regression; comprehensive campaign remains open |

## Design priorities for the next review

1. Agree the target lifecycle and request/approval model before changing states, endpoints or migration history.
2. Agree priority mathematics, service criticality and calculated/effective override semantics together; SLA policies depend on these decisions.
3. Agree assignment eligibility/limits/retries and per-cycle skill/feedback accounting.
4. Decide whether description classification and User inheritance are project requirements or checklist examples.
5. Review employee visibility of override reasons, session persistence/logout, report aggregation and global-lock external I/O.

No changes in these five groups are authorized merely by their presence in the checklist. Regression results for approved changes are recorded separately in approved-changes-validation-2026-11-18.md.
