# I-ITSM Phase 2 QA results

**Partial execution. Submission readiness remains unverified.** Phase 1 is the baseline for prior findings. The original master specification governs acceptance, including the always-available assignment fallback. The checks below are fresh Phase 2 observations.

## Environment and setup

The current project at outputs/i-itsm served its packaged frontend and Spring Boot backend together on 127.0.0.1:8083. MySQL was reached on 127.0.0.1:33306 using a newly created iitsm_qa_phase2 schema. The browser initially showed zero incidents. The schema is retained with synthetic records. Git status was attempted before and after testing. Both attempts reported that the current project is outside a Git repository.

From the workspace root, MySQL setup used the bundled mysql client with --socket=/tmp/iitsm-33306.sock -u root and CREATE DATABASE iitsm_qa_phase2. The startup command was:

```sh
work/tools/jdk-25.0.5.0.3.jdk/Contents/Home/bin/java -jar outputs/i-itsm/backend/target/i-itsm-1.0.0.jar --server.address=127.0.0.1 --app.scheduling-enabled=false
```

Runtime overrides selected the fresh schema, APP_PORT=8083, SEED_DEMO=true and work/qa-phase2/attachments. The JWT secret was generated locally. The AI key was empty and SMTP was pointed at localhost port 9 with scheduling disabled. Credentials are omitted. Only employee@iitsm.local signed in during the executed checks. The QA application process was stopped after testing.

## Browser results

Codex in-app browser accessibility automation opened the packaged frontend, filled the Employee login form and submitted it. The Employee portal loaded with the seeded employee name and an empty incident dashboard. **Employee sign-in and initial dashboard passed.** Full workflows A through F, technician and administrator UI sign-in, responsive viewport checks, uploads, console inspection and browser network inspection remain unexecuted. No full browser workflow received a pass.

## HTTP defect evidence

Evidence records input, status codes and relevant response fields in [http-results.json](../../../work/qa-phase2/http-results.json). These requests used the isolated server. The incident request held title, description, active category and service fixed with impact, urgency and criticality all set to 2.

| ID | Severity | Exact probe | Expected | Observed | Components |
|---|---|---|---|---|---|
| H04 | Medium | POST /api/auth/register with synthetic name, phase2-blank@example.invalid and twelve spaces as the password | Reject blank password | 201 with verification queued | AuthController.Register, AuthService |
| H05 | Medium | POST /api/incidents with affectedUsers equal to 1.5, 1.7, 5.5 or 100.5 | Reject fractional count | 201 each. Stored values were 1, 1, 5 and 100 respectively | IncidentController.Create, JSON binding |
| M01 | Medium | GET /api/incidents with from=bad or from=2026-02-30T00:00:00Z | Client validation error | 500 with generic operation-failed message | SearchService, Errors |
| M01b | Low | GET /api/incidents?from=2026-12-01T00:00:00Z&to=2026-01-01T00:00:00Z | Explicit validation if reversed ranges are disallowed | 200 with zero matches. Confirm intended behavior | SearchService |

All listed observations were reproduced. H04, H05 and M01 should be addressed before sign-off. M01b needs a behavior decision and does not independently block submission. Password reset and staff creation variants of H04 remain untested.

Integer counts 1, 5, 6 and 100 returned 201 and were preserved. Values -1, 0 and 10000001 returned 400. Under the original default formula the fixed factors contribute 16 before the affected-user contribution. The accepted values observed scores of 17 or 19, both MEDIUM as returned. This fixture does not exercise priority transitions at a tier boundary. Blank optional from and a valid enclosing date range each returned all eight incidents.

## Open experiments and test gaps

| Area | Status |
|---|---|
| H01 SMTP lock contention | Unexecuted. No delayed SMTP process or latency sample |
| H02 duplicate delivery after interruption | Unexecuted. No SMTP acceptance and process interruption experiment |
| H03 SLA target revision | Unexecuted. No deadline, pause or downtime experiment |
| Reporting reconciliation | Unexecuted. The validation records do not constitute the requested multi-role SLA fixture |
| Authorization matrix | Unexecuted. Employee login verified only |
| Attachment feedback and failed API UI, M03/M04 | Unexecuted. Remain Phase 1 source findings |
| Browser workflows and boundary cases | Open as listed above. Browser access succeeded |

Complete the missing Phase 2 evidence before sign-off. The minimal proposed Phase 3 fix order is strict integer binding for incident counts, consistent blank-password validation across all three entry points, and date parsing and range validation. Prioritize any reliability fixes according to measured impact once the experiments run.
