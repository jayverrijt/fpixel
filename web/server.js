const express = require('express');
const path = require('path');
const fs = require('fs');
const net = require('net');

const app = express();
const PORT = process.env.PORT || 3000;
const MINECRAFT_HOST = process.env.MINECRAFT_HOST || '127.0.0.1';
const MINECRAFT_PORT = parseInt(process.env.MINECRAFT_PORT || '25565', 10);
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, '../server/plugins/FPixelCore');

app.use(express.static(path.join(__dirname, 'public')));

// VarInt encoder
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

// Read VarInt
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

// Ping Minecraft Server via native SLP protocol
function pingMinecraft(host, port, timeoutMs = 2500) {
  return new Promise((resolve) => {
    const startTime = Date.now();
    const socket = new net.Socket();
    let received = Buffer.alloc(0);
    let resolved = false;

    socket.setTimeout(timeoutMs);

    socket.on('connect', () => {
      try {
        const hostBuf = Buffer.from(host, 'utf8');
        const portBuf = Buffer.alloc(2);
        portBuf.writeUInt16BE(port);

        // Handshake packet (packetId 0x00, protocol 767, host, port, nextState 1)
        const handshakeBody = Buffer.concat([
          encodeVarInt(0x00),
          encodeVarInt(767),
          encodeVarInt(hostBuf.length),
          hostBuf,
          portBuf,
          encodeVarInt(1)
        ]);
        const handshakePacket = Buffer.concat([
          encodeVarInt(handshakeBody.length),
          handshakeBody
        ]);

        // Status request packet (packetId 0x00)
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
      // Attempt parse
      try {
        let offset = 0;
        const packetLen = readVarInt(received, offset);
        if (!packetLen) return;
        offset += packetLen.bytesRead;

        const packetId = readVarInt(received, offset);
        if (!packetId) return;
        offset += packetId.bytesRead;

        const stringLen = readVarInt(received, offset);
        if (!stringLen) return;
        offset += stringLen.bytesRead;

        if (received.length >= offset + stringLen.value) {
          const jsonStr = received.toString('utf8', offset, offset + stringLen.value);
          const data = JSON.parse(jsonStr);
          resolved = true;
          socket.destroy();
          resolve({
            online: true,
            latency: Date.now() - startTime,
            data
          });
        }
      } catch (e) {
        // waiting for more chunks
      }
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

// Read ranks.yml
function getRanks() {
  const ranks = {};
  const rankFiles = [
    path.join(DATA_DIR, 'ranks.yml'),
    path.join(__dirname, '../server/plugins/FPixelCore/ranks.yml')
  ];
  for (const f of rankFiles) {
    if (fs.existsSync(f)) {
      try {
        const content = fs.readFileSync(f, 'utf8');
        const lines = content.split('\n');
        for (const line of lines) {
          const parts = line.split(':');
          if (parts.length === 2) {
            const user = parts[0].trim().toLowerCase();
            const rank = parts[1].trim().toUpperCase();
            if (user && rank) ranks[user] = rank;
          }
        }
        break;
      } catch (e) {}
    }
  }
  return ranks;
}

// Read Survival profiles & stats
function getSurvivalProfiles() {
  const profiles = [];
  const survivalDirs = [
    path.join(DATA_DIR, 'survival_data'),
    path.join(__dirname, '../server/plugins/FPixelCore/survival_data')
  ];

  for (const sDir of survivalDirs) {
    if (fs.existsSync(sDir)) {
      try {
        const files = fs.readdirSync(sDir);
        for (const file of files) {
          if (file.endsWith('.yml')) {
            const uuid = file.replace('.yml', '');
            const filePath = path.join(sDir, file);
            const content = fs.readFileSync(filePath, 'utf8');
            let level = 0;
            let health = 20;

            for (const line of content.split('\n')) {
              const [k, v] = line.split(':').map(s => s.trim());
              if (k === 'level' && v) level = parseInt(v, 10) || 0;
              if (k === 'health' && v) health = Math.round(parseFloat(v) || 20);
            }

            profiles.push({ uuid, level, health });
          }
        }
        break;
      } catch (e) {}
    }
  }
  return profiles;
}

// Usercache mapping UUID -> Username
function getUserCache() {
  const cache = {};
  const cacheFiles = [
    path.join(DATA_DIR, '../usercache.json'),
    path.join(__dirname, '../server/usercache.json')
  ];
  for (const f of cacheFiles) {
    if (fs.existsSync(f)) {
      try {
        const data = JSON.parse(fs.readFileSync(f, 'utf8'));
        if (Array.isArray(data)) {
          for (const entry of data) {
            if (entry.uuid && entry.name) {
              cache[entry.uuid] = entry.name;
            }
          }
        }
        break;
      } catch (e) {}
    }
  }
  return cache;
}

// API endpoint: /api/stats
app.get('/api/stats', async (req, res) => {
  const pingRes = await pingMinecraft(MINECRAFT_HOST, MINECRAFT_PORT);
  const ranks = getRanks();
  const survivalProfiles = getSurvivalProfiles();
  const userCache = getUserCache();

  // Combine player stats
  const enrichedProfiles = survivalProfiles.map(p => {
    const name = userCache[p.uuid] || 'Player';
    const rank = ranks[name.toLowerCase()] || 'DEFAULT';
    return {
      uuid: p.uuid,
      name,
      rank,
      level: p.level,
      health: p.health
    };
  });

  // Sort by level descending
  enrichedProfiles.sort((a, b) => b.level - a.level);

  const payload = {
    server: {
      name: 'FPixel Network',
      domain: 'mc.fpixel.nl',
      ip: '85.215.230.22',
      javaPort: 25565,
      bedrockPort: 19132,
      crossplay: true,
      online: pingRes.online,
      latency: pingRes.latency || 0,
      version: pingRes.online ? (pingRes.data.version?.name || 'Paper 26.3') : 'Paper 26.3',
      playersOnline: pingRes.online ? (pingRes.data.players?.online ?? 0) : 0,
      playersMax: pingRes.online ? (pingRes.data.players?.max ?? 100) : 100,
      samplePlayers: pingRes.online ? (pingRes.data.players?.sample || []) : []
    },
    gamemodes: [
      { id: 'bedwars', name: 'BedWars', count: '10 Arenas', desc: 'Authentieke generators, upgrades, shops & kits in void world', icon: '🛏️' },
      { id: 'skywars', name: 'SkyWars', count: '10 Arenas', desc: 'Vliegende eilanden, snelle loot kisten & void pvp', icon: '⚔️' },
      { id: 'bridge', name: 'The Bridge 1v1', count: '3 Arenas', desc: 'Open bases, 3x3 pit scoren, 5 punten tot de overwinning', icon: '🌉' },
      { id: 'sumo', name: 'Sumo 1v1', count: '3 Arenas', desc: 'Vuistgevechten op smalle platformen zonder wapens', icon: '🥊' },
      { id: 'tntrun', name: 'TNT Run', count: '10 Arenas', desc: 'Vervallende zandvloeren, direct op de topfloor', icon: '💣' },
      { id: 'parkour', name: 'Parkour Duels', count: '10 Arenas', desc: 'Mathematisch haalbare jumps & checkpoints', icon: '🏃' },
      { id: 'survival', name: 'Survival SMP', count: 'Urban & Galaxy', desc: 'Vanilla survival met per-world inventory isolatie', icon: '🌲' },
      { id: 'hub', name: 'Grand Hub', count: '8 Paviljoenen', desc: 'Berg-amfitheater, NPC interacties & super launchpads', icon: '🏛️' }
    ],
    leaderboard: enrichedProfiles
  };

  res.json(payload);
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`==========================================`);
  console.log(` FPixel Web Stats Portal running on port ${PORT}`);
  console.log(` Monitoring Minecraft Server: ${MINECRAFT_HOST}:${MINECRAFT_PORT}`);
  console.log(` Enforced Domain: mc.fpixel.nl`);
  console.log(`==========================================`);
});
