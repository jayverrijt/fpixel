# FPixel Survival World Daily Backup

- **Backup Timestamp:** 2026-10-29 04:00:22 UTC
- **Git Branch:** backups_survival
- **Source Host:** 9daf9706-3249-43cd-8496-9084ff2bb1b2
- **World Dimension:** `dimensions/minecraft/survival`
- **Player Profiles:** `world/players`
- **Plugin Storage:** `plugins/FPixelCore/survival_data`
- **Level Metadata:** `world/level.dat`

### Restore Instructions:
1. Stop the Minecraft server: `docker stop fpixel-minecraft`
2. Copy contents from this branch back into `server/`:
   - `dimensions/minecraft/survival` -> `server/world/dimensions/minecraft/survival`
   - `world/players` -> `server/world/players`
   - `world/level.dat` -> `server/world/level.dat`
   - `plugins/FPixelCore/survival_data` -> `server/plugins/FPixelCore/survival_data`
3. Start the Minecraft server: `docker start fpixel-minecraft`
