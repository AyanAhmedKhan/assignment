# Tests

## Backend checks

Run JUnit 5 service tests with Mockito and MVC tests without a database:

```powershell
.\mvnw.cmd test
```

Run the full suite against a **dedicated test database**:

```powershell
$env:TEST_DB_URL = 'jdbc:postgresql://localhost:5432/employees_test'
$env:TEST_DB_USERNAME = 'employee_app'
$env:TEST_DB_PASSWORD = 'employee_local_dev'
.\mvnw.cmd verify -Pintegration
```

Use `sh mvnw` and exported variables on macOS/Linux. The integration suite applies the real Flyway migration and rolls back test data after each test. Do not point it at a database containing real employee records. The database must start empty for the assertions about totals.

The tests cover service behavior, validation, HTTP responses, a real PostgreSQL CRUD lifecycle, combined filters, pagination, literal wildcard searches, database uniqueness, generated OpenAPI documentation, login, CSRF, permissions and logout. The GitHub Actions workflow runs the Java suite against PostgreSQL on Java 17 when pushed to a repository.

Optional browser tests need Node.js 20 or later and a running application:

```sh
npm ci
npx playwright install chromium
npm run test:e2e
```

Set `APP_URL` if the app is running on a different port. Browser tests create temporary records and remove them afterward. They check CRUD, delete cancellation, field errors, duplicate email handling, search/filter/pagination, mobile layout, error recovery, and escaping HTML in user input. An axe scan checks the directory, form, validation state and details for detectable WCAG A/AA issues; it is not a substitute for a complete accessibility audit. Run these tests only against a development instance.

To refresh the screenshots with the demo profile running, use `node tests/capture-screenshots.js`. This opens forms and views without saving or deleting records.

The optional hosted smoke check is `node tests/deployment-smoke.js`. Set `DEPLOY_ACCESS_FILE` to a private JSON file outside the repository containing `url`, `username` and `password`. Run it only against an owned deployment with fictional data: it checks login, CSRF, validation, CRUD and logout, and removes its temporary employee afterward.

## Account browser checks

Start the hosted profile locally on port 8081 as described in [local development](local-development.md), then run `npm run test:auth`. These tests cover signup validation, successful login to an empty directory, duplicate accounts, the two demo roles, cross-account access, and accessibility on a mobile viewport. They create test accounts in the development database.

Run `npm run format:check` to check Java and frontend formatting, or `npm run format` to apply it. Node.js and the formatter are development tools; they are not needed to run the JAR.
