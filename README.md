# I-ITSM

An integrated IT service management application built with **Java 25, Spring Boot 4.1.2, Maven, React 19, JavaScript and MySQL**. Employees report and verify incidents; technicians investigate and resolve them; administrators manage policies, assignments and service performance.

## Run the prepared local application

This workspace includes a generated, private `.env` and a seeded MySQL instance on port **33306**. The verified packaged application uses **http://localhost:8081** (the earlier demo and Vite servers have been stopped). See [LOCAL-ACCESS.md](LOCAL-ACCESS.md) for the generated demo login and exact local commands; run `./scripts/run-packaged.sh` to start the existing JAR. The portable source archive excludes credentials, runtime files and dependencies.

## Setup on macOS

Install Java 25, Maven, Node.js (22.12+ or a compatible current release), and MySQL 8.4+. For example, install Temurin 25 from [Adoptium](https://adoptium.net/temurin/releases/?version=25), then install Maven, Node and MySQL using Homebrew:

```sh
brew install maven node mysql
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -version
brew services start mysql
```

Both version commands must report Java 25. On Linux/Windows, install equivalent packages and set `JAVA_HOME` to the Java 25 JDK. MySQL should be reachable before starting Spring Boot.

Create databases and a restricted application user through your MySQL administrator session. Replace the example password; do not reuse it:

```sql
CREATE DATABASE iitsm CHARACTER SET utf8mb4;
CREATE DATABASE iitsm_test CHARACTER SET utf8mb4;
CREATE USER 'iitsm'@'localhost' IDENTIFIED BY '<your-unique-password>';
GRANT ALL PRIVILEGES ON iitsm.* TO 'iitsm'@'localhost';
GRANT ALL PRIVILEGES ON iitsm_test.* TO 'iitsm'@'localhost';
```

From the project directory:

```sh
cp .env.example .env
openssl rand -base64 48
```

Edit `.env`: set the database password and URL, use the generated value for `JWT_SECRET`, and choose a unique `DEMO_PASSWORD` of at least 12 characters. `.env` is ignored by Git. Keep `SEED_DEMO=true` only for the initial development bootstrap. Seed creation occurs only when there are no users. Seed accounts are already email verified so a development SMTP server is not required for the demonstrations.

Build and test the integrated JAR:

```sh
./scripts/build.sh
set -a
. ./.env
set +a
java -jar backend/target/i-itsm-1.0.0.jar
```

Open **http://localhost:8080**. The JAR serves the built React application and REST API. Maven includes `frontend/dist` as static resources; build the frontend before packaging the backend. Flyway executes migrations automatically; Hibernate validates the migrated schema, rather than creating it.

### Development with hot reload

```sh
./scripts/run-backend.sh
# In another terminal:
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. Vite proxies `/api` to port 8080. Default CORS accepts localhost and 127.0.0.1 on port 5173. Set `FRONTEND_ORIGIN` to a comma-separated allow-list when changing ports or hosts; the first origin is used for email links. For same-origin production serving on 8080, set it to `http://localhost:8080` (use HTTPS at deployment).

`VITE_API_BASE_URL` is a build-time frontend setting. Put it in `frontend/.env.local` if the API lives on another origin; leave it empty for same-origin serving or the Vite proxy. Never put private credentials in Vite variables.

### Demo roles

| Role | Email |
|---|---|
| Administrator | admin@iitsm.local |
| Technician | technician@iitsm.local |
| Second technician | technician2@iitsm.local |
| Employee | employee@iitsm.local |
| Second employee | employee2@iitsm.local |

All development accounts use the `DEMO_PASSWORD` supplied at their first creation. Changing that variable later does not change existing passwords. Public registration always creates an unverified employee. An administrator can promote a verified employee to technician or administrator.

### Email

Set the `SMTP_*` variables for your SMTP provider. For local testing, a mail catcher such as Mailpit may listen on SMTP port 1025. Account verification and recovery messages enter a durable mail queue automatically. Incident email notifications are independently enabled under **Configuration**; in-app notifications always work. Delivery runs every 30 seconds, with at most three attempts. Administrators can inspect delivery status without seeing token links. Incident transactions remain committed if SMTP delivery fails. Without SMTP, seed accounts and in-app workflows remain usable, but newly registered users must wait for verification email delivery.

### AI assignment

Set `AI_API_KEY`, `AI_URL` and `AI_MODEL` for an OpenAI-compatible chat-completions provider. The default URL is the real OpenAI API. Only non-sensitive structured priority/category/workload context is sent; titles, descriptions and names are excluded. The provider recommends one enabled strategy, then Java selects an eligible technician. Missing keys, timeouts, non-200 responses, invalid output and disabled choices always fall back to **LEAST_LOADED**, even if that strategy is disabled for normal selection. No external key is required to start or test the application.

### Tests

```sh
cd backend
mvn test
# Default fast integration database: H2, test scope only, MySQL compatibility mode.
# Production always uses MySQL.
```

For the same suite against an isolated MySQL database:

```sh
export TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/iitsm_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
./scripts/test-mysql.sh
```

Run from the project root. The script loads `.env`; `TEST_DB_USER` and `TEST_DB_PASSWORD` may override the application credentials. Use an empty dedicated test database initially. Test fixtures are development accounts, and workflow test mutations are rolled back. JUnit **5.14.1** runs all tests. Since Spring Framework 7's JUnit extension requires JUnit 6 APIs, the JUnit 5 integration harness boots and closes Spring directly, with Spring Boot test configuration and Mockito provider beans. It exercises the real Spring services, transactions, repositories and HTTP security chain.

```sh
cd frontend
npm test
npm run build
```

The live demonstrations create clearly labelled development incidents and a short-target demonstration SLA service:

```sh
python3 scripts/e2e.py
```

Run only with a seeded development server on 8080 and `SEED_DEMO=true`. Results are saved to `docs/e2e-results.json`. The script includes standard resolution, waiting/resume, reopening, scheduled warnings/breach detection, audit/notifications and role enforcement.

## Documentation

- [Technical design, requirements, diagrams and tradeoffs](docs/technical-design.md)
- [API reference](docs/api.md) and [generated OpenAPI document](docs/openapi.json)
- [Actual test results and test-case table](docs/test-results.md)
- [Demonstration guide](docs/demonstrations.md)
- [Implementation checklist and phase record](IMPLEMENTATION.md)
- [Known limitations and future work](docs/limitations.md)

Interactive Swagger UI: **http://localhost:8080/swagger-ui/index.html**. API requests use `Authorization: Bearer <token>`; see the API guide. Passwords are BCrypt hashed; JWTs expire after one hour, stay in browser memory, and are invalidated by account/password changes.
