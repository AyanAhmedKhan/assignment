"""Create a private deployment configuration once, without printing credentials."""
import os
from pathlib import Path
import secrets

destination = Path(__file__).resolve().parent / ".env"
settings = {
    "APP_HOST": "assignment.srv957712.hstgr.cloud",
    "PROXY_NETWORK": "root_default",
    "TLS_RESOLVER": "mytlschallenge",
    "IMAGE_TAG": "1.1.1",
    "ADMIN_USERNAME": "admin",
    "ADMIN_PASSWORD": secrets.token_urlsafe(24),
    "DB_PASSWORD": secrets.token_hex(24),
    "DB_ADMIN_PASSWORD": secrets.token_hex(24),
}
try:
    fd = os.open(destination, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
except FileExistsError:
    print("Keeping existing deploy/.env and credentials.")
else:
    with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as output:
        output.write("".join(f"{key}={value}\n" for key, value in settings.items()))
    print("Created private deploy/.env with independent random credentials.")
