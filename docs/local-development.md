# Local development

## Run Java locally

Prerequisites:

- JDK 17 or later, compatible with Spring Boot 3.5. Set `JAVA_HOME` to the JDK directory.
- PostgreSQL 16 or later. The project was verified against PostgreSQL 18.
- Internet access on the first build. The Maven Wrapper downloads Maven 3.9.11; a separate Maven installation is not required.

Start only the database with Compose:

```sh
docker compose up -d db
```

Alternatively, with a local PostgreSQL installation, run the setup script once using an administrator account:

```sh
psql -U postgres -f database/setup.sql
```

That script creates the `employee_app` role and the `employees` and `employees_test` databases. It deliberately does not drop or replace existing roles or databases.

Start the app on Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

On macOS or Linux:

```sh
sh mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Omit the profile argument to start without sample records. Flyway creates the schema automatically, and Hibernate validates it on startup. Changing Java entities does not silently change the database schema.

For a non-default database, set environment variables before starting the application. For example, in PowerShell:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/employees'
$env:DB_USERNAME = 'employee_app'
$env:DB_PASSWORD = 'employee_local_dev'
$env:APP_PORT = '8080'
.\mvnw.cmd spring-boot:run
```

In a POSIX shell, use `export DB_URL='jdbc:postgresql://localhost:5432/employees'` and the equivalent variables. Native Java startup does not read `.env`; Compose does.

To build a runnable JAR:

```sh
sh mvnw clean verify
java -jar target/kloudspot-assignment-1.1.1.jar
```

Use `.\mvnw.cmd clean verify` on Windows.

The native `demo` profile is open on loopback for API examples and the employee browser tests. Docker defaults to `cloud,demo` to exercise login and registration locally; its ports bind to loopback and its development session cookie does not require HTTPS. The VPS uses secure cookies and HTTPS.

## Test accounts and registration locally

With PostgreSQL running, start the hosted profile on loopback:

```powershell
$env:PGHOST = '127.0.0.1'
$env:PGPORT = '5432'
$env:PGDATABASE = 'employees'
$env:PGUSER = 'employee_app'
$env:PGPASSWORD = 'employee_local_dev'
$env:ADMIN_USERNAME = 'admin'
$env:ADMIN_PASSWORD = 'local-admin-password-2026'
java -jar target/kloudspot-assignment-1.1.1.jar --spring.profiles.active=cloud,demo --server.address=127.0.0.1 --server.port=8081 --server.servlet.session.cookie.secure=false
```

Keep the insecure-cookie override limited to loopback development. Use `deploy/compose.yaml` on the VPS.
