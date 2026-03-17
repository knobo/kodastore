// KodaStore UI — Application

const API = '/api';

// ===== State =====

let currentView = 'streams';
let lastBackView = 'streams';

// ===== Helpers =====

function hashColor(str) {
  let h = 0;
  for (let i = 0; i < str.length; i++) h = ((h << 5) - h + str.charCodeAt(i)) | 0;
  return Math.abs(h) % 8;
}

function formatTime(iso) {
  const d = new Date(iso);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function relativeTime(iso) {
  const diff = Date.now() - new Date(iso).getTime();
  if (diff < 60000) return 'just now';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
  return formatTime(iso);
}

function jsonPretty(obj) {
  return JSON.stringify(obj, null, 2);
}

async function apiFetch(path) {
  const res = await fetch(API + path);
  if (!res.ok) {
    const body = await res.json().catch(() => ({ message: res.statusText }));
    throw { status: res.status, ...body };
  }
  return res.json();
}

async function apiPost(path, body) {
  const res = await fetch(API + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  const json = await res.json();
  if (!res.ok) throw { status: res.status, ...json };
  return json;
}

// ===== Toast =====

function showToast(msg, type = 'info') {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.textContent = msg;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(12px)';
    toast.style.transition = 'all 200ms';
    setTimeout(() => toast.remove(), 200);
  }, 4000);
}

// ===== Navigation =====

function switchView(name) {
  if (name !== 'stream-detail') lastBackView = name;
  currentView = name;

  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  const view = document.getElementById(`view-${name}`);
  if (view) view.classList.add('active');

  document.querySelectorAll('.nav-item').forEach(n => {
    n.classList.toggle('active', n.dataset.view === name);
  });

  if (name === 'global') loadGlobalFeed();
}

document.querySelectorAll('.nav-item[data-view]').forEach(btn => {
  btn.addEventListener('click', () => switchView(btn.dataset.view));
});

document.getElementById('backToList').addEventListener('click', () => {
  switchView(lastBackView);
});

// ===== Health Check =====

async function checkHealth() {
  const dot = document.querySelector('.status-dot');
  const text = document.querySelector('.status-text');
  try {
    const res = await fetch('/actuator/health');
    const data = await res.json();
    if (data.status === 'UP') {
      dot.className = 'status-dot connected';
      text.textContent = 'Connected';
    } else {
      dot.className = 'status-dot error';
      text.textContent = 'Degraded';
    }
  } catch {
    dot.className = 'status-dot error';
    text.textContent = 'Disconnected';
  }
}

// ===== Streams View =====

const streamSearch = document.getElementById('streamSearch');
let searchTimeout;

streamSearch.addEventListener('input', () => {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    const q = streamSearch.value.trim();
    if (q.length > 0) searchStream(q);
  }, 300);
});

streamSearch.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    const q = streamSearch.value.trim();
    if (q) searchStream(q);
  }
});

async function searchStream(query) {
  const list = document.getElementById('streamList');
  list.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

  try {
    const state = await apiFetch(`/streams/${encodeURIComponent(query)}`);
    if (state.events.length === 0) {
      list.innerHTML = `
        <div class="empty-state">
          <p>No events found for "${escapeHtml(query)}"</p>
          <span class="hint">Check the stream ID and try again</span>
        </div>`;
      return;
    }
    renderStreamCard(list, state);
  } catch (err) {
    if (err.status === 404) {
      list.innerHTML = `
        <div class="empty-state">
          <p>Stream "${escapeHtml(query)}" not found</p>
          <span class="hint">Check the stream ID and try again</span>
        </div>`;
    } else {
      list.innerHTML = `
        <div class="empty-state">
          <p>Error loading stream</p>
          <span class="hint">${escapeHtml(err.message || 'Unknown error')}</span>
        </div>`;
    }
  }
}

function renderStreamCard(container, state) {
  const types = [...new Set(state.events.map(e => e.eventType))];
  const chips = types.map(t => {
    const c = hashColor(t);
    return `<span class="event-type-chip event-color-${c}">${escapeHtml(t)}</span>`;
  }).join('');

  container.innerHTML = `
    <div class="stream-card" data-stream-id="${escapeHtml(state.streamId)}">
      <div class="stream-card-header">
        <span class="stream-card-id">${escapeHtml(state.streamId)}</span>
        <span class="stream-card-version">v${state.version} &middot; ${state.events.length} events</span>
      </div>
      <div class="stream-card-events">${chips}</div>
    </div>`;

  container.querySelector('.stream-card').addEventListener('click', () => {
    openStreamDetail(state.streamId);
  });
}

// ===== Stream Detail =====

async function openStreamDetail(streamId) {
  switchView('stream-detail');
  document.getElementById('detailStreamId').textContent = streamId;
  document.getElementById('detailMeta').innerHTML = '<div class="loading"><div class="spinner"></div></div>';
  document.getElementById('detailTimeline').innerHTML = '';

  try {
    const state = await apiFetch(`/streams/${encodeURIComponent(streamId)}`);
    document.getElementById('detailMeta').innerHTML = `
      <div class="meta-item">Version <span class="meta-value">${state.version}</span></div>
      <div class="meta-item">Events <span class="meta-value">${state.events.length}</span></div>`;

    const timeline = document.getElementById('detailTimeline');
    timeline.innerHTML = state.events.map(event => {
      const c = hashColor(event.eventType);
      const meta = Object.keys(event.metadata).length > 0
        ? `<div class="json-section">
            <div class="json-label">Metadata</div>
            <div class="json-block">${escapeHtml(jsonPretty(event.metadata))}</div>
          </div>`
        : '';

      return `
        <div class="timeline-event">
          <div class="timeline-dot dot-color-${c}"></div>
          <div class="timeline-card">
            <div class="timeline-card-header">
              <div class="timeline-card-header-left">
                <span class="event-type-badge event-color-${c}">${escapeHtml(event.eventType)}</span>
              </div>
              <div class="timeline-card-header-right">
                <span>v${event.streamVersion}</span>
                <span>#${event.globalOffset}</span>
                <span>${relativeTime(event.createdAt)}</span>
              </div>
            </div>
            <div class="timeline-card-body">
              <div class="json-section">
                <div class="json-label">Payload</div>
                <div class="json-block">${escapeHtml(jsonPretty(event.payload))}</div>
              </div>
              ${meta}
            </div>
          </div>
        </div>`;
    }).join('');
  } catch (err) {
    document.getElementById('detailTimeline').innerHTML = `
      <div class="empty-state">
        <p>Failed to load stream</p>
        <span class="hint">${escapeHtml(err.message || 'Unknown error')}</span>
      </div>`;
  }
}

// ===== Global Feed =====

document.getElementById('globalRefresh').addEventListener('click', loadGlobalFeed);
document.getElementById('globalLimit').addEventListener('change', loadGlobalFeed);

async function loadGlobalFeed() {
  const limit = document.getElementById('globalLimit').value;
  const body = document.getElementById('globalBody');
  body.innerHTML = '<tr><td colspan="6"><div class="loading"><div class="spinner"></div></div></td></tr>';

  try {
    const events = await apiFetch(`/streams?fromOffset=0&limit=${limit}`);
    if (events.length === 0) {
      body.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:40px;color:var(--text-3)">No events yet</td></tr>';
      return;
    }

    body.innerHTML = events.map(event => {
      const c = hashColor(event.eventType);
      return `
        <tr>
          <td><span class="offset-badge">#${event.globalOffset}</span></td>
          <td><a class="stream-link" data-stream="${escapeHtml(event.streamId)}">${escapeHtml(event.streamId)}</a></td>
          <td><span class="version-num">v${event.streamVersion}</span></td>
          <td><span class="event-type-badge event-color-${c}">${escapeHtml(event.eventType)}</span></td>
          <td><span class="timestamp" title="${escapeHtml(event.createdAt)}">${relativeTime(event.createdAt)}</span></td>
          <td><button class="detail-btn" data-event='${escapeHtml(JSON.stringify(event))}'>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M4 6l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button></td>
        </tr>`;
    }).join('');

    // Wire up stream links
    body.querySelectorAll('.stream-link').forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        lastBackView = 'global';
        openStreamDetail(link.dataset.stream);
      });
    });

    // Wire up detail buttons
    body.querySelectorAll('.detail-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const event = JSON.parse(btn.dataset.event);
        openEventModal(event);
      });
    });
  } catch (err) {
    body.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:40px;color:var(--red)">${escapeHtml(err.message || 'Failed to load')}</td></tr>`;
  }
}

// ===== Categories =====

const categorySearch = document.getElementById('categorySearch');
let catTimeout;

categorySearch.addEventListener('input', () => {
  clearTimeout(catTimeout);
  catTimeout = setTimeout(() => {
    const q = categorySearch.value.trim();
    if (q.length > 0) loadCategory(q);
  }, 300);
});

categorySearch.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    const q = categorySearch.value.trim();
    if (q) loadCategory(q);
  }
});

async function loadCategory(category) {
  const container = document.getElementById('categoryResults');
  container.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

  try {
    const events = await apiFetch(`/categories/${encodeURIComponent(category)}?fromOffset=0&limit=100`);
    if (events.length === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <p>No events in category "${escapeHtml(category)}"</p>
        </div>`;
      return;
    }

    // Group by stream
    const streams = {};
    events.forEach(e => {
      if (!streams[e.streamId]) streams[e.streamId] = [];
      streams[e.streamId].push(e);
    });

    container.innerHTML = Object.entries(streams).map(([streamId, evts]) => {
      const types = [...new Set(evts.map(e => e.eventType))];
      const chips = types.map(t => {
        const c = hashColor(t);
        return `<span class="event-type-chip event-color-${c}">${escapeHtml(t)}</span>`;
      }).join('');

      return `
        <div class="stream-card" data-stream-id="${escapeHtml(streamId)}">
          <div class="stream-card-header">
            <span class="stream-card-id">${escapeHtml(streamId)}</span>
            <span class="stream-card-version">${evts.length} events</span>
          </div>
          <div class="stream-card-events">${chips}</div>
        </div>`;
    }).join('');

    container.querySelectorAll('.stream-card').forEach(card => {
      card.addEventListener('click', () => {
        lastBackView = 'categories';
        openStreamDetail(card.dataset.streamId);
      });
    });
  } catch (err) {
    container.innerHTML = `
      <div class="empty-state">
        <p>Error loading category</p>
        <span class="hint">${escapeHtml(err.message || 'Unknown error')}</span>
      </div>`;
  }
}

// ===== Append Events =====

const appendForm = document.getElementById('appendForm');
const expectedVersionSelect = document.getElementById('appendExpectedVersion');
const exactVersionGroup = document.getElementById('exactVersionGroup');

expectedVersionSelect.addEventListener('change', () => {
  exactVersionGroup.style.display = expectedVersionSelect.value === 'exact' ? '' : 'none';
});

appendForm.addEventListener('submit', async (e) => {
  e.preventDefault();

  const streamId = document.getElementById('appendStreamId').value.trim();
  const evSelect = expectedVersionSelect.value;
  let expectedVersion = null;
  if (evSelect === '-1') expectedVersion = -1;
  else if (evSelect === 'exact') expectedVersion = parseInt(document.getElementById('appendExactVersion').value);

  let events;
  try {
    events = JSON.parse(document.getElementById('appendEventsJson').value);
    if (!Array.isArray(events)) events = [events];
  } catch (err) {
    showToast('Invalid JSON: ' + err.message, 'error');
    return;
  }

  const resultPanel = document.getElementById('appendResult');
  const resultStatus = document.getElementById('appendResultStatus');
  const resultBody = document.getElementById('appendResultBody');

  try {
    const result = await apiPost(`/streams/${encodeURIComponent(streamId)}/events`, {
      expectedVersion,
      events
    });

    resultPanel.style.display = '';
    resultStatus.className = 'result-status success';
    resultStatus.textContent = `${result.length} event(s) appended successfully`;
    resultBody.textContent = jsonPretty(result);
    showToast(`${result.length} event(s) appended to ${streamId}`, 'success');
  } catch (err) {
    resultPanel.style.display = '';
    resultStatus.className = 'result-status error';
    resultStatus.textContent = err.code || `Error ${err.status}`;
    resultBody.textContent = jsonPretty(err);
    showToast(err.message || 'Failed to append events', 'error');
  }
});

// ===== Event Modal =====

function openEventModal(event) {
  const c = hashColor(event.eventType);
  document.getElementById('modalTitle').innerHTML =
    `<span class="event-type-badge event-color-${c}" style="margin-right:8px">${escapeHtml(event.eventType)}</span> #${event.globalOffset}`;

  const meta = Object.keys(event.metadata).length > 0
    ? `<div class="json-section">
        <div class="json-label">Metadata</div>
        <div class="json-block">${escapeHtml(jsonPretty(event.metadata))}</div>
      </div>`
    : '';

  document.getElementById('modalBody').innerHTML = `
    <div style="display:flex;gap:16px;margin-bottom:16px;font-size:12px;color:var(--text-3);font-family:var(--font-mono)">
      <span>Stream: <a class="stream-link" style="cursor:pointer">${escapeHtml(event.streamId)}</a></span>
      <span>Version: ${event.streamVersion}</span>
      <span>Time: ${formatTime(event.createdAt)}</span>
    </div>
    <div class="json-section">
      <div class="json-label">Payload</div>
      <div class="json-block">${escapeHtml(jsonPretty(event.payload))}</div>
    </div>
    ${meta}`;

  document.getElementById('modalBody').querySelector('.stream-link')?.addEventListener('click', () => {
    closeModal();
    openStreamDetail(event.streamId);
  });

  document.getElementById('eventModal').classList.add('open');
}

function closeModal() {
  document.getElementById('eventModal').classList.remove('open');
}

document.getElementById('modalClose').addEventListener('click', closeModal);
document.getElementById('eventModal').addEventListener('click', (e) => {
  if (e.target === e.currentTarget) closeModal();
});
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') closeModal();
});

// ===== Utilities =====

function escapeHtml(str) {
  if (typeof str !== 'string') return str;
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

// ===== Init =====

checkHealth();
setInterval(checkHealth, 15000);
