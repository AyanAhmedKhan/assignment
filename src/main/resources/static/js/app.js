import { employeeApi } from './employee-api.js';
import { createEmployeeForm } from './employee-form.js';
import {
  departments,
  escapeHtml,
  employeeId,
  renderEmployeeRows,
  renderEmployeeDetails,
} from './employee-view.js';
import { initializeIcons } from './icons.js';
import { loadSession, signOut } from './session.js';

const $ = (selector) => document.querySelector(selector);
const state = { page: 0, totalPages: 0, loading: false };
let listController;
let listSequence = 0;
let routeSequence = 0;
let searchTimer;
let toastTimer;
let deletingEmployee = null;
let deleting = false;
let canEdit = false;
const deleteDialog = $('#delete-dialog');
const employeeForm = createEmployeeForm(async (message) => {
  toast(message);
  await Promise.all([loadSummary(), refreshView()]);
});

initializeIcons();
for (const [value, label] of Object.entries(departments)) {
  $('#department-filter').add(new Option(label, value));
}
$('#add-employee').addEventListener('click', () => employeeForm.open());
$('#sign-out').addEventListener('click', () => signOut().catch((error) => toast(error.message)));

function toast(message) {
  clearTimeout(toastTimer);
  $('#toast').textContent = message;
  $('#toast').hidden = false;
  toastTimer = setTimeout(() => {
    $('#toast').hidden = true;
  }, 4500);
}

async function loadSummary() {
  try {
    const summary = await employeeApi.summary();
    for (const key of ['total', 'active', 'inactive', 'departments'])
      $(`#stat-${key}`).textContent = summary[key];
    $('#summary-error').hidden = true;
  } catch {
    for (const key of ['total', 'active', 'inactive', 'departments'])
      $(`#stat-${key}`).textContent = '—';
    $('#summary-error').hidden = false;
  }
}

function updatePagination() {
  $('#previous-page').disabled = state.loading || state.page <= 0;
  $('#next-page').disabled = state.loading || state.page + 1 >= state.totalPages;
  $('#page-label').textContent = `${state.page + 1} / ${Math.max(1, state.totalPages)}`;
}

async function loadEmployees() {
  listController?.abort();
  listController = new AbortController();
  const sequence = ++listSequence;
  state.loading = true;
  updatePagination();
  $('#employees-body').setAttribute('aria-busy', 'true');
  const [sortBy, direction] = $('#sort').value.split(':');
  const params = new URLSearchParams({
    page: state.page,
    size: $('#page-size').value,
    sortBy,
    direction,
  });
  for (const [key, selector] of [
    ['search', '#search'],
    ['department', '#department-filter'],
    ['status', '#status-filter'],
  ]) {
    if ($(selector).value.trim()) params.set(key, $(selector).value.trim());
  }
  $('#clear-filters').hidden = !['search', 'department', 'status'].some((key) => params.has(key));
  try {
    const result = await employeeApi.list(params, listController.signal);
    if (sequence !== listSequence) return;
    if (state.page > 0 && state.page >= result.totalPages) {
      state.page = Math.max(0, result.totalPages - 1);
      return loadEmployees();
    }
    state.totalPages = result.totalPages;
    $('#result-count').textContent = result.totalElements;
    $('#employees-body').innerHTML = renderEmployeeRows(result.content, canEdit);
    $('#list-state').hidden = result.content.length > 0;
    $('#list-state').innerHTML =
      result.totalElements === 0 && !$('#clear-filters').hidden
        ? '<h3>No matching employees</h3><p>Try a different search or clear your filters.</p><button class="button secondary" data-reset-filters>Clear filters</button>'
        : '<h3>Your team starts here</h3><p>Add your first employee to get the directory going.</p><button class="button primary" data-create>Add employee</button>';
    const first = result.totalElements ? result.page * result.size + 1 : 0;
    $('#range-label').textContent =
      `Showing ${first}–${result.page * result.size + result.content.length} of ${result.totalElements} employees`;
    $('#list-announcement').textContent =
      `${result.totalElements} employees found. Page ${state.page + 1}.`;
  } catch (error) {
    if (error.name === 'AbortError' || sequence !== listSequence) return;
    state.totalPages = 0;
    $('#employees-body').innerHTML = '';
    $('#result-count').textContent = '—';
    $('#list-state').hidden = false;
    $('#list-state').innerHTML =
      `<h3>Couldn't load employees</h3><p>${escapeHtml(error.message)}</p><button class="button secondary" data-retry>Try again</button>`;
    $('#range-label').textContent = 'Directory unavailable';
    $('#list-announcement').textContent = error.message;
  } finally {
    if (sequence === listSequence) {
      state.loading = false;
      $('#employees-body').setAttribute('aria-busy', 'false');
      updatePagination();
    }
  }
}

function openDelete(employee) {
  deletingEmployee = employee;
  $('#delete-description').textContent =
    `This will permanently remove ${employee.fullName} (${employeeId(employee.id)}) from the directory. This cannot be undone.`;
  $('#delete-error').hidden = true;
  deleteDialog.showModal();
}
$('#cancel-delete').addEventListener('click', () => deleteDialog.close());
deleteDialog.addEventListener('cancel', (event) => {
  if (deleting) event.preventDefault();
});
$('#confirm-delete').addEventListener('click', async () => {
  if (deleting) return;
  deleting = true;
  $('#confirm-delete').disabled = true;
  $('#cancel-delete').disabled = true;
  $('#confirm-delete').textContent = 'Deleting…';
  try {
    await employeeApi.remove(deletingEmployee.id);
    deleteDialog.close();
    toast('Employee removed from the directory.');
    if (location.hash.startsWith('#employees/')) location.hash = '#employees';
    else await loadEmployees();
    await loadSummary();
  } catch (error) {
    $('#delete-error').textContent = error.message;
    $('#delete-error').hidden = false;
  } finally {
    deleting = false;
    $('#confirm-delete').disabled = false;
    $('#cancel-delete').disabled = false;
    $('#confirm-delete').textContent = 'Delete employee';
  }
});

async function refreshView() {
  const sequence = ++routeSequence;
  const match = location.hash.match(/^#employees\/(\d+)$/);
  $('#directory').hidden = Boolean(match);
  $('#employee-detail').hidden = !match;
  if (!match) {
    document.title = 'Kloudspot Assignment | Employee directory';
    return loadEmployees();
  }
  const detail = $('#employee-detail');
  detail.innerHTML = '<p class="muted" role="status">Loading employee…</p>';
  try {
    const employee = await employeeApi.get(match[1]);
    if (sequence !== routeSequence) return;
    document.title = `${employee.fullName} | Kloudspot Assignment`;
    detail.innerHTML = renderEmployeeDetails(employee, canEdit);
    $('#detail-edit')?.addEventListener('click', () => employeeForm.open(employee));
    $('#detail-delete')?.addEventListener('click', () => openDelete(employee));
  } catch (error) {
    if (sequence !== routeSequence) return;
    detail.innerHTML = `<a class="back-link" href="#employees">Back to employees</a><div class="detail-card"><h2>Couldn't load this employee</h2><p class="form-intro">${escapeHtml(error.message)}</p><button class="button secondary" id="retry-detail">Try again</button></div>`;
    $('#retry-detail').addEventListener('click', refreshView);
  }
}

$('#employees-body').addEventListener('click', async (event) => {
  const button = event.target.closest('[data-edit], [data-delete]');
  if (!button) return;
  button.disabled = true;
  try {
    const employee = await employeeApi.get(button.dataset.edit || button.dataset.delete);
    if (button.dataset.edit) employeeForm.open(employee);
    else openDelete(employee);
  } catch (error) {
    toast(error.message);
  } finally {
    button.disabled = false;
  }
});

function resetFilters() {
  clearTimeout(searchTimer);
  $('#search').value = '';
  $('#department-filter').value = '';
  $('#status-filter').value = '';
  state.page = 0;
  loadEmployees();
}
$('#clear-filters').addEventListener('click', resetFilters);
$('#list-state').addEventListener('click', (event) => {
  if (event.target.closest('[data-retry]')) {
    loadEmployees();
    loadSummary();
  }
  if (event.target.closest('[data-reset-filters]')) resetFilters();
  if (event.target.closest('[data-create]')) employeeForm.open();
});
$('#search').addEventListener('input', () => {
  clearTimeout(searchTimer);
  // Cancel the previous request immediately, including during the debounce window.
  listController?.abort();
  searchTimer = setTimeout(() => {
    state.page = 0;
    loadEmployees();
  }, 250);
});
for (const selector of ['#department-filter', '#status-filter', '#sort', '#page-size']) {
  $(selector).addEventListener('change', () => {
    clearTimeout(searchTimer);
    state.page = 0;
    loadEmployees();
  });
}
$('#previous-page').addEventListener('click', () => {
  state.page--;
  loadEmployees();
});
$('#next-page').addEventListener('click', () => {
  state.page++;
  loadEmployees();
});
window.addEventListener('hashchange', refreshView);

async function start() {
  try {
    const session = await loadSession();
    canEdit = session.role !== 'VIEWER';
    $('#add-employee').hidden = !canEdit;
    $('#sign-out').hidden = !session.enabled;
    $('#workspace-name').textContent = session.displayName;
    $('#workspace-name').title = session.displayName;
    if (session.demo) {
      $('#account-notice').textContent = canEdit
        ? 'Demo editor: changes here are shared with other demo visitors. Create an account for your own directory.'
        : 'Demo viewer: this sample directory is read-only. Create an account to manage your own employees.';
      $('#account-notice').hidden = false;
    }
    await Promise.all([loadSummary(), refreshView()]);
  } catch (error) {
    $('#account-notice').textContent = error.message;
    $('#account-notice').hidden = false;
  }
}
start();
