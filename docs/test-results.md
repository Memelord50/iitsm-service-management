# Actual verification report

Executed on 10 November 2026, Asia/Kolkata. This report records executed checks; external-provider deployment checks are explicitly separated below.

## Results

| Check | Actual result |
|---|---|
| Java runtime | Oracle JDK 25.0.5.0.3, workspace-local |
| Backend clean build | `mvn clean package`: BUILD SUCCESS |
| Real database | MySQL 8.4.4, dedicated `iitsm_test` schema |
| MySQL backend suite | 67 tests, 0 failures, 0 errors, 0 skipped |
| Default H2 backend suite | Same 67 tests, 0 failures, 0 errors, 0 skipped; test-only database |
| Frontend clean install | `npm ci`: success |
| Frontend suite | Vitest: 8 tests passed |
| Frontend production build | Vite: success; entry and charts split into separate bundles |
| Flyway | V1 and V2 executed successfully; schema validated by Hibernate |
| Packaged app | Java JAR starts on port 8081 and serves React HTML, JS/CSS and REST APIs |
| Runtime package isolation | No H2, Mockito or JUnit dependency in the production JAR |
| Live demonstrations | All 3 requested workflows pass; additional administrator/access assertions pass |
| Browser mobile | 390×844: sign-in, dashboard, navigation drawer, incident list; document width 390 |
| Browser desktop | 1440×1000: dashboard and persisted SLA monitor; document width 1440 |
| Browser packaged-origin errors | No captured errors from origin localhost:8081 |
| Diagram syntax | All 7 Mermaid architecture/use-case/class/ER/state/assignment/sequence blocks parsed successfully |

There are **75 distinct automated test cases** (67 backend plus 8 frontend). Running the same backend tests on MySQL and H2 does not increase the distinct-test count. Live workflow and browser checks are additional evidence.

## Test execution and fixtures

JUnit 5.14.1 executes domain tests and a programmatic Spring Boot integration context. External AI and SMTP beans are Mockito substitutes only in tests. The external HTTP adapter test uses a local controlled HTTP server. The production application contains the actual HTTP/SMTP implementations; no production service returns mocked incidents or metrics.

Workflow test fixture: seeded employee/second employee/admin/two technicians, three services, four categories, real migrated tables; base incident impact=2, urgency=2, criticality=2, affectedUsers=4. A mocked UTC Clock starts at 2026-11-10T10:00:00Z and advances explicitly for timing assertions. Each workflow test has a transaction rolled back afterward. Domain tests use default Settings unless the case changes them. Provider tests do not require internet credentials. Exact case input variations and assertions are in the named test methods.

The source files are `backend/src/test/java/com/iitsm/DomainTest.java`, `WorkflowTest.java`, `ExternalProviderTest.java`, and `frontend/src/app.test.jsx`. The following table is generated from executed Surefire XML/Vitest results, rather than a planned test list.

## Backend case table

| ID | Objective / test method | Input | Expected result | Actual result | Status |
|---|---|---|---|---|---|
| B001 | `aiSelectsEnabledJavaStrategy` | Default Settings and case-specific states/candidates | Ai Selects Enabled Java Strategy | All assertions passed | PASS |
| B002 | `disabledAiChoiceRejected` | Only ROUND_ROBIN enabled; AI returns SKILL_FIRST | Disabled Ai Choice Rejected | All assertions passed | PASS |
| B003 | `disabledFallbackStillAvailable` | Normal selection SKILL_FIRST; invalid AI output | Disabled Fallback Still Available | All assertions passed | PASS |
| B004 | `formulaConfigurationChangesResult` | Critical cutoff changed to 9 | Formula Configuration Changes Result | All assertions passed | PASS |
| B005 | `formulaUsesAllFourFactors` | Factors (1,1,1,1) and (3,3,3,100) | Formula Uses All Four Factors | All assertions passed | PASS |
| B006 | `leastLoadedHasDeterministicTieBreak` | Two equal-load candidates, IDs 2 and 1 | Least Loaded Has Deterministic Tie Break | All assertions passed | PASS |
| B007 | `lifecycleAllowsInvestigation` | Default Settings and case-specific states/candidates | Lifecycle Allows Investigation | All assertions passed | PASS |
| B008 | `lifecycleRejectsPrematureClosure` | Default Settings and case-specific states/candidates | Lifecycle Rejects Premature Closure | All assertions passed | PASS |
| B009 | `lifecycleRequiresWaitingForResponse` | Default Settings and case-specific states/candidates | Lifecycle Requires Waiting For Response | All assertions passed | PASS |
| B010 | `matrixEscalatesAffectedUsers` | MATRIX; affectedUsers=100 | Matrix Escalates Affected Users | All assertions passed | PASS |
| B011 | `matrixEscalatesCriticalBusiness` | MATRIX; criticality=3 | Matrix Escalates Critical Business | All assertions passed | PASS |
| B012 | `matrixUsesImpactAndUrgency` | MATRIX; factors (2,3,1,1) | Matrix Uses Impact And Urgency | All assertions passed | PASS |
| B013 | `missingKeyUsesFallback` | Default Settings and case-specific states/candidates | Missing Key Uses Fallback | All assertions passed | PASS |
| B014 | `noEligibleCandidateLeavesUnassigned` | Empty candidate list | No Eligible Candidate Leaves Unassigned | All assertions passed | PASS |
| B015 | `pausedResolutionDoesNotAccrueTime` | Default Settings and case-specific states/candidates | Paused Resolution Does Not Accrue Time | All assertions passed | PASS |
| B016 | `priorityRankingUsesSeverity` | Skilled candidate load 15 vs unskilled load 0; LOW/CRITICAL | Priority Ranking Uses Severity | All assertions passed | PASS |
| B017 | `rejectionReopens` | Default Settings and case-specific states/candidates | Rejection Reopens | All assertions passed | PASS |
| B018 | `reopeningRequiresClosed` | Default Settings and case-specific states/candidates | Reopening Requires Closed | All assertions passed | PASS |
| B019 | `resolvedVerificationCloses` | Default Settings and case-specific states/candidates | Resolved Verification Closes | All assertions passed | PASS |
| B020 | `resumedResolutionDeductsAccumulatedPause` | Default Settings and case-specific states/candidates | Resumed Resolution Deducts Accumulated Pause | All assertions passed | PASS |
| B021 | `roundRobinChoosesOldestAssignment` | Last assignment IDs 10 and 2 | Round Robin Chooses Oldest Assignment | All assertions passed | PASS |
| B022 | `skillFirstDiffersFromLeastLoaded` | Skilled load 5 vs unskilled load 0 | Skill First Differs From Least Loaded | All assertions passed | PASS |
| B023 | `stoppedSlaIsStable` | Default Settings and case-specific states/candidates | Stopped Sla Is Stable | All assertions passed | PASS |
| B024 | `workloadStateDefinition` | Default Settings and case-specific states/candidates | Workload State Definition | All assertions passed | PASS |
| B025 | `externalProviderSendsContextAndParsesStrategy` | Local HTTP provider returns SKILL_FIRST | External Provider Sends Context And Parses Strategy | All assertions passed | PASS |
| B026 | `missingProviderKeyFailsClearly` | Empty key; provider URL not called | Missing Provider Key Fails Clearly | All assertions passed | PASS |
| B027 | `acknowledgementStopsOnlyResponse` | Acknowledgement at creation +30 seconds | Acknowledgement Stops Only Response | All assertions passed | PASS |
| B028 | `assignmentAloneDoesNotStopResponse` | Seeded real-database fixture and named operation | Assignment Alone Does Not Stop Response | All assertions passed | PASS |
| B029 | `attachmentAccessRestricted` | Text attachment download by reporter vs other employee | Attachment Access Restricted | All assertions passed | PASS |
| B030 | `completeWorkflowAuditNotificationsFeedback` | Seeded real-database fixture and named operation | Complete Workflow Audit Notifications Feedback | All assertions passed | PASS |
| B031 | `creationStartsBothClocksAtCreation` | Fresh incident created before acknowledgement | Creation Starts Both Clocks At Creation | All assertions passed | PASS |
| B032 | `deactivatedPolicyLeavesSnapshotIntact` | Seeded real-database fixture and named operation | Deactivated Policy Leaves Snapshot Intact | All assertions passed | PASS |
| B033 | `duplicateFeedbackDenied` | Seeded real-database fixture and named operation | Duplicate Feedback Denied | All assertions passed | PASS |
| B034 | `employeeCannotOverridePriority` | Seeded real-database fixture and named operation | Employee Cannot Override Priority | All assertions passed | PASS |
| B035 | `employeeCannotReadAnotherIncident` | Seeded real-database fixture and named operation | Employee Cannot Read Another Incident | All assertions passed | PASS |
| B036 | `employeeCannotReadInternalNotes` | Seeded real-database fixture and named operation | Employee Cannot Read Internal Notes | All assertions passed | PASS |
| B037 | `fallbackWorksWithEveryNormalStrategyDisabled` | enabledStrategies is empty; AI unavailable | Fallback Works With Every Normal Strategy Disabled | All assertions passed | PASS |
| B038 | `feedbackBeforeClosureDenied` | Seeded real-database fixture and named operation | Feedback Before Closure Denied | All assertions passed | PASS |
| B039 | `httpMissingAuthenticationRejected` | Unauthenticated GET /api/incidents | Http Missing Authentication Rejected | All assertions passed | PASS |
| B040 | `httpRoleAuthorizationUsesJwtFilter` | Employee JWT sent to /api/admin/users | Http Role Authorization Uses Jwt Filter | All assertions passed | PASS |
| B041 | `invalidLoginDenied` | Seeded real-database fixture and named operation | Invalid Login Denied | All assertions passed | PASS |
| B042 | `malformedAttachmentRejected` | fake.pdf containing non-PDF bytes | Malformed Attachment Rejected | All assertions passed | PASS |
| B043 | `missingPolicyPreservesTargetAndNotifiesAdmin` | Seeded real-database fixture and named operation | Missing Policy Preserves Target And Notifies Admin | All assertions passed | PASS |
| B044 | `noEligibleTechnicianPreservesNewIncident` | Seeded real-database fixture and named operation | No Eligible Technician Preserves New Incident | All assertions passed | PASS |
| B045 | `pauseAndResumePreserveElapsed` | Pause at +60s; response at +660s | Pause And Resume Preserve Elapsed | All assertions passed | PASS |
| B046 | `priorityRevisionPreservesAccumulatedPauseAndDeadlineOrigin` | Pause +60..180s, then CRITICAL override | Priority Revision Preserves Accumulated Pause And Deadline Origin | All assertions passed | PASS |
| B047 | `priorityRevisionPreservesSnapshotAndStart` | Seeded real-database fixture and named operation | Priority Revision Preserves Snapshot And Start | All assertions passed | PASS |
| B048 | `reassignmentAfterAcknowledgementDoesNotRestartResponse` | Reassign after response clock stopped | Reassignment After Acknowledgement Does Not Restart Response | All assertions passed | PASS |
| B049 | `reassignmentBeforeAcknowledgementTransfersWorkload` | Active incident transferred before first acknowledgement | Reassignment Before Acknowledgement Transfers Workload | All assertions passed | PASS |
| B050 | `recoveryChangesPasswordAndInvalidatesTokens` | Recovery token; replacement password; prior credentials | Recovery Changes Password And Invalidates Tokens | All assertions passed | PASS |
| B051 | `rejectionDoesNotCreditSkill` | Seeded real-database fixture and named operation | Rejection Does Not Credit Skill | All assertions passed | PASS |
| B052 | `reopenPreservesResponseAndAddsResolutionCycle` | Seeded real-database fixture and named operation | Reopen Preserves Response And Adds Resolution Cycle | All assertions passed | PASS |
| B053 | `reopenReassignsUnavailableOwnerWithoutLosingPreviousAssignment` | Close incident, make owner unavailable, reopen | Reopen Reassigns Unavailable Owner Without Losing Previous Assignment | All assertions passed | PASS |
| B054 | `reopenWindowEnforced` | Reopening 91 days after closure | Reopen Window Enforced | All assertions passed | PASS |
| B055 | `reportCountsRealIncidents` | Seeded real-database fixture and named operation | Report Counts Real Incidents | All assertions passed | PASS |
| B056 | `resolvedReassignmentDoesNotAddWorkload` | Resolve, reassign, then reject | Resolved Reassignment Does Not Add Workload | All assertions passed | PASS |
| B057 | `responseNeverPausesWithResolution` | Seeded real-database fixture and named operation | Response Never Pauses With Resolution | All assertions passed | PASS |
| B058 | `searchIsScopedAndFiltered` | Seeded real-database fixture and named operation | Search Is Scoped And Filtered | All assertions passed | PASS |
| B059 | `skillRecognizedAtConfiguredThreshold` | Threshold=1; one verified resolution | Skill Recognized At Configured Threshold | All assertions passed | PASS |
| B060 | `snapshotColumnsRemainImmutableAfterFlush` | Attempt to mutate original target, start and policy; flush/clear | Snapshot Columns Remain Immutable After Flush | All assertions passed | PASS |
| B061 | `startRequiresAcknowledgement` | Seeded real-database fixture and named operation | Start Requires Acknowledgement | All assertions passed | PASS |
| B062 | `successfulLogin` | Seeded real-database fixture and named operation | Successful Login | All assertions passed | PASS |
| B063 | `unavailableTechnicianExcluded` | Seeded real-database fixture and named operation | Unavailable Technician Excluded | All assertions passed | PASS |
| B064 | `verificationTokenIsSingleUse` | Register, consume token, attempt reuse | Verification Token Is Single Use | All assertions passed | PASS |
| B065 | `verifiedCreditNotDuplicatedAcrossReopening` | Verify, reopen, resolve and verify again | Verified Credit Not Duplicated Across Reopening | All assertions passed | PASS |
| B066 | `warningThresholdsAreConfigurable` | Thresholds 25/90; clock at 25% | Warning Thresholds Are Configurable | All assertions passed | PASS |
| B067 | `warningsAt50And80AndBreachAreIdempotent` | Clock at 50%, 80%, target+1s; repeated detection | Warnings At50 and 80 And Breach Are Idempotent | All assertions passed | PASS |

## Frontend case table

| ID | Objective | Input | Expected result | Actual result | Status |
|---|---|---|---|---|---|
| F001 | login submits credentials and returns authenticated role | Rendered React components / controlled fetch responses | Login submits credentials and returns authenticated role | All assertions passed | PASS |
| F002 | invalid login displays server error | Rendered React components / controlled fetch responses | Invalid login displays server error | All assertions passed | PASS |
| F003 | registration presents a full name and secure password minimum | Rendered React components / controlled fetch responses | Registration presents a full name and secure password minimum | All assertions passed | PASS |
| F004 | creation sends numeric incident factors to actual API | Rendered React components / controlled fetch responses | Creation sends numeric incident factors to actual api | All assertions passed | PASS |
| F005 | technician list does not expose employee creation action | Rendered React components / controlled fetch responses | Technician list does not expose employee creation action | All assertions passed | PASS |
| F006 | API attaches bearer token and propagates authorization error | Rendered React components / controlled fetch responses | Api attaches bearer token and propagates authorization error | All assertions passed | PASS |
| F007 | badge uses readable lifecycle labels | Rendered React components / controlled fetch responses | Badge uses readable lifecycle labels | All assertions passed | PASS |
| F008 | duration and labels retain meaning | Rendered React components / controlled fetch responses | Duration and labels retain meaning | All assertions passed | PASS |


## Live end-to-end evidence

`scripts/e2e.py` ran against **the final packaged application at port 8081**, with real JWT authentication, HTTP requests, MySQL persistence and scheduled SLA processing. It created clearly labelled demonstration incidents. See [e2e-results.json](e2e-results.json) for references and execution timestamp.

1. Standard resolution: created both clocks, verified deterministic fallback, rejected another employee's access, acknowledged, investigated, requested information, resumed after response, resolved, verified, and submitted feedback.
2. Reopening: preserved the original response and stopped resolution records, appended a new resolution cycle, resolved and verified again.
3. Warnings/breach: created a dedicated 4-second response/120-second resolution policy, observed unique 50%, 80% and breach events through the scheduler while resolution continued independently.
4. Administrator assertions: actual SLA/audit/notification/report responses and employee access denial to administration.

## Packaging and browser evidence

The JAR's embedded index exactly matches the served index. All referenced JS/CSS assets were downloaded completely with HTTP 200. OpenAPI exposes the bearer scheme and correctly marks public authentication operations. The artifact SHA-256 and asset sizes are recorded in [package-check.json](test-evidence/package-check.json). Browser evidence and screenshots are in [browser-check.json](test-evidence/browser-check.json), [desktop overview](screenshots/overview.png), and [mobile overview](screenshots/mobile.png).

The mobile check used 390×844 and desktop used 1440×1000. Neither viewport had page-level horizontal overflow; wide tables scroll within their container. Mobile drawer navigation and incident list were exercised. The desktop dashboard displays real persisted demonstration counts, and SLA monitor shows independent clocks, stopped historical cycles and recorded breaches. Short SLA durations were corrected to display seconds rather than rounding to zero minutes; the frontend duration test verifies 4 seconds → `4s`.

## Issues found and resolved during verification

- MySQL's initialization crashed within the host sandbox due to a blocked hardware cache-size query. The user started the isolated server from Terminal.
- The original Unix socket path exceeded macOS's length limit; the script now uses `/tmp/iitsm-33306.sock`.
- JUnit 5 does not satisfy Spring Framework 7's JUnit extension API. Integration tests retain JUnit 5 and start/close a real Spring Boot context directly.
- Explicit UTC JDBC timestamp mapping resolved H2 schema validation while preserving MySQL DATETIME(6) and Java Instant.
- Re-stubbing a previously throwing Mockito method required `doThrow` in test setup.
- Development CORS now supports an explicit comma-separated origin allow-list; email links use its first origin.
- The final JAR includes built React assets, and public static routes are permitted while API authorization remains enforced.
- Reopening with an unavailable owner now preserves the previous technician in assignment history.
- Short-target SLA display now uses seconds below one minute.

Historical development browser HMR errors occurred during bulk formatting; they are not packaged-origin runtime errors. Asset smoke checks initially closed their sockets before consuming response bodies, causing client-abort log entries; final checks consume full responses.

## Database consistency check

The live `/technicians` workload values matched independent SQL counts of active assigned incidents for both technicians: technician 2 = 2 and technician 3 = 2 at verification time. SQL checks found zero duplicate cycle/threshold signals and zero duplicate response cycles. Flyway V1 and V2 both have successful history entries. See [database-consistency.json](test-evidence/database-consistency.json).

## Verification limits

No live SMTP account or live external AI key was supplied. Token workflows, mail queuing and provider-selection/fallback code are implemented; external SMTP delivery and a successful response from a live paid provider remain deployment checks. External-provider mocks are confined to tests. Attachment malware scanning, high-volume load testing and production infrastructure are outside this verified delivery; see [limitations.md](limitations.md).

Raw compact result evidence: [MySQL](test-evidence/mysql-tests.json), [H2](test-evidence/h2-tests.json), [Vitest](test-evidence/frontend-tests.json), [diagram parsing](test-evidence/diagram-check.json).
