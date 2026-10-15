# Local development

Follow the [README](../README.md#run-locally) for the complete first-run commands. Both the Docker path and the native `local` profile provide login, registration and demo accounts on `http://localhost:8080` without editing source code.

## Native prerequisites and database

- JDK 17 or later, compatible with Spring Boot 3.5. Set `JAVA_HOME` to the JDK directory and confirm `java -version` works.
- PostgreSQL 16 or later, running on localhost:5432. Put its `bin` directory on PATH so `psql` is available.
- Internet access on the first build. The Maven Wrapper downloads Maven 3.9.11 and project dependencies.

From the project root, run once using a PostgreSQL administrator:

```sh
psql -h localhost -U postgres -f database/setup.sql
```

This creates the `employee_app` role, with the local-only password `employee_local_dev`, and the `employees` and `employees_test` databases. If these names already exist, the script stops rather than replacing accounts or data. Use the existing credentials through environment variables, or choose a separate development database.

Alternatively, `docker compose up -d --wait db` starts just the PostgreSQL container with the default development credentials and the `employees` database. To create the separate integration-test database in that container, run:

```sh
docker compose exec db createdb -U employee_app employees_test
```

Run that creation command once; if the database already exists, keep it. Do not use a database containing real records for tests.

## Start the full application

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

macOS/Linux:

```sh
sh mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Open `http://localhost:8080`. The local profile group enables the account security and demo profiles, then applies loopback-only settings and HTTP-compatible session cookies. The admin is `admin` / `local-admin-password-2026`. Flyway creates the schema and Hibernate validates it. The VPS continues to use the separate `cloud,demo` profiles with secure cookies.

## Custom settings

For a database on another port or with different credentials, set environment variables before running the same startup command. For example, in PowerShell:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5433/employees'
$env:DB_USERNAME = 'employee_app'
$env:DB_PASSWORD = 'employee_local_dev'
$env:APP_PORT = '8082'
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

In a POSIX shell, use `export DB_URL='jdbc:postgresql://localhost:5433/employees'` and the equivalent variables. Native Java startup does not read `.env`; Compose does. `ADMIN_USERNAME` and `ADMIN_PASSWORD` can override the local administrator credentials; passwords must contain at least 16 characters and at most 72 UTF-8 bytes.

## Run the executable JAR

```sh
sh mvnw clean verify
java -jar target/kloudspot-assignment-1.1.1.jar --spring.profiles.active=local
```

Use `.\mvnw.cmd clean verify` on Windows. The database setup is the same as for Maven startup.

## Optional open API profile

For the HTTP examples and employee browser tests, use `demo` instead of `local`. It runs an unauthenticated directory on loopback. Stop the other app first if it uses port 8080.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Omitting profiles also runs the open API, without seeding sample employees. Use `local` for the complete application with accounts. Do not expose the local or open API profiles on a shared server; the VPS instructions are in [deploy/README.md](../deploy/README.md).
