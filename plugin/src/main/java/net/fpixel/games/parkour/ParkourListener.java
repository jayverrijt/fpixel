package net.fpixel.games.parkour;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ParkourListener implements Listener {
    private final FPixelPlugin plugin;
    private final ParkourManager parkourManager;
    private final Map<UUID, Long> lastBoostTime = new HashMap<>();

    public ParkourListener(FPixelPlugin plugin, ParkourManager parkourManager) {
        this.plugin = plugin;
        this.parkourManager = parkourManager;
    }

    private boolean canBoost(UUID uuid) {
        long now = System.currentTimeMillis();
        return now - lastBoostTime.getOrDefault(uuid, 0L) >= 400L;
    }

    private void recordBoost(UUID uuid) {
        lastBoostTime.put(uuid, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equalsIgnoreCase("parkour_studio")) return;

        String mapId = parkourManager.getEditingMapId(player);
        if (mapId == null) return;

        CustomParkourMap map = parkourManager.getMap(mapId);
        if (map == null) return;

        Block block = event.getBlockPlaced();
        Material mat = block.getType();

        // 1. Checkpoint Marker: Stone, Oak, or Polished Blackstone pressure plates
        if (mat == Material.STONE_PRESSURE_PLATE || mat == Material.OAK_PRESSURE_PLATE || mat == Material.POLISHED_BLACKSTONE_PRESSURE_PLATE) {
            Location loc = block.getLocation();
            map.addCheckpoint(loc.getX(), loc.getY(), loc.getZ());
            parkourManager.saveMaps();
            ChatUtil.sendActionBar(player, "§a§l✔ Checkpoint #" + map.getCheckpoints().size() + " geregistreerd!");
            ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.8f);
        }
        // 2. GIGA BOOST PAD: Light Weighted Pressure Plate (Gold)
        else if (mat == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            ChatUtil.sendActionBar(player, "§6§l🚀 Giga Boost Pad geregistreerd! (Gouden Drukplaat)");
            ChatUtil.playSound(player, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.8f, 1.5f);
        }
        // 3. Finish Line Marker: Heavy Weighted Pressure Plate (Iron)
        else if (mat == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
            Location loc = block.getLocation();
            map.setFinish(loc.getX(), loc.getY(), loc.getZ());
            parkourManager.saveMaps();
            ChatUtil.sendActionBar(player, "§6§l🏁 Finish lijn geregistreerd op deze positie!");
            ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }
        // 4. Slime Blocks: Boost Slime vs Standaard Slime
        else if (mat == Material.SLIME_BLOCK) {
            Block baseBlock = block.getRelative(org.bukkit.block.BlockFace.DOWN);
            if (baseBlock.getType() == Material.EMERALD_BLOCK || baseBlock.getType() == Material.REDSTONE_BLOCK) {
                ChatUtil.sendActionBar(player, "§a§l⚡ Boost Slime Block geplaatst! (Lanceert spelers)");
                ChatUtil.playSound(player, Sound.BLOCK_SLIME_BLOCK_FALL, 1.0f, 1.8f);
            } else {
                ChatUtil.sendActionBar(player, "§aStandaard Slime Block geplaatst (normaal stuiteren).");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equalsIgnoreCase("parkour_studio")) return;

        String mapId = parkourManager.getEditingMapId(player);
        if (mapId == null) return;

        CustomParkourMap map = parkourManager.getMap(mapId);
        if (map == null) return;

        Block block = event.getBlock();
        Location loc = block.getLocation();

        // Check if checkpoint was removed
        boolean removedCp = map.removeCheckpointNear(loc, 1.0);
        if (removedCp) {
            parkourManager.saveMaps();
            ChatUtil.sendActionBar(player, "§c✖ Checkpoint verwijderd. (Resterend: " + map.getCheckpoints().size() + ")");
            ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
            return;
        }

        // Check if finish was removed
        Location finish = map.getFinishLocation(block.getWorld());
        if (finish != null && finish.distanceSquared(loc) <= 1.5) {
            map.setFinish(null);
            parkourManager.saveMaps();
            ChatUtil.sendActionBar(player, "§c✖ Finish lijn verwijderd.");
            ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!parkourManager.isInTestSession(player)) return;

        ParkourTestSession session = parkourManager.getTestSession(player);
        if (session == null) return;

        CustomParkourMap map = session.getMap();
        Location loc = player.getLocation();

        // 1. Check void fall respawn
        if (loc.getY() < map.getVoidY()) {
            int cpIndex = session.getCurrentCheckpoint();
            List<Location> cps = map.getCheckpointLocations(player.getWorld());

            Location target = (cpIndex > 0 && cpIndex <= cps.size())
                    ? cps.get(cpIndex - 1)
                    : map.getStartLocation(player.getWorld());

            if (target != null) {
                player.setFallDistance(0);
                player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                player.setHealth(20.0);
                player.setFoodLevel(20);
                player.setFireTicks(0);
                player.teleport(target.clone().add(0, 0.2, 0));
                ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
                ChatUtil.sendActionBar(player, "§cRespawn bij checkpoint " + cpIndex + "!");
            }
            return;
        }

        // 2. Check next checkpoint trigger
        List<Location> cps = map.getCheckpointLocations(player.getWorld());
        int current = session.getCurrentCheckpoint();
        if (current < cps.size()) {
            Location nextCp = cps.get(current);
            if (loc.distanceSquared(nextCp) <= 3.0) {
                int newCp = current + 1;
                session.setCurrentCheckpoint(newCp);
                ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.8f);
                ChatUtil.sendTitle(player, "§a§lCHECKPOINT " + newCp, "§7" + newCp + " / " + cps.size(), 0, 25, 5);
                ChatUtil.sendActionBar(player, "§aCheckpoint " + newCp + " / " + cps.size() + " bereikt!");
            }
        }

        // 3. Dynamic Interactive Map Pads (Giga Boost & Slime Boosters)
        Block underPlayer = loc.getBlock();
        Block belowPlayer = loc.clone().subtract(0, 0.5, 0).getBlock();

        // A. GIGA BOOST PAD: Golden Pressure Plate (LIGHT_WEIGHTED_PRESSURE_PLATE)
        if (underPlayer.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE || belowPlayer.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            if (canBoost(player.getUniqueId())) {
                recordBoost(player.getUniqueId());
                org.bukkit.util.Vector dir = player.getLocation().getDirection();
                dir.setY(0).normalize().multiply(1.85);
                dir.setY(0.95);
                player.setVelocity(dir);
                player.setFallDistance(0);
                ChatUtil.playSound(player, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.0f, 1.5f);
                ChatUtil.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.2f);
                ChatUtil.sendActionBar(player, "§6§l🚀 GIGA BOOST! 🚀");
                player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, loc, 25, 0.3, 0.3, 0.3, 0.15);
                player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, loc, 15, 0.2, 0.1, 0.2, 0.05);
            }
        }

        // B. Slime Blocks: Boost Slime Block vs Standard Slime Block
        Material standOn = belowPlayer.getType();
        if (standOn == Material.SLIME_BLOCK) {
            Block baseBlock = loc.clone().subtract(0, 1.5, 0).getBlock();
            if (baseBlock.getType() == Material.EMERALD_BLOCK || baseBlock.getType() == Material.REDSTONE_BLOCK) {
                if (canBoost(player.getUniqueId())) {
                    recordBoost(player.getUniqueId());
                    org.bukkit.util.Vector dir = player.getLocation().getDirection();
                    dir.setY(0).normalize().multiply(1.1);
                    dir.setY(0.75);
                    player.setVelocity(dir);
                    player.setFallDistance(0);
                    ChatUtil.playSound(player, Sound.BLOCK_SLIME_BLOCK_FALL, 1.0f, 1.8f);
                    ChatUtil.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.7f, 1.4f);
                    ChatUtil.sendActionBar(player, "§a⚡ Slime Springboard Boost! ⚡");
                    player.getWorld().spawnParticle(org.bukkit.Particle.ITEM_SLIME, loc, 20, 0.3, 0.3, 0.3, 0.1);
                }
            }
            // Standard slime block: no forced velocity override, vanilla bounce physics apply!
        } else if (standOn == Material.EMERALD_BLOCK) {
            if (canBoost(player.getUniqueId())) {
                recordBoost(player.getUniqueId());
                org.bukkit.util.Vector dir = player.getLocation().getDirection();
                dir.setY(0).normalize().multiply(1.1);
                dir.setY(0.75);
                player.setVelocity(dir);
                player.setFallDistance(0);
                ChatUtil.playSound(player, Sound.BLOCK_SLIME_BLOCK_FALL, 1.0f, 1.8f);
                ChatUtil.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.7f, 1.4f);
                ChatUtil.sendActionBar(player, "§a⚡ Slime Springboard Boost! ⚡");
                player.getWorld().spawnParticle(org.bukkit.Particle.ITEM_SLIME, loc, 20, 0.3, 0.3, 0.3, 0.1);
            }
        } else if (standOn == Material.GOLD_BLOCK || standOn == Material.REDSTONE_BLOCK) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 80, 1, false, false, true));
            ChatUtil.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.8f);
            ChatUtil.sendActionBar(player, "§e⚡ Speed Boost! ⚡");
        }

        // 4. Check finish line trigger
        Location finishLoc = map.getFinishLocation(player.getWorld());
        if (finishLoc != null && loc.distanceSquared(finishLoc) <= 3.0 && !session.isCompleted()) {
            session.setCompleted(true);
            parkourManager.spawnCompletionFireworks(finishLoc);
            ChatUtil.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            double time = session.getElapsedSeconds();
            ChatUtil.sendTitle(player, "§a§lPARCOURS VOLTOOID!", "§eTijd: §6" + String.format("%.2f", time) + "s", 5, 60, 15);

            ChatUtil.sendMessage(player, " ");
            ChatUtil.sendMessage(player, "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            ChatUtil.sendMessage(player, "§a§l          TEST SUCCESVOL VOLTOOID!");
            ChatUtil.sendMessage(player, "§7 Map: §f" + map.getName() + " §7(ID: §b" + map.getId() + "§7)");
            ChatUtil.sendMessage(player, "§7 Neergezette Tijd: §6§l" + String.format("%.2f", time) + " seconden");
            ChatUtil.sendMessage(player, " ");

            Rank rank = plugin.getRankManager().getRank(player);
            if (rank != null && rank.getWeight() >= Rank.OWNER.getWeight()) {
                ChatUtil.sendMessage(player, "§a§lBeoordeel deze kaart als [OWNER]:");
                ChatUtil.sendMessage(player, " §a• Typ §e/fpixel parkour approve " + map.getId() + " §aom live te zetten in parkour games!");
                ChatUtil.sendMessage(player, " §c• Typ §e/fpixel parkour reject " + map.getId() + " [reden] §com af te wijzen.");
                ChatUtil.sendMessage(player, " §7Of keur hem direct goed via het webportaal: §bmc.fpixel.nl");
            } else {
                ChatUtil.sendMessage(player, "§7Je kunt een [OWNER] vragen om deze map te beoordelen op §bmc.fpixel.nl");
                ChatUtil.sendMessage(player, "§7of met §e/fpixel test " + map.getId());
            }
            ChatUtil.sendMessage(player, "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            ChatUtil.sendMessage(player, " ");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;

        // Tester Interaction Tools
        if (parkourManager.isInTestSession(player)) {
            ParkourTestSession session = parkourManager.getTestSession(player);
            if (session == null) return;

            CustomParkourMap map = session.getMap();

            // Slot 0 Compass: Restart course
            if (item.getType() == Material.COMPASS) {
                event.setCancelled(true);
                Location start = map.getStartLocation(player.getWorld());
                if (start != null) {
                    player.teleport(start.clone().add(0, 0.2, 0));
                    session.setCurrentCheckpoint(0);
                    session.setCompleted(false);
                    ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                    ChatUtil.sendActionBar(player, "§aTeruggezet naar start!");
                }
                return;
            }

            // Slot 7 Emerald: Approve map (Owner only)
            if (item.getType() == Material.EMERALD) {
                event.setCancelled(true);
                Rank rank = plugin.getRankManager().getRank(player);
                if (rank != null && rank.getWeight() >= Rank.OWNER.getWeight()) {
                    parkourManager.approveMap(player, map.getId());
                    parkourManager.stopTestingSession(player);
                }
                return;
            }

            // Slot 8 Barrier: Exit test
            if (item.getType() == Material.BARRIER) {
                event.setCancelled(true);
                parkourManager.stopTestingSession(player);
                return;
            }
        }

        // Builder Interaction Tools
        String editingId = parkourManager.getEditingMapId(player);
        if (editingId != null && player.getWorld().getName().equalsIgnoreCase("parkour_studio")) {
            CustomParkourMap map = parkourManager.getMap(editingId);
            if (map == null) return;

            // Slot 7 Compass: Teleport to start
            if (item.getType() == Material.COMPASS) {
                event.setCancelled(true);
                Location start = map.getStartLocation(player.getWorld());
                if (start != null) {
                    player.teleport(start.clone().add(0, 0.2, 0));
                    ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                    ChatUtil.sendActionBar(player, "§bGevlogen naar startplatform!");
                }
                return;
            }

            // Slot 8 Red Bed: Exit to Lobby
            if (item.getType() == Material.RED_BED) {
                event.setCancelled(true);
                parkourManager.setEditingMapId(player, null);
                player.getInventory().clear();
                plugin.getLobbyManager().teleportToLobby(player);
                ChatUtil.sendMessage(player, "§aTeruggekeerd naar de Lobby. Je map (§e" + map.getId() + "§a) is veilig opgeslagen!");
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (parkourManager.isInTestSession(player)) {
            parkourManager.stopTestingSession(player);
        }
        parkourManager.setEditingMapId(player, null);
    }
}
