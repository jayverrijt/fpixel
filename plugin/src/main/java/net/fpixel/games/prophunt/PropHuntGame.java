package net.fpixel.games.prophunt;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PropHuntGame extends Arena {

    public enum Role {
        PROP("§a§lPROP", ChatColor.GREEN),
        HUNTER("§c§lHUNTER", ChatColor.RED);

        private final String display;
        private final ChatColor color;

        Role(String display, ChatColor color) {
            this.display = display;
            this.color = color;
        }

        public String getDisplay() { return display; }
        public ChatColor getColor() { return color; }
    }

    private static final Material[] PROP_MATERIALS = {
            Material.CRAFTING_TABLE,
            Material.FURNACE,
            Material.BOOKSHELF,
            Material.ANVIL,
            Material.CHEST,
            Material.BARREL,
            Material.MELON,
            Material.HAY_BLOCK,
            Material.CAULDRON,
            Material.TNT
    };

    private static final Sound[] TAUNT_SOUNDS = {
            Sound.ENTITY_VILLAGER_AMBIENT,
            Sound.ENTITY_CAT_PURREOW,
            Sound.BLOCK_BELL_USE,
            Sound.ENTITY_GOAT_SCREAMING_PREPARE_RAM,
            Sound.ENTITY_CHICKEN_AMBIENT,
            Sound.BLOCK_ANVIL_LAND,
            Sound.ENTITY_GHAST_SCREAM
    };

    private final Map<UUID, Role> roles = new HashMap<>();
    private final Map<UUID, Material> propDisguises = new HashMap<>();
    private final Map<UUID, BlockDisplay> propDisplays = new HashMap<>();
    private final Set<UUID> aliveProps = new HashSet<>();
    private final Set<UUID> aliveHunters = new HashSet<>();
    private final Map<UUID, Long> lastTauntTime = new HashMap<>();

    private Location hunterSpawn;
    private Location hunterReleaseLocation;
    private final List<Location> propSpawns = new ArrayList<>();

    private boolean isHidingPhase = true;
    private int hidingPhaseTime = 20; // 20 seconds for props to hide
    private int maxGameDuration = 180; // 3 minutes total

    public PropHuntGame(FPixelPlugin plugin, String id, String displayName) {
        super(plugin, id, displayName, "PROP_HUNT", 2, 4);
    }

    public void setHunterSpawn(Location loc) { this.hunterSpawn = loc; }
    public void setHunterReleaseLocation(Location loc) { this.hunterReleaseLocation = loc; }
    public void addPropSpawn(Location loc) { propSpawns.add(loc); }
    public Location getHunterSpawn() { return hunterSpawn; }
    public Location getHunterReleaseLocation() { return hunterReleaseLocation != null ? hunterReleaseLocation : hunterSpawn; }
    public List<Location> getPropSpawns() { return propSpawns; }

    @Override
    public void onGameStart() {
        roles.clear();
        propDisguises.clear();
        clearAllDisplays();
        aliveProps.clear();
        aliveHunters.clear();
        lastTauntTime.clear();
        isHidingPhase = true;

        List<UUID> all = new ArrayList<>(players);
        all.addAll(simulatedPlayers.keySet());

        if (all.size() < 2) return;

        Collections.shuffle(all);

        // 1 Hunter, remaining are Props
        UUID hunterUuid = all.get(0);
        roles.put(hunterUuid, Role.HUNTER);
        aliveHunters.add(hunterUuid);

        for (int i = 1; i < all.size(); i++) {
            UUID pUuid = all.get(i);
            roles.put(pUuid, Role.PROP);
            aliveProps.add(pUuid);

            // Assign random prop block
            Material propMat = PROP_MATERIALS[new Random().nextInt(PROP_MATERIALS.length)];
            propDisguises.put(pUuid, propMat);
        }

        // Initialize Hunter
        Player hunter = Bukkit.getPlayer(hunterUuid);
        Location hSpawn = hunterSpawn != null ? hunterSpawn : lobbySpawn;
        if (hunter != null) {
            hunter.teleport(hSpawn);
            hunter.setGameMode(GameMode.ADVENTURE);
            hunter.getInventory().clear();
            hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, hidingPhaseTime * 20, 1, false, false));
            hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, hidingPhaseTime * 20, 10, false, false));
            giveHunterItems(hunter);

            ChatUtil.sendTitle(hunter, "§c§lYOU ARE THE HUNTER", "§7Wait 20s while props hide!", 10, 60, 20);
            ChatUtil.sendMessage(hunter, "§c§lROLE: §4Hunter");
            ChatUtil.sendMessage(hunter, "§7• Find and eliminate disguised props!");
            ChatUtil.sendMessage(hunter, "§c• Penalty: Hitting non-prop blocks damages you!");
            ChatUtil.playSound(hunter, Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.0f);
        }

        net.fpixel.core.testing.SimulatedPlayer hunterBot = simulatedPlayers.get(hunterUuid);
        if (hunterBot != null) {
            hunterBot.spawnEntity(hSpawn, "PROP_HUNT");
        }

        // Initialize Props
        int pIdx = 0;
        for (UUID propUuid : aliveProps) {
            Location pSpawn = propSpawns.isEmpty() ? lobbySpawn : propSpawns.get(pIdx % propSpawns.size());
            pIdx++;

            Material disguise = propDisguises.get(propUuid);

            Player p = Bukkit.getPlayer(propUuid);
            if (p != null) {
                p.teleport(pSpawn);
                p.setGameMode(GameMode.ADVENTURE);
                p.getInventory().clear();
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, hidingPhaseTime * 20, 0, false, false));

                givePropItems(p);
                spawnPropDisplay(p, disguise);

                ChatUtil.sendTitle(p, "§a§lYOU ARE A PROP!", "§7Disguised as: §e" + disguise.name(), 10, 60, 20);
                ChatUtil.sendMessage(p, "§a§lROLE: §2Prop (" + disguise.name() + ")");
                ChatUtil.sendMessage(p, "§7• Crouch/Sneak to lock into the block grid!");
                ChatUtil.sendMessage(p, "§7• Right-click your whistle to Taunt for coins!");
                ChatUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            }

            net.fpixel.core.testing.SimulatedPlayer propBot = simulatedPlayers.get(propUuid);
            if (propBot != null) {
                propBot.spawnEntity(pSpawn, "PROP_HUNT");
            }
        }

        broadcast("§e§m--------------------------------------------------");
        broadcast("                §6§lPROP HUNT");
        broadcast("  §7Hiders are disguised as room props!");
        broadcast("  §7Hunters are blindfolded for 20 seconds!");
        broadcast("§e§m--------------------------------------------------");
    }

    private void giveHunterItems(Player hunter) {
        hunter.getInventory().clear();
        ItemStack crowbar = new ItemBuilder(Material.IRON_SWORD)
                .name("§c§lHunter Crowbar")
                .lore("§7Strike suspicious props!", "§cMiss penalty: -1 Heart")
                .build();
        ItemStack shotgun = new ItemBuilder(Material.CROSSBOW)
                .name("§6§lHunter Shotgun")
                .lore("§7Shoot props from distance!", "§cMiss penalty: -1 Heart")
                .build();
        hunter.getInventory().setItem(0, crowbar);
        hunter.getInventory().setItem(1, shotgun);
        hunter.getInventory().setItem(9, new ItemStack(Material.ARROW, 64));
    }

    private void givePropItems(Player prop) {
        prop.getInventory().clear();
        ItemStack taunt = new ItemBuilder(Material.BELL)
                .name("§e§lTaunt Whistle §7(Right Click)")
                .lore("§7Make a funny sound to mock hunters!", "§a+15 Coins per taunt (10s cooldown)")
                .build();
        prop.getInventory().setItem(0, taunt);
    }

    private void spawnPropDisplay(Player player, Material material) {
        try {
            BlockData blockData = material.createBlockData();
            BlockDisplay display = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);
            display.setBlock(blockData);
            display.setInterpolationDuration(2);
            display.setTeleportDuration(2);
            // Center block around player feet
            org.bukkit.util.Transformation trans = new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(-0.5f, 0f, -0.5f),
                    new org.joml.Quaternionf(),
                    new org.joml.Vector3f(1.0f, 1.0f, 1.0f),
                    new org.joml.Quaternionf()
            );
            display.setTransformation(trans);
            propDisplays.put(player.getUniqueId(), display);
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not spawn BlockDisplay for prop: " + t.getMessage());
        }
    }

    @Override
    public void handleGameTick() {
        if (state != GameState.IN_GAME) return;

        // 1. Hiding phase countdown
        if (isHidingPhase) {
            int remaining = Math.max(0, hidingPhaseTime - gameTime);
            if (remaining > 0) {
                for (Player p : getAllBukkitPlayers()) {
                    ChatUtil.sendActionBar(p, "§e§lHiding Phase: §f" + remaining + "s §7remaining...");
                }
            } else {
                isHidingPhase = false;
                Location releaseLoc = getHunterReleaseLocation();
                for (UUID hUuid : aliveHunters) {
                    Player h = Bukkit.getPlayer(hUuid);
                    if (h != null) {
                        h.removePotionEffect(PotionEffectType.BLINDNESS);
                        h.removePotionEffect(PotionEffectType.SLOWNESS);
                        if (releaseLoc != null) {
                            h.teleport(releaseLoc);
                        }
                        ChatUtil.sendTitle(h, "§c§lSEEK AND DESTROY!", "§eFind all hidden props!", 5, 40, 10);
                        ChatUtil.playSound(h, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    }
                    net.fpixel.core.testing.SimulatedPlayer bot = simulatedPlayers.get(hUuid);
                    if (bot != null && releaseLoc != null && bot.getEntity() != null) {
                        bot.getEntity().teleport(releaseLoc);
                    }
                }
                broadcast("§c§lHUNTERS RELEASED! §7Props, stay still and blend in!");
            }
        }

        // 2. Update Prop Displays & Grid Lock
        for (UUID propUuid : aliveProps) {
            Player p = Bukkit.getPlayer(propUuid);
            if (p == null) continue;

            BlockDisplay display = propDisplays.get(propUuid);
            if (display != null && display.isValid()) {
                if (p.isSneaking()) {
                    // Lock into grid coordinates
                    Location gridLoc = new Location(
                            p.getWorld(),
                            Math.floor(p.getLocation().getX()) + 0.5,
                            Math.floor(p.getLocation().getY()),
                            Math.floor(p.getLocation().getZ()) + 0.5,
                            0f,
                            0f
                    );
                    display.teleport(gridLoc);
                    ChatUtil.sendActionBar(p, "§a§lLOCKED IN PLACE §7(Release sneak to move)");
                } else {
                    display.teleport(p.getLocation());
                    Material disguise = propDisguises.getOrDefault(propUuid, Material.CRAFTING_TABLE);
                    ChatUtil.sendActionBar(p, "§e[Disguise: " + disguise.name() + "] §7Sneak to lock into grid!");
                }
            }

            // 3. Auto-Taunt if prop hasn't taunted for 25s
            long now = System.currentTimeMillis();
            long last = lastTauntTime.getOrDefault(propUuid, 0L);
            if (now - last > 25000 && !isHidingPhase) {
                triggerTaunt(p, true);
            }
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (state != GameState.IN_GAME) return;
        Player p = event.getPlayer();
        if (isHidingPhase && roles.get(p.getUniqueId()) == Role.HUNTER) {
            // Keep hunter in spawn during hiding phase
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ())) {
                event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
            }
        }
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (state != GameState.IN_GAME) return;
        Player player = event.getPlayer();

        // 1. Prop Manual Taunt Whistle
        if (roles.get(player.getUniqueId()) == Role.PROP && event.getItem() != null && event.getItem().getType() == Material.BELL) {
            if (event.getAction().name().contains("RIGHT")) {
                event.setCancelled(true);
                long now = System.currentTimeMillis();
                long last = lastTauntTime.getOrDefault(player.getUniqueId(), 0L);
                if (now - last < 10000) {
                    long remaining = 10 - ((now - last) / 1000);
                    ChatUtil.sendActionBar(player, "§cTaunt whistle on cooldown: " + remaining + "s");
                    return;
                }
                triggerTaunt(player, false);
            }
            return;
        }

        // 2. Hunter Miss Penalty on clicking normal blocks (GMod rule)
        if (!isHidingPhase && roles.get(player.getUniqueId()) == Role.HUNTER) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
                // Check if this was a disguised prop
                boolean hitProp = false;
                for (UUID propUuid : aliveProps) {
                    Player prop = Bukkit.getPlayer(propUuid);
                    if (prop != null && prop.getLocation().distanceSquared(event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5)) <= 2.2) {
                        hitProp = true;
                        break;
                    }
                }
                if (!hitProp) {
                    applyHunterMissPenalty(player);
                }
            }
        }
    }

    private void triggerTaunt(Player prop, boolean auto) {
        lastTauntTime.put(prop.getUniqueId(), System.currentTimeMillis());

        Sound sound = TAUNT_SOUNDS[new Random().nextInt(TAUNT_SOUNDS.length)];
        prop.getWorld().playSound(prop.getLocation(), sound, 1.2f, 1.0f);
        prop.getWorld().spawnParticle(Particle.NOTE, prop.getLocation().add(0, 1.2, 0), 5, 0.3, 0.3, 0.3, 0.1);

        String tag = auto ? "§6§l[AUTO-TAUNT]" : "§e§l[PROP TAUNT]";
        for (Player p : getAllBukkitPlayers()) {
            if (p.getLocation().distanceSquared(prop.getLocation()) <= 400) { // 20 blocks
                ChatUtil.sendMessage(p, tag + " §7A prop made a sound nearby!");
            }
        }

        FPixelUser user = plugin.getUserManager().getUser(prop);
        user.addCoins(15);
        ChatUtil.sendActionBar(prop, "§a§l+15 Coins §7(Successful Taunt!)");
    }

    private void applyHunterMissPenalty(Player hunter) {
        if (hunter.getHealth() > 2.0) {
            hunter.damage(2.0); // 1 heart penalty
            ChatUtil.sendActionBar(hunter, "§c§l-1♥ Penalty! §7That was an ordinary block!");
            ChatUtil.playSound(hunter, Sound.BLOCK_ANVIL_HIT, 0.5f, 1.8f);
        } else {
            ChatUtil.sendActionBar(hunter, "§c§lCareful! §7Low health - don't spam hits!");
        }
    }

    @Override
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (state != GameState.IN_GAME) return;

        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        Player hunter = null;
        if (damager instanceof Player p) {
            hunter = p;
        } else if (damager instanceof Arrow arrow && arrow.getShooter() instanceof Player p) {
            hunter = p;
        }

        if (hunter == null || roles.get(hunter.getUniqueId()) != Role.HUNTER) {
            event.setCancelled(true);
            return;
        }

        if (isHidingPhase) {
            event.setCancelled(true);
            return;
        }

        if (victim instanceof Player propPlayer && roles.get(propPlayer.getUniqueId()) == Role.PROP) {
            // Hit a REAL prop!
            event.setCancelled(false);
            event.setDamage(10.0); // 2-shot kill

            ChatUtil.playSound(hunter, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
            propPlayer.getWorld().playSound(propPlayer.getLocation(), Sound.ENTITY_SLIME_HURT, 1.0f, 1.0f);
            propPlayer.getWorld().spawnParticle(Particle.DUST, propPlayer.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, new Particle.DustOptions(Color.RED, 1.5f));

            if (propPlayer.getHealth() - 10.0 <= 0.0) {
                event.setCancelled(true);
                eliminateProp(propPlayer, hunter);
            }
        } else {
            event.setCancelled(true);
        }
    }

    private void eliminateProp(Player prop, Player hunter) {
        aliveProps.remove(prop.getUniqueId());
        Material disguise = propDisguises.getOrDefault(prop.getUniqueId(), Material.CRAFTING_TABLE);

        // Remove BlockDisplay
        BlockDisplay display = propDisplays.remove(prop.getUniqueId());
        if (display != null && display.isValid()) {
            display.getWorld().spawnParticle(Particle.POOF, display.getLocation().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.05);
            display.remove();
        }

        prop.removePotionEffect(PotionEffectType.INVISIBILITY);
        prop.setGameMode(GameMode.ADVENTURE);
        prop.getInventory().clear();
        prop.setAllowFlight(true);
        prop.setFlying(true);
        prop.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));

        broadcast("§a§lPROP FOUND! " + hunter.getName() + " §7found §e" + prop.getName() + " §7disguised as §b" + disguise.name() + "!");
        ChatUtil.sendTitle(prop, "§c§lFOUND!", "§7You were caught by " + hunter.getName() + "!", 5, 40, 10);
        ChatUtil.playSound(hunter, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);

        checkWinCondition();
    }

    @Override
    public void checkWinCondition() {
        if (state != GameState.IN_GAME) return;

        // 1. All Props eliminated -> Hunters Win
        if (aliveProps.isEmpty()) {
            endGame(aliveHunters.isEmpty() ? null : Bukkit.getPlayer(aliveHunters.iterator().next()));
            broadcast("§c§lHUNTERS WIN! §7All disguised props have been found!");
            return;
        }

        // 2. All Hunters died/left -> Props Win
        if (aliveHunters.isEmpty()) {
            endGame(null);
            broadcast("§a§lPROPS WIN! §7All hunters have left the game!");
            return;
        }

        // 3. Time out -> Props Win
        if (gameTime >= maxGameDuration) {
            endGame(null);
            broadcast("§a§lPROPS WIN! §7Props survived the entire time without being found!");
        }
    }

    private void clearAllDisplays() {
        for (BlockDisplay d : propDisplays.values()) {
            if (d != null && d.isValid()) {
                d.remove();
            }
        }
        propDisplays.clear();
    }

    @Override
    public void onGameEnd(Player winner) {
        clearAllDisplays();
        if (winner != null) {
            FPixelUser u = plugin.getUserManager().getUser(winner);
            u.addPropHuntWin();
        } else {
            // Props won!
            for (UUID pId : aliveProps) {
                Player p = Bukkit.getPlayer(pId);
                if (p != null) {
                    FPixelUser u = plugin.getUserManager().getUser(p);
                    u.addPropHuntWin();
                }
            }
        }
    }

    @Override
    public void onArenaReset() {
        roles.clear();
        propDisguises.clear();
        clearAllDisplays();
        aliveProps.clear();
        aliveHunters.clear();
        lastTauntTime.clear();
        isHidingPhase = true;
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§7" + id);
        lines.add(" ");

        if (state == GameState.WAITING || state == GameState.STARTING) {
            lines.add("§fPlayers: §a" + players.size() + "/" + maxPlayers);
            lines.add("§fStarting in: §a" + countdown + "s");
            lines.add("  ");
            lines.add("§fMode: §6Prop Hunt");
        } else if (state == GameState.IN_GAME) {
            Role role = roles.getOrDefault(player.getUniqueId(), Role.PROP);
            lines.add("§fRole: " + role.getDisplay());
            if (role == Role.PROP) {
                Material mat = propDisguises.getOrDefault(player.getUniqueId(), Material.CRAFTING_TABLE);
                lines.add("§fProp: §e" + mat.name());
            }
            lines.add(" ");
            lines.add("§fProps Alive: §a" + aliveProps.size());
            lines.add("§fHunters: §c" + aliveHunters.size());

            int timeLeft = Math.max(0, maxGameDuration - gameTime);
            int mins = timeLeft / 60;
            int secs = timeLeft % 60;
            if (isHidingPhase) {
                lines.add("§eHiding: §f" + Math.max(0, hidingPhaseTime - gameTime) + "s");
            } else {
                lines.add("§fTime Left: §e" + String.format("%02d:%02d", mins, secs));
            }
        } else {
            lines.add("§fGame: §eEnding");
        }

        lines.add("   ");
        lines.add("§emc.fpixel.net");
        return lines;
    }
}
