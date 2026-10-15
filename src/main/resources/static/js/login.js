import { loadCsrfToken } from './session.js';

const message = document.querySelector('#login-message');
const query = new URLSearchParams(window.location.search);
function showMessage(text) {
  message.textContent = text;
  message.hidden = false;
}
if (query.has('error')) showMessage('The username or password is incorrect. Please try again.');
else if (query.has('logout')) showMessage('You have been signed out.');
else if (query.has('expired')) showMessage('Your session expired. Please sign in again.');
else if (query.has('registered'))
  showMessage('Your account is ready. Sign in with your email to open your directory.');

try {
  const security = await loadCsrfToken();
  if (!security.enabled) {
    window.location.replace('/');
  } else {
    const token = document.querySelector('#csrf-token');
    token.name = security.parameterName;
    token.value = security.token;
    document.querySelector('#sign-in').disabled = false;
    document.querySelector('#demo-accounts').hidden = !security.demoAccounts;
  }
} catch {
  showMessage('Unable to connect. Please reload this page and try again.');
}

document.querySelectorAll('[data-demo-user]').forEach((button) => {
  button.addEventListener('click', () => {
    const form = document.querySelector('#login-form');
    form.elements.username.value = button.dataset.demoUser;
    form.elements.password.value = 'KlodspotDemo2026!';
    form.requestSubmit();
  });
});
