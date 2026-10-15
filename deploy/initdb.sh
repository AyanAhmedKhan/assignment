#!/bin/sh
set -eu
# The app owns its schema, but cannot administer PostgreSQL or other databases.
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=ON_ERROR_STOP=1 \
  --set=app_password="$APP_DB_PASSWORD" <<'SQL'
CREATE USER klodspot WITH PASSWORD :'app_password';
ALTER DATABASE employees OWNER TO klodspot;
GRANT ALL ON SCHEMA public TO klodspot;
SQL
