# CONTEXT & MASTER BLUEPRINT: FPIXEL 2 MINECRAFT NETWORK

> **Gebruik:** Dit bestand bevat de volledige architectuur, configuraties, mechanics, codestructuur en richtlijnen van het **FPixel 2** project. Geef dit bestand aan een AI-model (bijv. Gemini) bij het starten van een nieuwe sessie of release om direct alle context te laden zonder token-verlies.

---

## 1. PROJECT IDENTITEIT & OVERZICHT
- **Projectnaam:** FPixel 2 (Minecraft Network & CMS)
- **Productie Webadres:** [https://mc.fpixel.nl](https://mc.fpixel.nl)
- **Server IP:** `mc.fpixel.nl`
  - **Java Editie Standaard Poort:** `25565`
  - **Bedrock Editie Poort (Geyser/Floodgate):** `19132`
- **Doel:** Een besloten, hoogwaardig Nederlands Minecraft servernetwerk voor vrienden en community. Biedt Java & Bedrock crossplay, een breed scala aan minigames (Survival SMP, Parkour Studio, BedWars, SkyWars, TNT Run, Murder Mystery, Prop Hunt, The Bridge/Sumo 1v1), en een geïntegreerd web-CMS voor whitelistbeheer, rangentoekenning, community parkour-curatie en serverbeheer.

---

## 2. INFRASTRUCTUUR, HOSTING & CI/CD
- **Productie Server:** Hosted op `fpixel.nl` (Linux / Ubuntu).
- **Projectlocatie op Server:** `/home/jayv/fpixel`
- **Backup Opslaglocatie:** `/home/jayv/backups` (lokaal ook in `/Users/jayv/Projects/Fpixel2_backups/`)
- **Web Reverse Proxy:** OpenResty / Nginx die SSL beëindigt en verkeer op poort 80/443 doorstuurt naar de Node.js Express applicatie op poort `3000`.
- **Minecraft Server Engine:** Paper 1.21.x draaiend onder Java 25 (Temurin).
- **Crossplay Ondersteuning:** GeyserMC & Floodgate voor Bedrock spelers (herkenbaar aan de punt prefix, bijv. `.Smileygaming27` of `.minnieheks`).
- **CI/CD Pipeline (`.github/workflows/deploy.yml`):**
  1. **Trigger:** Elke push naar de `main` branch (of handmatige `workflow_dispatch`).
  2. **Job 1 (`build`):**
     - Stelt Java 25 in.
     - Valideert Gradle Wrapper en compileert de plugin via `./gradlew build --no-daemon`.
     - Verifieert en uploadt het artifact `server/plugins/FPixelCore.jar`.
  3. **Job 2 (`deploy`):**
     - Maakt via SSH verbinding met `fpixel.nl` (met secret `SSH_PRIVATE_KEY`).
     - Voert een pre-deploy backup uit via `scripts/backup.sh`.
     - Doet `git fetch origin main`, `git reset --hard origin/main` en ruimt op via `git clean -fd`.
     - Start de deploy en herstart services via `scripts/deploy.sh`.

---

## 3. PROJECT DIRECTORY STRUCTUUR
```text
Fpixel2/
├── .github/workflows/deploy.yml   # GitHub Actions build & deploy pipeline
├── scripts/
│   ├── backup.sh                  # Maakt tar.gz snapshot van server/ & werelden
│   └── deploy.sh                  # Production deploy & container orchestration
├── server/                        # Minecraft Paper server root
│   ├── paper.jar                  # Paper 1.21.x server binary
│   ├── server.properties          # Server instellingen (RCON actief op 25575, whitelist: true)
│   ├── whitelist.json             # Officiële Minecraft whitelist met UUIDs en namen
│   ├── usercache.json             # Cache van UUID naar spelersnaam
│   ├── plugins/
│   │   └── FPixelCore/            # Datafolder van onze custom plugin
│   │       ├── ranks.yml          # Spelers rangen (UUID/naam : RANK)
│   │       ├── web_auth.json      # SHA-256 webwachtwoorden & verificatiecodes
│   │       ├── custom_parkour.json# Gemaakte en goedgekeurde parkour maps
│   │       └── survival_data/     # Web waypoints en coördinaten
│   └── world/                     # Survival werelden (Overworld, Nether, End)
├── plugin/                        # Custom Java 25 Spigot/Paper Plugin
│   ├── build.gradle.kts           # Gradle configuratie
│   ├── gradlew / gradlew.bat      # Gradle wrapper
│   └── src/main/java/net/fpixel/  # Plugin broncode
│       ├── FPixelPlugin.java      # Hoofdklasse & command registraties
│       ├── core/
│       │   ├── rank/              # RankManager, Rank enum, permissies
│       │   └── util/              # RCON & Chat utilities
│       └── games/
│           ├── parkour/           # ParkourGame, ParkourManager, jump pads, checkpoints
│           └── ... (overige minigames)
└── web/                           # Node.js CMS & Webportaal
    ├── server.js                  # Express backend, RCON client, API routes
    ├── package.json               # Dependencies (express, cookie-parser, etc.)
    └── public/                    # Frontend assets
        ├── index.html             # Single Page App (Publiek + CMS Beheer)
        ├── app.js                 # Frontend logica (Auth, Leaflet map, CMS controller)
        └── style.css              # Custom donkere esport/Minecraft stylesheet
```

---

## 4. MINECRAFT PLUGIN ARCHITECTUUR (`FPixelCore`)

### A. Rangen Systeem (`RankManager`)
- **Beschikbare Rangen (hiërarchie):**
  `OWNER` (100) > `ADMIN` (90) > `MOD` (80) > `YOUTUBE` (70) > `MVP_PLUS` (60) > `MVP` (50) > `VIP_PLUS` (40) > `VIP` (30) > `MEMBER` (10).
- **Opslag:** `server/plugins/FPixelCore/ranks.yml`.
- **Mechaniek:** Slaat zowel UUIDs als spelersnamen op. In-game worden displaynamen, tablijsten en chat tags automatisch voorzien van de juiste prefixes (bijv. `§4[OWNER]`).
- **Synchronisatie:** Rangen die in het web-CMS worden toegewezen worden direct via RCON `setrank <speler> <rang>` in-game gesynchroniseerd.

### B. Parkour Studio & Duels (`ParkourGame` & `ParkourManager`)
- **Map Creatie:** Spelers met `[MOD]` of hoger typen `/fpixel parkour create <naam>` (bijv. `/fpixel parkour create test`).
  - *Naamgeving:* De map krijgt direct de custom naam als ID en displaynaam (bijv. ID `TEST`, naam `Test`).
  - *Start Spawn:* Spawnt met een Golden Pressure Plate als Start Jump Pad op een void platform.
- **Review & Goedkeuring:**
  - In-game testen: `/fpixel test <id/naam>` (werkt zowel met slug als displaynaam).
  - Web CMS Goedkeuring: Eigenaars (`[OWNER]`) kunnen maps live zetten, tijdelijk uit circulatie halen of afwijzen via `mc.fpixel.nl`.
- **Geavanceerde Parkour Mechanics:**
  - *Giga Boost Pad:* Golden Pressure Plate op een Gold Block of Emerald Block lanceert de speler met een grote boost naar voren en omhoog.
  - *Standard Boost Pad:* Stone Pressure Plate op een Gold of Emerald block geeft een gerichte jump boost.
  - *Checkpoints:* Stone Pressure Plates op neutrale blokken fungeren puur als checkpoint en geven **geen** boost.
  - *Slime Springboards:* Slime blocks geven bij aanraking extra bounce/momentum.
  - *Lobby Kompas:* Linker- of rechtermuisklik opent direct het spelmenu of teleporteert naar de lobby.
  - *Scoreboard:* Toont de actieve mapnaam (`§6§lTest`), huidige checkpoint en verstreken tijd.

### C. Overige Minigames
1. **Survival SMP:** Vanilla overleving, 3 dimensies, verbonden met live waypoints op de webkaart.
2. **BedWars:** 10 arena's, generator upgrades (ijzer/goud/emeralds), team gameplay.
3. **SkyWars:** Zwevende eilanden boven void, 10 gevechtskits, kisten looting.
4. **TNT Run:** Instortende vloer zodra je rent, double jump perks, Romeins colosseum.
5. **Murder Mystery:** Moordenaar, Detective met boog, onschuldigen, meswerp mechanica.
6. **Prop Hunt:** Verstoppertje vermomd als blokken (crafting table, flower pot), taunts, jager strafsysteem.
7. **The Bridge & Sumo 1v1:** Snelle duels, snelle void respawns, sumo dojo ringen.

---

## 5. WEBAPPLICATIE & CMS (`mc.fpixel.nl`)

### A. Publieke Functies
1. **Server & Minigames Showcase:** IP/Poort 1-klik kopieerknoppen voor Java (`mc.fpixel.nl:25565`) en Bedrock (`19132`), live ping status en actuele spelersaantallen.
2. **Interactieve Survival Kaart:** Leaflet.js kaart met multi-dimensie switch (Overworld, Nether, End), live coördinaten en openbare waypoints.
3. **Whitelist Aanvraagformulier:** Publieke aanmelding met gebruikersnaam, live head preview (`mc-heads.net`), Java/Bedrock selectie, Discord tag en motivatie.

### B. Geauthenticeerd CMS Beheer (`#tab-cms`)
Admin en Owner functionaliteiten zijn samengevoegd in één interface met strikt behoud van permissies:
- **Subtab 1 - Overzicht (Dashboard):** KPI statistieken (aantal gewhiteliste spelers, openstaande verzoeken, parkour maps, actieve stafleden).
- **Subtab 2 - Whitelist & Aanvragen:**
  - Openstaande join verzoeken met avatar, motivatie en knoppen: `✅ Goedkeuren` (voegt direct toe via RCON `whitelist add` en broadcast naar server), `❌ Afwijzen`, en `🗑️ Verwijderen`.
  - Tools om testaanvragen met 1 klik op te schonen of alle aanvragen te wissen.
  - Actieve Whitelist tabel met realtime zoekfilter, speleravatars en handmatig toevoegen/verwijderen.
- **Subtab 3 - Spelers & Rangen:**
  - Automatische omzetting van ruwe UUIDs (zoals Geyser offline hashes) naar echte spelersnamen (`SmileyGaming27`, `Amigapower2018`).
  - Tabel met spelershoofden, badges en rollen.
  - Rang toekennen & intrekken via formulier en RCON `setrank` (**Strikt voorbehouden aan `[OWNER]`**; voor Admins/Mods getoond in alleen-lezen modus met lock-banner).
- **Subtab 4 - Parkour Studio:**
  - CMS om gemaakte community parcoursen te beoordelen.
  - Filters: Alle, In Afwachting, Goedgekeurd, Uit Circulatie.
  - Goedkeuren, tijdelijk deactiveren of definitief afwijzen (**`[OWNER]` vereist**).
- **Subtab 5 - Server & Systeem:**
  - Live in-game broadcast omroep via RCON `say`.
  - Volledige Server Backup knop (`scripts/backup.sh` of `fpixel backup`) (**`[ADMIN]` of `[OWNER]`**).
  - Beveiligde Server Noodstop modal met verplichte "STOP" confirmatie en 5-seconden countdown (**Strikt `[OWNER]`**).

---

## 6. AUTHENTICATIE & IN-GAME WACHTWOORD RESET
- **Opslag:** `server/plugins/FPixelCore/web_auth.json`.
- **Wachtwoord Hashes:** SHA-256 met unieke salt per speler.
- **In-Game Password Reset Flow:**
  1. Speler vult gebruikersnaam in op de site en klikt op *Stuur Code*.
  2. Server genereert een cryptografisch veilige 6-cijferige code (5 minuten geldig).
  3. Server verstuurt de code via RCON `tellraw` direct naar de Minecraft in-game chat van de speler.
  4. Speler voert de 6-cijferige code en een nieuw wachtwoord in op de website.
  5. Het wachtwoord wordt gehasht opgeslagen en de speler wordt direct automatisch ingelogd.
- **Alternatief:** In-game commando `/webpass <wachtwoord>`.

---

## 7. ONTWIKKELINGS- & BEHEERRICHTLIJNEN VOOR AI ASSISTENTS
- **Backup Verplichting:** Voer vóór ingrijpende pushes of serverwijzigingen altijd `bash scripts/backup.sh` uit.
- **Design Stijl:** Behoud altijd het authentieke, strakke, donkere esport/Minecraft thema (`JetBrains Mono`, contrastrijke badges, geen zwevende generieke AI-templates).
- **RCON Protocol:** Alle live server synchronisaties (whitelist, rangen, omroepen, stop) verlopen via het interne RCON protocol via `web/server.js`.
- **Platform Pariteit:** Houd te allen tijde rekening met zowel Java Edition spelers als Bedrock Edition (Geyser) spelers.
