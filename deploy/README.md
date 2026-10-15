# Hostinger VPS deployment

This deployment uses the VPS's existing Traefik proxy. It does not replace its configuration or bind additional public ports. The application joins `root_default`; PostgreSQL is available only on the private project network. Change `APP_HOST`, `PROXY_NETWORK` and `TLS_RESOLVER` if deploying on another server.

The application and image are named **Kloudspot Assignment** / `kloudspot-assignment`. The existing Compose project, database role, volume, proxy labels and server directory retain their original identifiers so upgrades continue to use the same stored data. The public demo credentials also stay stable across the branding correction.

## First deployment

Copy the source to `/opt/klodspot-assignment` and run from that directory:

```sh
python3 deploy/bootstrap-env.py
docker compose --env-file deploy/.env -f deploy/compose.yaml config --quiet
docker compose --env-file deploy/.env -f deploy/compose.yaml build app
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d --wait --wait-timeout 240
```

The bootstrap script creates `deploy/.env` with mode 600 and independent random passwords. It never replaces an existing file. The administrator username defaults to `admin`; retrieve the generated password from that file using your authenticated SSH session. Keep this file private and out of Git, build contexts and submission archives.

Open `https://assignment.srv957712.hstgr.cloud`. Traefik requests a certificate using its existing ACME resolver. Port 443 must be reachable, and both A and AAAA records must point to this VPS. HTTP is redirected by the existing proxy. The application trusts forwarded headers only because it is reachable through this private proxy network.

The `cloud` profile requires an administrator password of at least 16 characters and at most 72 UTF-8 bytes. It enables account login, registration, CSRF checks and secure HTTP-only session cookies. Sessions expire after 30 minutes of inactivity. Employee reads and writes are restricted to the signed-in account. Login/signup assets, signup itself, CSRF initialization and a minimal health endpoint are public. Traefik limits login requests to five per minute per source IP and signup requests to three, with matching burst limits.

The `demo` profile creates `demo` (editor) and `viewer` (read-only), both using the public password `KlodspotDemo2026!`. Each receives its own sample directory, seeded only when empty. Registered accounts start with no employees. Removing the `demo` profile hides the demo buttons and prevents demo logins; it preserves their stored data. PostgreSQL has a separate administrator account; the application database role has no superuser or database-creation privileges.

The account migration preserves existing employees by assigning unowned records to the administrator on the first cloud startup. It does not share them with new or demo accounts. Account passwords are stored as BCrypt hashes. The environment controls the administrator's password; signup controls normal members' credentials.

## Operations

Run these commands from the project directory:

```sh
docker compose --env-file deploy/.env -f deploy/compose.yaml ps
docker compose --env-file deploy/.env -f deploy/compose.yaml logs --tail 100 app
curl --fail https://assignment.srv957712.hstgr.cloud/actuator/health
```

The app runs as a non-root user, with a read-only filesystem, a writable temporary directory, a memory limit and bounded container logs. Both containers restart automatically with Docker. Database files persist in `klodspot-assignment_employee_data`.

Create a logical backup before updating the app or database:

```sh
umask 077
mkdir -p /opt/klodspot-assignment-backups
docker compose --env-file deploy/.env -f deploy/compose.yaml exec -T db \
  pg_dump -U postgres -d employees -Fc \
  > /opt/klodspot-assignment-backups/employees-$(date -u +%Y%m%dT%H%M%SZ).dump
```

Keep an encrypted copy off the VPS and verify restoration into a separate empty database. No off-site backup service is configured by this project.

For releases, set a new `IMAGE_TAG` in `deploy/.env`, build, and run `up -d --wait` as above. Retain the previous image. To roll back an application-only release, set the previous tag and run `up -d --no-build --wait`. Review database migration compatibility before rolling back; a container rollback does not undo database migrations.

Version 1.1.0 introduces account ownership. Do not run a pre-account image against this database after users register: old code does not filter by account. Recover from the pre-upgrade backup into a separate database, or fix forward using code that preserves ownership checks.

Version 1.1.1 corrects the branding and changes the image repository to `kloudspot-assignment`; it has no database migration. To return to 1.1.0, restore that release's Compose file as well as its image tag, because it uses the previous image repository.

To rotate the login password, update `ADMIN_PASSWORD` in the private environment file and run `up -d`. This restarts the app and invalidates current sessions. Database password rotation also requires changing the PostgreSQL role password; changing an environment variable alone does not update an initialized database.

`docker compose --env-file deploy/.env -f deploy/compose.yaml down` stops only this project and preserves its data. Do not add `--volumes` unless you intend to permanently delete its database.

References: [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html), [Traefik rate limiting](https://doc.traefik.io/traefik/reference/routing-configuration/http/middlewares/ratelimit/).
