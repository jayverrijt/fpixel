// ==========================================================================
// FPIXEL NETWORK - ENTERPRISE CLIENT APPLICATION (app.js)
// Real-time Web CMS, Player Worlds, Map Maker & Staff Console
// ==========================================================================

let currentUser = null;
let currentTab = 'home';
let currentStaffSubtab = 'reviews';

// Initialization
document.addEventListener('DOMContentLoaded', () => {
  initApp();
});

async function initApp() {
  await checkAuth();
  fetchServerStats();
  setInterval(fetchServerStats, 10000); // Poll status every 10s
}

// -------------------------------------------------------------
// TOAST NOTIFICATIONS & CLIPBOARD
// -------------------------------------------------------------
function showToast(message, type = 'info') {
  const toast = document.getElementById('toast');
  if (!toast) return;

  toast.textContent = message;
  toast.className = `toast show ${type}`;

  setTimeout(() => {
    toast.className = 'toast';
  }, 4000);
}

function copyIp(text, label = 'Server IP') {
  navigator.clipboard.writeText(text).then(() => {
    showToast(`Gekopieerd: ${text} (${label})`, 'success');
  }).catch(() => {
    showToast(`Adres: ${text}`, 'info');
  });
}

function copyText(text, label = 'Tekst') {
  navigator.clipboard.writeText(text).then(() => {
    showToast(`Gekopieerd: ${text}`, 'success');
  });
}

// -------------------------------------------------------------
// NAVIGATION & TAB SWITCHING
// -------------------------------------------------------------
function switchTab(tabId) {
  currentTab = tabId;

  // Update navbar button states
  document.querySelectorAll('.nav-link').forEach(btn => btn.classList.remove('active'));
  const activeBtn = document.getElementById(`nav-btn-${tabId}`);
  if (activeBtn) activeBtn.classList.add('active');

  // Update tab pane visibility
  document.querySelectorAll('.tab-pane').forEach(pane => pane.classList.remove('active'));
  const activePane = document.getElementById(`tab-${tabId}`);
  if (activePane) activePane.classList.add('active');

  // Trigger tab-specific data load
  if (tabId === 'worlds') {
    loadWorlds();
  } else if (tabId === 'maker') {
    loadMakerMaps();
  } else if (tabId === 'staff') {
    loadStaffData();
  }
}

function switchStaffTab(subtabId) {
  currentStaffSubtab = subtabId;

  document.querySelectorAll('.subnav-pill').forEach(btn => btn.classList.remove('active'));
  event.currentTarget.classList.add('active');

  document.querySelectorAll('.staff-subpane').forEach(p => p.style.display = 'none');
  const target = document.getElementById(`staff-section-${subtabId}`);
  if (target) target.style.display = 'block';

  if (subtabId === 'ranks') {
    loadRanks();
  }
}

function handleCtaWorlds() {
  if (currentUser && currentUser.loggedIn) {
    switchTab('worlds');
  } else {
    openLoginModal('login');
  }
}

function handleCtaMaker() {
  if (currentUser && currentUser.loggedIn) {
    switchTab('maker');
  } else {
    openLoginModal('login');
  }
}

// -------------------------------------------------------------
// MODALS
// -------------------------------------------------------------
function openModal(modalId) {
  const m = document.getElementById(modalId);
  if (m) m.style.display = 'flex';
}

function closeModal(modalId) {
  const m = document.getElementById(modalId);
  if (m) m.style.display = 'none';
}

function openLoginModal(mode = 'login') {
  switchAuthModalTab(mode);
  openModal('modal-auth');
}

function switchAuthModalTab(mode) {
  const loginBtn = document.getElementById('modal-tab-login-btn');
  const resetBtn = document.getElementById('modal-tab-reset-btn');
  const loginPane = document.getElementById('modal-pane-login');
  const resetPane = document.getElementById('modal-pane-reset');

  if (mode === 'login') {
    loginBtn.classList.add('active');
    resetBtn.classList.remove('active');
    loginPane.style.display = 'block';
    resetPane.style.display = 'none';
  } else {
    resetBtn.classList.add('active');
    loginBtn.classList.remove('active');
    loginPane.style.display = 'none';
    resetPane.style.display = 'block';
    document.getElementById('reset-step-1').style.display = 'block';
    document.getElementById('reset-step-2').style.display = 'none';
  }
}

// -------------------------------------------------------------
// SERVER STATS & LIVE COUNTER
// -------------------------------------------------------------
async function fetchServerStats() {
  try {
    const res = await fetch('/api/stats');
    if (!res.ok) return;
    const data = await res.json();

    const heroCount = document.getElementById('hero-player-count');
    if (heroCount) heroCount.textContent = data.players?.online || 0;

    const statusPill = document.getElementById('server-status-pill');
    const statusText = document.getElementById('status-text');
    if (data.online) {
      statusPill.className = 'status-indicator online';
      statusText.textContent = 'ONLINE';
    } else {
      statusPill.className = 'status-indicator offline';
      statusText.textContent = 'OFFLINE';
    }
  } catch (e) {
    console.warn('Failed to fetch server stats:', e);
  }
}

// -------------------------------------------------------------
// AUTHENTICATION & PROFILE
// -------------------------------------------------------------
async function checkAuth() {
  try {
    const res = await fetch('/api/me');
    const data = await res.json();

    if (data.loggedIn && data.user) {
      currentUser = { loggedIn: true, ...data.user };
      renderAuthUser(currentUser);
    } else {
      currentUser = { loggedIn: false };
      renderLoggedOut();
    }
  } catch (e) {
    currentUser = { loggedIn: false };
    renderLoggedOut();
  }
}

function renderAuthUser(user) {
  document.getElementById('auth-logged-out').style.display = 'none';
  document.getElementById('auth-logged-in').style.display = 'flex';

  document.querySelectorAll('.auth-required-tab').forEach(el => el.style.display = 'inline-flex');

  // Staff Console link
  const staffBtn = document.getElementById('nav-btn-staff');
  if (user.isStaff) {
    staffBtn.style.display = 'inline-flex';
  } else {
    staffBtn.style.display = 'none';
  }

  // Update Navbar info
  document.getElementById('nav-username').textContent = user.username;
  document.getElementById('nav-user-coins').textContent = `🪙 ${user.coins || 0}`;

  const cleanName = user.username.replace(/^\./, '');
  document.getElementById('nav-user-avatar').src = `https://mc-heads.net/avatar/${cleanName}/64`;

  const rankBadge = document.getElementById('nav-user-rank');
  rankBadge.textContent = user.rank;
  rankBadge.className = `rank-badge rank-${(user.rank || 'member').toLowerCase()}`;

  // Server Stop button strictly for OWNER
  const stopBtn = document.getElementById('btn-server-stop');
  if (stopBtn) {
    stopBtn.disabled = (user.rank !== 'OWNER');
  }

  // Owner rank form card
  const ownerCard = document.getElementById('owner-rank-form-card');
  if (ownerCard) {
    ownerCard.style.display = (user.rank === 'OWNER') ? 'block' : 'none';
  }
}

function renderLoggedOut() {
  document.getElementById('auth-logged-out').style.display = 'flex';
  document.getElementById('auth-logged-in').style.display = 'none';
  document.querySelectorAll('.auth-required-tab').forEach(el => el.style.display = 'none');
  document.getElementById('nav-btn-staff').style.display = 'none';

  if (currentTab !== 'home') {
    switchTab('home');
  }
}

async function submitLogin() {
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;

  if (!username || !password) {
    showToast('Vul je gebruikersnaam en wachtwoord in.', 'error');
    return;
  }

  try {
    const res = await fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(`Welkom terug, ${data.user.username}!`, 'success');
      closeModal('modal-auth');
      await checkAuth();
      switchTab('worlds');
    } else {
      showToast(data.error || 'Inloggen mislukt.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout bij inloggen.', 'error');
  }
}

async function logout() {
  try {
    await fetch('/api/logout', { method: 'POST' });
    showToast('Je bent uitgelogd.', 'info');
    await checkAuth();
  } catch (e) {
    location.reload();
  }
}

// In-Game Chat Password Reset
async function requestResetCode() {
  const username = document.getElementById('reset-username').value.trim();
  if (!username) {
    showToast('Vul je gebruikersnaam in.', 'error');
    return;
  }

  try {
    const res = await fetch('/api/auth/reset-request', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message, 'success');
      document.getElementById('reset-step-1').style.display = 'none';
      document.getElementById('reset-step-2').style.display = 'block';
    } else {
      showToast(data.error || 'Kon reset code niet verzenden.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout bij aanvragen van code.', 'error');
  }
}

async function confirmResetCode() {
  const username = document.getElementById('reset-username').value.trim();
  const code = document.getElementById('reset-code').value.trim();
  const newPassword = document.getElementById('reset-new-password').value;

  if (!username || !code || !newPassword) {
    showToast('Vul alle velden in.', 'error');
    return;
  }

  try {
    const res = await fetch('/api/auth/reset-confirm', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, code, newPassword })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message, 'success');
      closeModal('modal-auth');
      await checkAuth();
    } else {
      showToast(data.error || 'Ongeldige code of fout bij resetten.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout bij bevestigen.', 'error');
  }
}

// -------------------------------------------------------------
// CUSTOM PLAYER WORLDS ("REALMS")
// -------------------------------------------------------------
async function loadWorlds() {
  const loading = document.getElementById('worlds-loading');
  const empty = document.getElementById('worlds-empty');
  const grid = document.getElementById('worlds-grid');

  loading.style.display = 'block';
  empty.style.display = 'none';
  grid.style.display = 'none';

  try {
    const res = await fetch('/api/worlds');
    if (!res.ok) throw new Error();
    const data = await res.json();

    loading.style.display = 'none';

    // Update limit indicator
    const limitCount = document.getElementById('world-limit-count');
    if (limitCount && data.limits) {
      limitCount.textContent = `${data.limits.owned} / ${data.limits.max}`;
    }

    const worlds = data.worlds || [];
    if (worlds.length === 0) {
      empty.style.display = 'block';
      return;
    }

    grid.innerHTML = '';
    worlds.forEach(w => {
      grid.appendChild(createWorldCard(w));
    });
    grid.style.display = 'grid';
  } catch (e) {
    loading.style.display = 'none';
    showToast('Kon werelden niet inladen.', 'error');
  }
}

function createWorldCard(w) {
  const card = document.createElement('div');
  card.className = 'world-item-card';

  const isOwner = w.isCreator || (currentUser && currentUser.isOwner);

  card.innerHTML = `
    <div class="item-card-header">
      <div>
        <span class="item-uid-tag">${w.uid}</span>
        <h3 class="item-card-title">${escapeHtml(w.name)}</h3>
      </div>
      <span class="rank-badge ${w.isPrimarySmp ? 'rank-owner' : 'rank-member'}">
        ${w.isPrimarySmp ? 'SMP HOOFDWERELD' : 'REALM'}
      </span>
    </div>

    <div class="item-meta-row">
      <span class="meta-pill">👑 Maker: <strong>${escapeHtml(w.creatorName || 'Server')}</strong></span>
      <span class="meta-pill">🎮 Modus: <strong>${escapeHtml(w.defaultGameMode || 'SURVIVAL')}</strong></span>
      <span class="meta-pill ${w.cheatsEnabled ? 'meta-cheats-on' : 'meta-cheats-off'}">
        ⚡ Cheats: ${w.cheatsEnabled ? 'AAN' : 'UIT'}
      </span>
      <span class="meta-pill">🌍 Overworld, Nether & End</span>
    </div>

    <div class="item-join-box" onclick="copyText('/joinworld ${w.uid}', 'In-game commando')">
      <span class="join-cmd-text">/joinworld ${w.uid}</span>
      <button class="btn-copy-tag">Kopieer 📋</button>
    </div>

    <div class="item-actions-row">
      ${isOwner ? `
        <button class="btn btn-sm btn-secondary" onclick="openShareModal('${w.uid}', '${escapeHtml(w.name)}')">
          👥 Vrienden Delen
        </button>
        <button class="btn btn-sm btn-purple" onclick="openBackupsModal('${w.uid}', '${escapeHtml(w.name)}')">
          💾 Git Backups & Restore
        </button>
        ${!w.isPrimarySmp ? `
          <button class="btn btn-sm btn-danger" onclick="deleteWorld('${w.uid}')">
            🗑️
          </button>
        ` : ''}
      ` : `
        <span style="font-size: 0.8rem; color: #94a3b8;">Gedeeld met jou</span>
      `}
    </div>
  `;

  return card;
}

function openCreateWorldModal() {
  document.getElementById('create-world-name').value = '';
  document.getElementById('create-world-seed').value = '';
  const gmEl = document.getElementById('create-world-gamemode');
  if (gmEl) gmEl.value = 'SURVIVAL';
  document.getElementById('create-world-cheats').checked = false;
  openModal('modal-create-world');
}

async function submitCreateWorld() {
  const name = document.getElementById('create-world-name').value.trim();
  const seed = document.getElementById('create-world-seed').value.trim();
  const gmEl = document.getElementById('create-world-gamemode');
  const gamemode = gmEl ? gmEl.value : 'SURVIVAL';
  const cheats = document.getElementById('create-world-cheats').checked;

  if (!name) {
    showToast('Vul een wereldnaam in.', 'error');
    return;
  }

  showToast('Wereld wordt aangemaakt en geladen...', 'info');
  try {
    const res = await fetch('/api/worlds/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, seed, cheats, gamemode })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message, 'success');
      closeModal('modal-create-world');
      loadWorlds();
    } else {
      showToast(data.error || 'Kon wereld niet aanmaken.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout bij wereld creatie.', 'error');
  }
}

// Sharing & Permissions
function openShareModal(uid, name) {
  document.getElementById('share-world-uid').value = uid;
  document.getElementById('share-modal-title').textContent = `Vrienden Delen: ${name}`;
  document.getElementById('share-target-player').value = '';
  document.getElementById('share-allow-cheats').checked = false;

  loadShareList(uid);
  openModal('modal-share-world');
}

async function loadShareList(uid) {
  const container = document.getElementById('share-list');
  container.innerHTML = '<span style="font-size:0.85rem; color:#94a3b8;">Laden...</span>';

  try {
    const res = await fetch('/api/worlds');
    const data = await res.json();
    const w = (data.worlds || []).find(item => item.uid === uid);

    if (!w || !w.sharedPlayers || Object.keys(w.sharedPlayers).length === 0) {
      container.innerHTML = '<span style="font-size:0.85rem; color:#64748b;">Nog geen vrienden uitgenodigd.</span>';
      return;
    }

    container.innerHTML = '';
    for (const [pName, canCheat] of Object.entries(w.sharedPlayers)) {
      const row = document.createElement('div');
      row.className = 'share-item-row';
      row.style.cssText = 'display:flex; justify-content:space-between; align-items:center; padding:6px 0; border-bottom:1px solid rgba(255,255,255,0.05);';
      row.innerHTML = `
        <span style="font-weight:600;">${escapeHtml(pName)} ${canCheat ? '<span style="color:#10b981; font-size:0.75rem;">(Cheats AAN)</span>' : ''}</span>
        <button class="btn btn-sm btn-danger" onclick="unsharePlayer('${uid}', '${pName}')">Verwijder</button>
      `;
      container.appendChild(row);
    }
  } catch (e) {
    container.innerHTML = '<span style="color:#ef4444;">Fout bij ophalen.</span>';
  }
}

async function submitShareWorld() {
  const uid = document.getElementById('share-world-uid').value;
  const targetPlayer = document.getElementById('share-target-player').value.trim();
  const allowCheats = document.getElementById('share-allow-cheats').checked;

  if (!targetPlayer) {
    showToast('Vul een spelernaam in.', 'error');
    return;
  }

  try {
    const res = await fetch('/api/worlds/share', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ uid, targetPlayer, allowCheats })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message, 'success');
      document.getElementById('share-target-player').value = '';
      loadShareList(uid);
    } else {
      showToast(data.error || 'Kon speler niet toevoegen.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout.', 'error');
  }
}

async function unsharePlayer(uid, targetPlayer) {
  try {
    const res = await fetch('/api/worlds/unshare', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ uid, targetPlayer })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(data.message, 'success');
      loadShareList(uid);
    } else {
      showToast(data.error, 'error');
    }
  } catch (e) {
    showToast('Netwerkfout.', 'error');
  }
}

// Git Backups & Hot Restore
function openBackupsModal(uid, name) {
  document.getElementById('backups-world-uid').value = uid;
  document.getElementById('backups-modal-title').textContent = `Zero-Downtime Backups: ${name}`;
  loadBackups(uid);
  openModal('modal-world-backups');
}

async function loadBackups(uid) {
  const loading = document.getElementById('backups-loading');
  const tableWrap = document.getElementById('backups-list');
  const tbody = document.getElementById('backups-table-body');

  loading.style.display = 'block';
  tableWrap.style.display = 'none';

  try {
    const res = await fetch(`/api/worlds/backups/${uid}`);
    const data = await res.json();

    loading.style.display = 'none';
    tbody.innerHTML = '';

    const backups = data.backups || [];
    if (backups.length === 0) {
      tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:#64748b;">Nog geen eerdere git snapshots vastgelegd.</td></tr>';
    } else {
      backups.forEach(b => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td><code style="color:#38bdf8;">${b.hash}</code></td>
          <td>${b.date || '-'}</td>
          <td>${escapeHtml(b.message || '')}</td>
          <td>
            <button class="btn btn-sm btn-purple" onclick="restoreBackup('${uid}', '${b.hash}')">
              ⚡ Hot Restore
            </button>
          </td>
        `;
        tbody.appendChild(tr);
      });
    }

    tableWrap.style.display = 'block';
  } catch (e) {
    loading.style.display = 'none';
    showToast('Kon backups niet laden.', 'error');
  }
}

async function triggerWorldBackupFromModal() {
  const uid = document.getElementById('backups-world-uid').value;
  showToast('Git backup snapshot wordt aangemaakt...', 'info');

  try {
    const res = await fetch('/api/worlds/backup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ uid })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message, 'success');
      setTimeout(() => loadBackups(uid), 1500);
    } else {
      showToast(data.error || 'Backup mislukt.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout bij maken backup.', 'error');
  }
}

async function restoreBackup(uid, commitHash) {
  if (!confirm(`Weet je zeker dat je deze wereld wilt herstellen naar commit ${commitHash}? Actieve spelers worden veilig naar de Hub geteleporteerd en de wereld herlaadt zonder server downtime.`)) {
    return;
  }

  showToast('Hot-restore gestart... Wereld wordt hersteld.', 'info');
  try {
    const res = await fetch('/api/worlds/restore', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ uid, commitHash })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message, 'success');
      closeModal('modal-world-backups');
    } else {
      showToast(data.error || 'Restore mislukt.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout bij restore.', 'error');
  }
}

async function deleteWorld(uid) {
  if (!confirm(`Weet je zeker dat je wereld ${uid} wilt verwijderen? Deze actie is onomkeerbaar!`)) {
    return;
  }

  try {
    const res = await fetch('/api/worlds/delete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ uid })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message, 'success');
      loadWorlds();
    } else {
      showToast(data.error || 'Kon wereld niet verwijderen.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout.', 'error');
  }
}

// -------------------------------------------------------------
// MAP MAKER
// -------------------------------------------------------------
async function loadMakerMaps() {
  const loading = document.getElementById('maker-loading');
  const empty = document.getElementById('maker-empty');
  const grid = document.getElementById('maker-grid');

  loading.style.display = 'block';
  empty.style.display = 'none';
  grid.style.display = 'none';

  try {
    const res = await fetch('/api/maker/maps');
    const data = await res.json();

    loading.style.display = 'none';

    const maps = data.maps || [];
    if (maps.length === 0) {
      empty.style.display = 'block';
      return;
    }

    grid.innerHTML = '';
    maps.forEach(m => {
      grid.appendChild(createMapCard(m));
    });
    grid.style.display = 'grid';
  } catch (e) {
    loading.style.display = 'none';
    showToast('Kon maps niet laden.', 'error');
  }
}

function createMapCard(m) {
  const card = document.createElement('div');
  card.className = 'maker-item-card';

  const statusColors = {
    'DRAFT': 'rank-default',
    'PENDING_REVIEW': 'rank-donor',
    'APPROVED': 'rank-moderator',
    'REJECTED': 'rank-admin',
    'ARCHIVED': 'rank-member'
  };

  card.innerHTML = `
    <div class="item-card-header">
      <div>
        <span class="item-uid-tag">${m.id ? m.id.toUpperCase() : 'MAP'}</span>
        <h3 class="item-card-title">${escapeHtml(m.name)}</h3>
      </div>
      <span class="rank-badge ${statusColors[m.status] || 'rank-default'}">
        ${m.status}
      </span>
    </div>

    <div class="item-meta-row">
      <span class="meta-pill">🎮 Modus: <strong>${m.gameType || 'PARKOUR'}</strong></span>
      <span class="meta-pill">👥 Teams: <strong>${m.teamCount || 2}</strong></span>
    </div>

    ${m.rejectionReason ? `
      <div class="alert alert-info" style="margin-bottom: 12px; font-size: 0.8rem; background: rgba(239, 68, 68, 0.15); border-color: #ef4444; color: #fca5a5;">
        Reden afkeuring: ${escapeHtml(m.rejectionReason)}
      </div>
    ` : ''}

    <div class="item-join-box" onclick="copyText('/join ${m.worldName}', 'Bouwwereld commando')">
      <span class="join-cmd-text">/join ${m.worldName}</span>
      <button class="btn-copy-tag">Bouwen 🔨</button>
    </div>

    <div class="item-actions-row">
      ${m.status === 'DRAFT' || m.status === 'REJECTED' ? `
        <button class="btn btn-sm btn-purple" onclick="submitMapForReview('${m.id}')">
          🚀 Indienen voor Staff Review
        </button>
      ` : `
        <span style="font-size:0.8rem; color:#94a3b8;">
          ${m.status === 'APPROVED' ? '✅ Goedgekeurd & Actief in minigames' : '⏳ In beoordeling bij staff'}
        </span>
      `}
    </div>
  `;

  return card;
}

function openCreateMapModal() {
  document.getElementById('create-map-name').value = '';
  document.getElementById('create-map-type').value = 'BEDWARS';
  document.getElementById('create-map-teams').value = '4';
  openModal('modal-create-map');
}

async function submitCreateMap() {
  const name = document.getElementById('create-map-name').value.trim();
  const gameType = document.getElementById('create-map-type').value;
  const teamCount = document.getElementById('create-map-teams').value;

  if (!name) {
    showToast('Vul een mapnaam in.', 'error');
    return;
  }

  showToast('Bouwwereld en eilanden worden gegenereerd...', 'info');
  try {
    const res = await fetch('/api/maker/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, gameType, teamCount })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message, 'success');
      closeModal('modal-create-map');
      loadMakerMaps();
    } else {
      showToast(data.error || 'Kon map niet starten.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout bij aanmaken map.', 'error');
  }
}

async function submitMapForReview(id) {
  try {
    const res = await fetch('/api/maker/submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message, 'success');
      loadMakerMaps();
    } else {
      showToast(data.error || 'Fout bij indienen.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout.', 'error');
  }
}

// -------------------------------------------------------------
// UNIFIED STAFF CONSOLE
// -------------------------------------------------------------
async function loadStaffData() {
  loadReviewQueue();
}

async function loadReviewQueue() {
  const empty = document.getElementById('staff-review-empty');
  const grid = document.getElementById('staff-review-grid');
  const badge = document.getElementById('staff-review-badge');
  const subnavBadge = document.getElementById('staff-subnav-review-count');

  try {
    const res = await fetch('/api/maker/maps');
    const data = await res.json();
    const queue = data.reviewQueue || [];

    if (badge) {
      badge.textContent = queue.length;
      badge.style.display = queue.length > 0 ? 'inline-block' : 'none';
    }
    if (subnavBadge) subnavBadge.textContent = queue.length;

    if (queue.length === 0) {
      empty.style.display = 'block';
      grid.innerHTML = '';
      return;
    }

    empty.style.display = 'none';
    grid.innerHTML = '';

    queue.forEach(m => {
      const card = document.createElement('div');
      card.className = 'review-item-card';

      card.innerHTML = `
        <div class="item-card-header">
          <div>
            <span class="item-uid-tag">${m.id ? m.id.toUpperCase() : 'MAP'}</span>
            <h3 class="item-card-title">${escapeHtml(m.name)}</h3>
          </div>
          <span class="rank-badge rank-donor">IN AFWACHTING</span>
        </div>

        <div class="item-meta-row">
          <span class="meta-pill">👑 Maker: <strong>${escapeHtml(m.creatorName || 'Speler')}</strong></span>
          <span class="meta-pill">🎮 Spelmodus: <strong>${m.gameType || 'PARKOUR'}</strong></span>
        </div>

        <div class="item-join-box" onclick="copyText('/fpixel map test ${m.id}', 'Staff inspectie commando')">
          <span class="join-cmd-text">/fpixel map test ${m.id}</span>
          <button class="btn-copy-tag">Testen 🔍</button>
        </div>

        <div class="item-actions-row">
          <button class="btn btn-sm btn-purple" onclick="staffReviewMap('${m.id}', 'approve')">
            ✅ Goedkeuren & Live Zetten
          </button>
          <button class="btn btn-sm btn-danger" onclick="openRejectModal('${m.id}')">
            ❌ Afkeuren
          </button>
        </div>
      `;

      grid.appendChild(card);
    });
  } catch (e) {
    showToast('Kon review wachtrij niet laden.', 'error');
  }
}

async function staffReviewMap(id, action, reason = '') {
  try {
    const res = await fetch('/api/maker/review', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id, action, reason })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message, 'success');
      loadReviewQueue();
    } else {
      showToast(data.error || 'Kon actie niet uitvoeren.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout bij review actie.', 'error');
  }
}

function openRejectModal(id) {
  document.getElementById('reject-map-id').value = id;
  document.getElementById('reject-map-reason').value = '';
  openModal('modal-reject-map');
}

function submitRejectMap() {
  const id = document.getElementById('reject-map-id').value;
  const reason = document.getElementById('reject-map-reason').value.trim();
  closeModal('modal-reject-map');
  staffReviewMap(id, 'reject', reason);
}

// Ranks Management
async function loadRanks() {
  const tbody = document.getElementById('ranks-table-body');
  tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:#94a3b8;">Spelers ophalen...</td></tr>';

  try {
    const res = await fetch('/api/admin/ranks');
    const data = await res.json();

    const isOwner = currentUser && currentUser.rank === 'OWNER';
    tbody.innerHTML = '';

    const players = data.players || [];
    players.forEach(p => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>
          <div style="display:flex; align-items:center; gap:8px;">
            <img src="https://mc-heads.net/avatar/${p.cleanName}/24" style="width:24px; height:24px; border-radius:4px;">
            <strong>${escapeHtml(p.username)}</strong>
          </div>
        </td>
        <td>
          <span class="rank-badge rank-${p.rank.toLowerCase()}">${p.rank}</span>
        </td>
        <td>${getRankWeight(p.rank)}</td>
        <td>
          ${isOwner ? `
            <select onchange="updatePlayerRank('${p.username}', this.value)" style="padding:4px 8px; border-radius:4px; background:#0c0f14; color:#fff; border:1px solid rgba(255,255,255,0.1);">
              <option value="MEMBER" ${p.rank === 'MEMBER' ? 'selected' : ''}>MEMBER</option>
              <option value="DONOR" ${p.rank === 'DONOR' ? 'selected' : ''}>DONOR [❤]</option>
              <option value="MODERATOR" ${p.rank === 'MODERATOR' ? 'selected' : ''}>MODERATOR [🎩]</option>
              <option value="ADMIN" ${p.rank === 'ADMIN' ? 'selected' : ''}>ADMIN [⚔]</option>
              <option value="OWNER" ${p.rank === 'OWNER' ? 'selected' : ''}>OWNER [👑]</option>
            </select>
          ` : `
            <span style="color:#64748b; font-size:0.8rem;">Alleen [OWNER] kan wijzigen</span>
          `}
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="4" style="color:#ef4444; text-align:center;">Kon rangen niet ophalen.</td></tr>';
  }
}

function getRankWeight(r) {
  switch (r) {
    case 'OWNER': return 100;
    case 'ADMIN': return 90;
    case 'MODERATOR': return 80;
    case 'DONOR': return 50;
    case 'MEMBER': default: return 10;
  }
}

async function updatePlayerRank(username, newRank) {
  try {
    const res = await fetch('/api/admin/ranks/set', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, rank: newRank })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(data.message, 'success');
      loadRanks();
    } else {
      showToast(data.error || 'Kon rang niet aanpassen.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout.', 'error');
  }
}

async function assignRank() {
  const username = document.getElementById('assign-rank-username').value.trim();
  const rank = document.getElementById('assign-rank-select').value;

  if (!username) {
    showToast('Vul een gebruikersnaam in.', 'error');
    return;
  }

  await updatePlayerRank(username, rank);
  document.getElementById('assign-rank-username').value = '';
}

// Server Operations
async function sendBroadcast() {
  const input = document.getElementById('broadcast-message-input');
  const message = input.value.trim();

  if (!message) {
    showToast('Vul een bericht in.', 'error');
    return;
  }

  try {
    const res = await fetch('/api/server/broadcast', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast('Omroepbericht verzonden in-game!', 'success');
      input.value = '';
    } else {
      showToast(data.error || 'Kon bericht niet verzenden.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout.', 'error');
  }
}

async function triggerServerBackup() {
  if (!confirm('Weet je zeker dat je een volledige server backup wilt starten?')) return;

  showToast('Server backup wordt gestart...', 'info');
  try {
    const res = await fetch('/api/server/backup', { method: 'POST' });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message, 'success');
    } else {
      showToast(data.error || 'Backup mislukt.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout.', 'error');
  }
}

async function triggerServerStop() {
  if (!confirm('LET OP: Weet je 100% zeker dat je de Minecraft server veilig wilt stoppen?')) return;

  try {
    const res = await fetch('/api/server/stop', { method: 'POST' });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message, 'success');
    } else {
      showToast(data.error || 'Kon server niet stoppen.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout.', 'error');
  }
}

// Utility: HTML escape
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
