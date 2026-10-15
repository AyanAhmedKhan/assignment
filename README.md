# Kloudspot Assignment

An employee directory built with Java 17, Spring Boot and PostgreSQL. It supports employee CRUD, search, filters, pagination, registration and private account directories. The frontend is plain HTML, CSS and JavaScript, served by the same Spring Boot app.

[Open the deployed app](https://assignment.srv957712.hstgr.cloud)

![Employee directory](docs/screenshots/directory.png)

## Try it

Use the **Demo editor** or **Demo viewer** button on the sign-in page, or create an account to start with your own empty directory.

| Account | Username | Password | Access |
| --- | --- | --- | --- |
| Demo editor | `demo` | `KlodspotDemo2026!` | Add, edit and delete sample employees |
| Demo viewer | `viewer` | `KlodspotDemo2026!` | Read sample employees |

These are public demo credentials. Each demo account has a separate sample directory. Registered accounts cannot access either demo directory or another user's records. The administrator's credentials are configured privately on the server.

## Run locally

### Docker (recommended)

Install Git and Docker with Compose v2. On Windows or macOS, start Docker Desktop and use Linux containers. `docker info` must succeed before continuing. The first build needs internet access and free ports 8080 and 5432. Java, Maven, Node.js and PostgreSQL do not need to be installed separately for this path.

From a terminal:

```sh
git clone https://github.com/AyanAhmedKhan/assignment.git
cd assignment
docker compose up --build -d --wait --wait-timeout 180
```

If you downloaded the source ZIP, extract it and open a terminal in the folder containing `compose.yaml` instead of running the first two commands. No source edits or `.env` file are required. Compose builds the app, starts PostgreSQL, waits for both services to become healthy, and creates the schema and demo accounts automatically.

Open [localhost:8080](http://localhost:8080). Use either demo button or register an account. Local admin credentials are `admin` / `local-admin-password-2026`. [The health endpoint](http://localhost:8080/actuator/health) returns `{"status":"UP"}` when the app can reach the database.

Useful commands, run from the same folder:

```sh
docker compose ps
docker compose logs --tail=100 app db
docker compose down
```

The database is stored in a named volume. `docker compose down` preserves accounts and employees; running the startup command again reuses them.

If a port is occupied, copy `.env.example` to `.env` and change `APP_PORT` or `DB_PORT`, for example to 8082 and 5433, then run the startup command again and visit `http://localhost:8082`. On PowerShell use `Copy-Item .env.example .env`; on macOS/Linux use `cp .env.example .env`. These are optional configuration changes, not source edits. Changing database credentials after the volume has been initialized also requires updating the PostgreSQL role; it does not reset existing data.

### Java and PostgreSQL without Docker

Install JDK 17 or later, set `JAVA_HOME`, and start PostgreSQL 16 or later on port 5432. Make `psql` available on your terminal's PATH. From the project folder, create the local databases using your PostgreSQL administrator account:

```sh
psql -h localhost -U postgres -f database/setup.sql
```

Enter your PostgreSQL administrator password if prompted. Then start the application:

```powershell
# Windows PowerShell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

```sh
# macOS / Linux
sh mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile enables login, registration and the demo accounts at [localhost:8080](http://localhost:8080), using the database credentials created by the setup script. The Maven Wrapper downloads Maven and dependencies on the first run. No source edits or extra environment variables are needed with these defaults. Stop the application with Ctrl+C.

See [local development](docs/local-development.md) for custom database settings, the executable JAR and the optional open API profile.

## Project structure

```text
src/main/java/com/kloudspot/assignment/
  KloudspotAssignmentApplication.java
  account/       Registration, stored accounts and initial demo/admin accounts
  employee/      Controller, service, repository, entity and search filters
    dto/         API request, response and page records
  security/      Login, current account, sessions and CSRF
  config/        OpenAPI, response headers and sample employee data
  error/         Shared HTTP error handling
src/main/resources/
  db/migration/  Versioned schema changes
  static/
    index.html, login.html, register.html
    css/         Styles
    js/          Page behavior, form handling, API calls and rendering
src/test/        Service, MVC, PostgreSQL and security tests
tests/           Browser tests and screenshot scripts
deploy/          VPS Compose configuration and operating instructions
docs/            Setup, API examples and verification notes
```

Start with `EmployeeController` to follow an API request. It calls `EmployeeService`, which applies the current account's ownership rules and uses `EmployeeRepository` for persistence. `EmployeeRequest` contains the field validation rules. `ApiExceptionHandler` turns expected failures into consistent responses.

On the frontend, `app.js` handles the directory and navigation. `employee-form.js` owns the add/edit dialog, `employee-api.js` handles HTTP requests, and `employee-view.js` renders rows and details. Authentication screens share the session helpers in `session.js`.

## Important choices

- Employee emails are unique within an account's directory. The API and database both enforce this.
- Salary means annual gross pay in INR and uses `BigDecimal` / `NUMERIC(12,2)`.
- Department values are a fixed enum; joining dates cannot be in the future.
- Requests cannot choose their account ID or role. Ownership comes from the signed-in session.
- Passwords are hashed. Hosted writes require a CSRF token, and sessions use secure HTTP-only cookies.
- Flyway owns schema changes; Hibernate only validates the schema.

The open native development profile is intended for local API testing. The hosted `cloud` profile requires login. Email verification, password recovery and account administration screens are outside this assessment.

## Tests and API

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify -Pintegration
```

The second command needs a dedicated `employees_test` database. See [testing](docs/testing.md) for database settings, browser tests and formatting commands, and [verification notes](docs/verification.md) for the checks actually run.

After signing in, open [Swagger UI](https://assignment.srv957712.hstgr.cloud/swagger-ui.html). The [API guide](docs/api.md), [HTTP examples](docs/api.http) and [Postman collection](docs/kloudspot-assignment.postman_collection.json) document the endpoints and validation rules.

[VPS deployment instructions](deploy/README.md) cover HTTPS, credentials, backups and updates.
