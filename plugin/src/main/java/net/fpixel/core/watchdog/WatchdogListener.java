package net.fpixel.core.watchdog;

import net.fpixel.FPixelPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.potion.PotionEffectType;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for client handshake brand packets, plugin messages, and live movement/combat patterns
 * to feed real-time integrity signals to the WatchdogManager.
 */
public class WatchdogListener implements Listener {

    private final FPixelPlugin plugin;
    private final WatchdogManager watchdog;

    // Movement tracking
    private final Map<UUID, Integer> airTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBreakTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> breakCount = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBlockPlaceTime = new ConcurrentHashMap<>();

    public WatchdogListener(FPixelPlugin plugin, WatchdogManager watchdog) {
        this.plugin = plugin;
        this.watchdog = watchdog;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        lastBlockPlaceTime.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        airTicks.put(event.getPlayer().getUniqueId(), 0);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        watchdog.registerPlayer(player);

        // Verify client brand after 5 ticks when client packet is delivered
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                String brand = player.getClientBrandName();
                if (brand != null && !brand.isBlank()) {
                    watchdog.processClientBrand(player, brand);
                }
            }
        }, 5L);

        // Secondary verification after 25 ticks (1.25s) in case of slow handshake
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                String brand = player.getClientBrandName();
                if (brand != null && !brand.isBlank()) {
                    watchdog.processClientBrand(player, brand);
                }
            }
        }, 25L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uid = event.getPlayer().getUniqueId();
        airTicks.remove(uid);
        lastBreakTime.remove(uid);
        breakCount.remove(uid);
        watchdog.unregisterPlayer(event.getPlayer());
    }

    /**
     * Inspects custom channels requested by the client. Hack clients like Wurst, Meteor, Aristois
     * often register custom channels.
     */
    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        String channel = event.getChannel().toLowerCase();
        // Allow legitimate recipe-viewer / utility mod channels (e.g. jei:cheat_permission)
        if (channel.startsWith("jei:")) {
            return;
        }
        if (channel.contains("wurst") || channel.contains("meteor") || channel.contains("liquid")
                || channel.contains("aristois") || channel.contains("cheat") || channel.contains("hack")) {
            watchdog.penalize(event.getPlayer(), 100, "Verboden hack client kanaal geregistreerd: " + event.getChannel());
        }
    }

    /**
     * Live Movement Checks (Fly, Hover, High Speed).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
        if (player.getAllowFlight() || player.isFlying() || player.isGliding()) return;
        if (player.isInsideVehicle()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Bedrock Edition players use touch controls, client-side predictive physics and scaffolding packets
        boolean isBedrock = (plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrock(player))
                || player.getName().startsWith(".");
        if (isBedrock) {
            airTicks.put(player.getUniqueId(), 0);
            return;
        }

        // 1. Horizontal Speed Check
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Maximum normal horizontal speed on ground is ~0.65 (sprinting) or ~0.9 with Speed II
        double maxAllowedSpeed = 1.20;
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            maxAllowedSpeed += 0.40;
        }

        if (horizontalDist > maxAllowedSpeed && from.getY() <= to.getY()) {
            watchdog.penalize(player, 15, "Abnormale snelheid / Speed Hack (" + String.format("%.2f", horizontalDist) + " b/t)");
            return;
        }

        // 2. Fly & Mid-Air Hover Check
        UUID uid = player.getUniqueId();

        // Minigames (SkyWars, BedWars, The Bridge, Parkour) feature bridging, clutching, and knockback:
        // do not run mid-air hover penalties during active minigames
        if (plugin.getGameManager().getPlayerArena(player) != null) {
            airTicks.put(uid, 0);
            return;
        }

        // If sneaking or on ground (e.g. bridging on block edges), reset air count immediately!
        if (player.isSneaking() || player.isOnGround()) {
            airTicks.put(uid, 0);
            return;
        }

        // If player placed a block in the last 2.5 seconds, they are actively bridging/building
        long lastPlace = lastBlockPlaceTime.getOrDefault(uid, 0L);
        if (System.currentTimeMillis() - lastPlace < 2500) {
            airTicks.put(uid, 0);
            return;
        }

        double dy = to.getY() - from.getY();
        boolean inLiquidOrClimbing = player.isInWater() || player.isInLava() || player.isClimbing();
        boolean hasJumpBoost = player.hasPotionEffect(PotionEffectType.JUMP_BOOST) || player.hasPotionEffect(PotionEffectType.LEVITATION);

        if (!inLiquidOrClimbing && !hasJumpBoost) {
            // Check surrounding blocks around player footprint and below (detects slabs, stairs, fences, walls, chests, etc.)
            boolean hasGroundBelow = false;
            double px = to.getX();
            double py = to.getY();
            double pz = to.getZ();
            double[] offsets = {-0.4, 0.0, 0.4};
            for (double ox : offsets) {
                for (double oz : offsets) {
                    for (double oy : new double[]{-1.0, -0.5, 0.0}) {
                        Block b = to.getWorld().getBlockAt((int) Math.floor(px + ox), (int) Math.floor(py + oy), (int) Math.floor(pz + oz));
                        if (!b.isPassable() || b.getType() == Material.WATER || b.getType() == Material.LADDER || b.getType() == Material.SCAFFOLDING) {
                            hasGroundBelow = true;
                            break;
                        }
                    }
                    if (hasGroundBelow) break;
                }
                if (hasGroundBelow) break;
            }

            if (!hasGroundBelow) {
                int airCount = airTicks.getOrDefault(uid, 0) + 1;
                airTicks.put(uid, airCount);

                // If ascending in the air for more than 25 consecutive move packets without ground or effects
                if (dy > 0.35 && airCount > 25) {
                    watchdog.penalize(player, 5, "Vliegen in survival / Fly Hack (Ascending in mid-air)");
                    airTicks.put(uid, 0);
                    return;
                }

                // If hovering at exact same Y in mid-air for more than 50 packets without falling,
                // and there are NO solid blocks within 3 blocks directly beneath
                if (Math.abs(dy) < 0.0001 && airCount > 50) {
                    boolean nearbyFloor = false;
                    for (int dyCheck = 1; dyCheck <= 3; dyCheck++) {
                        Block b = to.clone().subtract(0, dyCheck, 0).getBlock();
                        if (!b.isPassable()) {
                            nearbyFloor = true;
                            break;
                        }
                    }
                    if (!nearbyFloor) {
                        // Rubberband player to avoid false-positive kicks from client jitter / bridging
                        player.teleport(from);
                        airTicks.put(uid, 0);
                        return;
                    }
                }
            } else {
                airTicks.put(uid, 0);
            }
        } else {
            airTicks.put(uid, 0);
        }
    }

    /**
     * Combat Reach & KillAura checks.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        double distance = player.getEyeLocation().distance(target.getLocation());
        // Max vanilla Java reach is ~3.0 - 3.5 blocks. Bedrock Edition has native reach up to 5.5 - 6.0 blocks with touch controls
        boolean isBedrock = (plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrock(player))
                || player.getName().startsWith(".");
        double maxAllowedReach = isBedrock ? 6.50 : 4.60;
        if (distance > maxAllowedReach) {
            watchdog.penalize(player, 15, "Onmogelijke aanvalsafstand / Reach Hack (" + String.format("%.2f", distance) + " blocks)");
        }
    }

    /**
     * FastBreak / Nuker checks.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;

        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastBreakTime.getOrDefault(uid, 0L);
        lastBreakTime.put(uid, now);

        // Detect multiple block breaks in less than 40ms (impossible without Nuker/FastBreak)
        if (now - last < 40) {
            int count = breakCount.getOrDefault(uid, 0) + 1;
            breakCount.put(uid, count);
            if (count > 4) {
                watchdog.penalize(player, 20, "Te snel blokken breken / FastBreak Nuker Hack (" + count + " blocks in <50ms)");
                breakCount.put(uid, 0);
            }
        } else {
            breakCount.put(uid, 0);
        }
    }
}
