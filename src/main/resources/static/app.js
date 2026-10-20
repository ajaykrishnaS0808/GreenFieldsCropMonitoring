const $ = (selector) => document.querySelector(selector);
const state = { crops: [], reminders: [], reports: [], yieldChart: null, areaChart: null, showAllReminders: false };
const colors = ['#a5e6a0', '#69c7a6', '#8fa8df', '#e7c77f', '#cf9cda', '#e39c84'];

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  });
  const data = response.headers.get('content-type')?.includes('application/json') ? await response.json() : await response.text();
  if (!response.ok) throw new Error(data?.error || data || `Request failed (${response.status})`);
  return data;
}
function showToast(message) {
  const toast = $('#toast'); toast.textContent = message; toast.classList.add('show');
  clearTimeout(showToast.timer); showToast.timer = setTimeout(() => toast.classList.remove('show'), 2800);
}
function safe(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
}
function dateLabel(date) {
  if (!date) return '—';
  const d = new Date(`${date}T12:00:00`);
  return Number.isNaN(d.getTime()) ? date : d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}
function statusClass(status) { return String(status || '').toLowerCase().replace(/[^a-z]/g, ''); }
function fillCropSelects() {
  ['#activityCrop', '#reminderCrop'].forEach(selector => {
    $(selector).innerHTML = state.crops.map(c => `<option value="${c.id}">${safe(c.name)} · ${safe(c.variety)}</option>`).join('');
  });
}
function renderCrops() {
  $('#cropTable').innerHTML = state.crops.length ? state.crops.map(c => `
    <tr><td><span class="crop-name">${safe(c.name)}</span><span class="crop-sub">${safe(c.variety)}</span></td>
    <td>${Number(c.areaAcres).toLocaleString('en-IN',{maximumFractionDigits:1})} ac</td><td>${safe(c.season)}</td>
    <td>${dateLabel(c.expectedHarvest)}</td><td><span class="status ${statusClass(c.status)}">${safe(c.status)}</span></td><td><button class="row-action" type="button" data-delete-crop="${c.id}">Delete</button></td></tr>`).join('') :
    '<tr><td colspan="6" class="empty">No crops yet. Add your first crop to get started.</td></tr>';
}
function renderReminders() {
  const pending = state.reminders.filter(r => !r.completed);
  $('#navReminderCount').textContent = pending.length;
  const visible = state.showAllReminders ? state.reminders : state.reminders.slice(0, 4);
  $('#reminderList').innerHTML = visible.length ? visible.map(r => {
    const overdue = !r.completed && r.dueDate < new Date().toISOString().slice(0,10);
    return `<div class="reminder-item"><input class="task-check" type="checkbox" ${r.completed ? 'checked' : ''} ${r.completed ? 'disabled' : ''} aria-label="Complete reminder" data-reminder="${r.id}">
      <div class="task-main"><div class="task-title">${safe(r.type)} · ${safe(r.cropName)}</div><span class="task-meta">${safe(r.notes)}</span></div>
      <span class="task-date ${overdue ? 'overdue' : ''}">${r.completed ? 'Done' : dateLabel(r.dueDate)}</span><button class="row-action" type="button" data-delete-reminder="${r.id}">Delete</button></div>`;
  }).join('') : '<div class="empty">No reminders yet. Add a task to plan field work.</div>';
  $('#showAllReminders').innerHTML = state.showAllReminders ? 'Show fewer <span>↑</span>' : `View all reminders <span>→</span>`;
  $('#showAllReminders').style.display = state.reminders.length > 4 ? 'block' : 'none';
}
function renderActivities(items) {
  const icons = { Irrigation: '♧', Harvest: '↗', Fertilizer: '✳', Inspection: '⌕', Planting: '♧', 'Pest control': '◇' };
  $('#activityList').innerHTML = items.length ? items.slice(0, 5).map(a => `
    <div class="timeline-item"><div class="timeline-icon ${String(a.type).toLowerCase()}">${icons[a.type] || '•'}</div>
    <div><div class="timeline-title">${safe(a.type)} · ${safe(a.cropName)}</div><div class="timeline-detail">${safe(a.details)}</div><div class="timeline-meta">${dateLabel(a.activityDate)}</div><button class="row-action timeline-delete" type="button" data-delete-activity="${a.id}">Delete</button></div></div>`).join('') :
    '<div class="empty">No activity recorded yet.</div>';
}
function renderReports() {
  $('#reportTable').innerHTML = state.reports.length ? state.reports.map(r => `
    <tr><td><span class="crop-name">${safe(r.cropName)}</span></td><td>${safe(r.season)}</td><td>${dateLabel(r.harvestDate)}</td>
    <td><b>${Number(r.yieldTons).toFixed(1)} t</b></td><td>${Number(r.yieldPerAcre).toFixed(2)}</td></tr>`).join('') :
    '<tr><td colspan="5" class="empty">No harvest records yet.</td></tr>';
}
function renderCharts(stats) {
  // Charts are optional UI: the rest of the app must still work if the CDN is unavailable.
  if (typeof Chart === 'undefined') {
    console.warn('Chart.js did not load; skipping dashboard charts.');
    return;
  }
  const byCrop = state.crops.map(c => ({ crop: c.name, yieldTons: Number(c.expectedYieldTons) || 0 }));
  if (state.yieldChart) state.yieldChart.destroy();
  state.yieldChart = new Chart($('#yieldChart'), {
    type: 'bar',
    data: { labels: byCrop.map(x => x.crop), datasets: [{ data: byCrop.map(x => x.yieldTons), backgroundColor: '#8ed98c', borderRadius: 5, maxBarThickness: 36 }] },
    options: { maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip: { callbacks: { label: c => `${Number(c.raw).toFixed(1)} tons expected` } } },
      scales: { x: { grid: { display: false }, ticks: { color: '#91a094', font: { size: 10 } }, border: { display: false } },
        y: { beginAtZero: true, grid: { color: '#2b372e' }, ticks: { color: '#7e8e81', font: { size: 9 } }, border: { display: false } } } }
  });
  const areaMap = new Map();
  state.crops.forEach(c => areaMap.set(c.name, (areaMap.get(c.name) || 0) + Number(c.areaAcres)));
  const labels = [...areaMap.keys()], values = [...areaMap.values()];
  if (state.areaChart) state.areaChart.destroy();
  state.areaChart = new Chart($('#areaChart'), {
    type: 'doughnut',
    data: { labels, datasets: [{ data: values, backgroundColor: colors, borderColor: '#171f1a', borderWidth: 4, hoverOffset: 5 }] },
    options: { cutout: '76%', maintainAspectRatio: false, plugins: { legend: { display: false } } }
  });
  $('#areaLegend').innerHTML = labels.map((label, i) => `<span><i style="background:${colors[i % colors.length]}"></i>${safe(label)} · ${values[i].toFixed(1)} ac</span>`).join('');
}
async function loadDashboard() {
  try {
    const [stats, crops, activities, reminders, reports] = await Promise.all([
      api('/api/stats'), api('/api/crops'), api('/api/activities'), api('/api/reminders'), api('/api/reports')
    ]);
    state.crops = crops; state.reminders = reminders; state.reports = reports;
    $('#totalAcres').textContent = Number(stats.totalAcres).toLocaleString('en-IN', {maximumFractionDigits:1});
    $('#activeCrops').textContent = stats.activeCrops;
    $('#cropCount').textContent = stats.cropCount;
    $('#expectedYield').textContent = Number(stats.expectedYield).toLocaleString('en-IN', {maximumFractionDigits:1});
    $('#upcomingReminders').textContent = stats.upcomingReminders;
    $('#donutAcres').textContent = Number(stats.totalAcres).toLocaleString('en-IN', {maximumFractionDigits:1});
    renderCrops();
    renderReminders();
    renderActivities(activities);
    renderReports();
    // Populate form dropdowns before optional charts, so a chart/CDN failure cannot break data entry.
    fillCropSelects();
    try {
      renderCharts(stats);
    } catch (chartError) {
      console.warn('Dashboard charts could not be rendered:', chartError);
    }
  } catch (error) {
    showToast(`Could not load data: ${error.message}`);
    console.error(error);
  }
}
function openDialog(id) {
  if (!state.crops.length && id !== 'cropDialog') { showToast('Add a crop first.'); return; }
  const dialog = document.getElementById(id);
  if (dialog && !dialog.open) dialog.showModal();
}
document.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', () => document.getElementById(button.dataset.close).close()));
$('#addCropJump').addEventListener('click', () => openDialog('cropDialog'));
$('#addCropInline').addEventListener('click', () => openDialog('cropDialog'));
$('#addActivityJump').addEventListener('click', () => openDialog('activityDialog'));
$('#activityFormJump').addEventListener('click', () => openDialog('activityDialog'));
$('#addReminderJump').addEventListener('click', () => openDialog('reminderDialog'));
$('#refreshBtn').addEventListener('click', loadDashboard);
$('#showAllReminders').addEventListener('click', () => { state.showAllReminders = !state.showAllReminders; renderReminders(); });

$('#cropForm').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const data = Object.fromEntries(new FormData(form));
  data.name = data.cropPreset === 'Other' ? String(data.customCropName || '').trim() : data.cropPreset;
  delete data.cropPreset; delete data.customCropName;
  if (!data.name) { showToast('Enter a crop name.'); return; }
  data.areaAcres = Number(data.areaAcres);
  data.expectedYieldTons = Number(data.expectedYieldTons);
  try {
    await api('/api/crops', { method: 'POST', body: JSON.stringify({ ...data, id: 0 }) });
    form.reset();
    $('#cropDialog').close();
    await loadDashboard();
    showToast('Crop saved successfully.');
  } catch (e) {
    showToast(e.message);
  }
});

$('#activityForm').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const data = Object.fromEntries(new FormData(form));
  data.cropId = Number(data.cropId);
  try {
    await api('/api/activities', { method: 'POST', body: JSON.stringify(data) });
    form.reset();
    $('#activityDialog').close();
    await loadDashboard();
    showToast('Activity logged.');
  } catch (e) {
    showToast(e.message);
  }
});

$('#reminderForm').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const data = Object.fromEntries(new FormData(form));
  data.cropId = Number(data.cropId);
  try {
    await api('/api/reminders', { method: 'POST', body: JSON.stringify(data) });
    form.reset();
    $('#reminderDialog').close();
    await loadDashboard();
    showToast('Reminder added.');
  } catch (e) {
    showToast(e.message);
  }
});


$('#reminderList').addEventListener('change', async event => {
  const input = event.target.closest('[data-reminder]');
  if (!input || !input.checked) return;
  try { await api(`/api/reminders/${input.dataset.reminder}/complete`, { method: 'PATCH' }); await loadDashboard(); showToast('Reminder marked complete.'); }
  catch (e) { input.checked = false; showToast(e.message); }
});

// Crop selector: reveal a free-text name only when Other is selected.
$('#cropForm').addEventListener('reset', () => {
  $('#customCropNameWrap').hidden = true;
  $('#customCropName').required = false;
});
$('#cropPreset').addEventListener('change', () => {
  const custom = $('#cropPreset').value === 'Other';
  $('#customCropNameWrap').hidden = !custom;
  $('#customCropName').required = custom;
  if (!custom) $('#customCropName').value = '';
});

// The profile ellipsis is a real actions menu, not decorative text.
const menuButton = $('#globalMenuButton');
const globalMenu = $('#globalMenu');
menuButton.addEventListener('click', () => {
  globalMenu.hidden = !globalMenu.hidden;
  menuButton.setAttribute('aria-expanded', String(!globalMenu.hidden));
});
document.addEventListener('click', event => {
  if (!globalMenu.contains(event.target) && !menuButton.contains(event.target)) {
    globalMenu.hidden = true;
    menuButton.setAttribute('aria-expanded', 'false');
  }
});
$('#menuRefresh').addEventListener('click', () => { globalMenu.hidden = true; loadDashboard(); showToast('Dashboard refreshed.'); });
$('#menuAddCrop').addEventListener('click', () => { globalMenu.hidden = true; openDialog('cropDialog'); });
$('#menuAddReminder').addEventListener('click', () => { globalMenu.hidden = true; openDialog('reminderDialog'); });
document.querySelectorAll('[data-chart-info]').forEach(button => button.addEventListener('click', () => {
  showToast(button.dataset.chartInfo === 'yieldChart'
    ? 'This graph shows expected tons from your saved crop plans. Actual harvests appear in Yield reports.'
    : 'This chart shows how your registered crop area is distributed.');
}));

async function deleteRecord(path, label) {
  if (!confirm(`Delete this ${label}? This cannot be undone.`)) return;
  try {
    await api(path, { method: 'DELETE' });
    await loadDashboard();
    showToast(`${label[0].toUpperCase()}${label.slice(1)} deleted.`);
  } catch (e) { showToast(e.message); }
}
document.addEventListener('click', event => {
  const button = event.target.closest('[data-delete-crop],[data-delete-reminder],[data-delete-activity]');
  if (!button) return;
  if (button.dataset.deleteCrop) deleteRecord(`/api/crops/${button.dataset.deleteCrop}`, 'crop');
  if (button.dataset.deleteReminder) deleteRecord(`/api/reminders/${button.dataset.deleteReminder}`, 'reminder');
  if (button.dataset.deleteActivity) deleteRecord(`/api/activities/${button.dataset.deleteActivity}`, 'activity');
});

loadDashboard();
