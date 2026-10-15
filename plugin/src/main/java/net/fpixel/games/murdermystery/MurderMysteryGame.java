package net.fpixel.games.murdermystery;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class MurderMysteryGame extends Arena {

    public enum Role {
        MURDERER("§c§lMURDERER", ChatColor.RED),
        DETECTIVE("§9§lDETECTIVE", ChatColor.BLUE),
        INNOCENT("§a§lINNOCENT", ChatColor.GREEN);

        private final String display;
        private final ChatColor color;

        Role(String display, ChatColor color) {
            this.display = display;
            this.color = color;
        }

        public String getDisplay() { return display; }
        public ChatColor getColor() { return color; }
    }

    private final Map<UUID, Role> roles = new HashMap<>();
    private final Map<UUID, Integer> goldCount = new HashMap<>();
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final List<Location> spawnPoints = new ArrayList<>();
    private final List<Location> goldSpawnPoints = new ArrayList<>();
    private final List<Item> activeGold = new ArrayList<>();

    private Location droppedBowLoc = null;
    private Item droppedBowItem = null;

    private int maxGameDuration = 180; // 3 minutes
    private int arrowRechargeSeconds = 0;
    private final Map<UUID, Long> knifeCooldowns = new HashMap<>();

    private UUID murdererUuid = null;
    private UUID detectiveUuid = null;
    private final Map<UUID, Role> preferredRoles = new HashMap<>();

    public void setPreferredRole(UUID uuid, Role role) {
        if (role == null) {
            preferredRoles.remove(uuid);
        } else {
            preferredRoles.put(uuid, role);
        }
    }

    public UUID getMurdererUuid() { return murdererUuid; }
    public UUID getDetectiveUuid() { return detectiveUuid; }
    public Role getPlayerRole(UUID uuid) { return roles.get(uuid); }

    public boolean isMurderer(UUID uuid) {
        if (uuid == null) return false;
        if (murdererUuid != null && uuid.equals(murdererUuid)) return true;
        Role r = roles.get(uuid);
        if (r == Role.MURDERER) return true;
        for (Map.Entry<UUID, Role> e : roles.entrySet()) {
            if (e.getValue() == Role.MURDERER && e.getKey().toString().equalsIgnoreCase(uuid.toString())) {
                return true;
            }
        }
        return false;
    }

    public boolean isMurderer(Player player) {
        if (player == null) return false;
        if (isMurderer(player.getUniqueId())) return true;
        if (murdererUuid != null && (player.getUniqueId().equals(murdererUuid) || player.getUniqueId().toString().equalsIgnoreCase(murdererUuid.toString()))) return true;
        if (player.getInventory().contains(Material.IRON_SWORD)) return true;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && hand.getType() == Material.IRON_SWORD) return true;
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.getType() == Material.IRON_SWORD) return true;
        return false;
    }

    public MurderMysteryGame(FPixelPlugin plugin, String id, String displayName) {
        super(plugin, id, displayName, "MURDER_MYSTERY", 3, 4);
    }

    public void addSpawnPoint(Location loc) {
        spawnPoints.add(loc);
    }

    public void addGoldSpawnPoint(Location loc) {
        goldSpawnPoints.add(loc);
    }

    public List<Location> getSpawnPoints() { return spawnPoints; }
    public List<Location> getGoldSpawnPoints() { return goldSpawnPoints; }

    @Override
    public void onGameStart() {
        roles.clear();
        goldCount.clear();
        alivePlayers.clear();
        activeGold.clear();
        knifeCooldowns.clear();
        droppedBowLoc = null;
        droppedBowItem = null;
        murdererUuid = null;
        detectiveUuid = null;

        List<UUID> humanList = new ArrayList<>(players);
        Collections.shuffle(humanList);
        List<UUID> botList = new ArrayList<>(simulatedPlayers.keySet());
        Collections.shuffle(botList);

        List<UUID> allParticipants = new ArrayList<>(humanList);
        allParticipants.addAll(botList);
        alivePlayers.addAll(allParticipants);

        if (allParticipants.size() < 2) return;

        // Role assignment: respect preferredRoles if set (e.g. from testing commands)
        UUID mId = null;
        UUID dId = null;

        for (UUID u : allParticipants) {
            Role pref = preferredRoles.get(u);
            if (pref == Role.MURDERER && mId == null) {
                mId = u;
            } else if (pref == Role.DETECTIVE && dId == null) {
                dId = u;
            }
        }

        if (mId == null) {
            for (UUID u : allParticipants) {
                if (!u.equals(dId)) {
                    mId = u;
                    break;
                }
            }
        }

        if (dId == null) {
            for (UUID u : allParticipants) {
                if (!u.equals(mId)) {
                    dId = u;
                    break;
                }
            }
        }

        murdererUuid = mId;
        detectiveUuid = dId;
        if (murdererUuid != null) roles.put(murdererUuid, Role.MURDERER);
        if (detectiveUuid != null) roles.put(detectiveUuid, Role.DETECTIVE);

        // Remaining are Innocents
        for (UUID u : allParticipants) {
            if (!u.equals(murdererUuid) && !u.equals(detectiveUuid)) {
                roles.put(u, Role.INNOCENT);
            }
        }

        // Teleport participants to scattered map rooms
        int sIdx = 0;
        for (UUID u : allParticipants) {
            Location spawn = spawnPoints.isEmpty() ? lobbySpawn : spawnPoints.get(sIdx % spawnPoints.size());
            sIdx++;

            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.teleport(spawn);
                p.setGameMode(GameMode.ADVENTURE);
                p.getInventory().clear();
                p.setHealth(20.0);
                p.setFoodLevel(20);

                Role role = roles.get(u);
                announceRole(p, role);
                giveRoleItems(p, role);
            }

            net.fpixel.core.testing.SimulatedPlayer bot = simulatedPlayers.get(u);
            if (bot != null) {
                LivingEntity ent = bot.spawnEntity(spawn, "MURDER_MYSTERY");
                if (ent != null && ent.getEquipment() != null) {
                    Role r = roles.get(u);
                    if (r == Role.MURDERER) {
                        ent.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                    } else if (r == Role.DETECTIVE) {
                        ent.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
                    }
                }
            }
        }

        broadcast("§e§m--------------------------------------------------");
        broadcast("              §6§lMURDER MYSTERY");
        broadcast("  §7A Murderer is among you! Survive or find them!");
        broadcast("  §c1 Murderer §7| §91 Detective §7| §aInnocents");
        broadcast("§e§m--------------------------------------------------");
    }

    private void announceRole(Player p, Role role) {
        switch (role) {
            case MURDERER -> {
                ChatUtil.sendTitle(p, "§c§lMURDERER", "§7Eliminate everyone before time runs out!", 10, 60, 20);
                ChatUtil.sendMessage(p, "§c§lROLE: §4Murderer");
                ChatUtil.sendMessage(p, "§7• Kill all innocents and the detective with your knife!");
                ChatUtil.sendMessage(p, "§7• Right-click your knife to throw it! (4s cooldown)");
                ChatUtil.sendMessage(p, "§7• Use your compass to track nearby players!");
                ChatUtil.playSound(p, Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.0f);
            }
            case DETECTIVE -> {
                ChatUtil.sendTitle(p, "§9§lDETECTIVE", "§7Find and shoot the murderer!", 10, 60, 20);
                ChatUtil.sendMessage(p, "§9§lROLE: §1Detective");
                ChatUtil.sendMessage(p, "§7• You have a Bow. Find and shoot the Murderer!");
                ChatUtil.sendMessage(p, "§7• Be careful: shooting an Innocent will kill you of guilt!");
                ChatUtil.playSound(p, Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.0f);
            }
            case INNOCENT -> {
                ChatUtil.sendTitle(p, "§a§lINNOCENT", "§7Survive and collect 10 gold for a bow!", 10, 60, 20);
                ChatUtil.sendMessage(p, "§a§lROLE: §2Innocent");
                ChatUtil.sendMessage(p, "§7• Stay alive and run from the murderer!");
                ChatUtil.sendMessage(p, "§7• Collect §610 Gold §7to earn a defensive bow!");
                ChatUtil.sendMessage(p, "§7• If the Detective dies, pick up their dropped bow!");
                ChatUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            }
        }
    }

    private void giveRoleItems(Player p, Role role) {
        p.getInventory().clear();
        goldCount.put(p.getUniqueId(), 0);

        if (role == Role.MURDERER) {
            ItemStack knife = new ItemBuilder(Material.IRON_SWORD)
                    .name("§c§lMurderer's Knife")
                    .lore("§7One-shot melee kill.", "§7Right-click or press Q to throw! (4s cooldown)")
                    .build();
            try {
                org.bukkit.inventory.meta.ItemMeta meta = knife.getItemMeta();
                if (meta != null) {
                    org.bukkit.inventory.meta.components.FoodComponent food = meta.getFood();
                    food.setCanAlwaysEat(true);
                    food.setNutrition(0);
                    food.setSaturation(0.0f);
                    meta.setFood(food);
                    knife.setItemMeta(meta);
                }
            } catch (Throwable ignored) {}

            ItemStack tracker = new ItemBuilder(Material.COMPASS)
                    .name("§e§lPlayer Tracker")
                    .lore("§7Points to the nearest survivor.")
                    .build();
            p.getInventory().setItem(0, knife);
            p.getInventory().setItem(1, tracker);
        } else if (role == Role.DETECTIVE) {
            ItemStack bow = new ItemBuilder(Material.BOW)
                    .name("§9§lDetective's Bow")
                    .lore("§7One-shot kills the murderer.", "§cWarning: Shooting innocent kills you!")
                    .build();
            p.getInventory().setItem(0, bow);
            p.getInventory().setItem(9, new ItemStack(Material.ARROW, 1));
        }
    }

    @Override
    public void handleGameTick() {
        if (state != GameState.IN_GAME) return;

        // Void fall protection: if player falls below Y 40, return to spawn
        for (Player p : getAllBukkitPlayers()) {
            if (p.getLocation().getY() < 40.0) {
                Location safe = lobbySpawn != null ? lobbySpawn : (!spawnPoints.isEmpty() ? spawnPoints.get(0) : null);
                if (safe != null) {
                    p.setFallDistance(0);
                    p.setVelocity(new Vector(0, 0, 0));
                    p.teleport(safe);
                }
            }
        }

        // Periodic gold spawning every 5 seconds
        if (gameTime % 5 == 0 && !goldSpawnPoints.isEmpty()) {
            spawnRandomGold();
        }

        // Proximity checks for alive players
        for (Player p : getBukkitPlayers()) {
            if (!alivePlayers.contains(p.getUniqueId()) || isSpectator(p.getUniqueId())) continue;

            Location loc = p.getLocation();
            Role role = roles.get(p.getUniqueId());

            // 1. Gold pickup (Murderers CANNOT pick up gold)
            if (role != Role.MURDERER) {
                Iterator<Item> gIt = activeGold.iterator();
                while (gIt.hasNext()) {
                    Item item = gIt.next();
                    if (!item.isValid()) {
                        gIt.remove();
                        continue;
                    }
                    if (item.getWorld().equals(loc.getWorld()) && item.getLocation().distanceSquared(loc) <= 2.2) {
                        pickupGold(p, item);
                    }
                }
            }

            // 2. Dropped Detective Bow pickup (ONLY Innocents can pick up)
            if (role == Role.INNOCENT && droppedBowLoc != null && droppedBowItem != null && droppedBowItem.isValid()) {
                if (p.getWorld().equals(droppedBowLoc.getWorld()) && p.getLocation().distanceSquared(droppedBowLoc) <= 2.5) {
                    pickupDroppedBow(p, droppedBowItem);
                }
            }

            // 3. Murderer Tracker updating
            if (role == Role.MURDERER) {
                Player nearest = getNearestSurvivor(p);
                if (nearest != null) {
                    p.setCompassTarget(nearest.getLocation());
                }
            }
        }

        // Detective / Hero Arrow Recharge (every 4 seconds)
        arrowRechargeSeconds++;
        if (arrowRechargeSeconds >= 4) {
            arrowRechargeSeconds = 0;
            for (Player p : getBukkitPlayers()) {
                if (alivePlayers.contains(p.getUniqueId()) && p.getInventory().contains(Material.BOW)) {
                    if (!p.getInventory().contains(Material.ARROW)) {
                        p.getInventory().setItem(9, new ItemStack(Material.ARROW, 1));
                        ChatUtil.playSound(p, Sound.ITEM_FLINTANDSTEEL_USE, 0.5f, 1.8f);
                    }
                }
            }
        }

        // Visual effects for dropped bow
        if (droppedBowLoc != null && droppedBowLoc.getWorld() != null) {
            droppedBowLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, droppedBowLoc.clone().add(0, 0.6, 0), 5, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void pickupGold(Player p, Item item) {
        if (!alivePlayers.contains(p.getUniqueId()) || isSpectator(p.getUniqueId())) return;
        Role r = roles.get(p.getUniqueId());
        if (r == Role.MURDERER) return;

        if (item.isValid()) {
            item.remove();
        }
        activeGold.remove(item);

        int count = goldCount.getOrDefault(p.getUniqueId(), 0) + 1;
        goldCount.put(p.getUniqueId(), count);

        ChatUtil.playSound(p, Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.5f);
        ChatUtil.sendActionBar(p, "§6§l+1 Gold §7(" + count + "/10)");

        // Reaching 10 gold gives innocent a bow!
        if (r == Role.INNOCENT && count >= 10 && !p.getInventory().contains(Material.BOW)) {
            ItemStack bow = new ItemBuilder(Material.BOW)
                    .name("§6§lHero's Bow")
                    .lore("§7Acquired with 10 gold!", "§cDo not shoot innocents!")
                    .build();
            p.getInventory().addItem(bow);
            p.getInventory().setItem(9, new ItemStack(Material.ARROW, 1));
            ChatUtil.sendTitle(p, "§6§lBOW ACQUIRED!", "§eYou collected 10 gold!", 5, 40, 10);
            ChatUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    private void pickupDroppedBow(Player p, Item item) {
        if (!alivePlayers.contains(p.getUniqueId()) || isSpectator(p.getUniqueId())) return;
        Role r = roles.get(p.getUniqueId());
        if (r != Role.INNOCENT) return; // Murderer and detectives cannot pick up

        if (droppedBowItem != null && droppedBowItem.isValid()) {
            droppedBowItem.remove();
        }
        if (item != null && item.isValid()) {
            item.remove();
        }
        droppedBowItem = null;
        droppedBowLoc = null;

        roles.put(p.getUniqueId(), Role.DETECTIVE);
        ItemStack bow = new ItemBuilder(Material.BOW)
                .name("§9§lDetective's Bow")
                .lore("§7Picked up from fallen Detective.")
                .build();
        p.getInventory().addItem(bow);
        p.getInventory().setItem(9, new ItemStack(Material.ARROW, 1));

        broadcast("§b§lBOW RECOVERED! " + p.getName() + " §7has picked up the Detective's bow!");
        ChatUtil.sendTitle(p, "§9§lYOU ARE THE DETECTIVE!", "§7Find and eliminate the murderer!", 5, 40, 10);
        ChatUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    @Override
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (state != GameState.IN_GAME) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getEntity() instanceof Player p)) {
            event.setCancelled(true);
            return;
        }

        // Dead players and spectators cannot pick up ANY item!
        if (!alivePlayers.contains(p.getUniqueId()) || isSpectator(p.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        Item itemEntity = event.getItem();
        Material mat = itemEntity.getItemStack().getType();

        // Arrows can NEVER be picked up by anyone!
        if (mat == Material.ARROW) {
            event.setCancelled(true);
            itemEntity.remove();
            return;
        }

        // Murderers cannot pick up Bow or Gold!
        Role role = roles.get(p.getUniqueId());
        if (role == Role.MURDERER || isMurderer(p)) {
            if (mat == Material.BOW || mat == Material.GOLD_INGOT) {
                event.setCancelled(true);
                return;
            }
        }

        // Dropped Bow pickup
        if (mat == Material.BOW) {
            event.setCancelled(true);
            if (role == Role.INNOCENT && !isMurderer(p)) {
                pickupDroppedBow(p, itemEntity);
            }
            return;
        }

        // Gold pickup
        if (mat == Material.GOLD_INGOT) {
            event.setCancelled(true);
            if (role != Role.MURDERER && !isMurderer(p)) {
                pickupGold(p, itemEntity);
            }
            return;
        }
    }

    @Override
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getProjectile() instanceof AbstractArrow arrow) {
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        }
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> event.getEntity().remove(), 1L);
        } else if (event.getEntity() instanceof Snowball sb && sb.getScoreboardTags().contains("thrown_knife")) {
            Location hitLoc = event.getEntity().getLocation();
            if (hitLoc.getWorld() != null) {
                hitLoc.getWorld().playSound(hitLoc, Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 0.8f);
                hitLoc.getWorld().spawnParticle(Particle.BLOCK, hitLoc, 10, 0.2, 0.2, 0.2, Bukkit.createBlockData(Material.IRON_BLOCK));
            }

            if (sb.getShooter() instanceof Player killer) {
                if (event.getHitEntity() instanceof Player hitPlayer) {
                    if (alivePlayers.contains(hitPlayer.getUniqueId()) && !isSpectator(hitPlayer.getUniqueId()) && !hitPlayer.equals(killer)) {
                        eliminatePlayer(hitPlayer, killer, "thrown knife");
                    }
                } else if (event.getHitEntity() != null && event.getHitEntity().getScoreboardTags().contains("fpixel_bot")) {
                    if (plugin.getTestingManager() != null) {
                        net.fpixel.core.testing.SimulatedPlayer bot = plugin.getTestingManager().findBotByEntity(this, event.getHitEntity());
                        if (bot != null && alivePlayers.contains(bot.getUuid())) {
                            eliminateSimulatedPlayer(bot);
                        }
                    }
                }
            }
            Bukkit.getScheduler().runTaskLater(plugin, sb::remove, 1L);
        }
    }

    private void spawnRandomGold() {
        if (activeGold.size() >= 15 || goldSpawnPoints.isEmpty()) return;
        Location spawn = goldSpawnPoints.get(new Random().nextInt(goldSpawnPoints.size()));
        if (spawn.getWorld() == null) return;

        Item item = spawn.getWorld().dropItem(spawn.clone().add(0, 0.2, 0), new ItemStack(Material.GOLD_INGOT));
        item.setGlowing(true);
        activeGold.add(item);
    }

    private Player getNearestSurvivor(Player murderer) {
        Player nearest = null;
        double minDst = Double.MAX_VALUE;
        for (Player p : getBukkitPlayers()) {
            if (p.equals(murderer) || !alivePlayers.contains(p.getUniqueId())) continue;
            double dst = p.getLocation().distanceSquared(murderer.getLocation());
            if (dst < minDst) {
                minDst = dst;
                nearest = p;
            }
        }
        return nearest;
    }

    @Override
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (state != GameState.IN_GAME) return;

        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        if (!(victim instanceof Player victimPlayer) || !alivePlayers.contains(victimPlayer.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        Player attacker = null;
        boolean isArrow = false;
        boolean isKnifeThrow = false;

        if (damager instanceof Player p) {
            attacker = p;
        } else if (damager instanceof Arrow arrow && arrow.getShooter() instanceof Player p) {
            attacker = p;
            isArrow = true;
        } else if (damager instanceof Snowball sb && sb.getShooter() instanceof Player p && sb.getScoreboardTags().contains("thrown_knife")) {
            attacker = p;
            isKnifeThrow = true;
        }

        if (attacker == null || !alivePlayers.contains(attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // 1. Thrown knife hit
        if (isKnifeThrow) {
            event.setCancelled(true);
            if (isMurderer(attacker) && !victimPlayer.equals(attacker)) {
                eliminatePlayer(victimPlayer, attacker, "thrown knife");
            }
            return;
        }

        // 2. Murderer melee knife kill
        if (isMurderer(attacker) && !isArrow) {
            ItemStack inHand = attacker.getInventory().getItemInMainHand();
            ItemStack offHand = attacker.getInventory().getItemInOffHand();
            if ((inHand != null && inHand.getType() == Material.IRON_SWORD) || (offHand != null && offHand.getType() == Material.IRON_SWORD)) {
                event.setCancelled(true);
                eliminatePlayer(victimPlayer, attacker, "stabbed");
                return;
            }
        }

        // 3. Detective / Hero Arrow Hit
        if (isArrow) {
            event.setCancelled(true);
            if (isMurderer(victimPlayer)) {
                // Shot the Murderer! Heroic win!
                eliminatePlayer(victimPlayer, attacker, "shot");
                broadcast("§6§lHEROIC SHOT! §9" + attacker.getName() + " §7shot the §cMurderer §f(" + victimPlayer.getName() + ")!");
                ChatUtil.sendTitle(attacker, "§6§lHEROIC SHOT!", "§aYou eliminated the Murderer!", 10, 60, 20);
                ChatUtil.playSound(attacker, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            } else {
                // Shot an innocent! Guilty death!
                eliminatePlayer(attacker, null, "guilt");
                ChatUtil.sendMessage(attacker, "§c§lYou shot an Innocent! You died of guilt!");
                ChatUtil.sendTitle(attacker, "§4§lYOU KILLED AN INNOCENT!", "§cYou died of guilt.", 5, 40, 10);
            }
            return;
        }

        event.setCancelled(true);
    }

    public void tryThrowKnife(Player player) {
        if (state != GameState.IN_GAME || !alivePlayers.contains(player.getUniqueId()) || isSpectator(player.getUniqueId())) return;
        if (!isMurderer(player)) return;

        long now = System.currentTimeMillis();
        long last = knifeCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 4000) {
            long remaining = (4000 - (now - last) + 999) / 1000;
            ChatUtil.sendActionBar(player, "§c§lKnife throw on cooldown: " + remaining + "s");
            ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            return;
        }
        knifeCooldowns.put(player.getUniqueId(), now);
        throwKnife(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state == GameState.IN_GAME && alivePlayers.contains(player.getUniqueId()) && isMurderer(player)) {
                ChatUtil.sendActionBar(player, "§a§lKNIFE READY TO THROW!");
                ChatUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
            }
        }, 80L);
    }

    private void throwKnife(Player player) {
        Snowball knifeProj = player.launchProjectile(Snowball.class);
        knifeProj.setItem(new ItemStack(Material.IRON_SWORD));
        knifeProj.setVelocity(player.getLocation().getDirection().multiply(1.8));
        knifeProj.setShooter(player);
        knifeProj.addScoreboardTag("thrown_knife");

        ChatUtil.playSound(player, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);
        ChatUtil.sendActionBar(player, "§c§lKNIFE THROWN!");

        // Knife collision runner
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!knifeProj.isValid() || knifeProj.isDead()) {
                task.cancel();
                return;
            }

            Location pLoc = knifeProj.getLocation();
            if (pLoc.getWorld() != null) {
                pLoc.getWorld().spawnParticle(Particle.CRIT, pLoc, 3, 0.1, 0.1, 0.1, 0.05);
            }

            for (Player target : getBukkitPlayers()) {
                if (target.equals(player) || !alivePlayers.contains(target.getUniqueId()) || isSpectator(target.getUniqueId())) continue;
                if (target.getLocation().add(0, 1.0, 0).distanceSquared(knifeProj.getLocation()) <= 2.25) {
                    knifeProj.remove();
                    task.cancel();
                    eliminatePlayer(target, player, "thrown knife");
                    return;
                }
            }

            for (net.fpixel.core.testing.SimulatedPlayer bot : simulatedPlayers.values()) {
                if (bot.getEntity() != null && bot.getEntity().isValid() && alivePlayers.contains(bot.getUuid())) {
                    if (bot.getEntity().getLocation().add(0, 1.0, 0).distanceSquared(knifeProj.getLocation()) <= 2.25) {
                        knifeProj.remove();
                        task.cancel();
                        eliminateSimulatedPlayer(bot);
                        return;
                    }
                }
            }
        }, 1L, 1L);
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (state != GameState.IN_GAME) return;
        Player player = event.getPlayer();
        if (!alivePlayers.contains(player.getUniqueId()) || isSpectator(player.getUniqueId())) return;

        ItemStack item = event.getItem();
        if (item == null) item = player.getInventory().getItemInMainHand();
        if (item == null) return;

        if (item.getType() == Material.IRON_SWORD && isMurderer(player)) {
            if (event.getAction().name().contains("RIGHT")) {
                event.setCancelled(true);
                tryThrowKnife(player);
            }
        }
    }

    @Override
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (state != GameState.IN_GAME) return;
        Player player = event.getPlayer();
        if (!alivePlayers.contains(player.getUniqueId()) || isSpectator(player.getUniqueId())) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getType() == Material.IRON_SWORD && isMurderer(player)) {
            event.setCancelled(true);
            tryThrowKnife(player);
        }
    }

    @Override
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (state != GameState.IN_GAME) return;
        Player player = event.getPlayer();
        if (!alivePlayers.contains(player.getUniqueId()) || isSpectator(player.getUniqueId())) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == Material.IRON_SWORD && isMurderer(player)) {
            event.setCancelled(true);
            tryThrowKnife(player);
        }
    }

    @Override
    public void onPlayerSwapHandItems(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        if (state != GameState.IN_GAME) return;
        Player player = event.getPlayer();
        if (!alivePlayers.contains(player.getUniqueId()) || isSpectator(player.getUniqueId())) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getType() == Material.IRON_SWORD && isMurderer(player)) {
            event.setCancelled(true);
            tryThrowKnife(player);
        }
    }

    @Override
    public void onItemConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.IRON_SWORD) {
            event.setCancelled(true);
        }
    }

    public void eliminatePlayer(Player victim, Player killer, String cause) {
        alivePlayers.remove(victim.getUniqueId());
        Role vRole = roles.get(victim.getUniqueId());

        victim.setGameMode(GameMode.ADVENTURE);
        victim.getInventory().clear();
        victim.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
        victim.setAllowFlight(true);
        victim.setFlying(true);

        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, new Particle.DustOptions(Color.RED, 1.5f));
        ChatUtil.playSound(victim, Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);
        ChatUtil.sendTitle(victim, "§c§lYOU DIED!", "§7You were " + cause + "!", 5, 40, 10);

        // Detective Bow Drop
        if (vRole == Role.DETECTIVE && !isMurderer(victim)) {
            droppedBowLoc = victim.getLocation().clone();
            droppedBowItem = droppedBowLoc.getWorld().dropItem(droppedBowLoc.clone().add(0, 0.5, 0), new ItemStack(Material.BOW));
            droppedBowItem.setGlowing(true);

            droppedBowLoc.getWorld().strikeLightningEffect(droppedBowLoc);
            broadcast("§b§lBOW DROPPED! §7The Detective was killed! An Innocent can pick up the bow!");
            for (Player p : getAllBukkitPlayers()) {
                ChatUtil.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            }
        }

        checkWinCondition();
    }

    @Override
    public void eliminateSimulatedPlayer(net.fpixel.core.testing.SimulatedPlayer bot) {
        if (bot != null) {
            alivePlayers.remove(bot.getUuid());
        }
        super.eliminateSimulatedPlayer(bot);
    }

    @Override
    public void checkWinCondition() {
        if (state != GameState.IN_GAME) return;

        UUID mId = murdererUuid;
        if (mId == null) {
            for (Map.Entry<UUID, Role> e : roles.entrySet()) {
                if (e.getValue() == Role.MURDERER) {
                    mId = e.getKey();
                    break;
                }
            }
        }

        // 1. Murderer died
        if (mId != null && !alivePlayers.contains(mId)) {
            endGame(null); // Innocents win
            broadcast("§a§lINNOCENTS WIN! §7The Murderer was defeated!");
            return;
        }

        // 2. Count alive non-murderers
        int aliveInnocents = 0;
        for (UUID u : alivePlayers) {
            if (!isMurderer(u)) {
                aliveInnocents++;
            }
        }

        if (aliveInnocents == 0) {
            Player murderer = mId != null ? Bukkit.getPlayer(mId) : null;
            endGame(murderer);
            broadcast("§c§lMURDERER WINS! §7All survivors have been eliminated!");
            return;
        }

        // 3. Time out (Innocents win)
        if (gameTime >= maxGameDuration) {
            endGame(null);
            broadcast("§a§lINNOCENTS WIN! §7Time ran out and survivors survived!");
        }
    }

    @Override
    public void onGameEnd(Player winner) {
        if (winner != null) {
            FPixelUser u = plugin.getUserManager().getUser(winner);
            u.addMurderMysteryWin();
        } else {
            // Innocents won by surviving or detective killed murderer
            for (UUID uId : alivePlayers) {
                if (roles.get(uId) != Role.MURDERER) {
                    Player p = Bukkit.getPlayer(uId);
                    if (p != null) {
                        FPixelUser u = plugin.getUserManager().getUser(p);
                        u.addMurderMysteryWin();
                    }
                }
            }
        }

        for (Player p : getAllBukkitPlayers()) {
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
            p.setAllowFlight(false);
            p.setFlying(false);
        }

        for (Item item : activeGold) {
            if (item.isValid()) item.remove();
        }
        activeGold.clear();
        if (droppedBowItem != null && droppedBowItem.isValid()) {
            droppedBowItem.remove();
            droppedBowItem = null;
        }
    }

    @Override
    public void onPlayerLeave(Player player) {
        alivePlayers.remove(player.getUniqueId());
        roles.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.setAllowFlight(false);
        player.setFlying(false);

        if (state == GameState.IN_GAME) {
            checkWinCondition();
        }
    }

    @Override
    public void onArenaReset() {
        roles.clear();
        goldCount.clear();
        alivePlayers.clear();
        knifeCooldowns.clear();
        murdererUuid = null;
        detectiveUuid = null;
        for (Item item : activeGold) {
            if (item.isValid()) item.remove();
        }
        activeGold.clear();
        if (droppedBowItem != null && droppedBowItem.isValid()) {
            droppedBowItem.remove();
            droppedBowItem = null;
        }
        droppedBowLoc = null;
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
            lines.add("§fMode: §eMurder Mystery");
        } else if (state == GameState.IN_GAME) {
            if (!alivePlayers.contains(player.getUniqueId()) || isSpectator(player.getUniqueId())) {
                lines.add("§fRole: §7SPECTATOR");
            } else {
                Role role = roles.get(player.getUniqueId());
                lines.add("§fRole: " + (role != null ? role.getDisplay() : "§7Unknown"));
            }
            lines.add(" ");

            int innocentCount = 0;
            for (UUID u : alivePlayers) {
                if (roles.get(u) != Role.MURDERER) innocentCount++;
            }
            lines.add("§fInnocents Left: §a" + innocentCount);
            int gold = goldCount.getOrDefault(player.getUniqueId(), 0);
            lines.add("§fGold: §6" + gold + "/10");

            int timeLeft = Math.max(0, maxGameDuration - gameTime);
            int mins = timeLeft / 60;
            int secs = timeLeft % 60;
            lines.add("§fTime Left: §e" + String.format("%02d:%02d", mins, secs));
        } else {
            lines.add("§fGame: §eEnding");
        }

        lines.add("   ");
        lines.add("§emc.fpixel.nl");
        return lines;
    }
}
