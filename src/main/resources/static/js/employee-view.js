import { icon } from './icons.js';

export const departments = {
  ENGINEERING: 'Engineering',
  PRODUCT: 'Product',
  DESIGN: 'Design',
  OPERATIONS: 'Operations',
  HUMAN_RESOURCES: 'Human Resources',
  FINANCE: 'Finance',
};
export const escapeHtml = (value) =>
  String(value ?? '').replace(
    /[&<>"']/g,
    (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char],
  );
export const employeeId = (id) => `EMP-${String(id).padStart(4, '0')}`;
const dateLabel = (value) =>
  new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(
    new Date(`${value}T00:00:00`),
  );
const salaryLabel = (value) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(value);
const avatar = (employee) =>
  `<span class="avatar color-${employee.id % 5}" aria-hidden="true">${escapeHtml(
    employee.fullName
      .split(/\s+/)
      .map((part) => [...part][0])
      .slice(0, 2)
      .join('')
      .toUpperCase(),
  )}</span>`;
const statusBadge = (status) =>
  `<span class="status ${status === 'INACTIVE' ? 'inactive' : ''}">${status === 'ACTIVE' ? 'Active' : 'Inactive'}</span>`;

export function renderEmployeeRows(employees, canEdit) {
  return employees
    .map((employee) => {
      const name = escapeHtml(employee.fullName);
      const actions = canEdit
        ? `
      <div class="row-actions">
        <button class="icon-button" data-edit="${employee.id}" aria-label="Edit ${name}" title="Edit employee">${icon('edit')}</button>
        <button class="icon-button delete-button" data-delete="${employee.id}" aria-label="Delete ${name}" title="Delete employee">${icon('trash')}</button>
      </div>`
        : '';

      return `
      <tr>
        <td><div class="person">
          ${avatar(employee)}
          <div>
            <a class="person-name" href="#employees/${employee.id}">${name}</a>
            <span class="person-email">${escapeHtml(employee.email)}</span>
          </div>
        </div></td>
        <td><span class="department-badge">${departments[employee.department]}</span></td>
        <td>${escapeHtml(employee.designation)}</td>
        <td>${dateLabel(employee.dateOfJoining)}</td>
        <td>${statusBadge(employee.status)}</td>
        <td>${actions}</td>
      </tr>`;
    })
    .join('');
}

export function renderEmployeeDetails(employee, canEdit) {
  const email = escapeHtml(employee.email);
  const fields = [
    ['Employee ID', employeeId(employee.id)],
    ['Full name', escapeHtml(employee.fullName)],
    ['Employment status', statusBadge(employee.status)],
    ['Email address', `<a href="mailto:${email}">${email}</a>`],
    ['Phone number', escapeHtml(employee.phoneNumber)],
    ['Department', departments[employee.department]],
    ['Designation', escapeHtml(employee.designation)],
    ['Date of joining', dateLabel(employee.dateOfJoining)],
    ['Annual salary (INR)', salaryLabel(employee.salary)],
  ];
  const actions = canEdit
    ? `
    <div class="detail-actions">
      <button class="button secondary" id="detail-delete">${icon('trash')}Delete</button>
      <button class="button primary" id="detail-edit">${icon('edit')}Edit employee</button>
    </div>`
    : '';

  return `
    <a class="back-link" href="#employees">${icon('left')}Back to employees</a>
    <div class="detail-heading">
      ${avatar(employee)}
      <div>
        <p class="eyebrow">${employeeId(employee.id)}</p>
        <h1>${escapeHtml(employee.fullName)}</h1>
        <p>${escapeHtml(employee.designation)} · ${departments[employee.department]}</p>
      </div>
      ${actions}
    </div>
    <article class="detail-card">
      <h2>Employee information</h2>
      <dl class="detail-grid">
        ${fields.map(([label, value]) => `<div><dt>${label}</dt><dd>${value}</dd></div>`).join('')}
      </dl>
    </article>`;
}
