import { csrfHeaders } from './session.js';

const form = document.querySelector('#registration-form');
const message = document.querySelector('#registration-message');
const submitButton = document.querySelector('#create-account');
let submitting = false;

function showErrors(errors, text) {
  message.textContent = text;
  message.hidden = false;
  for (const [name, error] of Object.entries(errors)) {
    const input = form.elements[name];
    if (!input) continue;
    input.setAttribute('aria-invalid', 'true');
    document.querySelector(`#error-${name}`).textContent = error;
  }
  form.querySelector('[aria-invalid="true"]')?.focus();
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (submitting) return;
  message.hidden = true;
  form.querySelectorAll('.field-error').forEach((element) => {
    element.textContent = '';
  });
  form
    .querySelectorAll('[aria-invalid]')
    .forEach((element) => element.removeAttribute('aria-invalid'));

  const values = Object.fromEntries(new FormData(form));
  values.fullName = values.fullName.trim();
  values.email = values.email.trim();
  const errors = {};
  if (!values.fullName) errors.fullName = 'Enter your name.';
  if (!values.email || form.elements.email.validity.typeMismatch)
    errors.email = 'Enter a valid email address.';
  if (values.password.length < 12 || values.password.length > 64)
    errors.password = 'Use a password of 12 to 64 characters.';
  if (!values.confirmPassword) errors.confirmPassword = 'Confirm your password.';
  else if (values.password !== values.confirmPassword)
    errors.confirmPassword = 'The passwords do not match.';
  if (Object.keys(errors).length) {
    showErrors(errors, 'Please check the highlighted fields.');
    return;
  }

  submitting = true;
  submitButton.disabled = true;
  submitButton.textContent = 'Creating account…';
  try {
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...(await csrfHeaders()) },
      body: JSON.stringify(values),
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      showErrors(
        body.errors || {},
        body.detail ||
          (response.status === 429
            ? 'Too many attempts. Please wait a minute and try again.'
            : 'Unable to create your account. Please try again.'),
      );
      return;
    }
    window.location.assign('/login.html?registered');
  } catch {
    showErrors({}, 'Unable to reach the server. Check your connection and try again.');
  } finally {
    submitting = false;
    submitButton.disabled = false;
    submitButton.textContent = 'Create account';
  }
});

csrfHeaders()
  .then(() => {
    submitButton.disabled = false;
  })
  .catch(() => {
    showErrors({}, 'Unable to connect. Please reload this page and try again.');
  });
