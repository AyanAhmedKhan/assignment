# API and validation

## Accounts on the hosted profile

Registration creates a member account with an empty, private employee directory. Every employee query, summary and write is scoped to the authenticated account. Another account's employee ID returns 404. The administrator also works within its own directory.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/csrf` | Obtain a CSRF token before login, registration or another write |
| `POST` | `/api/auth/register` | Create an account; returns 201 on success |
| `POST` | `/login` | Sign in using form fields `username`, `password` and `_csrf` |
| `GET` | `/api/session` | Read the signed-in account's display name, role and demo status |
| `POST` | `/logout` | Invalidate the session; requires CSRF protection |

Registration accepts `fullName`, `email`, `password` and `confirmPassword`. Names are trimmed and limited to 120 characters. Emails are normalized to lowercase and limited to 254 characters. Passwords must match, contain 12–64 characters and fit within BCrypt's 72-byte UTF-8 limit. Passwords are hashed before storage. Role and owner fields cannot be supplied by the client.

For JSON writes, first fetch `/api/csrf`, retain the cookies and send the `XSRF-TOKEN` cookie value as the `X-XSRF-TOKEN` header. Refresh the token after signing in. Hosted API requests without a session return 401; forbidden writes and missing or invalid CSRF tokens return 403. Login and registration have separate per-IP rate limits at the reverse proxy.

The `demo` account can edit its sample directory. The `viewer` account has a separate, read-only sample directory. Both use the public password `KlodspotDemo2026!` while the `demo` profile is active. Demo accounts are shared with other visitors, so use a registered account for a private directory.

Registration does not send email or verify email ownership. Email verification, password recovery and account administration are outside the assignment's scope.

## Validation and business rules

The API is the source of truth. The browser repeats the same checks to give immediate feedback. Validation still runs when someone sends a request directly to the API.

| Field | Rule |
| --- | --- |
| Full name | Required after trimming, at most 120 characters. Unicode names are supported. |
| Email | Required, valid email format, at most 254 characters. Trimmed, stored in lowercase and unique within the account’s directory without regard to case. |
| Phone number | Required, 7â€“15 digits, at most 32 characters including formatting. A leading `+`, spaces, parentheses and hyphens are allowed. |
| Department | Required: `ENGINEERING`, `PRODUCT`, `DESIGN`, `OPERATIONS`, `HUMAN_RESOURCES` or `FINANCE`. |
| Designation | Required after trimming, at most 120 characters. |
| Joining date | Required ISO date (`YYYY-MM-DD`), today or earlier. Future hires are outside this implementation's scope. |
| Salary | Required, non-negative, at most `9999999999.99`, up to two decimal places. Stored as `BigDecimal` / PostgreSQL `NUMERIC(12,2)`. |
| Status | Required: `ACTIVE` or `INACTIVE`. |

Salary is interpreted as **annual gross pay in INR**, since the assessment does not specify a currency or pay period. Departments are a fixed list; department administration is not included. Dates use the server's calendar for API validation and the browser's local calendar for immediate form feedback.

Unknown JSON properties, invalid enums, malformed dates, oversized searches and invalid pagination are rejected. Search is limited to 120 characters, page numbers start at zero, and page size is limited to 1â€“100. SQL wildcard characters in search input are treated as literal text.

A friendly service check catches duplicate emails, and a database unique index on the account and lowercase email also protects against simultaneous inserts. Writes are transactional. JPA versioning detects overlapping updates; this is not a full stale-form merge feature. A form opened earlier can still replace a newer change if its request starts after the newer transaction has committed.

## API

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/api/employees` | Paginated search results |
| `GET` | `/api/employees/summary` | Totals for the whole directory |
| `GET` | `/api/employees/{id}` | One employee, or 404 |
| `POST` | `/api/employees` | 201 Created with a `Location` header |
| `PUT` | `/api/employees/{id}` | Full replacement of editable fields |
| `DELETE` | `/api/employees/{id}` | 204 No Content, or 404 |

List parameters: `search`, `department`, `status`, `page`, `size`, `sortBy` (`fullName`, `dateOfJoining`, `id`) and `direction` (`asc`, `desc`). Defaults are page 0, size 10, name ascending. Search matches name **or** email; the supplied filters combine with **AND**.

Example list response:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0
}
```

Errors use Spring's `ProblemDetail` format. Validation and duplicate-email responses include an `errors` map keyed by form field. Bad input returns 400, missing records return 404, and conflicting writes return 409. Database exception details and stack traces are not sent to clients.

Example validation response (the `instance` path may also be present):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Please check the highlighted fields.",
  "errors": { "salary": "Salary cannot be negative" }
}
```

See [sample API requests](api.http), [a sample employee payload](sample-employee.json), or import [the Postman collection](kloudspot-assignment.postman_collection.json). The collection saves the created employee's ID so the get, update and delete requests can be run in order. These examples target the open local profile. On the hosted app, sign in first; Swagger uses the session and CSRF cookie for authenticated requests.
