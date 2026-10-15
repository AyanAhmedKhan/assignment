# Verification

Verified locally on 15 October 2026.

## Environment

- Windows, Java 24.0.2 (source and bytecode target: Java 17)
- Maven Wrapper 3.9.11
- Spring Boot 3.5.16, Flyway 11.20.3
- PostgreSQL 18.2, using isolated development and test databases
- Node.js 22.22.0, Playwright 1.58.2 with Chromium

## Results

| Check | Result |
| --- | --- |
| JUnit / Mockito service tests | 17 passed |
| Spring MVC request and validation tests | 20 passed |
| PostgreSQL integration tests | 7 passed |
| Hosted-profile security integration tests | 5 passed |
| Account registration, isolation and role integration tests | 11 passed |
| Employee browser tests | 6 passed |
| Account browser tests | 3 passed locally and on the public HTTPS deployment |
| Public HTTPS deployment smoke check | Passed |
| Container build and health checks on Hostinger | Passed |
| JavaScript syntax check | Passed |
| Java and frontend formatting | Passed |
| Docker Compose configuration validation | Passed |
| npm dependency audit | No vulnerabilities reported for the installed development dependencies |

The successful backend command was `mvnw.cmd -B verify -Pintegration`, with `TEST_DB_URL` pointing to an isolated PostgreSQL database. No tests were skipped. The executable JAR was then started against a separate development database with the demo profile, and `npm run test:e2e` passed against that running application.

The browser checks cover:

1. Employee creation, details, editing, canceling deletion and confirmed deletion.
2. Required fields, phone format, future joining dates, negative salary and duplicate email errors.
3. Search combined with department/status filters and server-side pagination.
4. Recovery from an API failure, mobile page width and closing the form with Escape.
5. Displaying HTML-like employee names as text without executing them.
6. Automated axe checks on the directory, add form, form errors and employee details.

The axe scan reported no detectable WCAG A/AA violations in those states. It does not establish complete accessibility conformance. The mobile test used a 390 × 844 viewport; desktop checks used 1440 pixels of width.

Screenshots in `docs/screenshots/` were captured from the working application with fictional demo data. They show the directory, add form, edit form, field errors, employee details, delete confirmation, mobile layout, login and registration.

All 60 backend tests passed. The security tests cover anonymous access, minimal health output, invalid credentials, administrator permissions, missing/invalid CSRF tokens, real masked-token login, cookie/header CSRF writes and session invalidation on logout. The account tests cover registration validation, password hashing and the UTF-8 byte limit, duplicate emails, rejected role/owner injection, isolation across list/detail/summary/update/delete, read-only permissions and disabled demo logins when the demo profile is absent.

The three account browser tests exercise invalid and successful signup, sign-in to a new empty directory, duplicate-email feedback, demo editor/viewer buttons, cross-account denial, read-only controls and accessible mobile authentication forms.

Release 1.1.0 was deployed to `assignment.srv957712.hstgr.cloud`. A database backup was taken with the old app stopped before migration. All 14 existing employees were preserved under the administrator; the editor and viewer each received 14 separate fictional records. The live registration check created an empty member directory, and its temporary account was removed after verification.

The Docker image was built on the Hostinger VPS using Java 17, including the 37 service/MVC tests. The running app uses PostgreSQL 18 in its own container with persistent storage and a non-superuser application role. The public HTTPS check verified the certificate, HTTP redirect, healthy database connection, login/logout, protected API, secure HTTP-only session cookies, CSRF rejection, server validation, browser create/edit/delete, authenticated OpenAPI, mobile login width and an axe scan of the login page. Temporary smoke-test records were deleted afterward. Login credentials are stored separately from the project and submission archive.

## Verification limits

- The GitHub Actions workflow is provided but was not run remotely. The full integration suite used Java 24 locally; the container build and deployment used Java 17.
- Testing used Chromium; other browser engines were not tested.
- No load test, independent penetration test or off-site backup service was configured. Email verification, password recovery, user administration and an audit trail are outside the assignment's scope.

The expected duplicate-key error in the integration test log comes from deliberately checking the database unique constraint. The assertion passed and the test transaction was rolled back.
