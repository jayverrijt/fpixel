package net.fpixel.game.scuffed;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages FPixel Scuffed Mode: toggles chaotic, game-breaking modifiers for minigame arenas.
 */
public class ScuffedModeManager {

    public enum Modifier {
        INSTABOOM_TNT("§c§lInstaboom TNT", "TNT explodes almost immediately on placement!"),
        KNOCKBACK_STICK("§d§lYeet Stick X", "Everyone spawns with a Knockback X stick!"),
        INSANE_HEALTH("§a§lInsane Health Pool", "Every player gets 200 HP (100 hearts)!"),
        ANTI_GRAVITY("§b§lAnti-Gravity", "Moon jump & periodic zero-gravity levitation waves!"),
        TURBO_SPEED("§e§lTurbo Speed V", "Permanent Speed IV + Haste IV!"),
        EXPLOSIVE_PROJECTILES("§6§lRPG Projectiles", "Arrows and snowballs explode on impact!"),
        PLAYER_SWAP("§5§lChaos Swap", "Randomly swaps two alive players every 20 seconds!"),
        PINATA_HITS("§d§lPiñata Hits", "Hitting players bursts random loot (or mobs)!"),
        CURSED_FOOTSTEPS("§4§lCursed Footsteps", "Random primed TNT spawns under players!"),
        PYM_PARTICLES("§3§lPym Particles", "Players randomly shrink (0.45x) or grow (2.1x)!"),
        BUTTER_FINGERS("§e§lButter Fingers", "15% chance to drop your held item when attacking!"),
        CREEPER_RAIN("§2§lCreeper Rain", "Creepers randomly fall from the sky near players!"),
        POTION_ROULETTE("§d§lPotion Roulette", "Random chaotic potion effects every 16 seconds!"),
        TRAMPOLINE_BOUNCE("§a§lTrampoline Chaos", "Randomly launches players sky-high!"),
        GLASS_CANNON("§c§lCrit Lightning", "20% chance for a 5x damage lightning strike!"),
        HOT_POTATO("§6§lHot Potato", "Pass the glowing potato by hitting someone before it explodes!");

        private final String displayName;
        private final String description;

        Modifier(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    private final FPixelPlugin plugin;
    private final Random rng = new Random();
    private boolean enabled = false;
    private final Map<String, Set<Modifier>> arenaModifiers = new ConcurrentHashMap<>();
    private final Map<String, UUID> hotPotatoHolder = new ConcurrentHashMap<>();
    private final Map<String, Integer> hotPotatoTimer = new ConcurrentHashMap<>();

    public ScuffedModeManager(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;

        if (enabled) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ChatUtil.sendTitle(p, "§c§lSCUFFED MODE §a§lON", "§ePrepare for total minigame chaos!", 10, 50, 15);
                ChatUtil.playSound(p, Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.4f);
            }
            Bukkit.broadcast(ChatUtil.component("§c§l[SCUFFED] §eScuffed Mode is nu §a§lAAN§e! Minigames krijgen gestoorde modifiers!"));

            // Apply immediately to any already-running IN_GAME arenas
            if (plugin.getGameManager() != null) {
                for (Arena arena : plugin.getGameManager().getArenas()) {
                    if (arena.getState() == GameState.IN_GAME) {
                        onArenaStart(arena);
                    }
                }
            }
        } else {
            for (Player p : Bukkit.getOnlinePlayers()) {
                resetPlayerAttributes(p);
                ChatUtil.sendTitle(p, "§c§lSCUFFED MODE §c§lOFF", "§7Back to normal gameplay.", 10, 40, 10);
                ChatUtil.playSound(p, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f);
            }
            arenaModifiers.clear();
            Bukkit.broadcast(ChatUtil.component("§c§l[SCUFFED] §eScuffed Mode is nu §c§lUIT§e."));
        }
    }

    public Set<Modifier> getActiveModifiers(Arena arena) {
        if (!enabled || arena == null) return Collections.emptySet();
        return arenaModifiers.getOrDefault(arena.getId().toLowerCase(), Collections.emptySet());
    }

    public boolean hasModifier(Arena arena, Modifier mod) {
        return getActiveModifiers(arena).contains(mod);
    }

    /**
     * Rolls a chaotic set of modifiers for an arena at match start and applies initial player effects.
     */
    public void onArenaStart(Arena arena) {
        if (!enabled || arena == null) return;

        List<Modifier> pool = new ArrayList<>(Arrays.asList(Modifier.values()));
        Collections.shuffle(pool, rng);
        int count = 4 + rng.nextInt(3); // 4 to 6 modifiers per match
        Set<Modifier> chosen = EnumSet.noneOf(Modifier.class);
        for (int i = 0; i < Math.min(count, pool.size()); i++) {
            chosen.add(pool.get(i));
        }
        arenaModifiers.put(arena.getId().toLowerCase(), chosen);

        arena.broadcast("§c§m--------------------------------------------------");
        arena.broadcast("§c§l          ⚡ SCUFFED MODE ACTIVE ⚡");
        for (Modifier mod : chosen) {
            arena.broadcast(" §8▸ " + mod.getDisplayName() + " §7- §f" + mod.getDescription());
        }
        arena.broadcast("§c§m--------------------------------------------------");

        for (Player p : arena.getBukkitPlayers()) {
            applyStartModifiers(p, chosen);
        }
    }

    private void applyStartModifiers(Player player, Set<Modifier> mods) {
        if (mods.contains(Modifier.KNOCKBACK_STICK)) {
            ItemStack stick = new ItemBuilder(Material.STICK)
                    .name("§d§lYEET STICK X")
                    .lore("§7Send enemies into orbit!")
                    .build();
            stick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 10);
            player.getInventory().addItem(stick);
        }

        if (mods.contains(Modifier.INSANE_HEALTH)) {
            setMaxHealth(player, 200.0);
            player.setHealth(200.0);
        }

        if (mods.contains(Modifier.ANTI_GRAVITY)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 600, 4, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 600, 0, false, false));
        }

        if (mods.contains(Modifier.TURBO_SPEED)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 600, 3, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 600, 3, false, false));
        }

        if (mods.contains(Modifier.PYM_PARTICLES)) {
            applyRandomScale(player);
        }
    }

    /**
     * Called once per second from Arena.tick() while IN_GAME.
     */
    public void onArenaTick(Arena arena, int gameTime) {
        if (!enabled || arena == null) return;
        Set<Modifier> mods = getActiveModifiers(arena);
        if (mods.isEmpty()) return;

        List<Player> alive = arena.getBukkitPlayers();
        if (alive.isEmpty()) return;

        // Periodic zero-gravity levitation burst
        if (mods.contains(Modifier.ANTI_GRAVITY) && gameTime > 0 && gameTime % 15 == 0) {
            for (Player p : alive) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 1, false, false));
                ChatUtil.sendActionBar(p, "§b§l☁ ZERO-G WAVE!");
            }
        }

        // Chaos Swap every 20 seconds
        if (mods.contains(Modifier.PLAYER_SWAP) && gameTime > 0 && gameTime % 20 == 0 && alive.size() >= 2) {
            List<Player> shuffled = new ArrayList<>(alive);
            Collections.shuffle(shuffled, rng);
            Player a = shuffled.get(0);
            Player b = shuffled.get(1);
            Location locA = a.getLocation().clone();
            Location locB = b.getLocation().clone();
            a.teleport(locB);
            b.teleport(locA);
            ChatUtil.sendTitle(a, "§5§lSWAPPED!", "§7With " + b.getName(), 5, 25, 5);
            ChatUtil.sendTitle(b, "§5§lSWAPPED!", "§7With " + a.getName(), 5, 25, 5);
            ChatUtil.playSound(a, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            ChatUtil.playSound(b, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        }

        // Cursed Footsteps: spawn primed TNT under a random alive player every 7 seconds
        if (mods.contains(Modifier.CURSED_FOOTSTEPS) && gameTime > 0 && gameTime % 7 == 0) {
            Player target = alive.get(rng.nextInt(alive.size()));
            if (target.getWorld() != null) {
                TNTPrimed tnt = target.getWorld().spawn(target.getLocation(), TNTPrimed.class);
                tnt.setFuseTicks(mods.contains(Modifier.INSTABOOM_TNT) ? 10 : 35);
                ChatUtil.sendActionBar(target, "§4§l💣 CURSED FOOTSTEP! RUN!");
            }
        }

        // Pym Particles: re-roll player scale every 18 seconds
        if (mods.contains(Modifier.PYM_PARTICLES) && gameTime > 0 && gameTime % 18 == 0) {
            for (Player p : alive) {
                applyRandomScale(p);
            }
        }

        // Creeper Rain every 14 seconds
        if (mods.contains(Modifier.CREEPER_RAIN) && gameTime > 0 && gameTime % 14 == 0) {
            Player target = alive.get(rng.nextInt(alive.size()));
            if (target.getWorld() != null) {
                Location dropLoc = target.getLocation().add(rng.nextInt(5) - 2, 10, rng.nextInt(5) - 2);
                target.getWorld().spawnEntity(dropLoc, EntityType.CREEPER);
                ChatUtil.sendActionBar(target, "§2§l☄ INCOMING CREEPER!");
            }
        }

        // Potion Roulette every 16 seconds
        if (mods.contains(Modifier.POTION_ROULETTE) && gameTime > 0 && gameTime % 16 == 0) {
            PotionEffectType[] chaosEffects = {
                    PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA,
                    PotionEffectType.REGENERATION, PotionEffectType.JUMP_BOOST,
                    PotionEffectType.LEVITATION, PotionEffectType.SPEED
            };
            PotionEffectType picked = chaosEffects[rng.nextInt(chaosEffects.length)];
            int amp = (picked == PotionEffectType.JUMP_BOOST || picked == PotionEffectType.SPEED) ? 4 : 1;
            for (Player p : alive) {
                p.addPotionEffect(new PotionEffect(picked, 80, amp, false, false));
                ChatUtil.sendActionBar(p, "§d§l🎲 POTION ROULETTE: §e" + picked.getKey().getKey().toUpperCase());
            }
        }

        // Trampoline Chaos every 9 seconds
        if (mods.contains(Modifier.TRAMPOLINE_BOUNCE) && gameTime > 0 && gameTime % 9 == 0) {
            Player target = alive.get(rng.nextInt(alive.size()));
            target.setVelocity(target.getVelocity().setY(1.25));
            ChatUtil.playSound(target, Sound.ENTITY_SLIME_JUMP, 1.0f, 1.3f);
            ChatUtil.sendActionBar(target, "§a§lBOING!");
        }

        // Hot Potato timer & assignment
        if (mods.contains(Modifier.HOT_POTATO)) {
            String key = arena.getId().toLowerCase();
            UUID holderId = hotPotatoHolder.get(key);
            if (holderId == null && gameTime > 0 && gameTime % 22 == 0) {
                Player picked = alive.get(rng.nextInt(alive.size()));
                hotPotatoHolder.put(key, picked.getUniqueId());
                hotPotatoTimer.put(key, 6);
                picked.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 140, 0, false, false));
                ChatUtil.sendTitle(picked, "§6§l🥔 HOT POTATO!", "§cHit someone within 6s!", 5, 30, 5);
            } else if (holderId != null) {
                int rem = hotPotatoTimer.getOrDefault(key, 0) - 1;
                Player holder = Bukkit.getPlayer(holderId);
                if (holder == null || !holder.isOnline() || !arena.getAlivePlayers().contains(holder)) {
                    hotPotatoHolder.remove(key);
                    hotPotatoTimer.remove(key);
                } else if (rem <= 0) {
                    hotPotatoHolder.remove(key);
                    hotPotatoTimer.remove(key);
                    holder.removePotionEffect(PotionEffectType.GLOWING);
                    if (holder.getWorld() != null) {
                        holder.getWorld().createExplosion(holder.getLocation(), 3.0f, false, false, holder);
                    }
                    ChatUtil.sendTitle(holder, "§4§l💥 BOOM!", "§7You held the Hot Potato too long!", 5, 30, 5);
                } else {
                    hotPotatoTimer.put(key, rem);
                    ChatUtil.sendActionBar(holder, "§6§l🥔 HOT POTATO: §c" + rem + "s!");
                }
            }
        }
    }

    public void onBlockPlace(Arena arena, BlockPlaceEvent event) {
        if (!hasModifier(arena, Modifier.INSTABOOM_TNT)) return;
        if (event.getBlockPlaced().getType() == Material.TNT) {
            event.getBlockPlaced().setType(Material.AIR, false);
            Location loc = event.getBlockPlaced().getLocation().add(0.5, 0.1, 0.5);
            if (loc.getWorld() != null) {
                TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class);
                tnt.setFuseTicks(1);
                tnt.setSource(event.getPlayer());
            }
        }
    }

    public void onProjectileHit(Arena arena, ProjectileHitEvent event) {
        if (!hasModifier(arena, Modifier.EXPLOSIVE_PROJECTILES)) return;
        Projectile proj = event.getEntity();
        Location loc = proj.getLocation();
        if (loc.getWorld() != null) {
            loc.getWorld().createExplosion(loc, 2.2f, false, false, proj.getShooter() instanceof Player p ? p : null);
        }
    }

    public void onPlayerDamage(Arena arena, EntityDamageByEntityEvent event) {
        if (!enabled || arena == null) return;
        Set<Modifier> mods = getActiveModifiers(arena);

        if (mods.contains(Modifier.PINATA_HITS) && event.getEntity() instanceof Player victim && victim.getWorld() != null) {
            if (rng.nextInt(100) < 35) {
                Material[] loot = {
                        Material.GOLDEN_APPLE, Material.ENDER_PEARL, Material.TNT,
                        Material.DIAMOND, Material.COOKED_BEEF, Material.SNOWBALL
                };
                ItemStack drop = new ItemStack(loot[rng.nextInt(loot.length)], 1);
                var item = victim.getWorld().dropItemNaturally(victim.getLocation().add(0, 1, 0), drop);
                item.setVelocity(new Vector((rng.nextDouble() - 0.5) * 0.6, 0.5, (rng.nextDouble() - 0.5) * 0.6));
            } else if (rng.nextInt(100) < 10) {
                victim.getWorld().spawnEntity(victim.getLocation().add(0, 1, 0), EntityType.CHICKEN);
            }
        }

        if (mods.contains(Modifier.BUTTER_FINGERS) && event.getDamager() instanceof Player attacker) {
            ItemStack hand = attacker.getInventory().getItemInMainHand();
            if (hand != null && hand.getType() != Material.AIR && rng.nextInt(100) < 15) {
                attacker.getInventory().setItemInMainHand(null);
                if (attacker.getWorld() != null) {
                    attacker.getWorld().dropItemNaturally(attacker.getLocation(), hand);
                }
                ChatUtil.sendActionBar(attacker, "§e§l🧈 BUTTER FINGERS! Dropped your item!");
            }
        }

        if (mods.contains(Modifier.GLASS_CANNON) && event.getEntity() instanceof Player victim && rng.nextInt(100) < 20) {
            event.setDamage(event.getDamage() * 5.0);
            if (victim.getWorld() != null) {
                victim.getWorld().strikeLightningEffect(victim.getLocation());
            }
            if (event.getDamager() instanceof Player attacker) {
                ChatUtil.sendActionBar(attacker, "§c§l⚡ 5x CRIT STRIKE!");
            }
        }

        if (mods.contains(Modifier.HOT_POTATO) && event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {
            String key = arena.getId().toLowerCase();
            UUID holderId = hotPotatoHolder.get(key);
            if (holderId != null && holderId.equals(attacker.getUniqueId())) {
                hotPotatoHolder.put(key, victim.getUniqueId());
                attacker.removePotionEffect(PotionEffectType.GLOWING);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 140, 0, false, false));
                ChatUtil.sendActionBar(attacker, "§aPassed the Hot Potato to " + victim.getName() + "!");
                ChatUtil.sendTitle(victim, "§6§l🥔 HOT POTATO!", "§cHit someone before it blows!", 5, 25, 5);
            }
        }
    }

    public void onArenaReset(Arena arena) {
        if (arena != null) {
            String key = arena.getId().toLowerCase();
            arenaModifiers.remove(key);
            hotPotatoHolder.remove(key);
            hotPotatoTimer.remove(key);
            for (Player p : arena.getAllBukkitPlayers()) {
                resetPlayerAttributes(p);
            }
        }
    }

    public void resetPlayerAttributes(Player player) {
        if (player == null) return;
        setMaxHealth(player, 20.0);
        if (player.getHealth() > 20.0) {
            player.setHealth(20.0);
        }
        setScale(player, 1.0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private void applyRandomScale(Player player) {
        double scale = rng.nextBoolean() ? 0.45 : 2.1;
        setScale(player, scale);
        ChatUtil.sendActionBar(player, scale < 1.0 ? "§3§l🔬 SHRUNK! (0.45x)" : "§3§l🦖 GIANT! (2.1x)");
    }

    private void setMaxHealth(Player player, double max) {
        try {
            AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(max);
        } catch (Throwable ignored) {}
    }

    private void setScale(Player player, double scale) {
        try {
            AttributeInstance attr = player.getAttribute(Attribute.SCALE);
            if (attr != null) attr.setBaseValue(scale);
        } catch (Throwable ignored) {}
    }
}
