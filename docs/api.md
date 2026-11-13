# REST API reference

Base URL: `http://localhost:8080/api`. Production UI and API are served by the same JAR. Interactive reference: `/swagger-ui/index.html`; machine-readable contract: `/v3/api-docs` and the captured [openapi.json](openapi.json).

## Conventions

Send JSON with `Content-Type: application/json`, except attachment uploads (`multipart/form-data`). Authenticate with `Authorization: Bearer <token>`. Login returns a one-hour JWT. The server rechecks current account state, role and token version on every authenticated request. Logout removes the token from browser memory. Authentication requests below are public except `/auth/me`.

Success responses are resource objects, arrays, a paginated object, or `{ "message": "..." }` as specified. Mutation DTOs have explicit allow-listed fields; persistence entities are not accepted or serialized directly. Absolute timestamps are UTC ISO-8601 strings, e.g. `2026-11-10T16:00:00Z`. IDs are numeric except the human-facing incident reference.

Errors use `{ "message": "description" }`:

| Status | Meaning |
|---|---|
| 400 | Malformed JSON, invalid values or Bean Validation failure |
| 401 | Missing/invalid/expired JWT or invalid login |
| 403 | Role/object access denied; inactive or unverified login |
| 404 | Record not found; deleted attachment |
| 409 | Duplicate/constraint conflict or concurrent update requiring retry |
| 413 | Upload exceeds configured multipart size |
| 422 | Invalid lifecycle operation, missing policy, ineligible assignment, duplicate feedback or invalid business configuration |
| 500 | Unexpected server/storage/database failure; details remain in server logs |

Roles: **E** employee, **T** technician, **A** administrator. **Scoped** means E can access only own reports, T only current assignments, A all incidents. Role requirements are enforced on the backend.

## Authentication

| Method / route | Request DTO | Success | Access |
|---|---|---|---|
| POST `/auth/register` | `name`, `email`, `password` | 201 message; verification queued | Public; always creates EMPLOYEE |
| POST `/auth/login` | `email`, `password` | 200 `{token,user}` | Public; active verified account |
| POST `/auth/forgot-password` | `email` | 200 generic queued message | Public |
| POST `/auth/resend-verification` | `email` | 200 generic queued message | Public |
| POST `/auth/verify` | `token` | 200 verification message | Public; single-use token |
| POST `/auth/reset-password` | `token`, `password` | 200 update message | Public; single-use token |
| GET `/auth/me` | — | 200 user projection | Authenticated |

Name: required, at most 100 characters. Email: valid, required, at most 200 characters at registration. New passwords: 12–72 characters. Tokens expire after one hour. Password recovery increments token version, invalidating existing sessions. Verification and recovery responses do not reveal whether an email exists.

User projection: `id,name,email,role,active,verified,available`. No password hash, account-token hash or JWT signing secret is returned.

## Catalog, users and configuration

| Method / route | Request | Response | Access |
|---|---|---|---|
| GET `/catalog` | — | 200 array of `{id,kind,name,active}` | Authenticated |
| POST `/admin/catalog` | `{kind,name,active}` | 200 created catalog object | A |
| PUT `/admin/catalog/{id}` | `{kind,name,active}` | 200 updated catalog object | A |
| GET `/admin/users?page=0` | Zero-based page; 50 rows | 200 user array | A |
| PUT `/admin/users/{id}` | `{role,active,available}` | 200 updated user | A |
| GET `/admin/settings` | — | 200 settings object | A |
| PUT `/admin/settings` | Complete settings DTO below | 200 incremented configuration version | A |
| GET `/admin/sla-policies` | — | 200 all policy versions | A |
| POST `/admin/sla-policies` | Policy DTO below | 200 newly created policy version | A |

Catalog kind is `SERVICE` or `CATEGORY`; it cannot change after creation. Name is required and at most 100 characters. Users cannot deactivate/demote themselves; technicians with active assignments must transfer those incidents before deactivation or role change. Availability may be toggled without changing role. Updating an account invalidates its existing JWTs.

Settings DTO:

```json
{
  "priorityStrategy": "FORMULA",
  "impactWeight": 3,
  "urgencyWeight": 3,
  "criticalityWeight": 2,
  "affectedWeight": 1,
  "mediumCutoff": 13,
  "highCutoff": 20,
  "criticalCutoff": 27,
  "matrix": "LOW,LOW,MEDIUM,LOW,MEDIUM,HIGH,MEDIUM,HIGH,CRITICAL",
  "enabledStrategies": "LEAST_LOADED,SKILL_FIRST,PRIORITY_WEIGHTED,ROUND_ROBIN",
  "skillWeight": 100,
  "loadWeight": 10,
  "skillThreshold": 60,
  "warningThresholds": "50,80",
  "reopenDays": 90,
  "emailEnabled": false
}
```

`priorityStrategy` accepts FORMULA or MATRIX. Factor weights are 0–100. Cutoffs must be positive and strictly increasing. Matrix has exactly nine valid priority names. Enabled strategies may be an empty string; fallback remains usable. Ranking weights and skill threshold are 1–10000. Warning percentages are unique integers in 1–99. Reopen window is 1–365 days. Response adds `id` and `revision`; those fields are not caller-controlled.

SLA policy DTO: `{serviceId,priority,responseSeconds,resolutionSeconds,active}`. Target seconds must be 1–31536000. Saving deactivates earlier policies for the same service/priority and creates a new version. Response adds `id,revision`. Saving an inactive version intentionally leaves no active policy for that combination; existing snapshots remain valid.

## Incident search and creation

`GET /incidents` returns:

```json
{"items":[],"total":0,"page":0,"size":20}
```

Supported query parameters: `q` (reference/title/description), `status`, `priority`, `serviceId`, `categoryId`, `reporterId`, `technicianId`, `from`, `to`, `slaStatus`, `page`, `size`, `sort`, `direction`. Date bounds are ISO Instants. SLA status: ACTIVE, WARNING, BREACHED (historical signals). Page defaults to 0; size defaults to 20 and is bounded to 1–100. Sort allow-list: createdAt, reference, status, priority, title; direction asc/desc, default desc. User scope is always added independently of requested filters. Invalid sort values fall back to createdAt. Priority sorting uses stored enum text; severity-based reports are separate.

`POST /incidents` (E only) returns 201 incident detail:

```json
{
  "title":"VPN connection fails",
  "description":"Connection times out after authentication.",
  "serviceId":1,
  "categoryId":4,
  "impact":2,
  "urgency":2,
  "criticality":2,
  "affectedUsers":5
}
```

Title is nonblank, ≤160 characters; description nonblank, ≤20000. Factors are 1–3; affected users 1–10000000. Service/category must be active and correctly typed. The service/calculated-priority combination requires an active SLA policy; failure rolls back creation. Assignment may remain NEW if no eligible technician exists, with administrator notification.

Incident summary fields: `id,reference,title,description,serviceId,categoryId,reporterId,technicianId,status,priority,impact,urgency,criticality,affectedUsers,createdAt,closedAt,version,service,category,reporter,technician`. Nullable technician fields identify an unassigned incident.

## Incident detail and lifecycle

| Method / route | Request | Response | Access |
|---|---|---|---|
| GET `/incidents/{id}` | — | 200 detail | Scoped |
| POST `/incidents/{id}/actions` | `{action,text}` | 200 detail | Per action below |
| POST `/incidents/{id}/comments` | `{body,internalNote}` | 200 detail | Scoped; internal notes only T/A |
| POST `/incidents/{id}/reassign` | `{technicianId,reason}` | 200 detail | A |
| POST `/incidents/{id}/priority` | `{priority,reason}` | 200 detail | A |
| POST `/incidents/{id}/feedback` | `{rating,comment}` | 200 detail | Reporter, CLOSED only, once |

`text`/comment body: ≤20000 characters. Body is required. Reasons are required, ≤1000 characters for override/reassignment. Feedback rating 1–5, optional comment ≤5000.

| Action | Actor | Allowed current states | Effect |
|---|---|---|---|
| ACKNOWLEDGE | Assigned T | ASSIGNED, REOPENED, IN_PROGRESS, WAITING_FOR_EMPLOYEE | Acknowledge current assignment once; stop original response clock if still running |
| START | Assigned T | ASSIGNED, REOPENED | Requires current acknowledgement; IN_PROGRESS |
| REQUEST_INFO | Assigned T | IN_PROGRESS | Required question; WAITING_FOR_EMPLOYEE; pause resolution |
| RESPOND | Reporter | WAITING_FOR_EMPLOYEE | Required response; IN_PROGRESS; resume resolution |
| RESOLVE | Assigned T | IN_PROGRESS | Requires current acknowledgement and resolution text; RESOLVED; stop resolution |
| VERIFY | Reporter | RESOLVED | CLOSED; verified skill credit |
| REJECT | Reporter | RESOLVED | Required reason; REOPENED; new resolution cycle |
| REOPEN | Reporter | CLOSED | Required reason; within configured window; new resolution cycle |

Detail adds these arrays to the summary:

- `comments`: id, authorId, author, body, internalNote, createdAt. Employee responses exclude internal notes.
- `assignments`: id, previousTechnicianId, technicianId, actorId, strategy, selectionSource, reason, createdAt, acknowledgedAt.
- `resolutions`: id, technicianId, description, resolvedAt, verifiedAt, rejectedAt, rejectionReason.
- `priorityHistory`: id, oldPriority, newPriority, actorId, configVersion, factors, reason, createdAt.
- `history`: id, actorId, action, details, createdAt.
- `feedback`: id, rating, comment, createdAt.
- `sla`: the cycle projection below.

No API permits arbitrary status updates, manual SLA pause, audit editing, snapshot editing or historical-cycle deletion.

## SLA projections and monitoring

Cycle fields: `id,kind,policyId,policyVersion,priority,originalTargetSeconds,startedAt,stoppedAt,pausedAt,pausedSeconds,targetSeconds,elapsedSeconds,deadline,revisions,pauses,signals`.

- Revisions: `id,policyId,policyVersion,oldTargetSeconds,newTargetSeconds,oldPriority,newPriority,actorId,createdAt`.
- Pauses: `id,startedAt,endedAt`.
- Signals: `id,threshold,deadline,detectedAt`; threshold 100 denotes breach, lower values are warning percentages.

| Method / route | Response | Access |
|---|---|---|
| GET `/admin/sla?page=0` | 200 latest 50 cycles; includes incidentId/reference/service | A |
| GET `/admin/sla-compliance` | 200 grouped service/snapshot-priority `{name,cycles,completed,compliance}` | A |
| GET `/reports` | 200 role-scoped aggregate object | Authenticated |
| GET `/technicians` | 200 technician workload, verified count and recognized skill projections | A all technicians; T self |

Reports: `total,active,resolved,breaches,slaCompliance,averageResolutionSeconds,averageFeedback,byStatus,byPriority,byCategory,byService,trends`. Group arrays contain `{name,value}`. Compliance is null when no completed cycle exists; average feedback is null when absent. Breach count is historical cycles, not necessarily currently overdue incidents. Resolution average excludes waiting pauses. Trend dates are UTC.

Technician projections add `workload,verifiedResolutions,skills` to safe user fields. Skills include `id,categoryId,category,thresholdAtRecognition,recognizedAt`.

## Attachments

| Method / route | Request | Response | Access |
|---|---|---|---|
| GET `/incidents/{id}/attachments` | — | 200 metadata array | Scoped |
| POST `/incidents/{id}/attachments` | multipart part named `file` | 201 metadata | Scoped |
| GET `/attachments/{id}` | — | 200 downloadable bytes | Scoped through parent incident |
| DELETE `/attachments/{id}` | — | 204 | Scoped plus uploader or A |

Allow-list: PDF, PNG, JPEG, plain text; 1 byte–5 MB, signature/extension checks. Metadata includes `id,filename,contentType,sizeBytes,uploaderId,createdAt`; storage keys/paths are never exposed. Downloads use attachment disposition, application/octet-stream and nosniff. Deletion is logical, with audit records and retained bytes.

## Notifications, delivery and audit

| Method / route | Response | Access |
|---|---|---|
| GET `/notifications?page=0` | 200 latest 50 current-user notifications | Authenticated, owner only |
| POST `/notifications/{id}/read` | 200 message | Notification owner |
| GET `/admin/delivery` | 200 `{notifications,accountEmails}`, each latest 100 | A |
| GET `/admin/audit?page=0` | 200 latest 50 append-only audit records | A |

Notification fields: `id,incidentId,message,createdAt,readFlag,deliveryStatus`. Admin delivery adds attempts/error and user/recipient information, but omits token-bearing mail bodies. Audit fields: `id,actorId,action,entityType,entityId,details,createdAt`. There are no audit mutation endpoints.

## Example session

Login with the configured demo password; use the returned token in subsequent requests. Do not hardcode it in source files:

```sh
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  --data '{"email":"employee@iitsm.local","password":"<your-demo-password>"}'

curl http://localhost:8080/api/incidents \
  -H 'Authorization: Bearer <returned-token>'
```

The repeatable `scripts/e2e.py` demonstrates authenticated creation, acknowledgement, investigation, information exchange, resolution, verification, feedback, reopening, SLA warnings and private-record denial against the actual server.
