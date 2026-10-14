// FPixel SMP Vrienden Portaal & Kaart Script

let currentUser = null;
let leafletMap = null;
let currentMapWorld = 'urban';
let mapMarkersData = [];
let markerLayerGroup = null;

// ==========================================
// TOAST & UTILITIES
// ==========================================

function showToast(msg) {
  const toast = document.getElementById('toast') || document.getElementById('copy-toast');
  if (!toast) return;
  toast.innerText = msg;
  toast.classList.add('show');
  setTimeout(() => {
    toast.classList.remove('show');
  }, 3000);
}

function copyToClipboard(text, message) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => {
      showToast(message || 'Gekopieerd naar klembord!');
    }).catch(() => fallbackCopy(text, message));
  } else {
    fallbackCopy(text, message);
  }
}

function fallbackCopy(text, message) {
  const textarea = document.createElement('textarea');
  textarea.value = text;
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  document.body.removeChild(textarea);
  showToast(message || 'Gekopieerd!');
}

function copyIp(text) {
  copyToClipboard(text, `Gekopieerd: ${text}`);
}

function getRankBadgeClass(rank) {
  switch ((rank || '').toUpperCase()) {
    case 'OWNER': return 'rank-owner';
    case 'ADMIN': return 'rank-admin';
    case 'MVP+':
    case 'MVP_PLUS': return 'rank-mvp-plus';
    case 'MVP': return 'rank-mvp';
    case 'VIP+':
    case 'VIP_PLUS': return 'rank-vip-plus';
    case 'VIP': return 'rank-vip';
    default: return 'rank-default';
  }
}

// ==========================================
// NAVIGATION & TAB SWITCHING
// ==========================================

function switchTab(tabId) {
  // Update nav buttons
  const navLinks = document.querySelectorAll('.nav-link');
  navLinks.forEach(link => {
    if (link.getAttribute('onclick')?.includes(tabId)) {
      link.classList.add('active');
    } else {
      link.classList.remove('active');
    }
  });

  // Update tab panes
  const panes = document.querySelectorAll('.tab-pane');
  panes.forEach(pane => {
    if (pane.id === `tab-${tabId}`) {
      pane.classList.add('active');
    } else {
      pane.classList.remove('active');
    }
  });

  // Map initialization or resizing
  if (tabId === 'map') {
    if (!leafletMap) {
      setTimeout(initSurvivalMap, 80);
    } else {
      setTimeout(() => {
        leafletMap.invalidateSize();
      }, 100);
    }
  }

  // Admin panel loading
  if (tabId === 'admin') {
    loadAdminData();
  }
}

// ==========================================
// AUTHENTICATION & PROFILE
// ==========================================

async function checkAuth() {
  try {
    const res = await fetch('/api/me');
    const data = await res.json();
    if (data.loggedIn && data.user) {
      renderLoggedIn(data.user);
    } else {
      renderLoggedOut();
    }
  } catch (err) {
    console.warn('Auth check error:', err);
    renderLoggedOut();
  }
}

function renderLoggedIn(user) {
  currentUser = user;

  // Header User Pill
  document.getElementById('auth-logged-out').style.display = 'none';
  document.getElementById('auth-logged-in').style.display = 'flex';
  
  const avatar = document.getElementById('nav-user-avatar');
  avatar.src = `https://mc-heads.net/avatar/${encodeURIComponent(user.username)}/64`;
  avatar.onerror = () => { avatar.src = 'https://mc-heads.net/avatar/MHF_Steve/64'; };

  document.getElementById('nav-username').innerText = user.username;
  document.getElementById('nav-user-coins').innerText = `🪙 ${(user.coins || 0).toLocaleString()}`;
  
  const rankPill = document.getElementById('nav-user-rank');
  rankPill.innerText = user.rank;
  rankPill.className = `rank-badge ${getRankBadgeClass(user.rank)}`;

  // Dashboard Personal Card
  document.getElementById('welcome-banner').style.display = 'none';
  const pCard = document.getElementById('personal-card');
  pCard.style.display = 'block';

  const bustImg = document.getElementById('profile-bust');
  bustImg.src = `https://mc-heads.net/body/${encodeURIComponent(user.username)}/right`;
  bustImg.onerror = () => { bustImg.src = 'https://mc-heads.net/body/MHF_Steve/right'; };

  document.getElementById('profile-name').innerText = user.username;
  const pRank = document.getElementById('profile-rank');
  pRank.innerText = user.rank;
  pRank.className = `rank-badge ${getRankBadgeClass(user.rank)}`;
  document.getElementById('profile-uuid').innerText = `UUID: ${user.uuid}`;
  document.getElementById('profile-coins').innerText = (user.coins || 0).toLocaleString();

  // Urban SMP Stats
  const uCoords = user.stats?.urban?.coords || { x: -653, y: -22, z: -116 };
  document.getElementById('urban-coords').innerText = `X: ${uCoords.x}, Y: ${uCoords.y}, Z: ${uCoords.z}`;
  document.getElementById('urban-level').innerText = `Lvl ${user.stats?.urban?.level || 0}`;

  const hp = user.stats?.urban?.health || 20;
  const hearts = '❤️'.repeat(Math.min(10, Math.max(1, Math.round(hp / 2))));
  document.getElementById('urban-health').innerText = `${hearts} (${hp} HP)`;

  // Admin Button Toggle
  const adminBtn = document.getElementById('nav-admin-btn');
  if (user.isAdmin) {
    adminBtn.style.display = 'inline-flex';
    fetchAdminBadgeCount();
  } else {
    adminBtn.style.display = 'none';
  }
}

function renderLoggedOut() {
  currentUser = null;
  document.getElementById('auth-logged-out').style.display = 'flex';
  document.getElementById('auth-logged-in').style.display = 'none';
  document.getElementById('welcome-banner').style.display = 'block';
  document.getElementById('personal-card').style.display = 'none';
  document.getElementById('nav-admin-btn').style.display = 'none';
}

function openLoginModal() {
  const modal = document.getElementById('login-modal');
  if (modal) {
    switchLoginMode('login');
    modal.classList.add('open');
  }
}

function closeLoginModal() {
  const modal = document.getElementById('login-modal');
  if (modal) modal.classList.remove('open');
}

function switchLoginMode(mode) {
  const tabLogin = document.getElementById('modal-tab-login');
  const tabCode = document.getElementById('modal-tab-code');
  const formLogin = document.getElementById('form-login');
  const formCode = document.getElementById('form-code-section');

  if (!tabLogin || !tabCode || !formLogin || !formCode) return;

  if (mode === 'code') {
    tabLogin.classList.remove('active');
    tabCode.classList.add('active');
    formLogin.style.display = 'none';
    formCode.style.display = 'block';

    const loginUser = document.getElementById('login-username')?.value.trim();
    if (loginUser && document.getElementById('setup-username')) {
      document.getElementById('setup-username').value = loginUser;
    }
  } else {
    tabCode.classList.remove('active');
    tabLogin.classList.add('active');
    formCode.style.display = 'none';
    formLogin.style.display = 'block';
  }
}

async function handlePasswordLogin(event) {
  event.preventDefault();
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;

  if (!username || !password) {
    showToast('Vul je gebruikersnaam en wachtwoord in.');
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
      closeLoginModal();
      checkAuth();
      showToast(`Ingelogd als ${data.user.username}!`);
      fetchStats();
    } else {
      showToast(data.error || 'Inloggen mislukt.');
    }
  } catch (err) {
    showToast('Fout bij verbinden met de server.');
  }
}

async function requestInGameCode() {
  const input = document.getElementById('setup-username');
  const username = input ? input.value.trim() : '';
  if (!username) {
    showToast('Vul eerst je Minecraft gebruikersnaam in.');
    return;
  }

  try {
    showToast('Verificatiecode verzenden naar Minecraft...');
    const res = await fetch('/api/auth/send-code', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(data.message);
      const verifyInput = document.getElementById('verify-code');
      if (verifyInput) verifyInput.focus();
    } else {
      showToast(data.error || 'Kan verificatiecode niet sturen.');
    }
  } catch (err) {
    showToast('Fout bij verzenden van verificatiecode.');
  }
}

async function handleVerifyCodeAndSetPassword(event) {
  event.preventDefault();
  const username = document.getElementById('setup-username').value.trim();
  const code = document.getElementById('verify-code').value.trim();
  const password = document.getElementById('setup-new-password').value;

  if (!username) {
    showToast('Vul je Minecraft gebruikersnaam in.');
    return;
  }
  if (!code || code.length !== 6) {
    showToast('Vul de 6-cijferige verificatiecode in.');
    return;
  }
  if (!password || password.length < 4) {
    showToast('Wachtwoord moet minimaal 4 tekens bevatten.');
    return;
  }

  try {
    const res = await fetch('/api/auth/verify-code', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, code, password })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      closeLoginModal();
      checkAuth();
      showToast(data.message || 'Wachtwoord ingesteld en ingelogd!');
      fetchStats();
    } else {
      showToast(data.error || 'Verificatie mislukt.');
    }
  } catch (err) {
    showToast('Fout bij verifiëren van code.');
  }
}

async function logout() {
  try {
    await fetch('/api/logout', { method: 'POST' });
    checkAuth();
    switchTab('dashboard');
    showToast('Je bent uitgelogd.');
  } catch (e) {
    console.error(e);
  }
}

// ==========================================
// INTERACTIVE SURVIVAL MAP (LEAFLET)
// ==========================================

function initSurvivalMap() {
  const container = document.getElementById('survival-map-container');
  if (!container || leafletMap) return;

  // Simple Flat Coordinate System for Minecraft [Y=-Z, X=X]
  leafletMap = L.map('survival-map-container', {
    crs: L.CRS.Simple,
    minZoom: -3,
    maxZoom: 3,
    zoomSnap: 0.25,
    attributionControl: false
  });

  // Tactical Minecraft Canvas Grid Layer
  const GridCanvasLayer = L.GridLayer.extend({
    createTile: function(coords) {
      const tile = document.createElement('canvas');
      const tileSize = this.getTileSize();
      tile.width = tileSize.x;
      tile.height = tileSize.y;
      const ctx = tile.getContext('2d');

      // Dark Tactical Background
      ctx.fillStyle = '#0b1120';
      ctx.fillRect(0, 0, tileSize.x, tileSize.y);

      // Subtle Grid Lines
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)';
      ctx.lineWidth = 1;

      const step = 64;
      for (let x = 0; x <= tileSize.x; x += step) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, tileSize.y);
        ctx.stroke();
      }
      for (let y = 0; y <= tileSize.y; y += step) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(tileSize.x, y);
        ctx.stroke();
      }

      // Border of tile
      ctx.strokeStyle = 'rgba(59, 130, 246, 0.12)';
      ctx.strokeRect(0, 0, tileSize.x, tileSize.y);

      return tile;
    }
  });

  new GridCanvasLayer({ tileSize: 256 }).addTo(leafletMap);

  // Group for markers
  markerLayerGroup = L.layerGroup().addTo(leafletMap);

  // Initial center on Spawn (0, 0)
  leafletMap.setView([0, 0], 0);

  // Live coordinates tracker on hover
  leafletMap.on('mousemove', (e) => {
    const mcX = Math.round(e.latlng.lng);
    const mcZ = Math.round(-e.latlng.lat);
    const coordsEl = document.getElementById('map-coords');
    if (coordsEl) {
      coordsEl.innerText = `X: ${mcX}, Z: ${mcZ}`;
    }
  });

  // Fetch markers from backend
  loadMapData();
}

async function loadMapData() {
  try {
    const res = await fetch('/api/map/data');
    if (!res.ok) return;
    const data = await res.json();
    mapMarkersData = data.markers || [];
    renderMapMarkers();
  } catch (err) {
    console.error('Failed to load map data:', err);
  }
}

function renderMapMarkers() {
  if (!markerLayerGroup || !leafletMap) return;
  markerLayerGroup.clearLayers();

  const filtered = mapMarkersData.filter(m => m.world === currentMapWorld);

  filtered.forEach(m => {
    // Determine player head or icon
    let headUrl = 'https://mc-heads.net/avatar/MHF_Chest/64';
    if (m.id === 'jayv') headUrl = 'https://mc-heads.net/avatar/Jay_V0/64';
    else if (m.id === 'smiley') headUrl = 'https://mc-heads.net/avatar/SmileyGaming27/64';
    else if (m.id === 'minnie') headUrl = 'https://mc-heads.net/avatar/minnieheks/64';
    else if (m.id === 'amiga') headUrl = 'https://mc-heads.net/avatar/Amigapower2018/64';
    else if (m.id === 'spawn') headUrl = 'https://mc-heads.net/avatar/MHF_Exclamation/64';

    const customIcon = L.divIcon({
      className: 'custom-mc-marker',
      html: `
        <div class="map-player-marker">
          <img src="${headUrl}" alt="${m.name}">
          <span>${m.name}</span>
        </div>
      `,
      iconSize: [60, 60],
      iconAnchor: [30, 30]
    });

    // In Minecraft: Y=-Z, X=X
    const marker = L.marker([-m.z, m.x], { icon: customIcon });

    marker.bindPopup(`
      <div style="font-family: 'Plus Jakarta Sans', sans-serif; min-width: 200px; padding: 4px;">
        <h4 style="margin: 0 0 6px 0; color: #f59e0b; font-size: 1rem; font-weight: 800;">${m.icon} ${m.name}</h4>
        <p style="margin: 0 0 10px 0; font-size: 0.85rem; color: #cbd5e1; line-height: 1.4;">${m.desc}</p>
        <div style="font-family: 'JetBrains Mono', monospace; font-size: 0.8rem; background: #0f172a; padding: 6px 10px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.1); color: #10b981;">
          📍 X: <strong>${m.x}</strong>, Z: <strong>${m.z}</strong>
        </div>
      </div>
    `);

    markerLayerGroup.addLayer(marker);
  });
}

function changeMapWorld(world) {
  currentMapWorld = world;
  const urbanBtn = document.getElementById('btn-world-urban');
  const galaxyBtn = document.getElementById('btn-world-galaxy');
  if (urbanBtn && galaxyBtn) {
    if (world === 'urban') {
      urbanBtn.classList.add('active');
      galaxyBtn.classList.remove('active');
    } else {
      galaxyBtn.classList.add('active');
      urbanBtn.classList.remove('active');
    }
  }

  renderMapMarkers();
  resetMapView();
}

function resetMapView() {
  if (leafletMap) {
    leafletMap.setView([0, 0], 0);
  }
}

// ==========================================
// WHITELIST REQUEST FORM
// ==========================================

function previewSkin(username) {
  const head = document.getElementById('skin-preview-head');
  if (!head) return;
  const clean = (username || '').trim();
  if (clean.length >= 2) {
    head.src = `https://mc-heads.net/avatar/${encodeURIComponent(clean)}/64`;
    head.onerror = () => { head.src = 'https://mc-heads.net/avatar/MHF_Steve/64'; };
  } else {
    head.src = 'https://mc-heads.net/avatar/MHF_Steve/64';
  }
}

async function handleWhitelistSubmit(event) {
  event.preventDefault();
  const username = document.getElementById('wl-username').value.trim();
  const reason = document.getElementById('wl-reason').value.trim();

  if (!username) {
    showToast('Vul een geldige Minecraft naam in.');
    return;
  }

  try {
    const res = await fetch('/api/whitelist/request', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, reason })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(data.message);
      document.getElementById('whitelist-form').reset();
      previewSkin('');
      if (currentUser?.isAdmin) {
        fetchAdminBadgeCount();
      }
    } else {
      showToast(data.error || 'Er is een fout opgetreden.');
    }
  } catch (err) {
    showToast('Fout bij versturen van whitelist aanvraag.');
  }
}

// ==========================================
// ADMIN CONTROL PANEL
// ==========================================

async function fetchAdminBadgeCount() {
  if (!currentUser?.isAdmin) return;
  try {
    const res = await fetch('/api/admin/whitelist/requests');
    if (!res.ok) return;
    const data = await res.json();
    const pending = (data.requests || []).filter(r => r.status === 'PENDING').length;
    const badge = document.getElementById('admin-badge');
    if (badge) badge.innerText = pending;
  } catch (e) {}
}

async function loadAdminData() {
  if (!currentUser?.isAdmin) return;

  // 1. Load Requests
  try {
    const reqRes = await fetch('/api/admin/whitelist/requests');
    if (reqRes.ok) {
      const data = await reqRes.json();
      renderAdminRequests(data.requests || []);
    }
  } catch (err) {
    console.error('Error fetching admin requests:', err);
  }

  // 2. Load Whitelisted Players
  try {
    const wlRes = await fetch('/api/admin/whitelist/list');
    if (wlRes.ok) {
      const data = await wlRes.json();
      renderAdminWhitelist(data.whitelist || []);
    }
  } catch (err) {
    console.error('Error fetching admin whitelist list:', err);
  }
}

function renderAdminRequests(requests) {
  const container = document.getElementById('pending-requests-list');
  if (!container) return;

  const pending = requests.filter(r => r.status === 'PENDING');
  const badge = document.getElementById('admin-badge');
  if (badge) badge.innerText = pending.length;

  if (pending.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; padding: 24px; text-align: center; color: var(--text-dim); background: var(--bg-card); border-radius: var(--radius-md);">
        ✨ Geen openstaande aanvragen. Iedereen is beoordeeld!
      </div>
    `;
    return;
  }

  container.innerHTML = pending.map(r => `
    <div class="request-card">
      <div class="req-top">
        <img class="req-avatar" src="https://mc-heads.net/avatar/${encodeURIComponent(r.username)}/64" alt="${r.username}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/64'">
        <div>
          <div class="req-name">${r.username}</div>
          <div class="req-who" style="color: var(--text-dim);">Aangevraagd op ${new Date(r.createdAt).toLocaleDateString('nl-NL')}</div>
        </div>
      </div>
      <div class="req-body">
        "${r.reason || 'Geen toelichting opgegeven.'}"
      </div>
      <div class="req-actions">
        <button class="btn btn-sm btn-approve" onclick="approveRequest('${r.id}', '${r.username}')">✅ Goedkeuren</button>
        <button class="btn btn-sm btn-reject" onclick="rejectRequest('${r.id}')">❌ Afwijzen</button>
      </div>
    </div>
  `).join('');
}

function renderAdminWhitelist(list) {
  const tbody = document.getElementById('whitelist-table-body');
  if (!tbody) return;

  if (list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="3" style="text-align: center; color: var(--text-dim); padding: 18px;">Geen spelers op whitelist.</td></tr>`;
    return;
  }

  tbody.innerHTML = list.map(p => `
    <tr>
      <td>
        <div class="player-cell">
          <img class="player-avatar" src="https://mc-heads.net/avatar/${encodeURIComponent(p.name)}/64" alt="${p.name}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/64'">
          <span>${p.name}</span>
        </div>
      </td>
      <td><code style="font-family: 'JetBrains Mono', monospace; font-size: 0.8rem; color: var(--text-dim);">${p.uuid}</code></td>
      <td>
        <button class="btn btn-sm btn-secondary" style="color: var(--red); border-color: rgba(239, 68, 68, 0.3);" onclick="removeWhitelisted('${p.name}')">Verwijder 🗑️</button>
      </td>
    </tr>
  `).join('');
}

async function approveRequest(requestId, username) {
  try {
    const res = await fetch('/api/admin/whitelist/approve', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ requestId })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(`✅ ${username} is goedgekeurd & in-game gewhitelist!`);
      loadAdminData();
      fetchStats();
    } else {
      showToast(data.error || 'Fout bij goedkeuren.');
    }
  } catch (err) {
    showToast('Fout bij communicatie met admin API.');
  }
}

async function rejectRequest(requestId) {
  try {
    const res = await fetch('/api/admin/whitelist/reject', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ requestId })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast('Aanvraag afgewezen.');
      loadAdminData();
    } else {
      showToast(data.error || 'Fout bij afwijzen.');
    }
  } catch (err) {
    showToast('Fout bij communicatie met admin API.');
  }
}

async function manualAddWhitelist() {
  const input = document.getElementById('manual-wl-input');
  const username = input ? input.value.trim() : '';
  if (!username) return;

  try {
    const res = await fetch('/api/admin/whitelist/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(data.message);
      input.value = '';
      loadAdminData();
      fetchStats();
    } else {
      showToast(data.error || 'Toevoegen mislukt.');
    }
  } catch (err) {
    showToast('Fout bij toevoegen.');
  }
}

async function removeWhitelisted(username) {
  if (!confirm(`Weet je zeker dat je ${username} van de Minecraft whitelist wilt verwijderen?`)) {
    return;
  }

  try {
    const res = await fetch('/api/admin/whitelist/remove', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(`${username} is verwijderd.`);
      loadAdminData();
      fetchStats();
    } else {
      showToast(data.error || 'Verwijderen mislukt.');
    }
  } catch (err) {
    showToast('Fout bij verwijderen van whitelist.');
  }
}

// ==========================================
// LIVE STATS & LEADERBOARD
// ==========================================

async function fetchStats() {
  try {
    const res = await fetch('/api/stats');
    if (!res.ok) throw new Error('API fetch error');
    const data = await res.json();
    renderStats(data);
  } catch (err) {
    console.warn('Stats fetch error:', err);
    renderOffline();
  }
}

function renderStats(data) {
  const s = data.server || {};

  // Status pill
  const pill = document.getElementById('server-status-pill');
  const statusText = document.getElementById('status-text');
  if (pill && statusText) {
    if (s.online) {
      pill.className = 'status-pill online';
      statusText.innerText = 'ONLINE';
    } else {
      pill.className = 'status-pill offline';
      statusText.innerText = 'OFFLINE';
    }
  }

  // Friends Leaderboard & Roster
  const tbody = document.getElementById('friends-tbody');
  if (tbody && data.leaderboard) {
    if (data.leaderboard.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-dim); padding: 24px;">Nog geen survival data beschikbaar.</td></tr>`;
    } else {
      tbody.innerHTML = data.leaderboard.map(p => {
        const isOnline = (s.samplePlayers || []).some(sp => sp.id === p.uuid || sp.name?.toLowerCase() === p.username.toLowerCase());
        const avatarUrl = `https://mc-heads.net/avatar/${encodeURIComponent(p.username)}/64`;
        const rankClass = getRankBadgeClass(p.rank);
        const coords = p.stats?.urban?.coords ? `X: ${p.stats.urban.coords.x}, Z: ${p.stats.urban.coords.z}` : 'X: 0, Z: 0';
        const level = p.stats?.urban?.level || 0;
        const coins = (p.coins || 0).toLocaleString();

        return `
          <tr>
            <td>
              <div class="player-cell">
                <img class="player-avatar" src="${avatarUrl}" alt="${p.username}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/64'">
                <span>${p.username}</span>
              </div>
            </td>
            <td><span class="rank-badge ${rankClass}">${p.rank}</span></td>
            <td><strong style="color: var(--gold);">🪙 ${coins}</strong></td>
            <td><span style="font-weight: 700; color: #fff;">Lvl ${level}</span></td>
            <td><code style="font-family: 'JetBrains Mono', monospace; font-size: 0.85rem; color: var(--cyan);">${coords}</code></td>
            <td>
              <span style="display: inline-flex; align-items: center; gap: 6px; font-weight: 600; font-size: 0.85rem; color: ${isOnline ? 'var(--green)' : 'var(--text-dim)'};">
                <span style="width: 8px; height: 8px; border-radius: 50%; background: ${isOnline ? 'var(--green)' : 'var(--text-dim)'};"></span>
                ${isOnline ? 'In-Game' : 'Offline'}
              </span>
            </td>
          </tr>
        `;
      }).join('');
    }
  }
}

function renderOffline() {
  const pill = document.getElementById('server-status-pill');
  const statusText = document.getElementById('status-text');
  if (pill && statusText) {
    pill.className = 'status-pill offline';
    statusText.innerText = 'OFFLINE';
  }
}

// Initial Boot
checkAuth();
fetchStats();
setInterval(fetchStats, 5000);
