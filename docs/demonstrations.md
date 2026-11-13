# Demonstration guide

Use only development accounts and data. `python3 scripts/e2e.py` performs the three workflows against the running real API and writes the observed results to `e2e-results.json`.

## 1. Standard resolution

1. Sign in as employee; report an incident using an active service/category.
2. Confirm the calculated priority, technician and two creation-time SLA cycles in incident detail.
3. Sign in as the assigned technician. Acknowledge, start investigation, and optionally add an internal note.
4. Request more information, then sign in as the employee and respond. Confirm the resolution pause/resume record; the response clock remains stopped.
5. Resolve with a description as technician. The workload drops once.
6. Verify as employee. The incident closes; submit a 1–5 rating and optional feedback.
7. Inspect assignments, resolutions, priority history, activity and notifications. Administrator Reports shows the persisted counts.

## 2. Warning and breach

The script creates a dedicated **Demonstration SLA** service with LOW response target 4 seconds and resolution target 120 seconds. This does not change existing ordinary services. It submits a LOW incident and leaves it unacknowledged. Within the next 15-second scheduler pass, the response cycle records distinct 50%, 80% and breach events. Resolution continues independently. Inspect **SLA monitor**, incident **SLA history**, administrator notifications and audit log. If left unresolved for more than 120 seconds, the resolution cycle also breaches, as expected. A production demonstration can use longer targets instead.

## 3. Reopening and history

After a standard incident is closed, use **Reopen** as its reporter and supply a reason. Confirm a new resolution cycle starts now, while the previous resolution cycle and original response stop remain unchanged. Technician workload increases exactly once. Start investigation with the existing acknowledged assignment, resolve and verify again. Only one skill credit remains for that incident; duplicate feedback is rejected.

## Capturing screenshots

The browser-verified example is `screenshots/overview.png`. To capture additional evidence, use your browser's screenshot facility after each role signs in. Suggested views: employee incident form, technician acknowledgement controls, a waiting-for-employee incident, resolution verification, all resolution SLA cycles, administrator SLA monitor, configuration and audit history. Demo data is clearly labelled; dashboard metrics are computed from those real persisted records.

Do not include `.env`, account email token links or live-provider secrets in screenshots or shared archives.
