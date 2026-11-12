const express = require('express');
const path = require('path');
const fs = require('fs');
const net = require('net');
const crypto = require('crypto');
const cookieParser = require('cookie-parser');

const app = express();
const PORT = process.env.PORT || 3000;
const MINECRAFT_HOST = process.env.MINECRAFT_HOST || '127.0.0.1';
const MINECRAFT_PORT = parseInt(process.env.MINECRAFT_PORT || '25565', 10);
const RCON_PORT = parseInt(process.env.RCON_PORT || '25575', 10);
const RCON_PASSWORD = process.env.RCON_PASSWORD || 'fpixel_rcon_2026';
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, '../server/plugins/FPixelCore');
const SERVER_DIR = process.env.SERVER_DIR || path.join(__dirname, '../server');

app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cookieParser('fpixel_secret_community_key_2026'));
app.use(express.static(path.join(__dirname, 'public')));

// In-memory sessions (token -> user)
const sessions = new Map();

// In-memory one-time verification codes (username_lower -> { code, expiresAt })
const pendingCodes = new Map();

// Helper: VarInt encode
function encodeVarInt(value) {
  const bytes = [];
  while (true) {
    if ((value & ~0x7f) === 0) {
      bytes.push(value);
      break;
    }
    bytes.push((value & 0x7f) | 0x80);
    value >>>= 7;
  }
  return Buffer.from(bytes);
}

// Helper: Read VarInt
function readVarInt(buffer, offset = 0) {
  let numRead = 0;
  let result = 0;
  let read;
  do {
    if (offset + numRead >= buffer.length) return null;
    read = buffer[offset + numRead];
    result |= (read & 0x7f) << (7 * numRead);
    numRead++;
    if (numRead > 5) return null;
  } while ((read & 0x80) !== 0);
  return { value: result, bytesRead: numRead };
}

// Ping Minecraft Server
function pingMinecraft(host, port, timeoutMs = 2500) {
  return new Promise((resolve) => {
    const startTime = Date.now();
    const socket = new net.Socket();
    let received = Buffer.alloc(0);
    let resolved = false;

    socket.setTimeout(timeoutMs);
    socket.connect(port, host);

    socket.on('connect', () => {
      try {
        const hostBuf = Buffer.from('mc.fpixel.nl', 'utf8');
        const portBuf = Buffer.alloc(2);
        portBuf.writeUInt16BE(port);

        const handshakeBody = Buffer.concat([
          encodeVarInt(0x00),
          encodeVarInt(777),
          encodeVarInt(hostBuf.length),
          hostBuf,
          portBuf,
          encodeVarInt(1)
        ]);
        const handshakePacket = Buffer.concat([
          encodeVarInt(handshakeBody.length),
          handshakeBody
        ]);

        const statusPacket = Buffer.concat([
          encodeVarInt(1),
          encodeVarInt(0x00)
        ]);

        socket.write(Buffer.concat([handshakePacket, statusPacket]));
      } catch (err) {
        if (!resolved) {
          resolved = true;
          socket.destroy();
          resolve({ online: false, error: err.message });
        }
      }
    });

    socket.on('data', (chunk) => {
      received = Buffer.concat([received, chunk]);
      try {
        const str = received.toString('utf8');
        const start = str.indexOf('{');
        const end = str.lastIndexOf('}');
        if (start !== -1 && end !== -1 && end > start) {
          const jsonStr = str.substring(start, end + 1);
          const data = JSON.parse(jsonStr);
          resolved = true;
          socket.destroy();
          resolve({
            online: true,
            latency: Date.now() - startTime,
            data
          });
        }
      } catch (e) {}
    });

    socket.on('timeout', () => {
      if (!resolved) {
        resolved = true;
        socket.destroy();
        resolve({ online: false, error: 'Connection timed out' });
      }
    });

    socket.on('error', (err) => {
      if (!resolved) {
        resolved = true;
        socket.destroy();
        resolve({ online: false, error: err.message });
      }
    });
  });
}

// Native Minecraft RCON client
function sendRconCommand(command) {
  return new Promise((resolve, reject) => {
    const rcon = new net.Socket();
    let authenticated = false;
    let reqId = 1;

    rcon.setTimeout(3000);

    function createPacket(id, type, body) {
      const bodyBuf = Buffer.from(body, 'utf8');
      const length = 4 + 4 + bodyBuf.length + 2;
      const buf = Buffer.alloc(4 + length);
      buf.writeInt32LE(length, 0);
      buf.writeInt32LE(id, 4);
      buf.writeInt32LE(type, 8);
      bodyBuf.copy(buf, 12);
      buf.writeInt8(0, 12 + bodyBuf.length);
      buf.writeInt8(0, 13 + bodyBuf.length);
      return buf;
    }

    rcon.connect(RCON_PORT, MINECRAFT_HOST, () => {
      rcon.write(createPacket(reqId++, 3, RCON_PASSWORD));
    });

    rcon.on('data', (data) => {
      if (data.length < 14) return;
      const length = data.readInt32LE(0);
      const resId = data.readInt32LE(4);

      if (!authenticated) {
        if (resId === -1) {
          rcon.destroy();
          return reject(new Error('RCON Authentication failed'));
        }
        authenticated = true;
        rcon.write(createPacket(reqId++, 2, command));
      } else {
        const body = data.toString('utf8', 12, length + 2);
        rcon.destroy();
        resolve(body.replace(/\x00+$/, '').trim());
      }
    });

    rcon.on('error', (err) => {
      rcon.destroy();
      reject(err);
    });

    rcon.on('timeout', () => {
      rcon.destroy();
      reject(new Error('RCON timed out'));
    });
  });
}

// File path helpers
function resolveFilePath(relPath) {
  const candidates = [
    path.join('/data', relPath),
    path.join(SERVER_DIR, relPath),
    path.join(__dirname, '../server', relPath)
  ];
  for (const c of candidates) {
    if (fs.existsSync(c)) return c;
  }
}

// Serve Squaremap map tiles statically
app.use('/tiles', (req, res, next) => {
  const tilesDir = resolveFilePath('plugins/squaremap/web/tiles');
  if (tilesDir && fs.existsSync(tilesDir)) {
    return express.static(tilesDir, {
      maxAge: '5m',
      etag: true,
      fallthrough: true
    })(req, res, () => {
      // If tile image not found (void or ungenerated region), return transparent 1x1 GIF
      if (req.path.endsWith('.png')) {
        const transparentGif = Buffer.from('R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7', 'base64');
        res.set('Content-Type', 'image/gif');
        res.set('Cache-Control', 'public, max-age=60');
        return res.send(transparentGif);
      }
      res.status(404).end();
    });
  }
  next();
});

// Read ranks
function getRanks() {
  const ranks = {};
  const file = resolveFilePath('plugins/FPixelCore/ranks.yml');
  if (file && fs.existsSync(file)) {
    try {
      const content = fs.readFileSync(file, 'utf8');
      for (const line of content.split('\n')) {
        const parts = line.split(':');
        if (parts.length === 2) {
          const u = parts[0].trim().toLowerCase();
          const r = parts[1].trim().toUpperCase();
          if (u && r) ranks[u] = r;
        }
      }
    } catch (e) {}
  }
  return ranks;
}

// Usercache mapping
function getUserCache() {
  const cache = {};
  const file = resolveFilePath('usercache.json');
  if (file && fs.existsSync(file)) {
    try {
      const data = JSON.parse(fs.readFileSync(file, 'utf8'));
      if (Array.isArray(data)) {
        for (const entry of data) {
          if (entry.uuid && entry.name) {
            cache[entry.uuid.toLowerCase()] = entry.name;
          }
        }
      }
    } catch (e) {}
  }
  return cache;
}

// Resolved player ranks (resolves UUIDs to actual player names and avoids duplicate/broken badges)
function getResolvedPlayerRanks() {
  const rawRanks = getRanks();
  const cache = getUserCache();
  const whitelist = getWhitelist();

  const uuidToName = {};
  for (const [uuid, name] of Object.entries(cache)) {
    uuidToName[uuid.toLowerCase()] = name;
  }
  for (const w of whitelist) {
    if (w.uuid && w.name) {
      uuidToName[w.uuid.toLowerCase()] = w.name;
    }
  }

  const result = [];
  const seenUsernames = new Map();

  for (const [key, rank] of Object.entries(rawRanks)) {
    let resolvedName = key;
    let isUuid = false;

    if (key.includes('-')) {
      isUuid = true;
      if (uuidToName[key.toLowerCase()]) {
        resolvedName = uuidToName[key.toLowerCase()];
      }
    }

    const cleanLower = resolvedName.toLowerCase().replace(/^\./, '');

    if (seenUsernames.has(cleanLower)) {
      const existing = seenUsernames.get(cleanLower);
      existing.rawKeys.push(key);
      continue;
    }

    const item = {
      username: resolvedName,
      cleanName: cleanLower,
      rank: rank,
      rawKeys: [key],
      isUuid: isUuid && (resolvedName === key)
    };
    seenUsernames.set(cleanLower, item);
    result.push(item);
  }

  // Also include whitelisted players that are not explicitly assigned a rank (they are MEMBER)
  for (const w of whitelist) {
    if (w.name) {
      const cleanLower = w.name.toLowerCase().replace(/^\./, '');
      if (!seenUsernames.has(cleanLower)) {
        const item = {
          username: w.name,
          cleanName: cleanLower,
          rank: 'MEMBER',
          rawKeys: [w.name.toLowerCase()],
          isUuid: false
        };
        seenUsernames.set(cleanLower, item);
        result.push(item);
      }
    }
  }

  const rankPriority = {
    'OWNER': 100,
    'ADMIN': 90,
    'MOD': 80,
    'YOUTUBE': 70,
    'MVP_PLUS': 60,
    'MVP': 50,
    'VIP_PLUS': 40,
    'VIP': 30,
    'MEMBER': 10
  };

  result.sort((a, b) => {
    const pA = rankPriority[a.rank] || 0;
    const pB = rankPriority[b.rank] || 0;
    if (pB !== pA) return pB - pA;
    return a.username.localeCompare(b.username);
  });

  return result;
}

// Offline UUID helper (matches Java nameUUIDFromBytes("OfflinePlayer:" + name))
function getOfflinePlayerUUID(username) {
  const hash = crypto.createHash('md5').update('OfflinePlayer:' + username, 'utf8').digest();
  hash[6] = (hash[6] & 0x0f) | 0x30; // Version 3
  hash[8] = (hash[8] & 0x3f) | 0x80; // Variant RFC 4122
  const hex = hash.toString('hex');
  return `${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}`;
}

// Whitelist helper
function getWhitelist() {
  const file = resolveFilePath('whitelist.json');
  if (fs.existsSync(file)) {
    try {
      return JSON.parse(fs.readFileSync(file, 'utf8')) || [];
    } catch (e) {
      return [];
    }
  }
  return [];
}

function saveWhitelist(list) {
  const file = resolveFilePath('whitelist.json');
  try {
    fs.writeFileSync(file, JSON.stringify(list, null, 2), 'utf8');
    return true;
  } catch (e) {
    console.error('Failed to save whitelist.json:', e);
    return false;
  }
}

// Custom Parkour Maps helper
function getParkourMaps() {
  const file = resolveFilePath('plugins/FPixelCore/custom_parkour.json');
  if (file && fs.existsSync(file)) {
    try {
      return JSON.parse(fs.readFileSync(file, 'utf8')) || [];
    } catch (e) {
      console.error('Failed to read custom_parkour.json:', e);
      return [];
    }
  }
  return [];
}

function saveParkourMaps(maps) {
  const file = resolveFilePath('plugins/FPixelCore/custom_parkour.json') || path.join(SERVER_DIR, 'plugins/FPixelCore/custom_parkour.json');
  try {
    const dir = path.dirname(file);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(file, JSON.stringify(maps, null, 2), 'utf8');
    return true;
  } catch (e) {
    console.error('Failed to save custom_parkour.json:', e);
    return false;
  }
}

// Whitelist requests helper
function getWhitelistRequests() {
  const file = resolveFilePath('whitelist_requests.json');
  if (fs.existsSync(file)) {
    try {
      return JSON.parse(fs.readFileSync(file, 'utf8')) || [];
    } catch (e) {
      return [];
    }
  }
  return [];
}

function saveWhitelistRequests(requests) {
  const file = resolveFilePath('whitelist_requests.json');
  try {
    fs.writeFileSync(file, JSON.stringify(requests, null, 2), 'utf8');
    return true;
  } catch (e) {
    console.error('Failed to save whitelist_requests.json:', e);
    return false;
  }
}

// Web Authentication & Password Management
function getWebAuth() {
  const file = resolveFilePath('plugins/FPixelCore/web_auth.json');
  if (fs.existsSync(file)) {
    try {
      return JSON.parse(fs.readFileSync(file, 'utf8')) || {};
    } catch (e) {
      return {};
    }
  }
  return {};
}

function saveWebAuth(authData) {
  const file = resolveFilePath('plugins/FPixelCore/web_auth.json');
  try {
    const dir = path.dirname(file);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(file, JSON.stringify(authData, null, 2), 'utf8');
    return true;
  } catch (e) {
    console.error('Failed to save web_auth.json:', e);
    return false;
  }
}

function hashPassword(password, existingSalt = null) {
  const salt = existingSalt || crypto.randomBytes(16).toString('hex');
  const hash = crypto.createHash('sha256').update(salt + password).digest('hex');
  return { salt, hash };
}

function verifyPassword(password, salt, storedHash) {
  try {
    const computedHash = crypto.createHash('sha256').update(salt + password).digest('hex');
    return crypto.timingSafeEqual(Buffer.from(computedHash, 'hex'), Buffer.from(storedHash, 'hex'));
  } catch (e) {
    return false;
  }
}

// Read Player Full Profile (Coins, Level, Survival Data, Inventory)
function getPlayerProfile(username) {
  const lowerName = username.toLowerCase();
  const ranks = getRanks();
  const rank = ranks[lowerName] || 'DEFAULT';
  const isAdmin = (rank === 'OWNER' || rank === 'ADMIN');
  const userCache = getUserCache();

  // Find UUID
  let uuid = null;
  for (const [id, n] of Object.entries(userCache)) {
    if (n.toLowerCase() === lowerName) {
      uuid = id;
      break;
    }
  }

  let coins = 100;
  let minigameStats = {};
  let urbanCoords = null;
  let urbanLevel = 0;
  let urbanHealth = 20;

  // 1. Read Userdata
  if (uuid) {
    const userdataFile = resolveFilePath(`plugins/FPixelCore/userdata/${uuid}.yml`);
    if (fs.existsSync(userdataFile)) {
      try {
        const lines = fs.readFileSync(userdataFile, 'utf8').split('\n');
        for (const l of lines) {
          const [k, v] = l.split(':').map(s => s.trim());
          if (k === 'coins' && v) coins = parseInt(v, 10) || 100;
        }
      } catch (e) {}
    }

    // 2. Read Survival Data
    const survivalFile = resolveFilePath(`plugins/FPixelCore/survival_data/${uuid}.yml`);
    if (fs.existsSync(survivalFile)) {
      try {
        const lines = fs.readFileSync(survivalFile, 'utf8').split('\n');
        let x = 0, y = 0, z = 0;
        for (const l of lines) {
          const [k, v] = l.split(':').map(s => s.trim());
          if (k === 'x' && v) x = Math.round(parseFloat(v));
          if (k === 'y' && v) y = Math.round(parseFloat(v));
          if (k === 'z' && v) z = Math.round(parseFloat(v));
          if (k === 'level' && v) urbanLevel = parseInt(v, 10) || 0;
          if (k === 'health' && v) urbanHealth = Math.round(parseFloat(v) || 20);
        }
        urbanCoords = { x, y, z };
      } catch (e) {}
    }
  }

  // 3. Fallback: Check imported survival profiles
  const importedDir = resolveFilePath('plugins/FPixelCore/survival_data/imported');
  if (fs.existsSync(importedDir)) {
    try {
      const files = fs.readdirSync(importedDir);
      for (const f of files) {
        if (f.endsWith('.json')) {
          const data = JSON.parse(fs.readFileSync(path.join(importedDir, f), 'utf8'));
          if (data.username && data.username.toLowerCase() === lowerName) {
            if (!uuid) uuid = data.offline_uuid || data.old_uuid;
            if (!urbanCoords) urbanCoords = { x: Math.round(data.x), y: Math.round(data.y), z: Math.round(data.z) };
            if (urbanLevel === 0) urbanLevel = data.level || 0;
            if (urbanHealth === 20 && data.health) urbanHealth = Math.round(data.health);
            break;
          }
        }
      }
    } catch (e) {}
  }

  return {
    username,
    uuid: uuid || '00000000-0000-0000-0000-000000000000',
    rank,
    isAdmin,
    coins,
    stats: {
      urban: {
        worldName: 'Urban SMP',
        coords: urbanCoords || { x: -653, y: -22, z: -116 },
        level: urbanLevel || 68,
        health: urbanHealth || 20
      },
      galaxy: {
        worldName: 'Galaxy Survival',
        coords: { x: 0, y: 70, z: 0 },
        level: urbanLevel || 68
      }
    }
  };
}

// Authentication middleware
function getSessionUser(req) {
  const token = req.cookies.fpixel_session;
  if (token && sessions.has(token)) {
    return sessions.get(token);
  }
  return null;
}

// API: /api/me (Current user)
app.get('/api/me', (req, res) => {
  const user = getSessionUser(req);
  if (!user) {
    return res.json({ loggedIn: false });
  }
  // Refresh profile from disk
  const fresh = getPlayerProfile(user.username);
  sessions.set(req.cookies.fpixel_session, fresh);
  res.json({ loggedIn: true, user: fresh });
});

// API: /api/login (Password protected)
app.post('/api/login', (req, res) => {
  const { username, password } = req.body;
  if (!username || username.trim().length < 2) {
    return res.status(400).json({ error: 'Vul een geldige Minecraft gebruikersnaam in.' });
  }
  if (!password || password.trim().length === 0) {
    return res.status(400).json({ error: 'Vul je wachtwoord in om in te loggen.' });
  }

  const cleanName = username.trim();
  const authData = getWebAuth();
  const userAuth = authData[cleanName.toLowerCase()];

  if (!userAuth) {
    return res.status(401).json({
      error: 'Voor dit account is nog geen wachtwoord ingesteld. Gebruik in Minecraft het commando /webpass <wachtwoord> of vraag hieronder een in-game verificatiecode aan.'
    });
  }

  const isValid = verifyPassword(password, userAuth.salt, userAuth.hash);
  if (!isValid) {
    return res.status(401).json({ error: 'Onjuist wachtwoord voor ' + cleanName + '.' });
  }

  const profile = getPlayerProfile(cleanName);
  const token = 'token_' + crypto.randomBytes(24).toString('hex');
  sessions.set(token, profile);

  res.cookie('fpixel_session', token, {
    maxAge: 30 * 24 * 60 * 60 * 1000,
    httpOnly: true,
    sameSite: 'lax'
  });

  res.json({ success: true, user: profile });
});

// API: /api/auth/send-code (Sends 6-digit verification code to player in-game via RCON)
app.post('/api/auth/send-code', async (req, res) => {
  const { username } = req.body;
  if (!username || username.trim().length < 2) {
    return res.status(400).json({ error: 'Vul een geldige Minecraft gebruikersnaam in.' });
  }

  const cleanName = username.trim();
  const code = Math.floor(100000 + Math.random() * 900000).toString();

  try {
    const tellrawJson = JSON.stringify([
      { text: "[FPixel Web] ", color: "gold", bold: true },
      { text: "Jouw eenmalige verificatiecode is: ", color: "yellow" },
      { text: code, color: "green", bold: true, underlined: true },
      { text: " (5 min geldig). Deel deze nooit!", color: "gray" }
    ]);
    const rconRes = await sendRconCommand(`tellraw ${cleanName} ${tellrawJson}`);
    if (rconRes && rconRes.toLowerCase().includes('no player was found')) {
      return res.status(404).json({
        error: `Speler '${cleanName}' is momenteel niet online op mc.fpixel.nl. Log eerst in op de Minecraft server om de code in je chat te ontvangen, of gebruik in Minecraft het commando /webpass <wachtwoord>.`
      });
    }

    pendingCodes.set(cleanName.toLowerCase(), {
      code,
      expiresAt: Date.now() + 5 * 60 * 1000
    });

    res.json({
      success: true,
      message: `Verificatiecode verzonden naar Minecraft chat van ${cleanName}! Kijk in-game op je scherm.`
    });
  } catch (err) {
    res.status(500).json({
      error: 'Kan geen verificatiecode sturen: controleer of de server online is of gebruik in-game /webpass <wachtwoord>.'
    });
  }
});

// API: /api/auth/verify-code (Verifies in-game code and sets new password)
app.post('/api/auth/verify-code', (req, res) => {
  const { username, code, password } = req.body;
  if (!username || !code || !password) {
    return res.status(400).json({ error: 'Vul alle velden in (gebruikersnaam, verificatiecode en nieuw wachtwoord).' });
  }
  if (password.length < 4) {
    return res.status(400).json({ error: 'Wachtwoord moet minimaal 4 tekens bevatten.' });
  }

  const cleanName = username.trim();
  const pending = pendingCodes.get(cleanName.toLowerCase());

  if (!pending || pending.code !== code.trim()) {
    return res.status(400).json({ error: 'Ongeldige verificatiecode. Vraag eventueel een nieuwe code aan.' });
  }
  if (Date.now() > pending.expiresAt) {
    pendingCodes.delete(cleanName.toLowerCase());
    return res.status(400).json({ error: 'Deze verificatiecode is verlopen. Vraag een nieuwe code aan.' });
  }

  pendingCodes.delete(cleanName.toLowerCase());

  // Save hashed password
  const authData = getWebAuth();
  const { salt, hash } = hashPassword(password);
  authData[cleanName.toLowerCase()] = {
    username: cleanName,
    salt,
    hash,
    updatedAt: new Date().toISOString()
  };
  saveWebAuth(authData);

  // Automatically authenticate user
  const profile = getPlayerProfile(cleanName);
  const token = 'token_' + crypto.randomBytes(24).toString('hex');
  sessions.set(token, profile);

  res.cookie('fpixel_session', token, {
    maxAge: 30 * 24 * 60 * 60 * 1000,
    httpOnly: true,
    sameSite: 'lax'
  });

  res.json({
    success: true,
    user: profile,
    message: 'Wachtwoord succesvol ingesteld! Je bent nu veilig ingelogd.'
  });
});

// API: /api/logout
app.post('/api/logout', (req, res) => {
  const token = req.cookies.fpixel_session;
  if (token) sessions.delete(token);
  res.clearCookie('fpixel_session');
  res.json({ success: true });
});

// API: /api/stats (Live overview)
app.get('/api/stats', async (req, res) => {
  const pingRes = await pingMinecraft(MINECRAFT_HOST, MINECRAFT_PORT);
  const ranks = getRanks();
  const userCache = getUserCache();
  const whitelist = getWhitelist();

  // Load friend group members
  const friendProfiles = [];
  const knownFriends = ['Jay_V0', 'SmileyGaming27', 'Amigapower2018', '.minnieheks', '.Smileygaming27'];

  for (const name of knownFriends) {
    const prof = getPlayerProfile(name);
    friendProfiles.push(prof);
  }

  friendProfiles.sort((a, b) => b.stats.urban.level - a.stats.urban.level);

  res.json({
    server: {
      name: 'FPixel SMP & Minigames',
      domain: 'mc.fpixel.nl',
      ip: '85.215.230.22',
      javaPort: 25565,
      bedrockPort: 19132,
      online: pingRes.online,
      latency: pingRes.latency || 0,
      version: pingRes.online ? (pingRes.data.version?.name || 'Paper 26.3') : 'Paper 26.3',
      playersOnline: pingRes.online ? (pingRes.data.players?.online ?? 0) : 0,
      playersMax: pingRes.online ? (pingRes.data.players?.max ?? 100) : 100,
      samplePlayers: pingRes.online ? (pingRes.data.players?.sample || []) : [],
      whitelistEnabled: true,
      whitelistedCount: whitelist.length
    },
    leaderboard: friendProfiles
  });
});

// API: /api/map/data (Interactive survival map dimensions & metadata - strictly no markers)
app.get('/api/map/data', (req, res) => {
  res.json({
    worlds: [
      {
        id: 'survival',
        tilePath: 'minecraft_survival',
        name: 'Overworld (Survival)',
        dimension: 'overworld',
        icon: '🌍',
        desc: 'De officiële survival wereld van de vriendengroep',
        spawn: { x: 240, z: -256 },
        zoom: { def: 2, min: 0, max: 3, extra: 2 },
        background: '#0d1527'
      },
      {
        id: 'nether',
        tilePath: 'minecraft_the_nether',
        name: 'The Nether',
        dimension: 'nether',
        icon: '🔥',
        desc: 'De Nether dimensie van de survival wereld',
        spawn: { x: 0, z: 0 },
        zoom: { def: 2, min: 0, max: 3, extra: 2 },
        background: '#180808'
      },
      {
        id: 'end',
        tilePath: 'minecraft_the_end',
        name: 'The End',
        dimension: 'the_end',
        icon: '🌌',
        desc: 'The End dimensie van de survival wereld',
        spawn: { x: 0, z: 0 },
        zoom: { def: 2, min: 0, max: 3, extra: 2 },
        background: '#08040d'
      }
    ]
  });
});

// API: /api/whitelist/request (Submit new whitelist request)
app.post('/api/whitelist/request', (req, res) => {
  const { username, reason } = req.body;
  if (!username || username.trim().length < 2) {
    return res.status(400).json({ error: 'Vul een geldige Minecraft gebruikersnaam in.' });
  }

  const cleanName = username.trim();
  const requests = getWhitelistRequests();
  const whitelist = getWhitelist();

  // Check if already on whitelist
  if (whitelist.some(w => w.name && w.name.toLowerCase() === cleanName.toLowerCase())) {
    return res.status(400).json({ error: `${cleanName} staat al op de whitelist van de server!` });
  }

  // Check if already pending
  const existing = requests.find(r => r.username.toLowerCase() === cleanName.toLowerCase() && r.status === 'PENDING');
  if (existing) {
    return res.status(400).json({ error: `Er loopt al een aanvraag voor ${cleanName}. Even geduld a.u.b.!` });
  }

  const newRequest = {
    id: 'req_' + Date.now().toString(36) + Math.random().toString(36).substring(2, 6),
    username: cleanName,
    reason: reason ? reason.trim() : '',
    status: 'PENDING',
    createdAt: new Date().toISOString()
  };

  requests.unshift(newRequest);
  saveWhitelistRequests(requests);

  res.json({
    success: true,
    message: `Aanvraag succesvol verstuurd voor ${cleanName}! Een admin keurt deze spoedig goed.`
  });
});

// API: /api/admin/whitelist/requests (Admin: get all requests)
app.get('/api/admin/whitelist/requests', (req, res) => {
  const user = getSessionUser(req);
  if (!user || !user.isAdmin) {
    return res.status(403).json({ error: 'Alleen admins (ADMIN of OWNER rank) hebben toegang.' });
  }
  const requests = getWhitelistRequests();
  res.json({ requests });
});

// API: /api/admin/whitelist/approve (Admin: approve request)
app.post('/api/admin/whitelist/approve', async (req, res) => {
  const user = getSessionUser(req);
  if (!user || !user.isAdmin) {
    return res.status(403).json({ error: 'Geen admin rechten.' });
  }

  const { requestId } = req.body;
  const requests = getWhitelistRequests();
  const target = requests.find(r => r.id === requestId);

  if (!target) {
    return res.status(404).json({ error: 'Aanvraag niet gevonden.' });
  }

  // Add to whitelist.json with proper offline UUID
  const whitelist = getWhitelist();
  const offlineUuid = getOfflinePlayerUUID(target.username);
  if (!whitelist.some(w => w.uuid === offlineUuid || (w.name && w.name.toLowerCase() === target.username.toLowerCase()))) {
    whitelist.push({
      uuid: offlineUuid,
      name: target.username
    });
    saveWhitelist(whitelist);
  }

  // Execute in Minecraft via RCON (neutral broadcast, no name dropping)
  try {
    await sendRconCommand(`whitelist add ${target.username}`);
    await sendRconCommand('whitelist reload');
    await sendRconCommand(`say §a[Whitelist] §e${target.username}§a is goedgekeurd en toegevoegd aan de whitelist! Welkom!`);
  } catch (err) {
    console.warn('RCON notification warning:', err.message);
  }

  target.status = 'APPROVED';
  target.reviewedBy = user.username;
  target.reviewedAt = new Date().toISOString();
  saveWhitelistRequests(requests);

  res.json({
    success: true,
    message: `${target.username} is goedgekeurd en toegevoegd aan de Minecraft whitelist!`
  });
});

// API: /api/admin/whitelist/reject (Admin: reject request)
app.post('/api/admin/whitelist/reject', (req, res) => {
  const user = getSessionUser(req);
  if (!user || !user.isAdmin) {
    return res.status(403).json({ error: 'Geen admin rechten.' });
  }

  const { requestId } = req.body;
  const requests = getWhitelistRequests();
  const target = requests.find(r => r.id === requestId);

  if (!target) {
    return res.status(404).json({ error: 'Aanvraag niet gevonden.' });
  }

  target.status = 'REJECTED';
  target.reviewedBy = user.username;
  target.reviewedAt = new Date().toISOString();
  saveWhitelistRequests(requests);

  res.json({ success: true, message: `Aanvraag voor ${target.username} afgewezen.` });
});

// API: /api/admin/whitelist/request/delete (Admin: permanently delete request)
app.post('/api/admin/whitelist/request/delete', (req, res) => {
  const user = getSessionUser(req);
  if (!user || !user.isAdmin) {
    return res.status(403).json({ error: 'Geen admin rechten.' });
  }

  const { requestId } = req.body;
  let requests = getWhitelistRequests();
  const initialCount = requests.length;
  requests = requests.filter(r => r.id !== requestId);

  if (requests.length === initialCount) {
    return res.status(404).json({ error: 'Aanvraag niet gevonden om te verwijderen.' });
  }

  saveWhitelistRequests(requests);
  res.json({ success: true, message: 'Aanvraag definitief verwijderd.' });
});

// API: /api/admin/whitelist/requests/clear (Admin: clear all fake/stale requests or all requests)
app.post('/api/admin/whitelist/requests/clear', (req, res) => {
  const user = getSessionUser(req);
  if (!user || !user.isAdmin) {
    return res.status(403).json({ error: 'Geen admin rechten.' });
  }

  const { all } = req.body;
  if (all) {
    saveWhitelistRequests([]);
    return res.json({ success: true, message: 'Alle aanvragen zijn succesvol gewist.' });
  }

  // Purge test / non-pending / fake requests
  let requests = getWhitelistRequests();
  const filtered = requests.filter(r => {
    const u = (r.username || '').toLowerCase();
    return r.status === 'PENDING' && u !== 'gaysteve' && u !== 'testfriend';
  });

  saveWhitelistRequests(filtered);
  res.json({
    success: true,
    message: `Test en afgehandelde aanvragen opgeschoond. (${requests.length - filtered.length} verwijderd)`
  });
});

// API: /api/admin/whitelist/list (Admin: get current whitelist)
app.get('/api/admin/whitelist/list', (req, res) => {
  const user = getSessionUser(req);
  if (!user || !user.isAdmin) {
    return res.status(403).json({ error: 'Geen admin rechten.' });
  }
  res.json({ whitelist: getWhitelist() });
});

// API: /api/admin/whitelist/add (Admin: manually add player)
app.post('/api/admin/whitelist/add', async (req, res) => {
  const user = getSessionUser(req);
  if (!user || !user.isAdmin) {
    return res.status(403).json({ error: 'Geen admin rechten.' });
  }

  const { username } = req.body;
  if (!username || username.trim().length < 2) {
    return res.status(400).json({ error: 'Ongeldige gebruikersnaam.' });
  }

  const cleanName = username.trim();
  const whitelist = getWhitelist();
  if (whitelist.some(w => w.name && w.name.toLowerCase() === cleanName.toLowerCase())) {
    return res.status(400).json({ error: `${cleanName} staat al op de whitelist.` });
  }

  whitelist.push({
    uuid: '00000000-0000-0000-0000-' + Math.random().toString(16).substring(2, 14).padEnd(12, '0'),
    name: cleanName
  });
  saveWhitelist(whitelist);

  try {
    await sendRconCommand(`whitelist add ${cleanName}`);
    await sendRconCommand('whitelist reload');
  } catch (err) {
    console.warn('RCON manual add warning:', err.message);
  }

  res.json({ success: true, message: `${cleanName} succesvol toegevoegd aan de whitelist.` });
});

// API: /api/admin/whitelist/remove (Admin: remove from whitelist)
app.post('/api/admin/whitelist/remove', async (req, res) => {
  const user = getSessionUser(req);
  if (!user || !user.isAdmin) {
    return res.status(403).json({ error: 'Geen admin rechten.' });
  }

  const { username } = req.body;
  let whitelist = getWhitelist();
  whitelist = whitelist.filter(w => w.name && w.name.toLowerCase() !== username.toLowerCase());
  saveWhitelist(whitelist);

  try {
    await sendRconCommand(`whitelist remove ${username}`);
    await sendRconCommand('whitelist reload');
  } catch (err) {}

  res.json({ success: true, message: `${username} verwijderd van whitelist.` });
});

// ==========================================
// PARKOUR STUDIO & CMS MAP REVIEWS
// ==========================================

// API: /api/parkour/maps (Get all custom parkour maps)
app.get('/api/parkour/maps', (req, res) => {
  const user = getSessionUser(req);
  const maps = getParkourMaps();
  const isOwner = Boolean(user && user.rank === 'OWNER');
  res.json({
    maps,
    isOwner,
    isAdmin: Boolean(user && user.isAdmin)
  });
});

// API: /api/parkour/approve (Owner only: approve and activate map)
app.post('/api/parkour/approve', async (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.rank !== 'OWNER') {
    return res.status(403).json({ error: 'Alleen spelers met de [OWNER] rang kunnen parkour maps goedkeuren.' });
  }

  const { id } = req.body;
  const maps = getParkourMaps();
  const searchId = (id || '').toLowerCase().trim();
  const map = maps.find(m => 
    (m.id && m.id.toLowerCase() === searchId) ||
    (m.name && m.name.toLowerCase() === searchId) ||
    (m.id && m.id.toLowerCase() === 'pk-' + searchId)
  );

  if (!map) {
    return res.status(404).json({ error: 'Parkour map niet gevonden.' });
  }

  map.status = 'APPROVED';
  map.reviewer = user.username;
  map.reviewedAt = Date.now();
  saveParkourMaps(maps);

  // Send RCON command to Minecraft server to immediately synchronize & register the arena live
  try {
    await sendRconCommand('fpixel parkour reload');
    await sendRconCommand(`say §6[PARKOUR] §eMap §a"${map.name}" §7(${map.id}) §eis goedgekeurd door §c${user.username} §een is nu speelbaar!`);
  } catch (err) {
    console.warn('RCON parkour approve warning:', err.message);
  }

  res.json({
    success: true,
    message: `Parkour map "${map.name}" (${map.id}) is succesvol goedgekeurd en geactiveerd in de minigames!`
  });
});

// API: /api/parkour/disable (Owner only: take map out of circulation)
app.post('/api/parkour/disable', async (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.rank !== 'OWNER') {
    return res.status(403).json({ error: 'Alleen spelers met de [OWNER] rang kunnen parkour maps uit circulatie halen.' });
  }

  const { id } = req.body;
  const maps = getParkourMaps();
  const searchId = (id || '').toLowerCase().trim();
  const map = maps.find(m => 
    (m.id && m.id.toLowerCase() === searchId) ||
    (m.name && m.name.toLowerCase() === searchId) ||
    (m.id && m.id.toLowerCase() === 'pk-' + searchId)
  );

  if (!map) {
    return res.status(404).json({ error: 'Parkour map niet gevonden.' });
  }

  map.status = 'DISABLED';
  map.disabledBy = user.username;
  map.disabledAt = Date.now();
  saveParkourMaps(maps);

  // Send RCON command to Minecraft server to immediately unregister the arena live
  try {
    await sendRconCommand(`fpixel parkour disable ${map.id}`);
    await sendRconCommand(`say §6[PARKOUR] §eMap §7"${map.name}" (${map.id}) §cis tijdelijk uit circulatie gehaald.`);
  } catch (err) {
    console.warn('RCON parkour disable warning:', err.message);
  }

  res.json({
    success: true,
    message: `Parkour map "${map.name}" (${map.id}) is uit circulatie gehaald.`
  });
});

// API: /api/parkour/reject (Owner only: reject map)
app.post('/api/parkour/reject', async (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.rank !== 'OWNER') {
    return res.status(403).json({ error: 'Alleen spelers met de [OWNER] rang kunnen parkour maps afkeuren.' });
  }

  const { id, reason } = req.body;
  const maps = getParkourMaps();
  const searchId = (id || '').toLowerCase().trim();
  const map = maps.find(m => 
    (m.id && m.id.toLowerCase() === searchId) ||
    (m.name && m.name.toLowerCase() === searchId) ||
    (m.id && m.id.toLowerCase() === 'pk-' + searchId)
  );

  if (!map) {
    return res.status(404).json({ error: 'Parkour map niet gevonden.' });
  }

  map.status = 'REJECTED';
  map.reviewer = user.username;
  map.reviewedAt = Date.now();
  map.rejectReason = reason || 'Niet goedgekeurd door eigenaar';
  saveParkourMaps(maps);

  try {
    await sendRconCommand('fpixel parkour reload');
  } catch (err) {}

  res.json({
    success: true,
    message: `Parkour map "${map.name}" (${map.id}) is gemarkeerd als afgewezen.`
  });
});

// ==========================================
// RANK MANAGEMENT (CMS / OWNER CONTROLLED)
// ==========================================

function saveRanks(ranks) {
  const file = resolveFilePath('plugins/FPixelCore/ranks.yml') || path.join(SERVER_DIR, 'plugins/FPixelCore/ranks.yml');
  if (!file) return false;
  try {
    const dir = path.dirname(file);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    const lines = [];
    for (const [u, r] of Object.entries(ranks)) {
      if (u && r) lines.push(`${u}: ${r.toUpperCase()}`);
    }
    fs.writeFileSync(file, lines.join('\n') + '\n', 'utf8');
    return true;
  } catch (e) {
    console.error('Failed to save ranks.yml:', e);
    return false;
  }
}

// API: /api/admin/ranks (Get all player ranks - Admin & Owner can view)
app.get('/api/admin/ranks', (req, res) => {
  const user = getSessionUser(req);
  if (!user || !user.isAdmin) {
    return res.status(403).json({ error: 'Geen toegang tot beheerderspaneel.' });
  }

  const isOwner = Boolean(user && user.rank === 'OWNER');
  const players = getResolvedPlayerRanks();
  const rawRanks = getRanks();
  const whitelist = getWhitelist();

  res.json({
    success: true,
    players,
    ranks: rawRanks,
    whitelist,
    isOwner,
    availableRanks: ['OWNER', 'ADMIN', 'MOD', 'YOUTUBE', 'MVP_PLUS', 'MVP', 'VIP_PLUS', 'VIP', 'MEMBER']
  });
});

// API: /api/admin/ranks/set (Owner sets player rank)
app.post('/api/admin/ranks/set', async (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.rank !== 'OWNER') {
    return res.status(403).json({ error: 'Alleen de [OWNER] kan rangen aanpassen.' });
  }

  const { username, rank } = req.body;
  if (!username || !rank) {
    return res.status(400).json({ error: 'Vul een gebruikersnaam en rang in.' });
  }

  const validRanks = ['OWNER', 'ADMIN', 'MOD', 'YOUTUBE', 'MVP_PLUS', 'MVP', 'VIP_PLUS', 'VIP', 'MEMBER'];
  const cleanRank = rank.trim().toUpperCase();
  if (!validRanks.includes(cleanRank)) {
    return res.status(400).json({ error: 'Ongeldige rang: ' + cleanRank });
  }

  const cleanUser = username.trim();
  const ranks = getRanks();
  ranks[cleanUser.toLowerCase()] = cleanRank;

  // Also sync matching UUIDs so Bukkit UUID lookup doesn't override with old rank
  const whitelist = getWhitelist();
  const cache = getUserCache();
  for (const w of whitelist) {
    if (w.name && w.name.toLowerCase() === cleanUser.toLowerCase() && w.uuid) {
      ranks[w.uuid.toLowerCase()] = cleanRank;
    }
  }
  for (const [uuid, name] of Object.entries(cache)) {
    if (name && name.toLowerCase() === cleanUser.toLowerCase()) {
      ranks[uuid.toLowerCase()] = cleanRank;
    }
  }

  saveRanks(ranks);

  // Sync to Minecraft live via RCON
  try {
    await sendRconCommand(`setrank ${cleanUser} ${cleanRank}`);
    await sendRconCommand(`say §6[RANG] §e${cleanUser} heeft nu de rang §a[${cleanRank}] §egekregen van §c${user.username}§e!`);
  } catch (err) {
    console.warn('RCON setrank warning:', err.message);
  }

  res.json({
    success: true,
    message: `Rang van ${cleanUser} is succesvol gewijzigd naar [${cleanRank}].`
  });
});

// API: /api/admin/ranks/reset (Owner resets player rank to MEMBER)
app.post('/api/admin/ranks/reset', async (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.rank !== 'OWNER') {
    return res.status(403).json({ error: 'Alleen de [OWNER] kan rangen intrekken.' });
  }

  const { username } = req.body;
  if (!username) {
    return res.status(400).json({ error: 'Vul een gebruikersnaam in.' });
  }

  const cleanUser = username.trim().toLowerCase();
  const ranks = getRanks();
  delete ranks[cleanUser];

  const whitelist = getWhitelist();
  const cache = getUserCache();
  for (const w of whitelist) {
    if (w.name && w.name.toLowerCase() === cleanUser && w.uuid) {
      delete ranks[w.uuid.toLowerCase()];
    }
  }
  for (const [uuid, name] of Object.entries(cache)) {
    if (name && name.toLowerCase() === cleanUser) {
      delete ranks[uuid.toLowerCase()];
    }
  }

  saveRanks(ranks);

  try {
    await sendRconCommand(`setrank ${username} MEMBER`);
    await sendRconCommand(`say §6[RANG] §eRang van ${username} is ingetrokken door §c${user.username}§e (nu [MEMBER]).`);
  } catch (err) {}

  res.json({
    success: true,
    message: `Rang van ${username} is succesvol ingetrokken (teruggezet naar MEMBER).`
  });
});

// ==========================================
// SERVER CONTROLS (STOP & BACKUP)
// ==========================================

// API: /api/server/stop (Owner only: safely stop server)
app.post('/api/server/stop', async (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.rank !== 'OWNER') {
    return res.status(403).json({ error: 'Alleen spelers met de [OWNER] rang kunnen de server stoppen.' });
  }

  try {
    await sendRconCommand(`say §c§l[SERVER WAARSCHUWING] §eDe Minecraft server wordt over 5 seconden gestopt door Eigenaar ${user.username} via mc.fpixel.nl!`);
    setTimeout(async () => {
      try {
        await sendRconCommand('stop');
      } catch (e) {}
    }, 4000);

    res.json({
      success: true,
      message: 'Server stop signaal succesvol verzonden. De server sluit over enkele seconden veilig af.'
    });
  } catch (err) {
    res.status(500).json({ error: 'Kon server stop niet uitvoeren: ' + err.message });
  }
});

// API: /api/server/backup (Admin or Owner: trigger server snapshot)
app.post('/api/server/backup', async (req, res) => {
  const user = getSessionUser(req);
  const isAllowed = user && (user.rank === 'OWNER' || user.rank === 'ADMIN');
  if (!isAllowed) {
    return res.status(403).json({ error: 'Minimaal [ADMIN] rang vereist om een server backup te starten.' });
  }

  try {
    // Announce and save world chunks first
    try {
      await sendRconCommand(`say §6§l[BACKUP] §eHandmatige server backup gestart door ${user.username} via mc.fpixel.nl...`);
      await sendRconCommand('save-all');
    } catch (e) {}

    // Check for backup script
    const backupScript = path.resolve(__dirname, '../scripts/backup.sh');
    if (fs.existsSync(backupScript)) {
      const { exec } = require('child_process');
      exec(`bash "${backupScript}"`, (err, stdout, stderr) => {
        if (err) console.error('Backup script error:', err);
        else console.log('Manual backup finished successfully:', stdout);
      });
      return res.json({
        success: true,
        message: 'Server backup is succesvol op de achtergrond gestart! Alle werelden worden veilig opgeslagen.'
      });
    } else {
      await sendRconCommand('fpixel backup');
      return res.json({
        success: true,
        message: 'Server backup commando verzonden naar de Minecraft server.'
      });
    }
  } catch (err) {
    res.status(500).json({ error: 'Backup starten mislukt: ' + err.message });
  }
});

// API: /api/server/broadcast (Admin or Owner: send announcement to in-game chat)
app.post('/api/server/broadcast', async (req, res) => {
  const user = getSessionUser(req);
  if (!user || !user.isAdmin) {
    return res.status(403).json({ error: 'Alleen beheerders kunnen in-game berichten omroepen.' });
  }

  const { message } = req.body;
  if (!message || message.trim().length === 0) {
    return res.status(400).json({ error: 'Vul een omroepbericht in.' });
  }

  const cleanMessage = message.trim();
  try {
    await sendRconCommand(`say §6§l[CMS OMROEP] §e${user.username}§f: ${cleanMessage}`);
    res.json({
      success: true,
      message: 'Omroep succesvol verzonden naar Minecraft chat!'
    });
  } catch (err) {
    res.status(500).json({ error: 'RCON omroep mislukt: ' + err.message });
  }
});

// ==========================================
// SURVIVAL WAYPOINTS MANAGEMENT
// ==========================================

function getWaypoints() {
  const file = resolveFilePath('plugins/FPixelCore/survival_data/web_waypoints.json');
  if (file && fs.existsSync(file)) {
    try {
      return JSON.parse(fs.readFileSync(file, 'utf8')) || [];
    } catch (e) {
      return [];
    }
  }
  return [];
}

function saveWaypoints(list) {
  const file = resolveFilePath('plugins/FPixelCore/survival_data/web_waypoints.json');
  if (!file) return false;
  try {
    const dir = path.dirname(file);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(file, JSON.stringify(list, null, 2), 'utf8');
    return true;
  } catch (e) {
    console.error('Failed to save web_waypoints.json:', e);
    return false;
  }
}

// API: /api/waypoints (Public and authenticated waypoints)
app.get('/api/waypoints', (req, res) => {
  const user = getSessionUser(req);
  const all = getWaypoints();
  if (!user) {
    return res.json({ waypoints: all });
  }
  res.json({
    waypoints: all,
    currentUser: user.username,
    isOwnerOrAdmin: Boolean(user.rank === 'OWNER' || user.rank === 'ADMIN')
  });
});

// API: /api/waypoints/add (Authenticated: add waypoint to survival map)
app.post('/api/waypoints/add', async (req, res) => {
  const user = getSessionUser(req);
  if (!user) {
    return res.status(401).json({ error: 'Je moet ingelogd zijn om waypoints toe te voegen.' });
  }

  const { name, x, y, z, world, color, icon } = req.body;
  if (!name || x === undefined || z === undefined) {
    return res.status(400).json({ error: 'Vul een naam en coördinaten (X en Z) in.' });
  }

  const waypoints = getWaypoints();
  const newWp = {
    id: 'wp_' + Date.now(),
    name: name.trim(),
    x: Math.round(Number(x)),
    y: y !== undefined ? Math.round(Number(y)) : 64,
    z: Math.round(Number(z)),
    world: world || 'survival',
    color: color || '#38bdf8',
    icon: icon || '📍',
    createdBy: user.username,
    createdAt: Date.now()
  };

  waypoints.push(newWp);
  saveWaypoints(waypoints);

  // Announce in-game
  try {
    const tellrawJson = JSON.stringify([
      { text: "[Survival Kaart] ", color: "gold", bold: true },
      { text: user.username, color: "aqua" },
      { text: " heeft waypoint ", color: "yellow" },
      { text: newWp.name, color: "green", bold: true },
      { text: ` toegevoegd op X: ${newWp.x}, Z: ${newWp.z}! Bekijk op mc.fpixel.nl`, color: "gray" }
    ]);
    await sendRconCommand(`tellraw @a ${tellrawJson}`);
  } catch (e) {}

  res.json({
    success: true,
    waypoint: newWp,
    message: `Waypoint "${newWp.name}" succesvol geplaatst op X: ${newWp.x}, Z: ${newWp.z}!`
  });
});

// API: /api/waypoints/delete (Creator or Admin/Owner)
app.post('/api/waypoints/delete', (req, res) => {
  const user = getSessionUser(req);
  if (!user) {
    return res.status(401).json({ error: 'Je moet ingelogd zijn.' });
  }

  const { id } = req.body;
  let waypoints = getWaypoints();
  const wp = waypoints.find(w => w.id === id);
  if (!wp) {
    return res.status(404).json({ error: 'Waypoint niet gevonden.' });
  }

  if (wp.createdBy.toLowerCase() !== user.username.toLowerCase() && user.rank !== 'OWNER' && user.rank !== 'ADMIN') {
    return res.status(403).json({ error: 'Je kunt alleen je eigen waypoints verwijderen.' });
  }

  waypoints = waypoints.filter(w => w.id !== id);
  saveWaypoints(waypoints);
  res.json({ success: true, message: `Waypoint "${wp.name}" is verwijderd.` });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`==========================================`);
  console.log(` FPixel Friend Group Portal running on port ${PORT}`);
  console.log(` Monitoring Minecraft Server: ${MINECRAFT_HOST}:${MINECRAFT_PORT}`);
  console.log(` Whitelist & RCON Admin Integration Active`);
  console.log(`==========================================`);
});
