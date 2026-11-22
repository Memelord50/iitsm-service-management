# Known limitations and future enhancements

These are explicit limits, not hidden placeholder implementations.

- A live external LLM account/key was not supplied. The real HTTP adapter was tested against a controlled provider response, AI selection/failure paths were mocked, and missing-key fallback was exercised live. Live-provider credentials, quota and model availability still require deployment validation.
- Verification and recovery require configured SMTP. Local test runs use synthetic accounts and disable real delivery. Delivery retries can duplicate email after an interruption between SMTP acceptance and the database commit.
- Attachment signatures/size/access are validated; content is not scanned for malware. Deletion is logical and bytes are retained. Add scanning, retention policies and an object-storage implementation for production.
- The serialization lock is held across the bounded AI call, prioritizing consistency over throughput. A high-volume deployment should move advice ahead of a short revalidation/assignment transaction and use more granular locks.
- Search and list endpoints have bounded pagination. Reports aggregate in Java and detail histories/catalog/technician lists are not paginated; very large installations should use SQL aggregates and paginated history endpoints.
- Token sessions last one hour with no refresh tokens. Browser reload signs out. Deploy behind HTTPS, configure exact origins and operational backups; no cloud deployment, infrastructure monitoring, production identity federation, rate limiter or MFA is included.
- Reopen eligibility is measured from the latest verified closure. A rejected resolution immediately creates another resolution cycle. Verified skill credit is deduplicated per incident, not per reopening cycle. These are deliberate documented policy choices.
- Lowering the recognition threshold takes effect on the next verified resolution; it does not retroactively bulk-award existing technician skills.
- Policy changes apply to future snapshots. Manual priority override revises running cycles only. Past stopped clocks remain immutable history.
- Administrators can create staff accounts with an initial password. Staff can sign in immediately and must verify email before changing the password.
- SLA checks run every 15 seconds, so detected-at timestamps may follow deadlines; both are recorded. All crossed missing warning thresholds are emitted during the next check.
- Screens use local time, but day-level trend grouping is UTC. No business-hours calendar is implemented because the requirement specifies 24/7 operation.
- The default fast integration suite uses H2 in MySQL mode; the same suite was separately executed against real MySQL. Production never selects H2.
- The host sandbox denies MySQL's hardware cache-line query. Starting the supplied isolated database script from the user's Terminal was necessary in this environment. The issue was resolved for verification; it is not an application database failure.

Future work: SQL aggregation and history pagination, SMTP retry controls, malware scanning, retention jobs, object storage, institutional SSO/MFA, rate limiting, operational metrics and multi-node load testing.
