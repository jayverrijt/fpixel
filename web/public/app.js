// FPixel Stats Dashboard Application

function copyToClipboard(text, message) {
  navigator.clipboard.writeText(text).then(() => {
    showToast(message || 'Gekopieerd!');
  }).catch(() => {
    // Fallback
    const textarea = document.createElement('textarea');
    textarea.value = text;
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
    showToast(message || 'Gekopieerd!');
  });
}

function showToast(msg) {
  const toast = document.getElementById('copy-toast');
  if (!toast) return;
  toast.innerText = msg;
  toast.classList.add('show');
  setTimeout(() => {
    toast.classList.remove('show');
  }, 2200);
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
  
  // Status Badge
  const badge = document.getElementById('status-badge');
  const badgeText = document.getElementById('status-text');
  if (s.online) {
    badge.className = 'status-badge online';
    badgeText.innerText = 'ONLINE';
  } else {
    badge.className = 'status-badge offline';
    badgeText.innerText = 'OFFLINE';
  }

  // Player counts
  const onlineCount = s.playersOnline ?? 0;
  const maxCount = s.playersMax ?? 100;
  const countStr = `${onlineCount} / ${maxCount}`;
  
  document.getElementById('nav-player-count').innerText = countStr;
  document.getElementById('metric-players').innerText = countStr;

  // Progress Bar
  const pct = Math.min(100, Math.round((onlineCount / (maxCount || 1)) * 100));
  const pBar = document.getElementById('player-progress');
  if (pBar) pBar.style.width = `${Math.max(4, pct)}%`;

  // Ping & Version
  document.getElementById('metric-ping').innerText = s.online ? `${s.latency} ms` : '-- ms';
  document.getElementById('metric-version').innerText = s.version || 'Paper 26.3';

  // Gamemodes Grid
  const gmContainer = document.getElementById('gamemodes-grid');
  if (gmContainer && data.gamemodes) {
    gmContainer.innerHTML = data.gamemodes.map(g => `
      <div class="gamemode-card">
        <div class="gamemode-top">
          <span class="gamemode-icon">${g.icon}</span>
          <span class="gamemode-count">${g.count}</span>
        </div>
        <h3 class="gamemode-name">${g.name}</h3>
        <p class="gamemode-desc">${g.desc}</p>
      </div>
    `).join('');
  }

  // Leaderboard
  const lbContainer = document.getElementById('leaderboard-body');
  if (lbContainer && data.leaderboard) {
    if (data.leaderboard.length === 0) {
      lbContainer.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--text-dim); padding: 24px;">Nog geen survival data beschikbaar.</td></tr>`;
    } else {
      lbContainer.innerHTML = data.leaderboard.map(p => {
        const isOnline = (s.samplePlayers || []).some(sp => sp.id === p.uuid || sp.name?.toLowerCase() === p.name.toLowerCase());
        const avatarUrl = `https://mc-heads.net/avatar/${encodeURIComponent(p.name)}/64`;
        const rankClass = getRankBadgeClass(p.rank);
        const hearts = '❤️'.repeat(Math.min(10, Math.max(1, Math.round(p.health / 2))));

        return `
          <tr>
            <td>
              <div class="player-cell">
                <img class="player-avatar" src="${avatarUrl}" alt="${p.name}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/64'">
                <span>${p.name}</span>
              </div>
            </td>
            <td><span class="rank-badge ${rankClass}">${p.rank}</span></td>
            <td><strong style="color: var(--gold);">Lvl ${p.level}</strong></td>
            <td><span title="${p.health} HP">${hearts}</span></td>
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
  const badge = document.getElementById('status-badge');
  const badgeText = document.getElementById('status-text');
  if (badge) badge.className = 'status-badge offline';
  if (badgeText) badgeText.innerText = 'OFFLINE';
  document.getElementById('metric-ping').innerText = '-- ms';
}

// Initial fetch and auto-refresh every 5 seconds
fetchStats();
setInterval(fetchStats, 5000);
