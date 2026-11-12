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
  // Support legacy tab redirects
  if (tabId === 'admin' || tabId === 'owner') tabId = 'cms';
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
  } else if (tabId === 'cms') {
    refreshCmsData();
  }
}

// Subtab switching inside unified CMS
function switchCmsSubTab(subTabId) {
  document.querySelectorAll('.cms-tab-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.cms-subtab-pane').forEach(p => p.classList.remove('active'));

  const btn = document.getElementById(`cms-tabbtn-${subTabId}`);
  const pane = document.getElementById(`cms-subtab-${subTabId}`);
  if (btn) btn.classList.add('active');
  if (pane) pane.classList.add('active');

  if (subTabId === 'overview') {
    refreshCmsData();
  } else if (subTabId === 'whitelist') {
    loadPendingRequests();
    loadWhitelist();
  } else if (subTabId === 'ranks') {
    loadRanksData();
  } else if (subTabId === 'parkour') {
    loadParkourData();
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
  const cmsNavBtn = document.getElementById('nav-cms-btn');
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

    // Permissions visibility for Unified CMS
    const isOwner = currentUser.rank === 'OWNER';
    const isAdmin = currentUser.isAdmin || isOwner || currentUser.rank === 'ADMIN';
    const isMod = currentUser.rank === 'MOD';
    const canAccessCms = isAdmin || isOwner || isMod;

    if (cmsNavBtn) cmsNavBtn.style.display = canAccessCms ? 'flex' : 'none';
    if (openWpBtn) openWpBtn.style.display = 'inline-flex';

    // Configure CMS view according to permissions
    const roleBadge = document.getElementById('cms-role-badge');
    const roleText = document.getElementById('cms-role-text');
    const stopBtns = document.querySelectorAll('.owner-only-btn');
    const rankNotice = document.getElementById('ranks-perm-notice');
    const rankAssignCard = document.getElementById('rank-assign-card');
    const rankInputs = rankAssignCard ? rankAssignCard.querySelectorAll('input, select, button') : [];

    if (isOwner) {
      if (roleBadge) {
        roleBadge.className = 'cms-role-pill role-pill-owner';
        if (roleText) roleText.textContent = '👑 Eigenaar Toegang (Volledig Beheer)';
      }
      stopBtns.forEach(b => b.style.display = 'inline-flex');
      if (rankNotice) rankNotice.style.display = 'none';
      rankInputs.forEach(i => i.disabled = false);
    } else if (isAdmin) {
      if (roleBadge) {
        roleBadge.className = 'cms-role-pill role-pill-admin';
        if (roleText) roleText.textContent = '🛡️ Administrator Toegang';
      }
      stopBtns.forEach(b => b.style.display = 'none');
      if (rankNotice) rankNotice.style.display = 'flex';
      rankInputs.forEach(i => i.disabled = true);
    } else if (isMod) {
      if (roleBadge) {
        roleBadge.className = 'cms-role-pill role-pill-mod';
        if (roleText) roleText.textContent = '⚔️ Moderator Toegang (Alleen-lezen)';
      }
      stopBtns.forEach(b => b.style.display = 'none');
      if (rankNotice) rankNotice.style.display = 'flex';
      rankInputs.forEach(i => i.disabled = true);
    }

  } else {
    // Logged Out
    if (loggedOutBox) loggedOutBox.style.display = 'flex';
    if (loggedInBox) loggedInBox.style.display = 'none';
    if (personalCard) personalCard.style.display = 'none';
    if (cmsNavBtn) cmsNavBtn.style.display = 'none';
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
    if (currentTab === 'admin' || currentTab === 'owner' || currentTab === 'cms') {
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
// UNIFIED CMS MANAGEMENT PLATFORM
// ==========================================

let allPendingRequests = [];
let allWhitelistPlayers = [];
let allRanksPlayers = [];
let allParkourMaps = [];
let currentParkourFilter = 'ALL';

async function refreshCmsData() {
  await Promise.all([
    loadPendingRequests(),
    loadWhitelist(),
    loadRanksData(),
    loadParkourData()
  ]);
}

function updateGlobalCmsBadge() {
  const pendingRequests = allPendingRequests.filter(r => (r.status || 'PENDING') === 'PENDING').length;
  const pendingParkour = allParkourMaps.filter(m => (m.status || 'PENDING_REVIEW').toUpperCase() === 'PENDING_REVIEW').length;
  const total = pendingRequests + pendingParkour;

  const badge = document.getElementById('cms-badge');
  if (badge) {
    badge.textContent = total;
    badge.style.display = total > 0 ? 'inline-block' : 'none';
  }
}

// ------------------------------------------
// 1. WHITELIST & JOIN REQUESTS
// ------------------------------------------

async function loadAdminData() {
  await loadPendingRequests();
  await loadWhitelist();
}

async function loadPendingRequests() {
  try {
    const res = await fetch('/api/admin/whitelist/requests');
    const data = await res.json();
    const list = data.requests || [];
    allPendingRequests = list;

    const pendingList = list.filter(r => (r.status || 'PENDING') === 'PENDING');

    const countLabel = document.getElementById('pending-req-count');
    const tabWlBadge = document.getElementById('cms-tab-wl-badge');
    const kpiReq = document.getElementById('cms-kpi-requests');

    if (countLabel) countLabel.textContent = pendingList.length;
    if (tabWlBadge) tabWlBadge.textContent = pendingList.length;
    if (kpiReq) kpiReq.textContent = pendingList.length;

    updateGlobalCmsBadge();

    const container = document.getElementById('pending-requests-list');
    if (!container) return;

    if (pendingList.length === 0) {
      container.innerHTML = `
        <div class="cms-empty-state">
          <div class="empty-icon">🎉</div>
          <h4>Geen Openstaande Aanvragen</h4>
          <p>Alle whitelist aanvragen zijn goedgekeurd of afgehandeld.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = pendingList.map(req => {
      const avatarUrl = `https://mc-heads.net/avatar/${encodeURIComponent(req.username)}/48`;
      const dateStr = req.createdAt ? new Date(req.createdAt).toLocaleString('nl-NL') : 'Recent';
      const isBedrock = (req.edition || '').toLowerCase() === 'bedrock';

      return `
        <div class="request-card">
          <div class="request-header">
            <img class="req-avatar" src="${avatarUrl}" alt="${escapeHtml(req.username)}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/48'">
            <div class="req-meta">
              <div class="req-name-row">
                <span class="req-name">${escapeHtml(req.username)}</span>
                <span class="edition-badge ${isBedrock ? 'bedrock' : 'java'}">${isBedrock ? '📱 Bedrock' : '☕ Java'}</span>
              </div>
              <span class="req-date">${dateStr}</span>
            </div>
          </div>
          <div class="req-details-box">
            <div class="req-line"><span class="req-k">Motivatie:</span></div>
            <div class="req-line"><span class="req-v">"${escapeHtml(req.reason || 'Geen toelichting opgegeven')}"</span></div>
            ${req.discord ? `<div class="req-line" style="margin-top:6px;"><span class="req-k">Discord:</span> <code style="color:var(--text-light); font-size:0.8rem;">${escapeHtml(req.discord)}</code></div>` : ''}
          </div>
          <div class="request-actions">
            <button class="btn btn-sm btn-success" onclick="approveRequest('${req.id}')" title="Direct toevoegen aan Minecraft whitelist">✅ Goedkeuren</button>
            <button class="btn btn-sm btn-outline" onclick="rejectRequest('${req.id}')" title="Aanvraag markeren als afgewezen">❌ Afwijzen</button>
            <button class="btn btn-sm btn-outline-danger" onclick="deleteRequest('${req.id}')" title="Aanvraag definitief verwijderen">🗑️ Verwijder</button>
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
    loadPendingRequests();
    loadWhitelist();
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
    showToast('Aanvraag gemarkeerd als afgewezen.');
    loadPendingRequests();
  } catch (err) {
    showToast('Fout: ' + err.message, true);
  }
}

async function deleteRequest(id) {
  if (!confirm('Weet je zeker dat je deze aanvraag definitief wilt verwijderen?')) return;
  try {
    const res = await fetch('/api/admin/whitelist/request/delete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ requestId: id })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Verwijderen mislukt.', true);
      return;
    }
    showToast(data.message || 'Aanvraag definitief verwijderd.');
    loadPendingRequests();
  } catch (err) {
    showToast('Fout bij verwijderen: ' + err.message, true);
  }
}

async function cleanFakeRequests() {
  if (!confirm('Wil je oude/test aanvragen (zoals gaysteve en testfriend) en reeds verwerkte aanvragen opschonen?')) return;
  try {
    const res = await fetch('/api/admin/whitelist/requests/clear', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ all: false })
    });
    const data = await res.json();
    showToast(data.message || 'Test aanvragen opgeschoond!');
    loadPendingRequests();
  } catch (err) {
    showToast('Fout bij opschonen: ' + err.message, true);
  }
}

async function clearAllRequests() {
  if (!confirm('LET OP: Weet je zeker dat je ALLE whitelist aanvragen wilt wissen?')) return;
  try {
    const res = await fetch('/api/admin/whitelist/requests/clear', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ all: true })
    });
    const data = await res.json();
    showToast(data.message || 'Alle aanvragen gewist!');
    loadPendingRequests();
  } catch (err) {
    showToast('Fout: ' + err.message, true);
  }
}

async function loadWhitelist() {
  try {
    const res = await fetch('/api/admin/whitelist/list');
    const data = await res.json();
    allWhitelistPlayers = data.whitelist || [];

    const countLabel = document.getElementById('active-wl-count');
    const kpiWl = document.getElementById('cms-kpi-whitelist');
    if (countLabel) countLabel.textContent = allWhitelistPlayers.length;
    if (kpiWl) kpiWl.textContent = allWhitelistPlayers.length;

    renderWhitelistTable(allWhitelistPlayers);
  } catch (err) {
    console.error('Error loading whitelist:', err);
  }
}

function renderWhitelistTable(list) {
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
            <img class="table-player-avatar" src="${avatarUrl}" alt="${escapeHtml(item.name)}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/32'">
            <span class="table-player-name">${escapeHtml(item.name)}</span>
          </div>
        </td>
        <td><code class="uuid-code">${escapeHtml(item.uuid || 'Offline/Bedrock')}</code></td>
        <td style="text-align: right;">
          <button class="btn btn-xs btn-outline-danger" onclick="removeWhitelist('${escapeHtml(item.name)}')">Verwijder ❌</button>
        </td>
      </tr>
    `;
  }).join('');
}

function filterWhitelist(query) {
  const q = (query || '').toLowerCase().trim();
  if (!q) {
    renderWhitelistTable(allWhitelistPlayers);
    return;
  }
  const filtered = allWhitelistPlayers.filter(p => 
    (p.name && p.name.toLowerCase().includes(q)) || 
    (p.uuid && p.uuid.toLowerCase().includes(q))
  );
  renderWhitelistTable(filtered);
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

// ------------------------------------------
// 2. SPELERS & RANGEN (CMS TABLE & OWNER SET)
// ------------------------------------------

async function loadOwnerData() {
  await loadRanksData();
  await loadParkourData();
}

async function loadRanksData() {
  try {
    const res = await fetch('/api/admin/ranks');
    const data = await res.json();
    if (!data.success) return;

    allRanksPlayers = data.players || [];
    const isOwner = Boolean(data.isOwner);

    const kpiRanks = document.getElementById('cms-kpi-ranks');
    const staffCount = allRanksPlayers.filter(p => p.rank !== 'MEMBER').length;
    if (kpiRanks) kpiRanks.textContent = staffCount;

    renderRanksTable(allRanksPlayers, isOwner);
  } catch (err) {
    console.error('Error loading ranks:', err);
  }
}

function renderRanksTable(players, isOwner) {
  const tbody = document.getElementById('ranks-table-body');
  if (!tbody) return;

  if (players.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--text-dim); padding: 24px;">Geen spelers met speciale rangen gevonden.</td></tr>`;
    return;
  }

  tbody.innerHTML = players.map(p => {
    const avatarUrl = `https://mc-heads.net/avatar/${encodeURIComponent(p.username)}/32`;
    const rankClass = `rank-${p.rank.toLowerCase().replace('_', '')}`;
    const isBedrock = p.username.startsWith('.');

    let roleDescription = 'Speler';
    if (p.rank === 'OWNER') roleDescription = '👑 Eigenaar';
    else if (p.rank === 'ADMIN') roleDescription = '🛡️ Administrator';
    else if (p.rank === 'MOD') roleDescription = '⚔️ Moderator';
    else if (p.rank === 'YOUTUBE') roleDescription = '🎬 YouTuber';
    else if (p.rank.includes('MVP')) roleDescription = '💎 MVP Donateur';
    else if (p.rank.includes('VIP')) roleDescription = '✨ VIP Donateur';

    let actions = `<span style="color:var(--text-dim); font-size:0.8rem;">Alleen-lezen</span>`;
    if (isOwner) {
      actions = `
        <div style="display:flex; gap:6px; justify-content:flex-end;">
          <button class="btn btn-xs btn-outline-warning" onclick="quickEditRank('${escapeHtml(p.username)}', '${p.rank}')">Wijzig</button>
          ${p.rank !== 'MEMBER' ? `<button class="btn btn-xs btn-outline-danger" onclick="resetRank('${escapeHtml(p.username)}')">Reset</button>` : ''}
        </div>
      `;
    }

    return `
      <tr>
        <td>
          <div class="table-player-cell">
            <img class="table-player-avatar" src="${avatarUrl}" alt="${escapeHtml(p.username)}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/32'">
            <div>
              <span class="table-player-name">${escapeHtml(p.username)}</span>
              ${isBedrock ? '<span class="edition-badge bedrock" style="margin-left:6px; font-size:0.65rem;">Bedrock</span>' : ''}
            </div>
          </div>
        </td>
        <td>
          <span class="rank-badge ${rankClass}">[${escapeHtml(p.rank)}]</span>
        </td>
        <td>
          <span class="status-indicator-tag">${roleDescription}</span>
        </td>
        <td style="text-align: right;">
          ${actions}
        </td>
      </tr>
    `;
  }).join('');
}

function filterRanksTable(query) {
  const q = (query || '').toLowerCase().trim();
  const isOwner = currentUser && currentUser.rank === 'OWNER';
  if (!q) {
    renderRanksTable(allRanksPlayers, isOwner);
    return;
  }
  const filtered = allRanksPlayers.filter(p => 
    (p.username && p.username.toLowerCase().includes(q)) || 
    (p.rank && p.rank.toLowerCase().includes(q))
  );
  renderRanksTable(filtered, isOwner);
}

function quickEditRank(username, currentRank) {
  const userInput = document.getElementById('rank-username');
  const rankSelect = document.getElementById('rank-select');
  if (userInput) userInput.value = username;
  if (rankSelect) rankSelect.value = currentRank || 'MEMBER';

  const card = document.getElementById('rank-assign-card');
  if (card) {
    card.scrollIntoView({ behavior: 'smooth', block: 'center' });
    if (userInput) userInput.focus();
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

async function resetRank(username) {
  if (!confirm(`Weet je zeker dat je de rang van ${username} wilt intrekken en resetten naar [MEMBER]?`)) return;
  try {
    const res = await fetch('/api/admin/ranks/reset', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Resetten mislukt.', true);
      return;
    }
    showToast(data.message || `Rang van ${username} is gereset naar MEMBER.`);
    loadRanksData();
  } catch (err) {
    showToast('Fout bij resetten: ' + err.message, true);
  }
}

// ------------------------------------------
// 3. COMMUNITY PARKOUR STUDIO
// ------------------------------------------

async function loadParkourData() {
  try {
    const res = await fetch('/api/parkour/maps');
    const data = await res.json();
    allParkourMaps = data.maps || [];

    const total = allParkourMaps.length;
    const pending = allParkourMaps.filter(m => (m.status || 'PENDING_REVIEW').toUpperCase() === 'PENDING_REVIEW').length;
    const approved = allParkourMaps.filter(m => (m.status || '').toUpperCase() === 'APPROVED').length;
    const disabled = allParkourMaps.filter(m => (m.status || '').toUpperCase() === 'DISABLED').length;

    const elTotal = document.getElementById('pk-stat-total');
    const elPending = document.getElementById('pk-stat-pending');
    const elApproved = document.getElementById('pk-stat-approved');
    const elDisabled = document.getElementById('pk-stat-disabled');
    const tabPkBadge = document.getElementById('cms-tab-pk-badge');
    const kpiParkour = document.getElementById('cms-kpi-parkour');

    if (elTotal) elTotal.innerText = total;
    if (elPending) elPending.innerText = pending;
    if (elApproved) elApproved.innerText = approved;
    if (elDisabled) elDisabled.innerText = disabled;
    if (tabPkBadge) tabPkBadge.innerText = pending;
    if (kpiParkour) kpiParkour.innerText = total;

    updateGlobalCmsBadge();
    renderParkourMaps(allParkourMaps);
  } catch (err) {
    console.error('Error loading parkour maps:', err);
  }
}

function filterParkourCards(status) {
  currentParkourFilter = status;
  renderParkourMaps(allParkourMaps);
}

function renderParkourMaps(maps) {
  const container = document.getElementById('parkour-maps-list');
  if (!container) return;

  let filtered = maps;
  if (currentParkourFilter !== 'ALL') {
    filtered = maps.filter(m => {
      const s = (m.status || 'PENDING_REVIEW').toUpperCase();
      return s === currentParkourFilter;
    });
  }

  if (filtered.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; padding: 36px; background: rgba(0, 0, 0, 0.2); border-radius: var(--radius-md); text-align: center; color: var(--text-dim);">
        <div style="font-size: 2.2rem; margin-bottom: 10px;">🏃</div>
        <h4 style="color: #fff; margin-bottom: 6px;">Geen Parcoursen Gevonden</h4>
        <p style="font-size: 0.9rem;">Er zijn geen community maps in deze categorie.</p>
      </div>
    `;
    return;
  }

  const isOwner = currentUser && currentUser.rank === 'OWNER';

  container.innerHTML = filtered.map(map => {
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
    if (isOwner) {
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
            <span class="active-game-pill">🎮 Actief in Games</span>
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
              ▶️ Weer Activeren
            </button>
          </div>
        `;
      } else {
        actionsHtml = `
          <div class="card-actions-row">
            <span class="rejected-game-pill">Afgewezen</span>
            <button class="btn btn-xs btn-outline-success" onclick="approveParkourMap('${map.id}')">
              Alsnog Goedkeuren
            </button>
          </div>
        `;
      }
    } else {
      actionsHtml = `
        <div class="card-actions-row">
          <span style="color:var(--text-dim); font-size:0.8rem;">👁️ Alleen Eigenaar kan maps goedkeuren</span>
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

// ------------------------------------------
// 4. SERVER & SYSTEEM CONTROLES
// ------------------------------------------

async function handleBroadcastSubmit(e) {
  e.preventDefault();
  const input = document.getElementById('broadcast-message-input');
  if (!input) return;
  const message = input.value.trim();
  if (!message) return;

  try {
    const res = await fetch('/api/server/broadcast', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message })
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Omroep verzenden mislukt.', true);
      return;
    }
    showToast(data.message || 'Omroep verzonden naar Minecraft chat!');
    input.value = '';
  } catch (err) {
    showToast('Fout bij omroep: ' + err.message, true);
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
