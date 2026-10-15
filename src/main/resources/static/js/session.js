export async function loadCsrfToken() {
  const response = await fetch('/api/csrf', { cache: 'no-store' });
  if (!response.ok) throw new Error('Unable to verify your session. Please reload and try again.');
  return response.json();
}

export async function csrfHeaders() {
  const security = await loadCsrfToken();
  if (!security.enabled) return {};

  const cookie = document.cookie.split('; ').find((value) => value.startsWith('XSRF-TOKEN='));
  if (!cookie) throw new Error('Your session could not be verified. Please reload and try again.');
  return { [security.headerName]: decodeURIComponent(cookie.substring('XSRF-TOKEN='.length)) };
}

export async function loadSession() {
  const response = await fetch('/api/session', { cache: 'no-store' });
  if (response.status === 401) {
    window.location.assign('/login.html?expired');
    throw new Error('Please sign in again.');
  }
  if (!response.ok) throw new Error('Unable to load your account. Please reload and try again.');
  return response.json();
}

export async function signOut() {
  const response = await fetch('/logout', { method: 'POST', headers: await csrfHeaders() });
  if (!response.ok) throw new Error('Unable to sign out. Please try again.');
  window.location.assign('/login.html?logout');
}
