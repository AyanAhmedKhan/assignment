import { csrfHeaders } from './session.js';

async function request(path, options = {}) {
  const securityHeaders = options.method ? await csrfHeaders() : {};
  let response;
  try {
    response = await fetch(`/api/employees${path}`, {
      ...options,
      headers: {
        Accept: 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...securityHeaders,
      },
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new Error('Unable to reach the server. Check your connection and try again.');
  }
  if (response.status === 401) {
    window.location.assign('/login.html?expired');
    throw new Error('Your session expired. Please sign in again.');
  }
  if (response.status === 204) return null;
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message =
      response.status === 403
        ? 'Your account does not have permission to make this change.'
        : 'Something went wrong. Please try again.';
    const error = new Error(body.detail || message);
    error.fields = body.errors || {};
    throw error;
  }
  return body;
}

export const employeeApi = {
  list(params, signal) {
    return request(`?${params}`, { signal });
  },
  get(id) {
    return request(`/${id}`);
  },
  summary() {
    return request('/summary');
  },
  save(id, employee) {
    return request(id ? `/${id}` : '', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(employee),
    });
  },
  remove(id) {
    return request(`/${id}`, { method: 'DELETE' });
  },
};
