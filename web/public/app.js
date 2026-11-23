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
  } else if (subtabId === 'watchdog') {
    loadWatchdogData();
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
    if (data.online || (data.server && data.server.online)) {
      statusPill.className = 'status-indicator online';
      statusText.textContent = 'ONLINE';
    } else {
      statusPill.className = 'status-indicator offline';
      statusText.textContent = 'OFFLINE';
    }

    // Dynamic Bedrock Status & Status Codes (100 = Outdated, 200 = Good to Join, 503 = Offline)
    const bedrockPill = document.getElementById('bedrock-status-pill');
    const bedrockText = document.getElementById('bedrock-status-text');
    if (bedrockPill && bedrockText && data.bedrock) {
      const code = data.bedrock.code;
      if (code === 200) {
        bedrockPill.className = 'bedrock-code-badge badge-code-200';
        bedrockText.textContent = 'Code 200: Spel is goed & joinbaar';
      } else if (code === 100) {
        bedrockPill.className = 'bedrock-code-badge badge-code-100';
        bedrockText.textContent = 'Code 100: Outdated (Update vereist)';
      } else {
        bedrockPill.className = 'bedrock-code-badge badge-code-503';
        bedrockText.textContent = 'Code 503: Bedrock Offline';
      }
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

  // Owner Watchdog override card
  const ownerWdCard = document.getElementById('owner-watchdog-override-card');
  if (ownerWdCard) {
    ownerWdCard.style.display = (user.rank === 'OWNER') ? 'block' : 'none';
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
      <span class="meta-pill">🏝️ Type: <strong>${escapeHtml(w.generatorType || 'NORMAL')}</strong></span>
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
  const genEl = document.getElementById('create-world-generator');
  const generatorType = genEl ? genEl.value : 'NORMAL';
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
      body: JSON.stringify({ name, seed, cheats, gamemode, generatorType })
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

    <div class="item-join-box" onclick="copyText('/mapbuild ${m.id}', 'Bouwwereld commando')">
      <span class="join-cmd-text">/mapbuild ${m.id}</span>
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
let currentStaffMapFilter = 'PENDING_REVIEW';

function setStaffMapFilter(filter) {
  currentStaffMapFilter = filter;
  const filterMap = {
    'PENDING_REVIEW': 'pending',
    'APPROVED': 'approved',
    'ARCHIVED': 'archived'
  };
  ['pending', 'approved', 'archived'].forEach(key => {
    const btn = document.getElementById('map-filter-' + key);
    if (!btn) return;
    if (filterMap[filter] === key) {
      btn.className = 'btn btn-sm btn-purple';
    } else {
      btn.className = 'btn btn-sm btn-ghost';
    }
  });
  loadReviewQueue();
}

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
    const allMaps = data.maps || [];
    const queue = data.reviewQueue || [];

    if (badge) {
      badge.textContent = queue.length;
      badge.style.display = queue.length > 0 ? 'inline-block' : 'none';
    }
    if (subnavBadge) subnavBadge.textContent = queue.length;

    let displayList = [];
    if (currentStaffMapFilter === 'PENDING_REVIEW') {
      displayList = allMaps.filter(m => m.status === 'PENDING_REVIEW');
    } else if (currentStaffMapFilter === 'APPROVED') {
      displayList = allMaps.filter(m => m.status === 'APPROVED');
    } else if (currentStaffMapFilter === 'ARCHIVED') {
      displayList = allMaps.filter(m => m.status === 'ARCHIVED' || m.status === 'DISABLED' || m.status === 'REJECTED');
    } else {
      displayList = allMaps;
    }

    if (displayList.length === 0) {
      empty.style.display = 'block';
      grid.innerHTML = '';
      return;
    }

    empty.style.display = 'none';
    grid.innerHTML = '';

    displayList.forEach(m => {
      const card = document.createElement('div');
      card.className = 'review-item-card';

      let statusBadge = '<span class="rank-badge rank-donor">IN AFWACHTING</span>';
      if (m.status === 'APPROVED') {
        statusBadge = '<span class="rank-badge rank-owner">ACTIEF IN GAMES</span>';
      } else if (m.status === 'ARCHIVED' || m.status === 'DISABLED') {
        statusBadge = '<span class="rank-badge" style="background: rgba(148, 163, 184, 0.2); color: #94a3b8; border: 1px solid rgba(148, 163, 184, 0.3);">UIT CIRCULATIE</span>';
      } else if (m.status === 'REJECTED') {
        statusBadge = '<span class="rank-badge rank-admin">AFGEKEURD</span>';
      }

      let actionButtons = '';
      if (m.status === 'PENDING_REVIEW') {
        actionButtons = `
          <button class="btn btn-sm btn-purple" onclick="staffReviewMap('${m.id}', 'approve')">
            ✅ Goedkeuren & Live
          </button>
          <button class="btn btn-sm btn-ghost" onclick="openRenameModal('${m.id}', '${escapeHtml(m.name)}')">
            ✏️ Hernoemen
          </button>
          <button class="btn btn-sm btn-danger" onclick="openRejectModal('${m.id}')">
            ❌ Afkeuren
          </button>
        `;
      } else if (m.status === 'APPROVED') {
        actionButtons = `
          <button class="btn btn-sm btn-ghost" onclick="openRenameModal('${m.id}', '${escapeHtml(m.name)}')">
            ✏️ Hernoemen
          </button>
          <button class="btn btn-sm btn-danger" onclick="staffReviewMap('${m.id}', 'disable')">
            📦 Uit Circulatie Halen
          </button>
        `;
      } else {
        actionButtons = `
          <button class="btn btn-sm btn-purple" onclick="staffReviewMap('${m.id}', 'enable')">
            🚀 Weer In Circulatie Brengen
          </button>
          <button class="btn btn-sm btn-ghost" onclick="openRenameModal('${m.id}', '${escapeHtml(m.name)}')">
            ✏️ Hernoemen
          </button>
        `;
      }

      card.innerHTML = `
        <div class="item-card-header">
          <div>
            <span class="item-uid-tag">${m.id ? m.id.toUpperCase() : 'MAP'}</span>
            <h3 class="item-card-title">${escapeHtml(m.name)}</h3>
          </div>
          ${statusBadge}
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
          ${actionButtons}
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

function openRenameModal(id, currentName) {
  document.getElementById('rename-map-id').value = id;
  document.getElementById('rename-map-newname').value = currentName || '';
  openModal('modal-rename-map');
}

async function submitRenameMap() {
  const id = document.getElementById('rename-map-id').value;
  const newName = document.getElementById('rename-map-newname').value.trim();
  if (!newName) {
    showToast('Vul een geldige naam in.', 'error');
    return;
  }
  closeModal('modal-rename-map');
  try {
    const res = await fetch('/api/maker/rename', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id, newName })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(data.message, 'success');
      loadReviewQueue();
    } else {
      showToast(data.error || 'Kon map niet hernoemen.', 'error');
    }
  } catch (e) {
    showToast('Netwerkfout bij hernoemen van map.', 'error');
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

// -------------------------------------------------------------
// WATCHDOG ANTICHEAT & INTEGRITY
// -------------------------------------------------------------
async function loadWatchdogData() {
  const countEl = document.getElementById('watchdog-violation-count');
  const tbody = document.getElementById('watchdog-table-body');
  if (!tbody) return;

  try {
    const res = await fetch('/api/admin/watchdog');
    const data = await res.json();
    if (!data.success) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: #ef4444;">Fout bij laden: ${data.error || 'Geen toegang'}</td></tr>`;
      return;
    }

    const violations = data.violations || [];
    if (countEl) countEl.textContent = violations.length;

    if (violations.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: #64748b; padding: 20px;">Geen recente overtredingen. Server is 100% veilig.</td></tr>`;
      return;
    }

    tbody.innerHTML = violations.map(v => {
      const scoreColor = v.score >= 90 ? '#10b981' : (v.score >= 50 ? '#eab308' : '#ef4444');
      const cleanPlayer = (v.player || 'Onbekend').replace(/^\./, '');
      return `
        <tr>
          <td style="color: #94a3b8; font-size: 0.85rem;">${v.timestamp || '-'}</td>
          <td style="display: flex; align-items: center; gap: 8px;">
            <img src="https://mc-heads.net/avatar/${cleanPlayer}/24" style="width: 24px; height: 24px; border-radius: 4px;" alt="${cleanPlayer}">
            <strong>${escapeHtml(v.player)}</strong>
          </td>
          <td><span style="font-family: monospace; background: rgba(255,255,255,0.06); padding: 2px 6px; border-radius: 4px; color: #38bdf8;">${escapeHtml(v.brand || 'Unknown')}</span></td>
          <td style="font-weight: bold; color: ${scoreColor};">${v.score} / 100</td>
          <td style="color: #fca5a5;">${escapeHtml(v.reason || 'Overtreding')}</td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: #ef4444;">Netwerkfout bij laden van Watchdog logs: ${err.message}</td></tr>`;
  }
}

async function setWatchdogScore() {
  const player = document.getElementById('wd-override-player').value.trim();
  const score = document.getElementById('wd-override-score').value;

  if (!player || score === '') {
    showToast('Vul een spelersnaam en score in.', 'error');
    return;
  }

  try {
    const res = await fetch('/api/admin/watchdog/setscore', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ player, score: parseInt(score, 10) })
    });
    const data = await res.json();
    if (data.success) {
      showToast(data.message || 'Score succesvol bijgewerkt!', 'success');
      document.getElementById('wd-override-player').value = '';
      document.getElementById('wd-override-score').value = '';
      loadWatchdogData();
    } else {
      showToast(data.error || 'Kon score niet bijwerken.', 'error');
    }
  } catch (err) {
    showToast('Fout bij verzenden van commando: ' + err.message, 'error');
  }
}

// -------------------------------------------------------------
// BEDROCK ERROR & HTTP CODES MODAL
// -------------------------------------------------------------
let cachedBedrockErrors = [];

async function openBedrockErrorsModal() {
  const modal = document.getElementById('modal-bedrock-errors');
  if (!modal) return;
  modal.style.display = 'flex';

  if (cachedBedrockErrors.length === 0) {
    try {
      const res = await fetch('/api/bedrock/errors');
      const data = await res.json();
      if (data.success && data.errors) {
        cachedBedrockErrors = data.errors;
      }
    } catch (e) {
      console.warn('Failed to load bedrock errors:', e);
    }
  }

  filterBedrockErrors();
}

function filterBedrockErrors() {
  const container = document.getElementById('bedrock-errors-container');
  const searchInput = document.getElementById('bedrock-error-search');
  if (!container) return;

  const query = searchInput ? searchInput.value.toLowerCase().trim() : '';

  const filtered = cachedBedrockErrors.filter(err => {
    if (!query) return true;
    if (err.httpCode.toString().includes(query)) return true;
    if (err.status.toLowerCase().includes(query)) return true;
    if (err.description.toLowerCase().includes(query)) return true;
    if (err.bedrockNames.some(name => name.toLowerCase().includes(query))) return true;
    return false;
  });

  if (filtered.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; padding: 24px; color: var(--text-muted);">
        Geen foutcode gevonden voor '<strong>${escapeHtml(query)}</strong>'. Probeer bijvoorbeeld <em>404</em>, <em>504</em> of <em>Terracotta</em>.
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.map(err => {
    const isOk = err.httpCode === 200;
    const isWarn = err.httpCode < 400;
    const badgeColor = isOk ? 'badge-code-200' : (isWarn ? 'badge-code-100' : 'badge-code-503');
    const borderCol = isOk ? '#34d399' : (isWarn ? '#fbbf24' : '#f87171');

    return `
      <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); border-left: 4px solid ${borderCol}; border-radius: 8px; padding: 14px 16px;">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; flex-wrap: wrap; gap: 8px;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <span class="bedrock-code-badge ${badgeColor}" style="margin: 0; font-size: 0.78rem;">HTTP ${err.httpCode} ${escapeHtml(err.status)}</span>
            <span style="font-size: 0.76rem; color: var(--text-muted); background: rgba(255,255,255,0.05); padding: 2px 8px; border-radius: 4px;">${escapeHtml(err.category)}</span>
          </div>
          <span style="font-size: 0.8rem; font-weight: 700; color: #fff;">Bedrock: <span style="color: var(--amber);">${escapeHtml(err.bedrockNames.join(', '))}</span></span>
        </div>
        <div style="font-size: 0.85rem; color: #e2e8f0; margin-bottom: 6px;">
          <strong>Oorzaak:</strong> ${escapeHtml(err.description)}
        </div>
        <div style="font-size: 0.82rem; color: #94a3b8; background: rgba(0,0,0,0.25); padding: 8px 12px; border-radius: 6px;">
          <strong style="color: var(--emerald);">💡 Oplossing:</strong> ${escapeHtml(err.solution)}
        </div>
      </div>
    `;
  }).join('');
}
