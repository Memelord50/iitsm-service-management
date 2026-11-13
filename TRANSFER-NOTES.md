# Transfer and publication notes

This is a separate clean copy of the existing local I-ITSM project. Backend, frontend, migrations, tests, seed implementation, scripts and existing documentation are unchanged. Only the publishing ignore file and these notes were added/updated in this copy. No application behavior changed.

The original project's `.env`, LOCAL-ACCESS.md, dependency folders, compiled JARs, generated builds, runtime attachments, local database data and IDE caches are excluded. `.env.example` contains placeholders only. Automated tests retain explicitly synthetic test passwords/signing material and a fake provider key; these are not application credentials and must never be used for deployment. No live credentials from the original `.env` were found in the copied files.

## Running on a fresh checkout

Install Java 25, Maven, Node.js and MySQL. Follow the portable setup sections of README.md: create your MySQL database/user, copy `.env.example` to `.env`, generate new private values, then run `./scripts/build.sh` and `./scripts/run-packaged.sh`. Dependency installation and builds regenerate omitted directories.

The development seed is `backend/src/main/java/com/iitsm/config/DemoSeed.java`. Flyway SQL files are in `backend/src/main/resources/db/migration`. Seed creation uses SEED_DEMO and the DEMO_PASSWORD you supply; no original accounts, incident records, passwords, attachments or MySQL data directory are included in this copy.

## Historical documentation

Existing docs/test-evidence and screenshots describe the original executed verification; tests have not been rerun for this unchanged transfer copy. Machine paths and localhost:8081 references in the historical reports refer to the original environment. This fresh copy's `.env.example` defaults to port 8080. LOCAL-ACCESS.md links in old documentation intentionally do not resolve here because that file contains private development credentials.

The original-only helper `scripts/start-workspace-mysql.sh` expects a downloaded MySQL under the original workspace's work/tools folder. On a new checkout, start your own installed MySQL using the portable README instructions instead. The packaged runner also has an optional original-workspace Java fallback; set JAVA_HOME to your installed Java 25 on a new checkout.

## Publish a new repository with GitHub Desktop

A fresh local Git repository on main is initialized only in this clean copy. No remote or commits are created during preparation.

1. Sign into GitHub Desktop.
2. Choose File → Add Local Repository and select this directory.
3. Review Changes. Ensure .env and LOCAL-ACCESS.md are absent. All source, tests, docs, .env.example and .gitignore should be present.
4. Enter `Initial commit: I-ITSM application` and choose Commit to main.
5. Choose Publish repository. Use a NEW name that does not already exist in the selected account, for example `i-itsm-service-management` if available.
6. Keep “Keep this code private” checked unless you deliberately want a public repository. Choose the correct personal account or organization.
7. Click Publish Repository, then Repository → View on GitHub to verify.

Do not create the remote on GitHub.com first, add a remote to an existing repository, or upload the old working folder/JAR. If the proposed name already exists, choose a different name; do not overwrite or force-push.

Official guide: https://docs.github.com/en/desktop/adding-and-cloning-repositories/adding-an-existing-project-to-github-using-github-desktop
