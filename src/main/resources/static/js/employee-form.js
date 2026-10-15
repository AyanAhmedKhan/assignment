import { employeeApi } from './employee-api.js';
import { departments } from './employee-view.js';

export function createEmployeeForm(onSaved) {
  const $ = (selector) => document.querySelector(selector);
  let editingId = null;
  let saving = false;
  const form = $('#employee-form');
  const formDialog = $('#employee-dialog');
  form.noValidate = true;
  const today = new Date();
  form.elements.dateOfJoining.max = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

  for (const [value, label] of Object.entries(departments)) {
    form.elements.department.add(new Option(label, value));
  }

  function clearFieldErrors() {
    form.querySelectorAll('.field-error').forEach((element) => {
      element.textContent = '';
    });
    form
      .querySelectorAll('[aria-invalid]')
      .forEach((element) => element.removeAttribute('aria-invalid'));
    $('#form-error').hidden = true;
  }

  function showFieldErrors(errors, message = 'Please check the highlighted fields.') {
    $('#form-error').textContent = message;
    $('#form-error').hidden = false;
    for (const [field, error] of Object.entries(errors)) {
      if (!form.elements[field]) continue;
      $(`#error-${field}`).textContent = error;
      form.elements[field].setAttribute('aria-invalid', 'true');
      form.elements[field].setAttribute('aria-describedby', `error-${field}`);
    }
  }

  function validateForm(values) {
    const errors = {};
    if (!values.fullName || values.fullName.length > 120)
      errors.fullName = 'Enter a name of up to 120 characters.';
    if (!values.email || form.elements.email.validity.typeMismatch || values.email.length > 254)
      errors.email = 'Enter a valid email address.';
    if (
      !/^(?=(?:\D*\d){7,15}\D*$)\+?[\d ()-]+$/.test(values.phoneNumber) ||
      values.phoneNumber.length > 32
    )
      errors.phoneNumber = 'Enter a phone number with 7 to 15 digits.';
    if (!Object.hasOwn(departments, values.department)) errors.department = 'Choose a department.';
    if (!values.designation || values.designation.length > 120)
      errors.designation = 'Enter a designation of up to 120 characters.';
    if (!values.dateOfJoining || !form.elements.dateOfJoining.validity.valid)
      errors.dateOfJoining = 'Enter a valid joining date, today or earlier.';
    if (!/^(?:\d+|\d*\.\d{1,2})$/.test(values.salary) || Number(values.salary) > 9999999999.99)
      errors.salary = 'Enter an amount from 0 to 9,999,999,999.99 with up to 2 decimal places.';
    if (!['ACTIVE', 'INACTIVE'].includes(values.status))
      errors.status = 'Choose an employment status.';
    return errors;
  }

  function openForm(employee = null) {
    editingId = employee?.id ?? null;
    form.reset();
    clearFieldErrors();
    $('#form-title').textContent = employee ? 'Edit employee' : 'Add employee';
    $('#save-employee').textContent = employee ? 'Save changes' : 'Add employee';
    if (employee) {
      for (const [key, value] of Object.entries(employee))
        if (form.elements[key]) form.elements[key].value = value;
    }
    formDialog.showModal();
    form.elements.fullName.focus();
  }

  function setFormBusy(busy) {
    saving = busy;
    form.querySelectorAll('button, input, select').forEach((element) => {
      element.disabled = busy;
    });
    $('#save-employee').textContent = busy
      ? 'Saving…'
      : editingId
        ? 'Save changes'
        : 'Add employee';
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (saving) return;
    clearFieldErrors();
    const values = Object.fromEntries(
      [...new FormData(form)].map(([key, value]) => [key, value.trim()]),
    );
    const errors = validateForm(values);
    if (Object.keys(errors).length) {
      showFieldErrors(errors);
      form.querySelector('[aria-invalid=true]')?.focus();
      return;
    }
    setFormBusy(true);
    try {
      // Send decimal salary as text so Java's BigDecimal keeps the entered decimal value.
      await employeeApi.save(editingId, values);
      formDialog.close();
      await onSaved(editingId ? 'Employee details updated.' : 'Employee added to the directory.');
    } catch (error) {
      showFieldErrors(error.fields || {}, error.message);
    } finally {
      setFormBusy(false);
      form.querySelector('[aria-invalid=true]')?.focus();
    }
  });
  form.addEventListener('input', (event) => {
    const field = event.target.name;
    if (!field) return;
    event.target.removeAttribute('aria-invalid');
    $(`#error-${field}`).textContent = '';
  });
  formDialog.addEventListener('cancel', (event) => {
    if (saving) event.preventDefault();
  });
  $('#close-form').addEventListener('click', () => formDialog.close());
  $('#cancel-form').addEventListener('click', () => formDialog.close());

  return { open: openForm };
}
