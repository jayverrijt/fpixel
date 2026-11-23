#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const username = process.argv[2];
const password = process.argv[3];

if (!username || !password) {
  console.log('Gebruik: node set_web_password.js <username> <password>');
  process.exit(1);
}

const candidates = [
  path.join(__dirname, '../server/plugins/FPixelCore/web_auth.json'),
  path.join('/data/plugins/FPixelCore/web_auth.json'),
  path.join('/home/jayv/fpixel/server/plugins/FPixelCore/web_auth.json'),
  path.join(__dirname, 'web_auth.json')
];

let targetFile = candidates[0];
for (const c of candidates) {
  if (fs.existsSync(c)) {
    targetFile = c;
    break;
  }
}

const dir = path.dirname(targetFile);
if (!fs.existsSync(dir)) {
  fs.mkdirSync(dir, { recursive: true });
}

let authData = {};
if (fs.existsSync(targetFile)) {
  try {
    authData = JSON.parse(fs.readFileSync(targetFile, 'utf8'));
  } catch (e) {}
}

const salt = crypto.randomBytes(16).toString('hex');
const hash = crypto.createHash('sha256').update(salt + password).digest('hex');

authData[username.toLowerCase()] = {
  username: username,
  salt: salt,
  hash: hash,
  updatedAt: new Date().toISOString()
};

fs.writeFileSync(targetFile, JSON.stringify(authData, null, 2), 'utf8');
console.log(`[OK] Web wachtwoord succesvol ingesteld voor ${username} in ${targetFile}`);
