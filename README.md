# 🎮 FPixel Network - Hypixel-Grade Minigame Server

Een 100% server-sided Minecraft minigame netwerk gebouwd op **Paper 1.21.4** met **GeyserMC & Floodgate** crossplay voor Bedrock en Java, en een volledig zelfgebouwde modulaire plugin suite (**FPixelCore**).

---

## 🚀 Snel Starten

### 1. Server Starten
```bash
cd server
./start.sh
```

### 2. Verbinden
- **Java Edition (PC/Mac/Linux)**:
  - Server Adres: `localhost` of `localhost:25565`
  - Versie: `1.20 - 1.21.4+` (Vanilla client, geen mods nodig!)
- **Bedrock Edition (Telefoon, Tablet, Windows 10/11, Console)**:
  - Server IP: `localhost` (of lokaal IP van de computer)
  - Poort: `19132` (UDP)
  - Werkt via **GeyserMC** en **Floodgate**!

---

## 🌟 Wat is er gebouwd?

### 1. 🏰 Hypixel-Style Lobby Systeem
- **Scoreboard (Sidebar)**: Volledig flikkervrij via Team-prefixes. Toont rang, munten, level, online spelers, server status en IP.
- **Geanimeerde Tablist**: Gesorteerd op rang (Owner ➔ Admin ➔ VIP ➔ Speler) met server-header en live ping.
- **Hotbar Items**:
  - `Slot 0`: **Navigator Compass** (Opent Game Selector)
  - `Slot 4`: **Spelerskop** (Toont profiel & statistieken)
  - `Slot 7`: **Player Visibility Dye** (Iedereen zichtbaar / Alleen VIPs / Niemand)
  - `Slot 8`: **Nether Star** (Snelmenu)
- **Lobby Mechanieken**:
  - **Double Jump**: Twee keer op spatie drukken lanceert je door de lucht met wolk-particles en windgeluid.
  - **Launchpads**: Drukplaten op redstone-blokken lanceren je met hoge snelheid vooruit.
  - **Void Fall Save**: Val je van de spawn af? Je wordt soepel teruggeteleporteerd naar de spawn met geluidseffect en actie-balk.
  - **Bescherming**: Geen schade, geen honger, geen blokken breken of plaatsen in de lobby.

### 2. 📱 Cross-Play & Bedrock Form GUI (Geyser + Floodgate)
- Bedrock spelers worden automatisch gedetecteerd.
- Bij het openen van het Game Selector menu krijgen Bedrock-spelers een **native Bedrock SimpleForm dialoog** met klikbare knoppen en game-iconen!
- Java-spelers zien de vertrouwde en interactieve **Chest GUI**.

### 3. ⚔️ Minigames

#### 🛏️ BedWars 1v1 / Teams (`bw-1`)
- **Eiland Generators**: Spawnt periodiek ijzer en goud bij team-bases, en diamanten in het midden.
- **Bed Defensie & Respawn**: Zolang je bed heel is, respawn je na 5 seconden (met countdown-scherm en blindness).
- **Bed Vernietiging**: Als een vijand je bed sloopt klinkt er een Ender Dragon roar en verschijnt er een schermbrede titel. Daarna is elke kill een **Final Kill**!
- **Shopkeeper GUI**: Koop blokken (wol, hout, end stone, obsidian), zwaarden, armor upgrades, gereedschap, golden apples, instant primed TNT en vuurballen (rechtsklik om te schieten!).

#### 🏃 TNT Run (`tnt-1`)
- **Decay Mechaniek**: Zodra je over zand of gravel loopt, verdwijnt het blok (en de TNT eronder) na 0.4s (8 ticks).
- **Double Jump Saves**: Elke speler heeft 3 double jumps om zichzelf te redden bij gevaarlijke sprongen!
- **Scoreboard**: Live spelers over en overgebleven double jumps.
- **Win Conditie**: Laatste speler die overblijft wint!

#### 🥊 Sumo Duels 1v1 (`sumo-1`)
- **Platform Arena**: Snelle 1-tegen-1 duels op een zwevend eilandje.
- **Sumo Stick**: Knockback II stick. Spelers nemen 0 health schade, maar wel sterke knockback impulsen.
- **Best of 3**: Eerste speler met 2 rondes winst pakt de overwinning en verhoogt zijn win streak!

### 4. ⚡ Instant Arena Rollback Engine
- Alle gebroken, geplaatste en veranderde blokken worden in het geheugen bijgehouden (`RollbackEngine`).
- Zodra een minigame eindigt, wordt de hele arena in **1 server-tick (0ms)** hersteld naar de originele staat. Geen laggende world-reloads!

---

## 🛠️ Commands

| Commando | Permissie | Omschrijving |
| :--- | :--- | :--- |
| `/lobby` of `/hub` | Iedereen | Teleporteert terug naar de hub |
| `/join <bedwars\|tntrun\|sumo>` | Iedereen | Zoekt automatisch een open arena en joint |
| `/setrank <speler> <rank>` | `fpixel.admin` | Wijzigt rang (`OWNER`, `ADMIN`, `MOD`, `VIP_PLUS`, `VIP`, `DEFAULT`) |
| `/fpixel list` | `fpixel.admin` | Toont status van alle actieve arena's |
| `/fpixel setlobby` | `fpixel.admin` | Zet de lobby spawn op je huidige positie |
| `/fpixel addcoins <speler> <aantal>` | `fpixel.admin` | Voegt munten toe aan een speler |
| `/fpixel reload` | `fpixel.admin` | Herlaadt configuratie en rangen |

---

## 🔨 Plugin Aanpassen & Compileren

De broncode bevindt zich in `plugin/`. Je kunt het project altijd opnieuw bouwen met Gradle:
```bash
cd plugin
gradle build
```
Het gecompileerde bestand (`FPixelCore.jar`) wordt automatisch direct in `server/plugins/` geplaatst!
