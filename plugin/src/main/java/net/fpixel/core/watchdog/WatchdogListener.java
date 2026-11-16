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
public class WatchdogListener implements Listener, PluginMessageListener {

    private final FPixelPlugin plugin;
    private final WatchdogManager watchdog;

    // Movement tracking
    private final Map<UUID, Integer> airTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBreakTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> breakCount = new ConcurrentHashMap<>();

    public WatchdogListener(FPixelPlugin plugin, WatchdogManager watchdog) {
        this.plugin = plugin;
        this.watchdog = watchdog;

        // Register incoming plugin channels for client brand interception
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "minecraft:brand", this);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "MC|Brand", this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equalsIgnoreCase("minecraft:brand") || channel.equalsIgnoreCase("MC|Brand")) {
            try {
                // Packet format: varint or raw UTF-8 string
                String brand = new String(message, StandardCharsets.UTF_8).trim();
                // Strip non-printable ascii characters often present from VarInt prefix
                brand = brand.replaceAll("[^\\x20-\\x7E]", "");
                watchdog.processClientBrand(player, brand);
            } catch (Exception ignored) {}
        }
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
        double dy = to.getY() - from.getY();
        UUID uid = player.getUniqueId();

        boolean inLiquidOrClimbing = player.isInWater() || player.isInLava() || player.isClimbing();
        boolean hasJumpBoost = player.hasPotionEffect(PotionEffectType.JUMP_BOOST) || player.hasPotionEffect(PotionEffectType.LEVITATION);

        if (!inLiquidOrClimbing && !hasJumpBoost) {
            // Check if there are blocks immediately beneath
            Block ground = to.clone().subtract(0, 0.5, 0).getBlock();
            boolean hasGroundBelow = ground.getType().isSolid() || ground.getType() == Material.WATER || ground.getType() == Material.LADDER || ground.getType() == Material.SCAFFOLDING;

            if (!hasGroundBelow) {
                int airCount = airTicks.getOrDefault(uid, 0) + 1;
                airTicks.put(uid, airCount);

                // If ascending in the air for more than 15 consecutive move packets without ground or effects
                if (dy > 0.35 && airCount > 15) {
                    watchdog.penalize(player, 15, "Vliegen in survival / Fly Hack (Ascending in mid-air)");
                    airTicks.put(uid, 0);
                    return;
                }

                // If hovering at exact same Y in mid-air for more than 20 packets without falling
                if (Math.abs(dy) < 0.0001 && airCount > 25) {
                    watchdog.penalize(player, 15, "Zweven in de lucht / Hover Hack");
                    airTicks.put(uid, 0);
                    return;
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
        // Max vanilla reach is ~3.0 - 3.5 blocks. Give generous buffer for ping (4.6 blocks)
        if (distance > 4.60) {
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
