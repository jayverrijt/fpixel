# 🎮 FPixel Network - Hypixel-Grade Minigame Server (Minecraft 26.3)

Een 100% server-sided Minecraft minigame netwerk gebouwd op **Paper 26.3 (Java 25+)** met **GeyserMC 26.3 & Floodgate** crossplay voor Bedrock en Java, **SkinsRestorer** voor officiële skins in offline/crossplay modus, en de custom plugin suite **FPixelCore**.

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
  - Versie: `Minecraft 26.3` (Vanilla client, geen mods nodig!)
- **Bedrock Edition (Telefoon, Tablet, Windows 10/11, Console)**:
  - Server IP: `localhost`
  - Poort: `19132` (UDP)
  - Draait via **GeyserMC 26.3 Preview** en **Floodgate**!

---

## 🌟 Nieuwste Updates

### ⚡ Paper 26.3 & Geyser 26.3
- Server geüpgraded naar **Paper 26.3 build 181** (draaiend op OpenJDK 25).
- Geyser geüpgraded naar de officiële **26.3 build** (`pr.6712 #3108`), waardoor Bedrock clients naadloos kunnen verbinden.
- Plugin toolchain geüpgraded naar Java 25 & `paper-api:26.3.build.181-stable`.

### 👤 Skins Fix (SkinsRestorer)
- **SkinsRestorer** geïntegreerd in de server:
  - Spelers krijgen automatisch hun officiële Mojang skin te zien.
  - Bedrock skins worden vertaald via Floodgate/SkinsRestorer.
  - Spelers kunnen zelf skins wijzigen met `/skin <naam>` of `/skin url <url>`.

### 🏰 Grand Floating Hub & Wachtlobby's
- **Grand Hub (Zwevend Plaza op `0, 100, 0`)**:
  - Grote ronde plaza van Quartz en Gold met centrale spawn dais.
  - 4 monumentale poorten met pilaren en Sea Lantern verlichting.
  - Launchpads op Emerald Blocks aan de randen.
  - Void protection: val je van het eiland? Je wordt soepel teruggeteleporteerd naar het midden!
- **Pre-Game Wachtlobby's**:
  - Elke arena heeft een eigen verhoogde zwevende observatie- en wachtruimte van glas en pilaren waar spelers wachten tot de countdown afloopt.

### 🗺️ 30 Prachtige Custom Maps (10 per Minigame!)

In het **Game Navigator Compass** kun je nu met **Links-klik** direct Quick-Playen, of met **Rechts-klik** (of `/maps <game>`) zelf een van de 10 maps kiezen!

#### 🛏️ BedWars (10 Themed Maps):
1. **Sky Castle** (`bw-1`): Koninklijk kasteel van Stone Bricks met goudaccenten en torens.
2. **Desert Dunes** (`bw-2`): Zandsteen piramides, terracotta accenten en cactussen.
3. **Neon Matrix** (`bw-3`): Cyber-futuristische arena van Black, Cyan en Magenta Concrete.
4. **Frost Peak** (`bw-4`): Winter wonderland van Packed Ice, sneeuw en blauw glas.
5. **Nether Citadel** (`bw-5`): Nether Bricks, Crimson hout en gloeiend magma.
6. **Fairy Hollow** (`bw-6`): Magische paddenstoeleneilanden met Mycelium en reuzenpaddenstoelen.
7. **Clockwork Sky** (`bw-7`): Steampunk sfeer met Cut Copper, Mud Bricks en kettingen.
8. **Sugar Rush** (`bw-8`): Snoepwereld van pastelroze beton, suikerstokken en taart.
9. **Ancient Shrine** (`bw-9`): Overwoekerde tempel van Mossy Stone, junglelianen en bamboe.
10. **Sunken Ocean** (`bw-10`): Atlantis thema met Prismarine, koralen en zeelantaarns.

#### 🏃 TNT Run (10 Multi-Tier Maps):
1. **The Colosseum** (`tnt-1`): Klassieke Romeinse arena met 4 lagen Sand + TNT en marmeren pilaren.
2. **Cyber Grid** (`tnt-2`): Neon Matrix vloeren van cyaan beton boven TNT.
3. **Frozen Abyss** (`tnt-3`): Gletsjer arena van sneeuw en ijs boven TNT.
4. **Lava Chamber** (`tnt-4`): Hete vulkaan kamer met rood zand en magma pilaren.
5. **Jungle Ruins** (`tnt-5`): Oerwoud tempel met grind en lianen.
6. **Space Station** (`tnt-6`): Ruimtestation met purpur en ender rod verlichting.
7. **The Factory** (`tnt-7`): Industriële koperen fabriek met mud bricks.
8. **Cotton Fall** (`tnt-8`): Suikerspin arena met zacht pastelroze vloeren.
9. **Sahara Storm** (`tnt-9`): Woestijn storm arena met fijn zandsteen.
10. **Void Shrine** (`tnt-10`): Zwevend heiligdom van glad kwarts en amethist.

#### 🥊 Sumo 1v1 (10 Dueling Arenas):
1. **Japanese Dojo** (`sumo-1`): Rond platform van Dark Oak en Red Wool met bamboe lantaarns.
2. **Neon Ring** (`sumo-2`): Cyber octogoon van Blackstone met oplichtende cyaan rand.
3. **Roman Pit** (`sumo-3`): Glad graniet duelplatform zwevend in de lucht.
4. **Slippery Peak** (`sumo-4`): Gladde ijsvloer voor extra spectaculaire knockbacks.
5. **Fire Caldera** (`sumo-5`): Vulkaanring omringd door vuurpotten en basalt.
6. **Sky Kingdom** (`sumo-6`): Hemels kwartsplatform met gouden kroonranden.
7. **Mushroom Cap** (`sumo-7`): Duelleer op de rode hoed van een reuzenpaddenstoel.
8. **Plank Arena** (`sumo-8`): Piratenschip dek platform met scheepslantaarns.
9. **Amethyst Spire** (`sumo-9`): Paars kristallen duelring van diepsteen en amethist.
10. **Oasis Ring** (`sumo-10`): Zandstenen oase ring met palmhout sokkels.

---

## 🛠️ Commands

| Commando | Permissie | Omschrijving |
| :--- | :--- | :--- |
| `/lobby` of `/hub` | Iedereen | Teleporteert direct terug naar de Grand Hub |
| `/join <game>` | Iedereen | Automatische matchmaking (zoekt eerste open arena) |
| `/maps <game>` | Iedereen | Opent de map selector met alle 10 maps voor de game |
| `/skin <naam>` | Iedereen | Verandert je Minecraft skin direct |
| `/setrank <speler> <rank>` | `fpixel.admin` | Wijzigt rang (`OWNER`, `ADMIN`, `MOD`, `VIP_PLUS`, `VIP`, `DEFAULT`) |
| `/fpixel list` | `fpixel.admin` | Toont status van alle 30 actieve arena's |
| `/fpixel buildmaps` | `fpixel.admin` | Herbouwt de Grand Hub en alle 30 maps direct in-game |
| `/fpixel setlobby` | `fpixel.admin` | Zet het lobby spawn punt op je huidige positie |
| `/fpixel addcoins <speler> <aantal>` | `fpixel.admin` | Voegt coins toe aan een speler |
