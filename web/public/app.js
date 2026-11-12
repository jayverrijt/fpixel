/**
 * FPixel Network - Official Web Portal & Community Hub
 * Modern, High-Performance ES6 Client Logic
 */

// Global State
let currentUser = null;
let currentTab = 'home';
let currentMapWorld = 'survival';
let leafletMap = null;
let currentTileLayer = null;
let waypointMarkers = [];
let cachedWaypoints = [];
let pendingClickCoords = null;

// ==========================================
// INITIALIZATION
// ==========================================

document.addEventListener('DOMContentLoaded', () => {
  initAuth();
  loadStats();
  initMap();

  // Refresh server stats every 30 seconds
  setInterval(loadStats, 30000);
});

// ==========================================
// NOTIFICATIONS & TOASTS
// ==========================================

function showToast(message, isError = false) {
  const toast = document.getElementById('toast');
  if (!toast) return;

  toast.textContent = message;
  toast.className = 'toast ' + (isError ? 'toast-error' : 'toast-success');
  toast.style.display = 'block';

  setTimeout(() => {
    toast.style.display = 'none';
  }, 4000);
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// Copy to clipboard with toast
function copyIp(text, label = 'Tekst') {
  navigator.clipboard.writeText(text).then(() => {
    showToast(`📋 ${label} gekopieerd: ${text}`);
  }).catch(() => {
    // Fallback
    const input = document.createElement('input');
    input.value = text;
    document.body.appendChild(input);
    input.select();
    document.execCommand('copy');
    document.body.removeChild(input);
    showToast(`📋 ${label} gekopieerd: ${text}`);
  });
}

// Skin preview for whitelist form
function previewSkin(name) {
  const head = document.getElementById('skin-preview-head');
  if (!head) return;
  const clean = (name || '').trim();
  if (clean.length >= 2) {
    head.src = `https://mc-heads.net/avatar/${encodeURIComponent(clean)}/64`;
  } else {
    head.src = 'https://mc-heads.net/avatar/MHF_Steve/64';
  }
}

// ==========================================
// NAVIGATION & TABS
// ==========================================

function switchTab(tabId) {
  currentTab = tabId;

  // Update nav buttons active state
  document.querySelectorAll('.nav-link').forEach(btn => btn.classList.remove('active'));

  const activeBtn = document.querySelector(`.nav-link[onclick*="'${tabId}'"]`);
  if (activeBtn) activeBtn.classList.add('active');

  // Toggle tab panes
  document.querySelectorAll('.tab-pane').forEach(pane => pane.classList.remove('active'));
  const targetPane = document.getElementById(`tab-${tabId}`);
  if (targetPane) targetPane.classList.add('active');

  window.scrollTo({ top: 0, behavior: 'smooth' });

  // Tab-specific triggers
  if (tabId === 'map') {
    setTimeout(() => {
      if (leafletMap) {
        leafletMap.invalidateSize();
      }
      loadWaypoints();
    }, 150);
  } else if (tabId === 'admin') {
    loadAdminData();
  } else if (tabId === 'owner') {
    loadOwnerData();
  }
}

// ==========================================
// AUTHENTICATION (/api/me, login, logout, reset)
// ==========================================

async function initAuth() {
  try {
    const res = await fetch('/api/me');
    const data = await res.json();
    currentUser = data.user || null;
    renderAuthState();
  } catch (err) {
    console.error('Failed to check session:', err);
    currentUser = null;
    renderAuthState();
  }
}

function renderAuthState() {
  const loggedOutBox = document.getElementById('auth-logged-out');
  const loggedInBox = document.getElementById('auth-logged-in');
  const personalCard = document.getElementById('personal-card');
  const adminNavBtn = document.getElementById('nav-admin-btn');
  const ownerNavBtn = document.getElementById('nav-owner-btn');
  const openWpBtn = document.getElementById('btn-open-waypoint-modal');

  if (currentUser) {
    // Logged In
    if (loggedOutBox) loggedOutBox.style.display = 'none';
    if (loggedInBox) loggedInBox.style.display = 'flex';
    if (personalCard) personalCard.style.display = 'block';

    const avatarUrl = `https://mc-heads.net/avatar/${encodeURIComponent(currentUser.username)}/64`;
    const bustUrl = `https://mc-heads.net/body/${encodeURIComponent(currentUser.username)}/120`;

    const navAvatar = document.getElementById('nav-user-avatar');
    if (navAvatar) navAvatar.src = avatarUrl;

    const navUsername = document.getElementById('nav-username');
    if (navUsername) navUsername.textContent = currentUser.username;

    const navCoins = document.getElementById('nav-user-coins');
    if (navCoins) navCoins.textContent = `🪙 ${(currentUser.coins || 0).toLocaleString()}`;

    const navRank = document.getElementById('nav-user-rank');
    if (navRank) {
      navRank.textContent = currentUser.rank || 'MEMBER';
      navRank.className = `rank-badge rank-${(currentUser.rank || 'default').toLowerCase().replace('_', '')}`;
    }

    // Personal Card info
    const profileBust = document.getElementById('profile-bust');
    if (profileBust) profileBust.src = bustUrl;

    const profileName = document.getElementById('profile-name');
    if (profileName) profileName.textContent = currentUser.username;

    const profileRank = document.getElementById('profile-rank');
    if (profileRank) {
      profileRank.textContent = currentUser.rank || 'MEMBER';
      profileRank.className = `rank-badge rank-${(currentUser.rank || 'default').toLowerCase().replace('_', '')}`;
    }

    const profileCoins = document.getElementById('profile-coins');
    if (profileCoins) profileCoins.textContent = (currentUser.coins || 0).toLocaleString();

    const profileUuid = document.getElementById('profile-uuid');
    if (profileUuid) profileUuid.textContent = `UUID: ${currentUser.uuid || 'N/A'}`;

    const urbanCoords = document.getElementById('urban-coords');
    if (urbanCoords) {
      if (currentUser.urbanCoords) {
        urbanCoords.textContent = `X: ${currentUser.urbanCoords.x}, Y: ${currentUser.urbanCoords.y}, Z: ${currentUser.urbanCoords.z}`;
      } else {
        urbanCoords.textContent = 'Survival Wereld (Spawn)';
      }
    }

    const urbanLevel = document.getElementById('urban-level');
    if (urbanLevel) urbanLevel.textContent = `Lvl ${currentUser.urbanLevel ?? 0}`;

    const statUserRank = document.getElementById('stat-user-rank-display');
    if (statUserRank) statUserRank.textContent = currentUser.rank || 'MEMBER';

    // Permissions visibility
    const isOwner = currentUser.rank === 'OWNER';
    const isAdmin = currentUser.isAdmin || isOwner;

    if (adminNavBtn) adminNavBtn.style.display = isAdmin ? 'flex' : 'none';
    if (ownerNavBtn) ownerNavBtn.style.display = isOwner ? 'flex' : 'none';
    if (openWpBtn) openWpBtn.style.display = 'inline-flex';

  } else {
    // Logged Out
    if (loggedOutBox) loggedOutBox.style.display = 'flex';
    if (loggedInBox) loggedInBox.style.display = 'none';
    if (personalCard) personalCard.style.display = 'none';
    if (adminNavBtn) adminNavBtn.style.display = 'none';
    if (ownerNavBtn) ownerNavBtn.style.display = 'none';
  }
}

// Modal open/close
function openLoginModal(mode = 'login') {
  const modal = document.getElementById('login-modal');
  if (modal) modal.classList.add('active');
  switchLoginMode(mode);
}

function closeLoginModal() {
  const modal = document.getElementById('login-modal');
  if (modal) modal.classList.remove('active');
}

function switchLoginMode(mode) {
  const tabLogin = document.getElementById('modal-tab-login');
  const tabCode = document.getElementById('modal-tab-code');
  const formLogin = document.getElementById('form-login');
  const formCodeSection = document.getElementById('form-code-section');
  const modalTitle = document.getElementById('modal-title');

  if (mode === 'login') {
    if (tabLogin) tabLogin.classList.add('active');
    if (tabCode) tabCode.classList.remove('active');
    if (formLogin) formLogin.style.display = 'block';
    if (formCodeSection) formCodeSection.style.display = 'none';
    if (modalTitle) modalTitle.textContent = '🔑 Inloggen op FPixel';
  } else {
    if (tabLogin) tabLogin.classList.remove('active');
    if (tabCode) tabCode.classList.add('active');
    if (formLogin) formLogin.style.display = 'none';
    if (formCodeSection) formCodeSection.style.display = 'block';
    if (modalTitle) modalTitle.textContent = '✉️ Wachtwoord Resetten via Minecraft Chat';
  }
}

// Standard Password Login
async function handlePasswordLogin(e) {
  e.preventDefault();
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;

  try {
    const res = await fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Inloggen mislukt.', true);
      return;
    }

    currentUser = data.user;
    renderAuthState();
    closeLoginModal();
    showToast(`Welkom terug, ${currentUser.username}!`);
  } catch (err) {
    showToast('Fout bij inloggen: ' + err.message, true);
  }
}

// Request In-Game Code via RCON tellraw
async function requestInGameCode() {
  const username = document.getElementById('setup-username').value.trim();
  if (!username) {
    showToast('Vul eerst je Minecraft gebruikersnaam in.', true);
    return;
  }

  showToast('Code versturen naar in-game chat...', false);

  try {
    const res = await fetch('/api/auth/send-code', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Kon code niet verzenden.', true);
      return;
    }

    showToast(data.message || 'Verificatiecode verzonden naar je Minecraft chat!');
  } catch (err) {
    showToast('Netwerkfout bij aanvragen code: ' + err.message, true);
  }
}

// Verify Code & Set Password
async function handleVerifyCodeAndSetPassword(e) {
  e.preventDefault();
  const username = document.getElementById('setup-username').value.trim();
  const code = document.getElementById('verify-code').value.trim();
  const password = document.getElementById('setup-new-password').value;

  if (!username || !code || !password) {
    showToast('Vul alle velden in.', true);
    return;
  }

  try {
    const res = await fetch('/api/auth/verify-code', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, code, password })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Ongeldige code.', true);
      return;
    }

    currentUser = data.user;
    renderAuthState();
    closeLoginModal();
    showToast(data.message || 'Wachtwoord succesvol opgeslagen! Je bent nu ingelogd.');
  } catch (err) {
    showToast('Fout bij verifiëren: ' + err.message, true);
  }
}

// Logout
async function logout() {
  try {
    await fetch('/api/logout', { method: 'POST' });
    currentUser = null;
    renderAuthState();
    showToast('Je bent uitgelogd.');
    if (currentTab === 'admin' || currentTab === 'owner') {
      switchTab('home');
    }
  } catch (err) {
    showToast('Fout bij uitloggen: ' + err.message, true);
  }
}

// ==========================================
// SERVER STATS & LEADERBOARD (/api/stats)
// ==========================================

async function loadStats() {
  try {
    const res = await fetch('/api/stats');
    const data = await res.json();

    const statusPill = document.getElementById('server-status-pill');
    const statusText = document.getElementById('status-text');
    const heroPlayerCount = document.getElementById('hero-player-count');

    if (data.server && data.server.online) {
      if (statusPill) statusPill.className = 'status-indicator online';
      if (statusText) statusText.textContent = 'ONLINE';
      if (heroPlayerCount) heroPlayerCount.textContent = `${data.server.playersOnline || 0} / ${data.server.playersMax || 100}`;
    } else {
      if (statusPill) statusPill.className = 'status-indicator offline';
      if (statusText) statusText.textContent = 'OFFLINE';
      if (heroPlayerCount) heroPlayerCount.textContent = '0';
    }

    renderFriendsLeaderboard(data.leaderboard || []);
  } catch (err) {
    console.warn('Could not load server stats:', err.message);
  }
}

function renderFriendsLeaderboard(players) {
  const tbody = document.getElementById('friends-tbody');
  if (!tbody) return;

  if (!players || players.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-dim); padding: 24px;">Nog geen spelerdata beschikbaar.</td></tr>`;
    return;
  }

  tbody.innerHTML = players.map(p => {
    const rank = p.rank || 'MEMBER';
    const rankClass = `rank-${rank.toLowerCase().replace('_', '')}`;
    const avatarUrl = `https://mc-heads.net/avatar/${encodeURIComponent(p.username)}/32`;

    let coordsStr = 'Spawn';
    if (p.urbanCoords) {
      coordsStr = `X: ${p.urbanCoords.x}, Z: ${p.urbanCoords.z}`;
    }

    return `
      <tr>
        <td>
          <div class="table-player-cell">
            <img class="table-player-avatar" src="${avatarUrl}" alt="${p.username}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/32'">
            <span class="table-player-name">${escapeHtml(p.username)}</span>
          </div>
        </td>
        <td><span class="rank-badge ${rankClass}">${escapeHtml(rank)}</span></td>
        <td><strong style="color: var(--gold);">🪙 ${(p.coins || 0).toLocaleString()}</strong></td>
        <td><span class="stat-v highlight">Lvl ${p.urbanLevel ?? 0}</span></td>
        <td><code>${coordsStr}</code></td>
        <td><span class="text-green font-bold">Actief</span></td>
      </tr>
    `;
  }).join('');
}

// ==========================================
// INTERACTIVE SURVIVAL MAP & WAYPOINTS
// ==========================================

function initMap() {
  const container = document.getElementById('survival-map-container');
  if (!container || leafletMap) return;

  // Standard flat CRS for Minecraft maps
  leafletMap = L.map('survival-map-container', {
    crs: L.CRS.Simple,
    minZoom: 0,
    maxZoom: 4,
    zoomControl: true,
    attributionControl: false
  });

  leafletMap.setView([-128, 128], 1);

  // Set default tile layer
  updateMapLayer('survival');

  // Mouse move updates coordinates
  leafletMap.on('mousemove', (e) => {
    const coordsEl = document.getElementById('map-coords');
    if (!coordsEl) return;
    const x = Math.round(e.latlng.lng * 4);
    const z = Math.round(-e.latlng.lat * 4);
    coordsEl.textContent = `X: ${x}, Z: ${z}`;
  });

  // Map click triggers waypoint add
  leafletMap.on('click', (e) => {
    const x = Math.round(e.latlng.lng * 4);
    const z = Math.round(-e.latlng.lat * 4);
    pendingClickCoords = { x, z, world: currentMapWorld };

    if (currentUser) {
      openAddWaypointModal(x, z, currentMapWorld);
    } else {
      showToast(`Coördinaten X: ${x}, Z: ${z}. Log in om een waypoint toe te voegen!`);
    }
  });
}

function updateMapLayer(worldId) {
  if (!leafletMap) return;

  if (currentTileLayer) {
    leafletMap.removeLayer(currentTileLayer);
  }

  currentMapWorld = worldId;

  // Update dimension label in footer
  const dimLabel = document.getElementById('map-dim-label');
  if (dimLabel) {
    if (worldId === 'nether') dimLabel.textContent = '🔥 Dimensie: The Nether';
    else if (worldId === 'end') dimLabel.textContent = '🌌 Dimensie: The End';
    else dimLabel.textContent = '🌍 Dimensie: Overworld (Survival)';
  }

  // Use internal tile router
  const tilePath = worldId === 'nether' ? 'minecraft_the_nether' : (worldId === 'end' ? 'minecraft_the_end' : 'minecraft_survival');
  currentTileLayer = L.tileLayer(`/tiles/${tilePath}/0/0/{z}_{x}_{y}.png`, {
    tileSize: 512,
    noWrap: true,
    errorTileUrl: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'
  }).addTo(leafletMap);

  renderWaypointMarkers();
}

function changeMapWorld(worldId) {
  document.querySelectorAll('.segment-btn').forEach(b => b.classList.remove('active'));
  const btn = document.getElementById(`btn-world-${worldId}`);
  if (btn) btn.classList.add('active');

  updateMapLayer(worldId);
}

function resetMapView() {
  if (leafletMap) {
    leafletMap.setView([-128, 128], 1);
  }
}

// Load and Render Waypoints (/api/waypoints)
async function loadWaypoints() {
  try {
    const res = await fetch('/api/waypoints');
    const data = await res.json();
    cachedWaypoints = data.waypoints || [];

    const countBadge = document.getElementById('waypoint-count');
    if (countBadge) countBadge.textContent = `${cachedWaypoints.length} punten`;

    renderWaypointsDrawer();
    renderWaypointMarkers();
  } catch (err) {
    console.error('Failed to load waypoints:', err);
  }
}

function renderWaypointMarkers() {
  if (!leafletMap) return;

  // Clear existing markers
  waypointMarkers.forEach(m => leafletMap.removeLayer(m));
  waypointMarkers = [];

  const worldWaypoints = cachedWaypoints.filter(w => (w.world || 'survival') === currentMapWorld);

  worldWaypoints.forEach(wp => {
    // Transform Minecraft coords to leaflet LatLng (lng = x/4, lat = -z/4)
    const lat = -wp.z / 4;
    const lng = wp.x / 4;

    const iconHtml = `<div style="font-size: 20px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.8)); text-align: center;">${wp.icon || '📍'}</div>`;
    const customIcon = L.divIcon({
      className: 'custom-wp-icon',
      html: iconHtml,
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    });

    const marker = L.marker([lat, lng], { icon: customIcon }).addTo(leafletMap);

    const deleteBtn = (currentUser && (wp.createdBy === currentUser.username || currentUser.rank === 'OWNER' || currentUser.rank === 'ADMIN'))
      ? `<br><button onclick="deleteWaypoint('${wp.id}')" style="background:#ef4444; color:#fff; border:none; padding:4px 8px; border-radius:4px; font-weight:700; margin-top:6px; cursor:pointer;">Verwijder 🗑️</button>`
      : '';

    marker.bindPopup(`
      <div style="font-family:'Plus Jakarta Sans',sans-serif; min-width:140px;">
        <strong style="font-size:14px; color:#fff;">${escapeHtml(wp.name)}</strong><br>
        <span style="font-family:'JetBrains Mono',monospace; font-size:12px; color:#38bdf8;">X: ${wp.x}, Y: ${wp.y || 64}, Z: ${wp.z}</span><br>
        <span style="font-size:11px; color:#94a3b8;">Door: ${escapeHtml(wp.createdBy || 'Server')}</span>
        ${deleteBtn}
      </div>
    `);

    waypointMarkers.push(marker);
  });
}

function renderWaypointsDrawer() {
  const container = document.getElementById('waypoints-list');
  if (!container) return;

  if (cachedWaypoints.length === 0) {
    container.innerHTML = `<div style="color:var(--text-dim); font-size:0.85rem;">Nog geen waypoints geplaatst. Klik op de kaart om er één toe te voegen!</div>`;
    return;
  }

  container.innerHTML = cachedWaypoints.map(wp => {
    const isOwnerOfWp = currentUser && (wp.createdBy === currentUser.username || currentUser.rank === 'OWNER' || currentUser.rank === 'ADMIN');
    return `
      <div class="waypoint-item">
        <div class="wp-meta">
          <span class="wp-name">${wp.icon || '📍'} ${escapeHtml(wp.name)}</span>
          <span class="wp-coords">X: ${wp.x}, Y: ${wp.y || 64}, Z: ${wp.z} (${escapeHtml(wp.world || 'survival')})</span>
          <span class="wp-by">Geplaatst door <strong>${escapeHtml(wp.createdBy || 'Server')}</strong></span>
        </div>
        ${isOwnerOfWp ? `<button class="btn btn-xs btn-outline-danger" onclick="deleteWaypoint('${wp.id}')">Verwijder</button>` : ''}
      </div>
    `;
  }).join('');
}

function openAddWaypointModal(x = 0, z = 0, world = 'survival') {
  if (!currentUser) {
    openLoginModal('login');
    showToast('Log eerst in om waypoints op te slaan.', true);
    return;
  }

  const modal = document.getElementById('waypoint-modal');
  if (modal) modal.classList.add('active');

  const xInput = document.getElementById('wp-x');
  const zInput = document.getElementById('wp-z');
  const worldSelect = document.getElementById('wp-world');

  if (xInput) xInput.value = x;
  if (zInput) zInput.value = z;
  if (worldSelect) worldSelect.value = world;
}

function closeAddWaypointModal() {
  const modal = document.getElementById('waypoint-modal');
  if (modal) modal.classList.remove('active');
}

async function handleWaypointSubmit(e) {
  e.preventDefault();
  const name = document.getElementById('wp-name').value.trim();
  const x = document.getElementById('wp-x').value;
  const y = document.getElementById('wp-y').value;
  const z = document.getElementById('wp-z').value;
  const world = document.getElementById('wp-world').value;
  const icon = document.getElementById('wp-icon').value;

  try {
    const res = await fetch('/api/waypoints/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, x, y, z, world, icon })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Kon waypoint niet toevoegen.', true);
      return;
    }

    closeAddWaypointModal();
    showToast(data.message || 'Waypoint succesvol geplaatst!');
    loadWaypoints();
  } catch (err) {
    showToast('Fout bij toevoegen: ' + err.message, true);
  }
}

async function deleteWaypoint(id) {
  if (!confirm('Weet je zeker dat je dit waypoint wilt verwijderen?')) return;

  try {
    const res = await fetch('/api/waypoints/delete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Kon niet verwijderen.', true);
      return;
    }
    showToast('Waypoint verwijderd.');
    loadWaypoints();
  } catch (err) {
    showToast('Fout: ' + err.message, true);
  }
}

// ==========================================
// WHITELIST REQUEST FORM (PUBLIC)
// ==========================================

async function handleWhitelistSubmit(e) {
  e.preventDefault();
  const username = document.getElementById('wl-username').value.trim();
  const reason = document.getElementById('wl-reason').value.trim();
  const discord = document.getElementById('wl-discord').value.trim();
  const edition = document.querySelector('input[name="wl-edition"]:checked')?.value || 'Java';

  if (!username) {
    showToast('Vul een Minecraft gebruikersnaam in.', true);
    return;
  }

  try {
    const res = await fetch('/api/whitelist/request', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username,
        reason: `${reason}${discord ? ` (Discord: ${discord})` : ''} [${edition}]`
      })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Aanvraag mislukt.', true);
      return;
    }

    showToast('✅ Aanvraag succesvol verstuurd! Een admin zal deze spoedig beoordelen.');
    document.getElementById('whitelist-form').reset();
    previewSkin('');
  } catch (err) {
    showToast('Fout bij versturen: ' + err.message, true);
  }
}

// ==========================================
// ADMIN CONTROL PANEL (/api/admin/*)
// ==========================================

async function loadAdminData() {
  loadPendingRequests();
  loadWhitelist();
}

async function loadPendingRequests() {
  try {
    const res = await fetch('/api/admin/whitelist/requests');
    const data = await res.json();
    const list = data.requests || [];

    const badge = document.getElementById('admin-badge');
    if (badge) badge.textContent = list.length;

    const countLabel = document.getElementById('pending-req-count');
    if (countLabel) countLabel.textContent = list.length;

    const container = document.getElementById('pending-requests-list');
    if (!container) return;

    if (list.length === 0) {
      container.innerHTML = `<div style="grid-column: 1 / -1; padding: 20px; background: rgba(0,0,0,0.2); border-radius: var(--radius-sm); color: var(--text-dim); text-align: center;">Geen openstaande whitelist aanvragen.</div>`;
      return;
    }

    container.innerHTML = list.map(req => {
      const avatarUrl = `https://mc-heads.net/avatar/${encodeURIComponent(req.username)}/48`;
      const dateStr = new Date(req.createdAt).toLocaleString('nl-NL');

      return `
        <div class="request-card">
          <div class="request-header">
            <img class="req-avatar" src="${avatarUrl}" alt="${req.username}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/48'">
            <div class="req-meta">
              <span class="req-name">${escapeHtml(req.username)}</span>
              <span class="req-date">${dateStr}</span>
            </div>
          </div>
          <div class="req-details-box">
            <div class="req-line"><span class="req-k">Bericht / Toelichting:</span></div>
            <div class="req-line"><span class="req-v">"${escapeHtml(req.reason || 'Geen toelichting opgegeven')}"</span></div>
          </div>
          <div class="request-actions">
            <button class="btn btn-sm btn-success" onclick="approveRequest('${req.id}')">✅ Goedkeuren</button>
            <button class="btn btn-sm btn-outline-danger" onclick="rejectRequest('${req.id}')">❌ Afwijzen</button>
          </div>
        </div>
      `;
    }).join('');
  } catch (err) {
    console.error('Error loading pending requests:', err);
  }
}

async function approveRequest(id) {
  try {
    const res = await fetch('/api/admin/whitelist/approve', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Goedkeuren mislukt.', true);
      return;
    }
    showToast(data.message || 'Speler goedgekeurd en toegevoegd aan whitelist!');
    loadAdminData();
  } catch (err) {
    showToast('Fout: ' + err.message, true);
  }
}

async function rejectRequest(id) {
  try {
    const res = await fetch('/api/admin/whitelist/reject', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Afwijzen mislukt.', true);
      return;
    }
    showToast('Aanvraag afgewezen.');
    loadPendingRequests();
  } catch (err) {
    showToast('Fout: ' + err.message, true);
  }
}

async function loadWhitelist() {
  try {
    const res = await fetch('/api/admin/whitelist/list');
    const data = await res.json();
    const list = data.whitelist || [];

    const countLabel = document.getElementById('active-wl-count');
    if (countLabel) countLabel.textContent = list.length;

    const tbody = document.getElementById('whitelist-table-body');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="3" style="text-align: center; color: var(--text-dim); padding: 24px;">Whitelist is leeg.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(item => {
      const avatarUrl = `https://mc-heads.net/avatar/${encodeURIComponent(item.name)}/32`;
      return `
        <tr>
          <td>
            <div class="table-player-cell">
              <img class="table-player-avatar" src="${avatarUrl}" alt="${item.name}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/32'">
              <span class="table-player-name">${escapeHtml(item.name)}</span>
            </div>
          </td>
          <td><code style="font-size: 0.75rem; color: var(--text-dim);">${item.uuid || 'Offline/Bedrock'}</code></td>
          <td>
            <button class="btn btn-xs btn-outline-danger" onclick="removeWhitelist('${escapeHtml(item.name)}')">Verwijder ❌</button>
          </td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    console.error('Error loading whitelist:', err);
  }
}

async function manualAddWhitelist() {
  const input = document.getElementById('manual-wl-input');
  const username = input.value.trim();
  if (!username) return;

  try {
    const res = await fetch('/api/admin/whitelist/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Toevoegen mislukt.', true);
      return;
    }
    showToast(data.message || `${username} toegevoegd aan whitelist.`);
    input.value = '';
    loadWhitelist();
  } catch (err) {
    showToast('Fout: ' + err.message, true);
  }
}

async function removeWhitelist(username) {
  if (!confirm(`Weet je zeker dat je ${username} wilt verwijderen van de whitelist?`)) return;

  try {
    const res = await fetch('/api/admin/whitelist/remove', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Verwijderen mislukt.', true);
      return;
    }
    showToast(data.message || `${username} verwijderd.`);
    loadWhitelist();
  } catch (err) {
    showToast('Fout: ' + err.message, true);
  }
}

async function triggerServerBackup() {
  if (!confirm('Wil je nu direct een volledige server backup snapshot maken?')) return;
  showToast('Server backup wordt gestart...', false);

  try {
    const res = await fetch('/api/server/backup', { method: 'POST' });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Backup starten mislukt.', true);
      return;
    }
    showToast(data.message || 'Backup succesvol gestart op de achtergrond!');
  } catch (err) {
    showToast('Fout bij starten backup: ' + err.message, true);
  }
}

// ==========================================
// OWNER CONTROL PANEL (RANKS, PARKOUR, STOP)
// ==========================================

async function loadOwnerData() {
  loadRanksData();
  loadParkourData();
}

async function loadRanksData() {
  try {
    const res = await fetch('/api/admin/ranks');
    const data = await res.json();
    if (!data.success) return;

    const ranks = data.ranks || {};
    const container = document.getElementById('ranks-overview-container');
    if (!container) return;

    const entries = Object.entries(ranks);
    if (entries.length === 0) {
      container.innerHTML = `<div style="color:var(--text-dim); font-size:0.85rem;">Geen speciale rangen ingesteld.</div>`;
      return;
    }

    container.innerHTML = entries.map(([name, rank]) => {
      const rankClass = `rank-${rank.toLowerCase().replace('_', '')}`;
      return `
        <div class="rank-item-pill">
          <span class="rank-user-name">${escapeHtml(name)}</span>
          <span class="rank-badge ${rankClass}">[${escapeHtml(rank)}]</span>
        </div>
      `;
    }).join('');
  } catch (err) {
    console.error('Error loading ranks:', err);
  }
}

async function handleSetRankSubmit(e) {
  e.preventDefault();
  const username = document.getElementById('rank-username').value.trim();
  const rank = document.getElementById('rank-select').value;

  if (!username) {
    showToast('Vul een gebruikersnaam in.', true);
    return;
  }

  try {
    const res = await fetch('/api/admin/ranks/set', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, rank })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Rang toekennen mislukt.', true);
      return;
    }

    showToast(data.message || `Rang succesvol gewijzigd naar [${rank}]!`);
    document.getElementById('rank-username').value = '';
    loadRanksData();
  } catch (err) {
    showToast('Fout bij toekennen rang: ' + err.message, true);
  }
}

// Parkour Studio CMS (/api/parkour/*)
async function loadParkourData() {
  try {
    const res = await fetch('/api/parkour/maps');
    const data = await res.json();
    const maps = data.maps || [];

    // Update Summary Counters
    const total = maps.length;
    const pending = maps.filter(m => (m.status || 'PENDING_REVIEW').toUpperCase() === 'PENDING_REVIEW').length;
    const approved = maps.filter(m => (m.status || '').toUpperCase() === 'APPROVED').length;
    const disabled = maps.filter(m => (m.status || '').toUpperCase() === 'DISABLED').length;

    const elTotal = document.getElementById('pk-stat-total');
    const elPending = document.getElementById('pk-stat-pending');
    const elApproved = document.getElementById('pk-stat-approved');
    const elDisabled = document.getElementById('pk-stat-disabled');
    const parkourBadge = document.getElementById('parkour-badge');

    if (elTotal) elTotal.innerText = total;
    if (elPending) elPending.innerText = pending;
    if (elApproved) elApproved.innerText = approved;
    if (elDisabled) elDisabled.innerText = disabled;
    if (parkourBadge) parkourBadge.innerText = pending;

    const container = document.getElementById('parkour-maps-list');
    if (!container) return;

    if (maps.length === 0) {
      container.innerHTML = `
        <div style="grid-column: 1 / -1; padding: 36px; background: rgba(0, 0, 0, 0.2); border-radius: var(--radius-md); text-align: center; color: var(--text-dim);">
          <div style="font-size: 2.2rem; margin-bottom: 10px;">🏃</div>
          <h4 style="color: #fff; margin-bottom: 6px;">Nog Geen Community Maps Gemaakt</h4>
          <p style="font-size: 0.9rem;">Spelers met [MOD] rang of hoger kunnen een map bouwen via <code>/fpixel parkour create [naam]</code>.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = maps.map(map => {
      const creator = map.creatorName || 'Onbekend';
      const avatarUrl = `https://mc-heads.net/avatar/${encodeURIComponent(creator)}/64`;
      const checkpointsCount = Array.isArray(map.checkpoints) ? map.checkpoints.length : 0;
      const statusUpper = (map.status || 'PENDING_REVIEW').toUpperCase();
      const dateStr = map.createdAt ? new Date(map.createdAt).toLocaleDateString('nl-NL') : 'Recent';

      let statusLabel = '⏳ In Review';
      let statusBadgeClass = 'status-pending';

      if (statusUpper === 'APPROVED') {
        statusLabel = '✅ Goedgekeurd';
        statusBadgeClass = 'status-approved';
      } else if (statusUpper === 'DISABLED') {
        statusLabel = '⏸️ Uit Circulatie';
        statusBadgeClass = 'status-disabled';
      } else if (statusUpper === 'REJECTED') {
        statusLabel = '❌ Afgewezen';
        statusBadgeClass = 'status-rejected';
      }

      let actionsHtml = '';
      if (statusUpper === 'PENDING_REVIEW') {
        actionsHtml = `
          <div class="card-actions-row">
            <button class="btn btn-sm btn-success" onclick="approveParkourMap('${map.id}')">
              ✅ Goedkeuren & Live
            </button>
            <button class="btn btn-sm btn-outline-danger" onclick="rejectParkourMap('${map.id}')">
              ❌ Afwijzen
            </button>
          </div>
        `;
      } else if (statusUpper === 'APPROVED') {
        actionsHtml = `
          <div class="card-actions-row">
            <span class="active-game-pill">🎮 Actief in Parkour Games</span>
            <button class="btn btn-sm btn-outline-warning" onclick="disableParkourMap('${map.id}')">
              ⏸️ Uit Circulatie
            </button>
          </div>
        `;
      } else if (statusUpper === 'DISABLED') {
        actionsHtml = `
          <div class="card-actions-row">
            <span class="disabled-game-pill">⏸️ Inactief</span>
            <button class="btn btn-sm btn-primary" onclick="approveParkourMap('${map.id}')">
              ▶️ Opnieuw Activeren
            </button>
          </div>
        `;
      } else {
        actionsHtml = `
          <div class="card-actions-row">
            <span class="rejected-game-pill">Afgewezen: ${escapeHtml(map.rejectReason || 'Niet passend')}</span>
            <button class="btn btn-xs btn-outline-success" onclick="approveParkourMap('${map.id}')">
              Alsnog Goedkeuren
            </button>
          </div>
        `;
      }

      return `
        <div class="parkour-card ${statusUpper === 'PENDING_REVIEW' ? 'pending-highlight' : ''}">
          <div class="parkour-card-header">
            <div class="header-left">
              <span class="parkour-id-pill">${map.id}</span>
              <span class="parkour-status-badge ${statusBadgeClass}">${statusLabel}</span>
            </div>
            <span class="map-date-text">${dateStr}</span>
          </div>

          <div class="parkour-card-body">
            <div class="creator-row">
              <img class="creator-avatar" src="${avatarUrl}" alt="${creator}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/64'">
              <div class="creator-meta">
                <h3 class="parkour-title">${escapeHtml(map.name || map.id)}</h3>
                <span class="creator-name">Maker: <strong>${escapeHtml(creator)}</strong></span>
              </div>
            </div>

            <div class="parkour-metrics-grid">
              <div class="metric-item">
                <span class="metric-k">🏁 Checkpoints:</span>
                <span class="metric-v"><strong>${checkpointsCount}</strong> punten</span>
              </div>
              <div class="metric-item">
                <span class="metric-k">📍 Start:</span>
                <span class="metric-v"><code>X: ${Math.round(map.startX || 0)}, Y: ${Math.round(map.startY || 100)}, Z: ${Math.round(map.startZ || 0)}</code></span>
              </div>
            </div>

            <div class="test-command-widget" onclick="copyIp('/fpixel test ${map.id}', 'Test Commando')" title="Klik om in-game test commando te kopiëren">
              <div class="test-cmd-code">/fpixel test ${map.id}</div>
              <button class="btn-copy-cmd">Kopieer 📋</button>
            </div>

            ${actionsHtml}
          </div>
        </div>
      `;
    }).join('');
  } catch (err) {
    console.error('Error loading parkour maps:', err);
  }
}

async function approveParkourMap(mapId) {
  try {
    const res = await fetch('/api/parkour/approve', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: mapId })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Kon map niet goedkeuren.', true);
      return;
    }
    showToast(data.message || 'Map succesvol goedgekeurd!');
    loadParkourData();
  } catch (err) {
    showToast('Fout bij goedkeuren: ' + err.message, true);
  }
}

async function disableParkourMap(mapId) {
  try {
    const res = await fetch('/api/parkour/disable', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: mapId })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Kon map niet uitschakelen.', true);
      return;
    }
    showToast(data.message || 'Map uit circulatie gehaald.');
    loadParkourData();
  } catch (err) {
    showToast('Fout: ' + err.message, true);
  }
}

async function rejectParkourMap(mapId) {
  const reason = prompt('Wat is de reden voor afwijzing? (optioneel):', 'Voldoet nog niet aan de kwaliteitsnormen');
  if (reason === null) return;

  try {
    const res = await fetch('/api/parkour/reject', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: mapId, reason })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Kon map niet afwijzen.', true);
      return;
    }
    showToast(data.message || 'Map afgewezen.');
    loadParkourData();
  } catch (err) {
    showToast('Fout bij afwijzen: ' + err.message, true);
  }
}

// Stop Server Modal
function openStopServerModal() {
  const modal = document.getElementById('stop-server-modal');
  if (modal) {
    document.getElementById('stop-confirm-input').value = '';
    modal.classList.add('active');
  }
}

function closeStopServerModal() {
  const modal = document.getElementById('stop-server-modal');
  if (modal) modal.classList.remove('active');
}

async function confirmStopServer() {
  const input = document.getElementById('stop-confirm-input');
  if (!input || input.value.trim().toUpperCase() !== 'STOP') {
    showToast('Typ exact "STOP" om te bevestigen.', true);
    return;
  }

  closeStopServerModal();
  showToast('Server stop signaal wordt verstuurd...', false);

  try {
    const res = await fetch('/api/server/stop', { method: 'POST' });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Kon server niet stoppen.', true);
      return;
    }
    showToast(data.message || 'Server sluit over enkele seconden af.');
  } catch (err) {
    showToast('Fout bij stoppen: ' + err.message, true);
  }
}
