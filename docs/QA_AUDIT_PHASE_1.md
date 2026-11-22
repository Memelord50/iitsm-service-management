# I-ITSM — Phase 1 QA audit

Audit date: 21 November 2026. Scope: inspection, existing automated tests, build checks and limited isolated HTTP probes. No application source, configuration or migration fixes were made.

## 1. Overall project assessment

**Verdict: not yet ready for unconditional submission sign-off.** The application is a functioning integrated modular monolith, not merely a UI prototype. Both builds succeed, 77 backend tests pass on H2 and again on fresh MySQL, and 11 frontend tests pass. However, independently executed HTTP probes found three validation defects, and source inspection found reliability and usability concerns. Full browser demonstrations, concurrent transactions, delivery failure recovery, and detailed numerical reporting remain unverified.

This verdict is about evidence and remaining defects, not a recommendation to redesign. No critical exploitable vulnerability was demonstrated. Passing these suites is not proof that all requirements or the later exhaustive checklist pass.

### Specification baseline

The original master specification was found in the attached `Pasted text.txt` (42,439 bytes). Its sections 1–28 are the primary baseline. Sections 25–26 describe development process; sections 27–28 restate acceptance and execution requirements. The matrix below covers the functional and technical requirement groups and the acceptance evidence; it is not an execution record for every checkbox in the later A–T checklist.

The later exhaustive checklist introduces materially different requirements. The original explicitly permits NEW/ASSIGNED/IN_PROGRESS/WAITING_FOR_EMPLOYEE/RESOLVED/CLOSED/REOPENED; requires an always-available fallback even when disabled for normal selection; does not mandate UUID user IDs, abstract User subclasses, percentage priority weights, AI text classification, or named skill/load/experience/priority assignment algorithms. Those differences must not be incorrectly counted as violations of the original.

The user's subsequent approved decisions also apply: chat from assignment; separate Information provided action resumes SLA; administrator/replacement-technician access to chat; closed chat read-only; periodic refresh; administrator-created staff can log in immediately but must verify email before password changes. Broader redesign remains unapproved.

### Evidence terminology

- **Implemented**: source path exists and meets the inspected requirement, subject to verification limits.
- **Partial**: some required behavior exists, with a stated limitation.
- **Missing**: no implementation found in the inspected source.
- **Broken**: an executed probe observed an incorrect result.
- **Untested**: behavior has not been established by execution in this audit.
- **PASS** below applies only to the stated assertion/test, not the whole feature.
- **Static** findings are code-path analysis, not runtime reproductions. **Risk** denotes an unexecuted failure scenario.

## 2. Features implemented

The source implements three roles, employee registration, JWT/BCrypt login, email-token issuance/consumption, staff creation, account activation/role changes, scoped incidents, seven-state lifecycle, acknowledgement, investigation notes/chat, resolution verification/rejection/reopening, attachments, feedback, two priority strategies, four deterministic assignment strategies, an external strategy advisor with fallback, skill recognition, dynamic workload accounting, independent SLA clocks, snapshots/revisions/pauses/signals, notifications, audit history, catalog administration, search, dashboards and reporting.

Meaningful OOP mechanisms include PriorityEngine.Strategy, AssignmentStrategy, StrategyAdvisor and FileStorage; strategy registries; Spring event publication; centralized lifecycle rules; repository abstraction; and dependency injection. The source uses public mutable entity fields and string-valued roles/states, so encapsulation is substantially weaker than the interface-oriented design.

### Requirement-to-implementation matrix

Paths below are relative to the project root. `B:` means `backend/src/main/java/com/iitsm/`; `F:` means `frontend/src/`. Tests: **D** = DomainTest (24), **W** = WorkflowTest (49), **E** = ExternalProviderTest (2), **X** = common/ErrorsTest (2), **F** = frontend/src/app.test.jsx (11). All existing tests in these suites passed in this audit. A suite named in a row means related coverage exists, not complete requirement coverage. “No” means no dedicated assertion was identified. “Static” means not behaviorally verified beyond inspection.

| ID / original section | Requirement | Status | Source files/classes | Automated tests | Actual verification / remaining concern |
|---|---|---|---|---|---|
| R01 §2 | Java 25, Spring Boot 4, Maven, React/JS, MySQL/JPA/Flyway | Implemented | backend/pom.xml; frontend/package.json; application.properties | W | PASS builds, MySQL connection and Flyway; no fresh dependency download audit. |
| R02 §4,16 | Modular monolith, controllers/services/persistence boundaries | Implemented | B: auth, incident, priority, assignment, sla, report, common packages | W | Static architecture review; generic Store plus UserRepository, combined service/category catalog. |
| R03 §3.1,19 | Employee-only public registration and verification | Partial | B: auth/AuthController, AuthService; F: Auth.jsx | W verificationTokenIsSingleUse; F registration | PASS token single-use test; BROKEN whitespace-only password accepted in HTTP probe. |
| R04 §3,19 | Three-role login and protected API access | Implemented | B: auth/JwtTokens, JwtFilter, Access, SecurityConfig | W successfulLogin, invalidLoginDenied, httpRoleAuthorizationUsesJwtFilter; F login | PASS all three isolated role logins; unauthenticated incidents/me 401; employee admin-users request 403 in W. |
| R05 §19 | Password hashing, recovery, token expiry/revocation | Partial | B: auth/AuthService, JwtTokens, AccountToken; user/User | W recoveryChangesPasswordAndInvalidatesTokens | PASS reset changes password/version; expiry/concurrent consumption matrix not executed; multiple outstanding reset links remain independently usable. |
| R06 §3.3 | Manage roles, activation and technician availability | Implemented | B: config/AdminController, AdminService.user; F: Admin.jsx | W unavailableTechnicianExcluded (partial) | Static self-demotion and active-work safeguards; full admin HTTP mutation matrix untested. |
| R07 approved | Admin creates technician/admin with initial password, immediate login | Implemented | B: AuthService.createStaff; User.canSignIn; V3 migration; F: Admin.jsx | W adminCreatedStaffCanLoginWithoutFalsifyingVerification, employeesCannotCreateStaff | PASS relevant service/filter assertions; live staff creation UI untested. |
| R08 approved | Verified email required for password change | Implemented | B: AuthService.requestToken/consume; AuthController.changePassword | W unverifiedPasswordRecoveryRequestsVerificationFirst, unverifiedAccountCannotConsumePreviouslyIssuedResetToken | PASS tests; real verification-to-reset mailbox journey not run. |
| R09 §3.1–3.3 | Employee ownership, current technician scope, administrator access | Implemented | B: Access; SearchService; IncidentService.detail | W employeeCannotReadAnotherIncident, searchIsScopedAndFiltered, reassignmentTransfersChatAccessAndPreservesHistory | PASS named cases; not every endpoint/role pair tested. |
| R10 §5.1 | Create incident with title, description, catalog and numeric factors | Partial | B: IncidentController.Create; IncidentService.create; F: Incidents.jsx | W creationStartsBothClocksAtCreation; F creation | PASS normal creation; BROKEN fractional affectedUsers 1.7 accepted and stored as 1. |
| R11 §5.1 | Unique reference, reporter/time/status/priority/SLA/assignment | Implemented | B: IncidentService.create; migrations V1/V2 | W creationStartsBothClocksAtCreation, assignmentAloneDoesNotStopResponse | PASS clocks and assignment; reference uniqueness constrained; collision retry not implemented/tested. |
| R12 §5.2 | Controlled lifecycle and explicit acknowledgement | Implemented | B: Lifecycle; IncidentService.action; F: IncidentDetail.jsx | D lifecycle tests; W startRequiresAcknowledgement | PASS valid/invalid examples; exhaustive transition/role matrix untested. |
| R13 §5.3 | Admin reassignment with history, actor/reason, notices | Implemented | B: AssignmentEngine.assign; AssignmentHistory; IncidentService.reassign | W reassignmentBeforeAcknowledgementTransfersWorkload, reassignmentAfterAcknowledgementDoesNotRestartResponse | PASS workload/response/history cases; concurrent reassignment untested. |
| R14 §5.4 | Resolution description and employee acceptance/rejection | Implemented | B: IncidentService.action; Resolution; F: IncidentDetail.jsx | W completeWorkflowAuditNotificationsFeedback, rejectionDoesNotCreditSkill | PASS service workflows; resolution verifier is implicit reporter, no separate verifier field. |
| R15 §5.4,9.6 | 90-day configurable reopen preserving history | Implemented | B: IncidentService.action; SlaService.reopen; Settings | W reopenPreservesResponseAndAddsResolutionCycle, reopenWindowEnforced | PASS ordinary reopen and day 91 denial; exact day 90 boundary not tested. |
| R16 §3.2 | Internal notes protected from employee reads | Implemented | B: IncidentService.comment/detail; Comment | W employeeCannotReadInternalNotes | PASS note-body exclusion; audit contains note-added event without note body. |
| R17 approved | Chat available from assignment, ordinary messages do not resume SLA | Implemented | B: IncidentService.comment/action; F: IncidentDetail.jsx | W messagesDoNotResumeSlaAndExplicitCompletionDoes; F chat test | PASS message versus RESPOND separation; explicit button allows completion without a new text payload as approved. |
| R18 approved | Admin/replacement history access, closed read-only chat | Implemented | B: Access; IncidentService.comment/detail; F: IncidentDetail.jsx | W closedChatIsReadOnlyAndUnrelatedUsersCannotRead, reassignmentTransfersChatAccessAndPreservesHistory; F closed chat | PASS covered assertions; administrator read and 5-second polling inspected, not browser-executed here. |
| R19 §6 | Formula strategy using all four factors | Implemented | B: PriorityEngine.Formula; Settings; F: Admin.jsx | D formulaUsesAllFourFactors, formulaConfigurationChangesResult | PASS examples; default weighted integer score documented, not percentage formula from later checklist. |
| R20 §6 | Configurable matrix priority strategy | Implemented | B: PriorityEngine.Matrix; Settings | D matrixUsesImpactAndUrgency, matrixEscalatesCriticalBusiness, matrixEscalatesAffectedUsers | PASS examples; nine cells configurable, one-level modifier; exhaustive combinations untested. |
| R21 §6 | Configuration version and calculation factors/history | Partial | B: AdminService.settingsSave; PriorityHistory; Events | W priorityRevisionPreservesSnapshotAndStart (related) | Static revision counter plus before/after audit; no structured historical configuration retrieval/rollback; not mandated rollback in original. |
| R22 §6 | Authorised override with reason/actor and SLA revision | Implemented | B: IncidentController.Override; IncidentService.override; SlaService.revise | W employeeCannotOverridePriority, priorityRevisionPreservesSnapshotAndStart | PASS tested cases; separate calculated/effective values and removal absent (later checklist). |
| R23 §7.1 | Four independent selectable strategies | Implemented | B: LeastLoaded, SkillFirst, RoundRobin, PriorityWeighted, AssignmentStrategy | D strategy ranking tests | PASS deterministic ranking examples; algorithms match documented original-permitted choices. |
| R24 §7.2 | Priority-aware deterministic suitability/load ranking | Implemented | B: PriorityWeighted; AssignmentStrategy.choose; Settings | D priorityRankingUsesSeverity, leastLoadedHasDeterministicTieBreak | PASS examples; priority multiplies skill weight, ID final tie-break. |
| R25 §7.1,7.3 | Enable/disable normal strategies; validate AI choice | Implemented | B: AdminService.settingsSave; AssignmentEngine.select | D aiSelectsEnabledJavaStrategy, disabledAiChoiceRejected | PASS mock advisor choices; configuration UI inspected. |
| R26 §7.3 | Real external LLM adapter through abstraction | Implemented, live untested | B: StrategyAdvisor; ExternalLlmAdvisor | E externalProviderSendsContextAndParsesStrategy | PASS controlled local HTTP provider; no live provider/key/quota validation in audit. |
| R27 §7.4 | Always-available fallback, including all normal strategies disabled | Implemented | B: AssignmentEngine.select | D missingKeyUsesFallback, disabledFallbackStillAvailable; W fallbackWorksWithEveryNormalStrategyDisabled | PASS. This is compliant with original and conflicts with later checklist's enabled-only fallback rule. |
| R28 §7.3–7.4 | Record AI/fallback decision and no arbitrary technician bypass | Implemented | B: AssignmentEngine.auto/assign; AssignmentHistory | D selection; W fallbackWorksWithEveryNormalStrategyDisabled | PASS selected source/strategy tests; raw exception messages used as fallback reasons need defensive review. |
| R29 §7 | Eligibility/availability and no candidate handling | Implemented | B: AssignmentEngine.auto/assign; User.canSignIn | W unavailableTechnicianExcluded, noEligibleTechnicianPreservesNewIncident | PASS; NEW remains unassigned and admins notified. Automatic retry/capacity limit not in original mandatory rules. |
| R30 §7.5 | Automatic skill recognition at configurable default 60 | Implemented with documented policy | B: SkillService; TechnicianSkill; SkillCredit; Settings | W skillRecognizedAtConfiguredThreshold, rejectionDoesNotCreditSkill, verifiedCreditNotDuplicatedAcrossReopening | PASS threshold 1 test and per-incident deduplication; 59/60/61 boundary untested; per-cycle credit differs from later checklist. |
| R31 §8 | Workload from actual active assigned incidents | Implemented | B: AssignmentEngine.active/workload | D workloadStateDefinition; W completeWorkflowAuditNotificationsFeedback | PASS creation, resolve, closure and reopened workload assertions; SQL count optimization absent. |
| R32 §8 | Reassign active/resolved/reopened workload without double-count | Implemented | B: AssignmentEngine.assign; IncidentService.action | W resolvedReassignmentDoesNotAddWorkload, reopenReassignsUnavailableOwnerWithoutLosingPreviousAssignment | PASS named cases; global settings lock serializes writes, multi-thread race tests absent. |
| R33 §9.1 | Service/priority SLA policies with versions and admin-only updates | Implemented | B: AdminService.policy; SlaPolicy; F: Admin.jsx | W deactivatedPolicyLeavesSnapshotIntact (related) | Static validation and revision logic; full policy CRUD/API validation not independently tested. |
| R34 §9.2 | 24/7 UTC Instant persistence/local display | Implemented | B: SLA/entities; application.properties; F: api.date | D elapsed tests; W persistence | PASS MySQL persistence in suite; no persisted LocalDateTime found; timezone boundaries not tested. |
| R35 §9.3 | Response starts at creation, stops first explicit ack only | Implemented | B: SlaService.initial/acknowledge | W assignmentAloneDoesNotStopResponse, acknowledgementStopsOnlyResponse | PASS; reassignment and reopen preserve original response stop. |
| R36 §9.4 | Initial resolution starts at creation and stops at resolution | Implemented | B: SlaService.initial/resolve | W creationStartsBothClocksAtCreation, completeWorkflowAuditNotificationsFeedback | PASS assertions; UI demonstration not rerun. |
| R37 §9.4 | Pause only waiting for employee; accumulated pause/resume | Implemented | B: SlaService.pause/resume; PauseRecord | D pausedResolutionDoesNotAccrueTime; W pauseAndResumePreserveElapsed, responseNeverPausesWithResolution | PASS tested period; multi-period/concurrent action boundary coverage limited. |
| R38 §9.5 | Immutable original SLA snapshot | Implemented | B: SlaCycle non-updatable columns | W snapshotColumnsRemainImmutableAfterFlush | PASS persisted snapshot unchanged after flush on both databases; direct database writes are not prevented by JPA metadata. |
| R39 §9.5 | Append revisions preserving start, pauses, actor/policy/version | Implemented | B: SlaRevision; SlaService.revise | W priorityRevisionPreservesSnapshotAndStart, priorityRevisionPreservesAccumulatedPauseAndDeadlineOrigin | PASS tested revisions; old-deadline crossing before revision is an untested risk (H03). |
| R40 §9.5 | Missing new policy preserves prior target and alerts admin | Implemented | B: SlaService.revise | W missingPolicyPreservesTargetAndNotifiesAdmin | PASS; creation/reopening without policy rejected atomically by service transaction. |
| R41 §9.5 | Deactivated policy does not change old snapshots | Implemented | B: AdminService.policy; SlaCycle | W deactivatedPolicyLeavesSnapshotIntact | PASS snapshot assertion; concurrent policy changes untested. |
| R42 §9.6 | New resolution cycle on reopening, original response retained | Implemented | B: SlaService.reopen; IncidentService.action | W reopenPreservesResponseAndAddsResolutionCycle | PASS; rejection also creates new cycle, documented policy choice. |
| R43 §9.7 | Configurable 50/80 warnings and breach after deadline | Implemented | B: SlaService.detect/signal; Settings | W warningsAt50And80AndBreachAreIdempotent, warningThresholdsAreConfigurable | PASS fake-clock checks and separate deadline/detection values; scheduler itself disabled in audit tests. |
| R44 §9.7 | Idempotent signals and technician/admin notices | Implemented | B: SlaSignal; Events; V1 unique constraint | W warningsAt50And80AndBreachAreIdempotent | PASS repeated serial detection; concurrent workers/downtime catch-up untested. |
| R45 §9.8 | SLA monitor for active/stopped/warnings/breaches | Partial | B: ReportService.slaDashboard; F: Admin.jsx | No dedicated UI assertion | Static general paginated monitor; no dedicated active/50/80/approaching-deadline filters. |
| R46 §9.8,14.2 | Compliance by service/priority and historical performance | Partial | B: ReportService.compliance/dashboard; F: Admin.jsx Reports | W reportCountsRealIncidents (counts only) | Snapshot-priority combined-clock grouping exists; historical incident counts exist; no SLA compliance time series or independent numerical reconciliation. |
| R47 §10 | Append-only audit actor/entity/time/details | Implemented | B: Events, AuditLog; QueryController.audit | W completeWorkflowAuditNotificationsFeedback | PASS existence only; exact before/after/recipient coverage for every event not tested; no ordinary edit/delete API. |
| R48 §11 | Central in-app event notifications | Implemented | B: Events.Notice; NotificationService.notice | W completeWorkflowAuditNotificationsFeedback | PASS rows exist; event-by-event title/content/recipient assertions absent. |
| R49 §11 | Configurable email with persisted status/failures | Partial | B: NotificationService.deliver; Notification; MailJob; F: Admin.jsx | JavaMailSender mock exists; no delivery-worker assertions found | Static 3-attempt outbox; real recipient previously confirmed receipt, not rerun/current audit PASS. H01/H02 risks. |
| R50 §11 | Email failure does not undo originating incident | Implemented, failure untested | B: NotificationService scheduled transaction separate from incident | No dedicated failure test | Static transaction separation; slow worker still blocks shared lock; crash duplicate window remains. |
| R51 §12 | External file bytes, metadata, replaceable storage | Implemented | B: AttachmentService; FileStorage; LocalFileStorage; Attachment | W attachmentAccessRestricted | PASS synthetic text upload/download; rollback cleanup registered, failure cleanup not injected. |
| R52 §12,19 | Type/size/generated names/access/no overwrite | Partial | B: AttachmentService.detect/upload; LocalFileStorage; AttachmentController | W malformedAttachmentRejected, attachmentAccessRestricted | PASS fake PDF rejection and unauthorized download; signatures are shallow, no full decode/malware scan; full size/path matrix untested. |
| R53 §12 | Audited attachment upload/deletion, paths hidden | Implemented | B: AttachmentService; AttachmentController | No dedicated deletion assertion | Static logical deletion retains bytes; uploader/admin permission; no raw storage key projected. |
| R54 §13 | Closure-only feedback with rating/comment/time and duplicate prevention | Implemented | B: IncidentService.feedback; Feedback; V1/V2 | W feedbackBeforeClosureDenied, duplicateFeedbackDenied, completeWorkflowAuditNotificationsFeedback | PASS per-incident rules; feedback reports aggregate average, not per-technician satisfaction. |
| R55 §14.1 | Server-side role-scoped search and relevant filters | Partial | B: SearchService; F: Incidents.jsx | W searchIsScopedAndFiltered | PASS exact-reference/status/ownership; BROKEN malformed date returns 500; date boundaries/sort concerns M01/M02. |
| R56 §14.1,17 | Pagination, sorting, sensible bounded sizes | Partial | B: SearchService, Store; F: Pager | No dedicated multipage assertion | Static incident size 1–100; priority sorted lexically; histories/catalog/reports unbounded. |
| R57 §14.2 | Counts by status/priority/category/service, workload and trends | Implemented | B: ReportService; F: Dashboard/Admin | W reportCountsRealIncidents | PASS total/active for one incident only; other metrics inspected, not numerically verified. |
| R58 §14.2 | Technician performance, average resolution, SLA breach/compliance | Partial | B: ReportService.technicians/dashboard/compliance | No arithmetic fixture beyond basic counts | Verified incident credit count and average completed cycle active time; no technician-specific duration analytics; Java whole-table aggregation. |
| R59 §15 | Relational schema, FKs/uniques/checks/indexes | Implemented with gaps | V1__initial.sql; V2__constraints.sql; V3__admin_created_accounts.sql | W on fresh MySQL | PASS 3 migrations, 35 FKs, 7 CHECK constraints; many required columns nullable, role/catalog-kind relationships not DB-enforced. |
| R60 §15 | Transactional multi-record operations and history retention | Implemented, concurrency untested | B: IncidentService/AdminService @Transactional; Store.lock; immutable history entities | W integration rollback/snapshot cases | Most W cases execute within test transaction and roll back; real commit/rollback/crash failure coverage limited. |
| R61 §15,20 | Development seed and repeatable migrations | Implemented | B: DemoSeed; Flyway; application.properties | W context startup | PASS fresh MySQL seeded 1 admin/2 tech/2 employees; migrations revalidated on packaged startup. Seed skips if any user exists. |
| R62 §16 | OOP encapsulation/abstraction/polymorphism/composition | Partial | B: strategy/provider/storage interfaces; public entities | D strategy tests | PASS strategy examples; entities expose mutable fields, service guards carry invariants; no abstract-user hierarchy required by original. |
| R63 §16 | Strategy/factory/state/events/repository/DI patterns | Implemented | PriorityEngine registry; AssignmentEngine registry; Lifecycle; Events; Store/UserRepository | D/W | Static real implementations; avoid claiming separate GoF State classes or Factory class. |
| R64 §16–17 | DTOs, validation, consistent status/errors | Partial | Controller record DTOs; Views whitelist; Errors | X duplicate-conflict tests; limited W HTTP | No direct entity serialization on reviewed APIs; real HTTP validation defects found; generic 500 logs exceptions. |
| R65 §17 | REST groups and OpenAPI documentation | Implemented | Auth/Admin/Incident/Attachment/Query controllers; OpenApiConfig | W limited HTTP | PASS /v3/api-docs HTTP 200, 32 paths on isolated packaged app; schemas not exhaustively contract-tested. |
| R66 §18 | Employee interface integrated with API | Implemented, browser untested | F: Auth, Dashboard, Incidents, IncidentDetail, Admin notifications | F component tests | PASS selected mocked-fetch components; not a real browser/backend end-to-end pass. |
| R67 §18 | Technician interface/actions/workload/skills | Implemented, browser untested | F: IncidentDetail, Admin technicians, Dashboard | F technician list; W service workflows | Static role/action controls and real endpoints; fresh UI journey pending. |
| R68 §18 | Administrator management/monitoring/reporting interface | Implemented, browser untested | F: Admin, IncidentDetail, Dashboard | No comprehensive admin frontend tests | Source-backed screens; report limitations and stale setup docs remain. |
| R69 §18 | Loading/empty/error/success/confirmation/form states | Partial | F: components, Auth, main, Admin, Incidents, IncidentDetail | F invalid login and chat tests | Loading/error toasts exist; indefinite loader after failed initial fetch; attachment failure toast overwritten (M03/M04). |
| R70 §18 | Responsive design and reusable components | Implemented, untested visually | F: style.css, components.jsx, main.jsx | F badge/format tests only | CSS breakpoints inspected; no fresh mobile/accessibility/browser visual pass. |
| R71 §19 | Secure tokens, CORS, secrets from environment | Partial | B: SecurityConfig, JwtTokens/AuthService; .env.example; F: api.js | W recovery/filter; F bearer test | JWT memory-only, exact configured CORS; no rate limiter/logout revocation; outbox retains raw links until successful delivery. |
| R72 §19 | Mass-assignment/IDOR protection and safe output | Implemented, incomplete coverage | Controller records; Views; Access; query scoping | W ownership/internal notes; F bearer test | PASS selected access tests; unknown-field/forged-role/all-ID matrix not run. |
| R73 §20 | Configurable DB/JWT/AI/mail/storage/ports/frontend URL | Implemented with operational gaps | application.properties; .env.example; scripts; vite.config.js | Startup/build tests | PASS isolated overrides; Vite proxy fixed 8080 versus prepared backend 8081; threshold env values applied only during initial demo seed. |
| R74 §20,24 | Accurate README/setup/diagrams/API/test docs | Partial | README; docs/technical-design.md, limitations.md, demonstrations.md, api.md, final-report.md | No automated documentation consistency test | Docs exist, but staff/SMTP descriptions stale; full fresh-machine README-only installation not performed. |
| R75 §21 | At least 15 meaningful automated tests and mocked providers | Implemented | backend/src/test; F: app.test.jsx | 77 backend + 11 frontend | PASS both DB suites and frontend; no lint task/coverage gate identified. |
| R76 §22 | Three complete demonstration workflows | Partially verified | docs/demonstrations.md; scripts/e2e.py; Incident/SLA services | W completeWorkflow..., warningsAt50..., reopenPreserves... | Service-level portions PASS; all three fresh browser demonstrations untested. Historical screenshots/results are not current passes. |
| R77 §23 | Reliable failures, concurrency controls, scheduler idempotence | Partial | Errors; Store.lock; SlaService; NotificationService | W repeated serial warnings; X | Locks and constraints present; concurrency/crash/restart/load fault scenarios not executed. |
| R78 §24 | Deliverable package and honest test evidence | Partial | docs/technical-design.md, final-report.md, implementation-learning-report.md, test-results.md, demonstrations.md | Current audit evidence added | Diagrams/schema/OOP/setup/workflows exist; historical reports must be reconciled with this audit before submission. |

## 3. Features partially implemented

- Validation: numeric JSON coercion and whitespace-only passwords bypass intended semantics; invalid date handling is inconsistent.
- Reporting: aggregate incident trends and combined SLA compliance exist, but historical SLA performance, dedicated approaching-deadline views, comprehensive technician performance and independently checked calculations are incomplete.
- Configuration history: revision number and before/after audit exist; structured version browsing is absent. Original requirement does not explicitly mandate rollback.
- Notification delivery: persisted retries exist, but no operator retry after three failures, no crash-safe deduplication of external delivery, and SMTP runs while holding the application-wide lock.
- Frontend resilience: error toasts do not replace loading screens with retry states; initial attachment failure can be overwritten by success feedback.
- OOP: polymorphism and composition are real, but public mutable entity state weakens encapsulation.
- Setup/documentation: staff creation and SMTP statements contradict current behavior; clean-machine installation has not been revalidated.

## 4. Missing features and specification differences

Against the original baseline, dedicated historical SLA performance trends and a focused approaching-deadline/threshold-filtered monitor were not found. The API exposes cycle histories and aggregate compliance, which partially address these requirements.

The following later-checklist items are missing but **must be reviewed as scope additions, not automatically implemented or labelled original-spec defects**: AI incident-text classification/evaluation dataset; expanded REPORTED/CLASSIFIED/PENDING_ASSIGNMENT/ACKNOWLEDGED lifecycle; cancellation; employee editing/reclassification, affected-user/priority/reassignment approval requests; automatic pending/missed-ack retries; workload maximum; exact percentage-weight priority rules; explicit experience-based assignment strategy; UUID user/display IDs; abstract User subclass hierarchy; priority override removal and separate calculated/effective priorities; configuration rollback; per-resolution-cycle feedback/skill credit; employee non-response reminders. The master spec explicitly requires the fallback behavior that the later checklist would reject.

No rate limiter, server-side logout endpoint, malware scanner, lint task, coverage gate, or browser test runner was found. Some are hardening/testing gaps rather than original explicit deliverables.

## 5. Environment readiness, commands and test results

### Safe environment

Application root: `outputs/i-itsm`. The older `outputs/i-itsm-github-ready` copy was not audited as the current application and should not be assumed to contain these changes.

Fresh isolated database created: **iitsm_qa_phase1_20261121**, MySQL **8.4.4**, localhost **33306**, socket `/tmp/iitsm-33306.sock`. Existing `iitsm` and `iitsm_test` were not used for audit mutations. No databases/data were dropped. Local root was used only for isolated audit setup/tests; least-privilege deployment credentials were not validated. This does not establish production database hardening.

Test seed accounts in this fresh database: admin@iitsm.local; technician@iitsm.local; technician2@iitsm.local; employee@iitsm.local; employee2@iitsm.local. Password values are deliberately omitted. The suite reads its test-only seed configuration. No real user account was used. The initial suite rolled back its incidents (zero persisted immediately afterward); later HTTP probes intentionally left synthetic incident/account records in the isolated schema. Do not reuse that schema for assumption-dependent tests without accounting for those fixtures.

H2 uses the test profile's in-memory URL. The test profile supports TEST_DB_URL/TEST_DB_USER/TEST_DB_PASSWORD. The helper test-mysql.sh requires an explicitly supplied URL but does not ensure it is not production: inspect the target before running it. Tests mock JavaMailSender and StrategyAdvisor and disable scheduling. The packaged smoke server also had scheduling disabled, blank AI key, no real SMTP credentials and a local unused SMTP port. No real email was sent.

Browser automation availability was checked using cua.getState: the in-app browser is available. No application browser workflow was run in this audit. This is an availability check, not UI correctness evidence.

### Startup commands identified

From the project root, after configuring local environment and Java/Maven:

```sh
./scripts/run-backend.sh
# Frontend, separate terminal:
cd frontend
npm run dev
```

Production-style local packaging/startup:

```sh
./scripts/build.sh
./scripts/run-packaged.sh
```

build.sh invokes npm ci, npm test, npm run build, then mvn clean package. It was inspected but not run verbatim; installed dependencies and non-clean builds were used to preserve existing working artifacts. run-backend.sh loads .env and runs mvn spring-boot:run. run-packaged.sh loads .env and starts the JAR. Default backend port is 8080; the prepared workspace uses 8081. Vite's proxy is fixed to 8080: match backend port or configure the frontend base URL and CORS. Do not blindly run the smoke/e2e script against the prepared main database.

### Exact automated test/build commands executed

The following were run from the workspace root unless a directory is specified. These are local cached builds, not a clean dependency-install test.

```sh
JAVA_HOME="$PWD/work/tools/jdk-25.0.5.0.3.jdk/Contents/Home" mvn -o -f outputs/i-itsm/backend/pom.xml -Dmaven.repo.local="$PWD/work/m2" "-DargLine=-javaagent:$PWD/work/m2/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" test > work/qa-phase1/backend-h2.log 2>&1
```

Initial result: environment-blocked. 29 reported cases, zero assertion failures, two errors: local socket creation denied in ExternalProviderTest and WorkflowTest context startup. This was not counted as a pass or application defect. After network permission was granted, the identical command was rerun with output `backend-h2-rerun.log`: **77 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.

```sh
work/tools/mysql-8.4.4-macos15-arm64/bin/mysql --socket=/tmp/iitsm-33306.sock -u root -e 'CREATE DATABASE iitsm_qa_phase1_20261121;'
TEST_DB_URL='jdbc:mysql://127.0.0.1:33306/iitsm_qa_phase1_20261121?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true' TEST_DB_USER=root TEST_DB_PASSWORD='' JAVA_HOME="$PWD/work/tools/jdk-25.0.5.0.3.jdk/Contents/Home" mvn -o -f outputs/i-itsm/backend/pom.xml -Dmaven.repo.local="$PWD/work/m2" "-DargLine=-javaagent:$PWD/work/m2/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" test > work/qa-phase1/backend-mysql.log 2>&1
```

Result: **77 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. Fresh database migrated V1→V2→V3 and Hibernate validated schema. Database inspection counted 35 foreign keys and 7 CHECK constraints. No upgrade of a populated pre-V3 database was executed.

```sh
# Working directory outputs/i-itsm/frontend:
npm test > ../../../work/qa-phase1/frontend-test.log 2>&1
npm run build > ../../../work/qa-phase1/frontend-build.log 2>&1
# Working directory workspace root:
JAVA_HOME="$PWD/work/tools/jdk-25.0.5.0.3.jdk/Contents/Home" mvn -o -f outputs/i-itsm/backend/pom.xml -Dmaven.repo.local="$PWD/work/m2" -DskipTests package > work/qa-phase1/backend-package.log 2>&1
```

Results: **11/11 frontend tests PASS**; Vite production build PASS (2,129 modules transformed); Maven executable JAR package PASS. The packaging command skipped re-execution of already-passing tests and used Maven incremental compilation. No lint script/plugin was configured, so lint is **NOT CONFIGURED**, not PASS. No separate Failsafe integration-test task exists: WorkflowTest runs under Surefire in `mvn test`.

| Backend suite | Cases | Verification level |
|---|---:|---|
| DomainTest | 24 | Priority, lifecycle, ranking, fallback, elapsed-time unit cases |
| ExternalProviderTest | 2 | Real adapter against controlled local HTTP provider / missing-key check |
| WorkflowTest | 49 | Spring/JPA service integration, mostly transaction rollback; limited HTTP/filter assertions |
| common.ErrorsTest | 2 | Error mapping tests |
| Total, each database run | 77 | All pass; the same cases run twice, not 154 distinct cases |

Frontend tests mock fetch; the test title mentioning “actual API” does not make it an end-to-end API test. WorkflowTest uses a mocked Clock and disabled scheduling; warning tests call detection directly. A single-incident report-count assertion is not proof of correct averages/compliance.

### Packaged startup and additional HTTP probes

Started the built Java JAR on **8083** against the isolated database with an ephemeral generated JWT secret (not logged), SEED_DEMO=false, AI_API_KEY empty, local SMTP port 9, isolated attachment directory and `--app.scheduling-enabled=false`. The effective Java invocation was:

```sh
work/tools/jdk-25.0.5.0.3.jdk/Contents/Home/bin/java -jar outputs/i-itsm/backend/target/i-itsm-1.0.0.jar --app.scheduling-enabled=false
python3 work/qa-phase1/http-probes.py
```

The startup log observed **Successfully validated 3 migrations** and **Started Application in 5.07 seconds**. The first detached launch produced no startup evidence and was not counted; the supervised launch succeeded. A permission-blocked initial HTTP attempt was likewise not counted. Subsequent executed checks:

| Probe | Actual result | Assessment |
|---|---|---|
| GET / | 200, packaged index served | PASS static entry point, not JS/browser rendering |
| GET /api/incidents without JWT | 401 | PASS |
| GET /api/auth/me without JWT | 401 | PASS |
| GET /v3/api-docs | 200, 32 documented paths | PASS document availability only |
| Seed Employee / Technician / Admin login | 200 each, correct role | PASS |
| GET /api/incidents?from=not-a-date as test employee | 500 with generic operation-failed message | FAIL input error handling |
| Create incident with affectedUsers=1.7 | 201; storedAffectedUsers=1 | FAIL integer semantic validation |
| Register with twelve spaces as password | 201, verification queued | FAIL whitespace password validation |

The last two probes created only synthetic isolated data; scheduling was disabled and the synthetic address used example.invalid. No password/token is included in probe evidence.

## 6. Security concerns

**H04 — Whitespace-only passwords accepted (executed).** AuthController.Register/Reset and AdminController.StaffInput use @NotNull and @Size, without rejecting blank passwords. POST /auth/register with twelve spaces returned 201. Registration should reject a blank credential. The same validation shape exists in reset/staff inputs, but those HTTP paths were not probed with blank passwords. Severity: high for authentication quality, not a demonstrated account takeover.

**M07 — Logout is client-only (static).** main.jsx clears the in-memory token; there is no logout/revocation endpoint. A previously copied token remains valid until its one-hour expiry or tokenVersion change. Memory-only storage and revocation on reset/account update are positive controls. Decide whether immediate logout invalidation is a submission requirement.

**M08 — Authentication/email issuance has no application rate limiter (static).** Public login/register/reset/resend endpoints have no throttling or lockout in inspected source. Attack-volume behavior and any external proxy protection were not tested.

**M09 — Multiple password-reset links remain valid independently (static).** AuthService.consume marks only the consumed token used. A different unexpired RESET token can still change the password afterward. No claim of exploitation is made; test the desired invalidation policy.

Raw verification/reset links are present in private mail_job.body until successful delivery, which overwrites body with Delivered. Failed jobs retain links after token expiry. API delivery projections exclude body; hashes are used in account_token. Restrict DB/backup access and define retention. Attachment checks are signatures/extensions, not complete format validation or malware scanning. Downloads enforce attachment/octet-stream and nosniff. Direct HTTPS/proxy, TLS certificate policy, dependency advisory scanning and secret history scanning were not validated.

The separate local MySQL root account allows passwordless local audit access; this development setup must not be presented as a hardened deployment. No credential values were written to this report.

## 7. Database concerns

Fresh-schema migrations and Hibernate validation passed. Keys/constraints protect many relationships and prevent duplicate feedback, skill credit, token hashes and SLA cycle-threshold signals. Original SLA snapshot columns are non-updatable through JPA; audit/priority/SLA revision entities are immutable through Hibernate and have no ordinary edit/delete endpoints.

Important limits:

- Many logical required columns in V1 remain nullable. Foreign keys do not prove that service_id points to a SERVICE or technician_id to a TECHNICIAN; application guards supply those invariants.
- One active policy per service/priority is maintained by the settings lock and application logic, not a database uniqueness rule. Direct writes could violate it.
- Singleton settings row locks serialize incident changes, assignment, policy/configuration, scheduler and SMTP work. This favors consistency but creates a shared blocking point.
- Reports load all cycles/signals/feedback into Java even for restricted users, then filter; incident summaries/history introduce repeated lookups. This is a scaling concern, not a demonstrated information disclosure.
- Workload is derived from incident state, avoiding cached counter drift; it currently materializes matching incidents to obtain list size.
- Full recovery, rollback injection, database outage, populated-schema upgrades, backup/restore and concurrent worker tests were not run.
- No data was deleted. The isolated QA database and synthetic evidence remain for review.

## 8. Critical bugs

**No critical defect was demonstrated in this audit.** This is not a declaration that none exist. Authorization coverage and adversarial/concurrency testing are incomplete.

## 9. High-priority bugs and risks

**H01 — SMTP blocks core writes through the singleton lock (static reliability risk).** NotificationService.deliver at line 44 obtains Settings row lock at line 45, then performs up to 30 notification sends plus 30 account-email sends inside the same transaction. IncidentService, AdminService and SlaService.check need this lock too. Slow SMTP can stall unrelated incident operations; connection/read timeouts do not remove lock contention. No SMTP load/fault experiment was run. Review transaction boundaries before claiming resilient delivery.

**H02 — External delivery is not crash-idempotent (static risk).** SMTP send happens before delivery status commits. A process/database failure after SMTP acceptance but before commit leaves the job eligible for resend. Three-attempt tracking alone does not prevent duplicate successful messages. Not a failure of originating incident rollback isolation; it is a distinct delivery guarantee gap. No crash injection run.

**H03 — Target revision can miss an already-crossed old SLA deadline (static risk requiring reproduction/policy decision).** SlaService.revise appends a new target without first calling detect on the old target. If a deadline has passed since the last 15-second check and an admin extends the target, subsequent detection evaluates only the new target. An elapsed old breach may never be recorded. Existing warnings remain preserved when already recorded. Existing tests do not cover this timing window.

**H04 — Blank-password validation defect:** executed, described in security section.

**H05 — Fractional affected-user estimate silently truncated (executed).** IncidentController.Create declares primitive int; default JSON coercion converts 1.7 to 1 before @Min/@Max validation. HTTP returned 201 with affectedUsers=1. This changes user input silently and can affect priority/assignment/SLA near numeric tier boundaries. Boundary impacts were not separately exercised. Reject fractional values rather than reporting success with altered data.

High priority denotes investigation/fix priority for sign-off; H01–H03 are not falsely labelled executed failures.

## 10. Medium and low-priority bugs

**M01 — Invalid date becomes server error (executed).** SearchService.search calls Instant.parse; Errors has no DateTimeParseException-specific handler (it is not IllegalArgumentException). GET from=not-a-date returned 500, not a client validation response. Reversed date ranges also lack explicit validation (static).

**M02 — Date filters and priority ordering are misleading (static).** Incidents.jsx constructs UTC midnight/end-of-day strings although displayed dates use local timezone; the date fields do not explain UTC. End date is inclusive 23:59:59Z, omitting the final fractional second of DATETIME(6). SearchService orders priority strings alphabetically instead of severity. Verify intended sort/date semantics before fixing.

**M03 — Attachment failure message can be hidden (static).** Incidents.jsx line 276 calls notify with attachment failure, then line 279 always calls onCreated; main.jsx's onCreated immediately sets “Incident reported successfully” in the same single toast state. The incident exists, but the user can miss the failed attachment. No frontend failure-path assertion covers this.

**M04 — Fetch failure leaves indefinite loading state (static).** Dashboard.jsx, IncidentDetail.jsx, Incidents.jsx and Admin.jsx show Loading while data is null; catches display a toast without an error/retry state. When toast expires, the page can continue looking busy. Expired-session 401 likewise does not return to sign-in automatically.

**M05 — Documentation is stale (confirmed text mismatch).** limitations.md says staff accounts cannot be created and SMTP was not supplied; current code has staff creation and earlier user-confirmed real delivery. technical-design.md eligibility text requires verification for all technicians, contradicting adminCreated exception, and mentions only V1/V2 while V3 exists. Update docs only after approved fixes and scope decisions; no such edits made here.

**M06 — Prepared frontend/backend port mismatch (static).** vite.config.js proxies API to 8080 while prepared server uses 8081. Packaged same-origin application works; a developer following mixed local instructions may get request failures.

**M10 — Large reports/history collections lack bounded database aggregation (static).** ReportService loads full tables; detail histories/catalog/technician lists are unpaginated. No load measurement was made.

**L01 — Completed verification/reset forms remain in their prior mode (static).** Auth.jsx strips token query parameters after success but keeps verify/reset mode and submit control until Back to sign in is clicked. A repeat submit can produce an avoidable invalid-link error.

**L02 — Environment threshold settings only initialize with demo seed (static).** SKILL_THRESHOLD/SLA_WARNING_THRESHOLDS/REOPEN_DAYS are consumed in DemoSeed, which exits when seeding is disabled or any user exists. Existing database settings are authoritative; changing environment variables alone does not update them. Document this explicitly.

**L03 — Limited user editing/search and delivery recovery UI (scope limitation).** User updates cover role/activation/availability, not name/email; user list has pagination but no search. Delivery view is last 100 records with no manual retry. Original “manage users” scope should be clarified before adding fields/workflows.

## 11. Features that could not be fully tested

| Area | Missing evidence/dependency or deliberate limit |
|---|---|
| Live external AI | No live provider invocation/credentials/quota validation; controlled adapter test passes. Text classification is not implemented. |
| Real verification/reset email journey | Audit forbids real accounts/data; SMTP disabled and no real recipient used. Earlier user receipt confirmation is historical evidence only. |
| SMTP failure, retry, restart/crash | Existing tests mock sender but contain no worker-delivery assertions; no fault-injection campaign executed. |
| Scheduler behavior over wall-clock time | Tests call detect with mocked Clock; scheduling disabled for safe isolation. Restart/catch-up and concurrent checks pending. |
| Complete browser workflows/responsiveness | Tool available; no fresh browser test campaign performed. Historical screenshots are not counted as current passes. |
| Concurrency | No simultaneous assignment/approval/verification/feedback tests in current suite; serial repeated checks are insufficient. |
| Reporting arithmetic | Existing test checks total/active only. Need hand-calculated multi-incident/cycle fixture and priority-change/rejection cases. |
| File/storage failure recovery | Only simple upload/access and invalid PDF checks; exact-size, partial write, DB failure, cleanup failure and malware checks pending. |
| Fresh installation/upgrade | Existing cached dependencies used; fresh MySQL schema verified, populated older-schema upgrade and README-only new-machine install not run. |
| Performance/security completeness | No load, dependency advisory, full secret-history, penetration, TLS deployment, backup/restore or accessibility audit performed. |
| Full later A–T checklist | Numerous requirements differ from original; needs approved scope mapping and additional tests before execution claims. |

## 12. Recommended next testing steps

1. Review this audit and approve a bounded correction list. Do not redesign to the later checklist without agreement.
2. Prioritize reproduced validation defects H04/H05/M01. Add regression tests that exercise HTTP deserialization/validation, not only direct service calls.
3. Reproduce H03 around old/new deadline boundaries, then decide and document the breach policy. Test before/at/after deadlines, paused changes and downtime.
4. Exercise SMTP timeout/failure/crash recovery with a local controlled SMTP server and disposable accounts; measure concurrent incident latency and duplicate sends.
5. Run the three original demonstration workflows in a fresh isolated browser environment across all roles. Include chat, explicit information completion, rejection, reopening, attachment failure and expired session.
6. Add concurrent transaction tests on MySQL for assignment, acknowledgement, verification, feedback and SLA detection; inspect committed records after every failure.
7. Build a small manually calculated report fixture, reconcile every aggregate and decide whether compliance should remain grouped by original snapshot priority.
8. Verify a clean packaged installation, populated migration upgrade, schema/seed repeatability and frontend proxy instructions. Reconcile README, limitations, diagrams and final report with approved behavior.

Submission sign-off should require no unresolved high-impact reproduced defects, explicit acceptance or mitigation of high-priority risks, successful current browser demonstrations, and accurate documentation. Existing green tests are a useful baseline, not the final acceptance gate.

## Evidence and change boundary

Original specification: [Pasted text.txt](/Users/sarthak.saxena1012gmail.com/.codex/attachments/86ff74c8-7e16-4371-9834-591a988bee0c/Pasted%20text.txt)

- [backend-h2.log](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/backend-h2.log)
- [backend-h2-rerun.log](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/backend-h2-rerun.log)
- [backend-mysql.log](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/backend-mysql.log)
- [frontend-test.log](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/frontend-test.log)
- [frontend-build.log](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/frontend-build.log)
- [backend-package.log](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/backend-package.log)
- [mysql-schema.txt](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/mysql-schema.txt)
- [test-inventory.json](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/test-inventory.json)
- [http-smoke.json](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/http-smoke.json)
- [http-probes.json](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/http-probes.json)
- [http-probes.py](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/http-probes.py)
- [packaged-startup.log](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/packaged-startup.log)
- [source-before.json](/Users/sarthak.saxena1012gmail.com/Documents/Codex/2026-11-10/files-pasted-by-the-user-master/work/qa-phase1/source-before.json)

A SHA-256 baseline covered 100 existing source/configuration files (Java, JS/JSX/CSS, SQL, properties, XML, JSON and shell scripts outside dependency/build/data directories). Comparison after tests/builds found zero changes. Build outputs were regenerated; this report and audit evidence were added. No source/configuration/migration file was modified, no important database was tested or deleted, and no code fix was applied.
