package net.fpixel.games.bedwars;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BedWarsGame extends Arena {
    private final Map<String, BedWarsTeam> teams = new HashMap<>();
    private final List<BedWarsSpawner> spawners = new ArrayList<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> finalKills = new HashMap<>();
    private final Map<UUID, Integer> bedsBroken = new HashMap<>();
    private final List<org.bukkit.entity.Entity> shopkeepers = new ArrayList<>();

    public BedWarsGame(FPixelPlugin plugin, String id, String displayName, int minPlayers, int maxPlayers) {
        super(plugin, id, displayName, "BEDWARS", minPlayers, maxPlayers);
        setupDefaultTeams();
    }

    private void setupDefaultTeams() {
        teams.put("RED", new BedWarsTeam("Red", ChatColor.RED, Color.RED, Material.RED_WOOL, Material.RED_BED));
        teams.put("BLUE", new BedWarsTeam("Blue", ChatColor.BLUE, Color.BLUE, Material.BLUE_WOOL, Material.BLUE_BED));
    }

    public void addSpawner(BedWarsSpawner spawner) {
        spawners.add(spawner);
    }

    public BedWarsTeam getTeam(String name) {
        return teams.get(name.toUpperCase());
    }

    public BedWarsTeam getPlayerTeam(Player player) {
        for (BedWarsTeam team : teams.values()) {
            if (team.hasMember(player.getUniqueId())) {
                return team;
            }
        }
        return null;
    }

    @Override
    public void onGameStart() {
        kills.clear();
        finalKills.clear();
        bedsBroken.clear();
        shopkeepers.clear();

        // Reset all resource spawners
        for (BedWarsSpawner spawner : spawners) {
            spawner.reset();
        }

        // Assign players to teams evenly
        List<Player> playerList = getBukkitPlayers();
        List<BedWarsTeam> teamList = new ArrayList<>(teams.values());

        for (int i = 0; i < playerList.size(); i++) {
            Player p = playerList.get(i);
            BedWarsTeam team = teamList.get(i % teamList.size());
            team.addMember(p.getUniqueId());
            team.setBedAlive(true);

            giveTeamKit(p, team);
            if (team.getSpawnLocation() != null) {
                p.teleport(team.getSpawnLocation());
            }
            ChatUtil.sendMessage(p, "§7You are on the " + team.getChatColor() + team.getName() + " Team§7!");
            ChatUtil.sendMessage(p, "§eRight-click the §a§lItem Shop §eor §b§lTeam Upgrades §evillagers!");
        }

        // Spawn Shopkeeper & Team Upgrade NPCs at each base
        for (BedWarsTeam team : teamList) {
            if (team.getSpawnLocation() != null && team.getSpawnLocation().getWorld() != null) {
                // 1. Item Shop Villager
                Location shopLoc = team.getSpawnLocation().clone().add(2.0, 0, 1.5);
                org.bukkit.entity.Villager v = (org.bukkit.entity.Villager) shopLoc.getWorld().spawnEntity(shopLoc, org.bukkit.entity.EntityType.VILLAGER);
                v.setCustomName("§a§lITEM SHOP §7(Right Click)");
                v.setCustomNameVisible(true);
                v.setAI(false);
                v.setSilent(true);
                v.setInvulnerable(true);
                v.addScoreboardTag("bedwars_shopkeeper");
                shopkeepers.add(v);

                // 2. Team Upgrades Villager
                Location upLoc = team.getSpawnLocation().clone().add(-2.0, 0, 1.5);
                org.bukkit.entity.Villager uv = (org.bukkit.entity.Villager) upLoc.getWorld().spawnEntity(upLoc, org.bukkit.entity.EntityType.VILLAGER);
                uv.setCustomName("§b§lTEAM UPGRADES §7(Right Click)");
                uv.setCustomNameVisible(true);
                uv.setAI(false);
                uv.setSilent(true);
                uv.setInvulnerable(true);
                uv.addScoreboardTag("bedwars_upgrades");
                shopkeepers.add(uv);
            }
        }

        // Assign simulated players to remaining teams
        int teamIdx = playerList.size();
        for (net.fpixel.core.testing.SimulatedPlayer bot : simulatedPlayers.values()) {
            BedWarsTeam team = teamList.get(teamIdx % teamList.size());
            team.addMember(bot.getUuid());
            team.setBedAlive(true);
            if (team.getSpawnLocation() != null) {
                bot.spawnEntity(team.getSpawnLocation(), "BEDWARS");
            }
            teamIdx++;
        }
    }

    private void giveTeamKit(Player player, BedWarsTeam team) {
        player.getInventory().clear();

        ItemStack helmet = new ItemBuilder(Material.LEATHER_HELMET).leatherColor(team.getLeatherColor()).unbreakable(true).build();
        ItemStack chest = new ItemBuilder(Material.LEATHER_CHESTPLATE).leatherColor(team.getLeatherColor()).unbreakable(true).build();
        ItemStack legs = new ItemBuilder(Material.LEATHER_LEGGINGS).leatherColor(team.getLeatherColor()).unbreakable(true).build();
        ItemStack boots = new ItemBuilder(Material.LEATHER_BOOTS).leatherColor(team.getLeatherColor()).unbreakable(true).build();

        if (team.getProtection() > 0) {
            helmet.addEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, team.getProtection());
            chest.addEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, team.getProtection());
            legs.addEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, team.getProtection());
            boots.addEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, team.getProtection());
        }

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chest);
        player.getInventory().setLeggings(legs);
        player.getInventory().setBoots(boots);

        ItemStack sword = new ItemBuilder(Material.WOODEN_SWORD).unbreakable(true).build();
        if (team.getSharpness() > 0) {
            sword.addEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, team.getSharpness());
        }
        player.getInventory().setItem(0, sword);
        player.getInventory().setItem(1, new ItemBuilder(Material.SHEARS).unbreakable(true).build());
    }

    @Override
    public void handleGameTick() {
        // Spawners tick
        for (BedWarsSpawner spawner : spawners) {
            spawner.tick();
        }

        // Check for void death at Y <= -500
        for (Player p : getBukkitPlayers()) {
            if (p.getLocation().getY() <= -500.0) {
                handleVoidDeath(p);
            }
        }

        // Apply team upgrades (Haste, Heal Pool, Traps, Forge Boosters)
        for (BedWarsTeam team : teams.values()) {
            if (team.getSpawnLocation() == null || team.getSpawnLocation().getWorld() == null) continue;

            // Extra forge ticks for higher forge tiers
            if (team.getForgeTier() >= 1 && gameTime % 2 == 0) {
                // Tier 1: +50% iron
                Location sl = team.getSpawnLocation();
                sl.getWorld().dropItem(sl, new ItemStack(Material.IRON_INGOT));
            }
            if (team.getForgeTier() >= 2 && gameTime % 5 == 0) {
                // Tier 2: +gold
                Location sl = team.getSpawnLocation();
                sl.getWorld().dropItem(sl, new ItemStack(Material.GOLD_INGOT));
            }
            if (team.getForgeTier() >= 3 && gameTime % 30 == 0) {
                // Tier 3: emeralds at base
                Location sl = team.getSpawnLocation();
                sl.getWorld().dropItem(sl, new ItemStack(Material.EMERALD));
            }

            // Check members for Haste & Heal Pool
            for (Player p : getBukkitPlayers()) {
                if (team.hasMember(p.getUniqueId())) {
                    if (team.getHaste() > 0) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 45, team.getHaste() - 1, true, false, false));
                    }
                    if (team.hasHealPool()) {
                        if (p.getLocation().distanceSquared(team.getSpawnLocation()) <= 225) { // 15 blocks
                            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 45, 0, true, false, false));
                        }
                    }
                } else if (team.hasTrap()) {
                    // Enemy stepped onto base island!
                    if (p.getLocation().distanceSquared(team.getSpawnLocation()) <= 144) { // 12 blocks
                        team.setTrap(false);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                        for (Player member : getBukkitPlayers()) {
                            if (team.hasMember(member.getUniqueId())) {
                                ChatUtil.sendTitle(member, "§c§lTRAP TRIGGERED!", "§eAn enemy entered your base!", 0, 30, 5);
                                ChatUtil.playSound(member, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                            }
                        }
                    }
                }
            }
        }

        // --- DYNAMIC TIMED EVENTS ---
        if (gameTime == 180) { // 3:00 - Diamond II
            for (BedWarsSpawner s : spawners) {
                if (s.getType() == BedWarsSpawner.SpawnerType.DIAMOND) s.setCustomInterval(18);
            }
            broadcast("§b§lEVENT §8» §fDiamond Generators upgraded to §bTier II§f!");
            for (Player p : getAllBukkitPlayers()) {
                ChatUtil.sendTitle(p, "§b§lDIAMOND II!", "§7Diamond generators produce faster!", 5, 40, 10);
                ChatUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
            }
        } else if (gameTime == 360) { // 6:00 - Emerald II
            for (BedWarsSpawner s : spawners) {
                if (s.getType() == BedWarsSpawner.SpawnerType.EMERALD) s.setCustomInterval(25);
            }
            broadcast("§a§lEVENT §8» §fEmerald Generators upgraded to §aTier II§f!");
            for (Player p : getAllBukkitPlayers()) {
                ChatUtil.sendTitle(p, "§a§lEMERALD II!", "§7Emerald generators produce faster!", 5, 40, 10);
                ChatUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
            }
        } else if (gameTime == 540) { // 9:00 - Diamond III
            for (BedWarsSpawner s : spawners) {
                if (s.getType() == BedWarsSpawner.SpawnerType.DIAMOND) s.setCustomInterval(10);
            }
            broadcast("§b§lEVENT §8» §fDiamond Generators upgraded to §bTier III§f (MAX)!");
            for (Player p : getAllBukkitPlayers()) {
                ChatUtil.sendTitle(p, "§b§lDIAMOND III!", "§7Maximum diamond generation!", 5, 40, 10);
                ChatUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
            }
        } else if (gameTime == 720) { // 12:00 - Emerald III
            for (BedWarsSpawner s : spawners) {
                if (s.getType() == BedWarsSpawner.SpawnerType.EMERALD) s.setCustomInterval(15);
            }
            broadcast("§a§lEVENT §8» §fEmerald Generators upgraded to §aTier III§f (MAX)!");
            for (Player p : getAllBukkitPlayers()) {
                ChatUtil.sendTitle(p, "§a§lEMERALD III!", "§7Maximum emerald generation!", 5, 40, 10);
                ChatUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
            }
        } else if (gameTime == 900) { // 15:00 - Bed Destruction
            for (BedWarsTeam team : teams.values()) {
                if (team.isBedAlive()) {
                    team.setBedAlive(false);
                    Location bedLoc = team.getBedLocation();
                    if (bedLoc != null && bedLoc.getWorld() != null) {
                        bedLoc.getBlock().setType(Material.AIR, false);
                        Block head = bedLoc.getBlock().getRelative(team.getBedFacing());
                        head.setType(Material.AIR, false);
                        bedLoc.getWorld().strikeLightningEffect(bedLoc);
                    }
                }
            }
            broadcast("§c§lBED DESTRUCTION §8» §cAll beds have been destroyed! No more respawns!");
            for (Player p : getAllBukkitPlayers()) {
                ChatUtil.sendTitle(p, "§c§lBEDS DESTROYED!", "§7All beds have vanished! Final lives only!", 5, 50, 15);
                ChatUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
                ChatUtil.playSound(p, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
            }
        } else if (gameTime == 1080) { // 18:00 - Sudden Death Dragons
            broadcast("§4§lSUDDEN DEATH §8» §cEnder Dragons have arrived!");
            Location center = lobbySpawn != null ? lobbySpawn.clone().subtract(0, 30, 0) : null;
            if (center != null && center.getWorld() != null) {
                try {
                    center.getWorld().spawn(center.clone().add(0, 15, 0), org.bukkit.entity.EnderDragon.class);
                } catch (Exception ignored) {}
            }
            for (Player p : getAllBukkitPlayers()) {
                ChatUtil.sendTitle(p, "§4§lSUDDEN DEATH!", "§cSurvive the dragons!", 5, 50, 15);
                ChatUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.6f);
            }
        } else if (gameTime >= 1200) { // 20:00 - Game Draw
            broadcast("§c§lGAME OVER §8» §7Time limit reached!");
            endGame(null);
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        BedWarsTeam playerTeam = getPlayerTeam(player);

        // Check if bed was broken
        if (block.getType().name().contains("BED")) {
            for (BedWarsTeam team : teams.values()) {
                if (team.isBedAlive() && isBedLocation(block.getLocation(), team.getBedLocation())) {
                    if (team == playerTeam) {
                        event.setCancelled(true);
                        ChatUtil.sendMessage(player, "§cYou cannot destroy your own bed!");
                        return;
                    }

                    // Destroy bed!
                    team.setBedAlive(false);
                    event.setDropItems(false);

                    // Clear both halves of the bed cleanly
                    Location bedLoc = team.getBedLocation();
                    if (bedLoc != null && bedLoc.getWorld() != null) {
                        bedLoc.getBlock().setType(Material.AIR, false);
                        Block head = bedLoc.getBlock().getRelative(team.getBedFacing());
                        head.setType(Material.AIR, false);
                    }
                    block.setType(Material.AIR, false);

                    bedsBroken.put(player.getUniqueId(), bedsBroken.getOrDefault(player.getUniqueId(), 0) + 1);

                    FPixelUser user = plugin.getUserManager().getUser(player);
                    user.addBedwarsBedBroken();

                    // Alert sound & title
                    for (Player p : getAllBukkitPlayers()) {
                        ChatUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                        ChatUtil.sendTitle(p, "§c§lBED DESTROYED!", team.getChatColor() + team.getName() + " Bed §7was destroyed by " + player.getName() + "!", 5, 40, 10);
                    }
                    broadcast("§c§lBED DESTRUCTION §8» " + team.getChatColor() + team.getName() + " Bed §7was broken by " + player.getName() + "!");

                    // In testing mode: breaking the enemy bot's bed automatically eliminates all bots on that team!
                    if (plugin.getTestingManager() != null && plugin.getTestingManager().isTestingMode()) {
                        for (UUID member : new ArrayList<>(team.getMembers())) {
                            net.fpixel.core.testing.SimulatedPlayer bot = simulatedPlayers.get(member);
                            if (bot != null) {
                                eliminateSimulatedPlayer(bot);
                            }
                        }
                    }
                    return;
                }
            }
        }

        // Only allow breaking player placed blocks
        if (!rollbackEngine.isPlayerPlaced(block.getLocation())) {
            event.setCancelled(true);
            ChatUtil.sendMessage(player, "§cYou can only break blocks placed by players!");
        } else {
            rollbackEngine.logBlockBreak(block);
        }
    }

    private boolean isBedLocation(Location blockLoc, Location bedLoc) {
        if (bedLoc == null || blockLoc == null) return false;
        return blockLoc.getWorld().equals(bedLoc.getWorld()) && blockLoc.distanceSquared(bedLoc) <= 6;
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Prevent building up to or near the waiting lobby (anti-waiting area exploit)
        if (lobbySpawn != null) {
            if (block.getY() >= lobbySpawn.getBlockY() - 3 || block.getLocation().distanceSquared(lobbySpawn) <= 64) {
                event.setCancelled(true);
                ChatUtil.sendMessage(player, "§cYou cannot build into the waiting lobby!");
                return;
            }
        }

        if (block.getType() == Material.TNT) {
            // Auto prime TNT!
            event.setCancelled(true);
            ItemStack inHand = event.getItemInHand();
            inHand.setAmount(inHand.getAmount() - 1);

            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class);
            tnt.setFuseTicks(40); // 2 seconds
            return;
        }

        rollbackEngine.logBlockPlace(block);
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Item Shop shortcut
        if (item != null && item.getType() == Material.NETHER_STAR) {
            event.setCancelled(true);
            BedWarsShop.open(player);
            return;
        }

        // Fireball launcher
        if (item != null && item.getType() == Material.FIRE_CHARGE && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            item.setAmount(item.getAmount() - 1);
            Fireball fireball = player.launchProjectile(Fireball.class);
            fireball.setIsIncendiary(false);
            fireball.setYield(2.5f);
            ChatUtil.playSound(player, Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);
            return;
        }
    }

    @Override
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (state != GameState.IN_GAME) return;
        org.bukkit.entity.Entity entity = event.getRightClicked();
        if (entity != null) {
            if (entity.getScoreboardTags().contains("bedwars_upgrades") || (entity.getCustomName() != null && entity.getCustomName().contains("UPGRADES"))) {
                event.setCancelled(true);
                BedWarsShop.openUpgrades(event.getPlayer());
                return;
            }
            if (entity.getScoreboardTags().contains("bedwars_shopkeeper") || entity.getType() == org.bukkit.entity.EntityType.VILLAGER) {
                event.setCancelled(true);
                BedWarsShop.open(event.getPlayer());
                return;
            }
        }
    }

    @Override
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (state != GameState.IN_GAME) return;
        Player player = event.getPlayer();
        if (!players.contains(player.getUniqueId())) return;
        if (player.getLocation().getY() <= -500.0) {
            handleVoidDeath(player);
        }
    }

    public void handleVoidDeath(Player player) {
        if (state != GameState.IN_GAME || !players.contains(player.getUniqueId())) return;
        player.setFallDistance(0);
        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);

        BedWarsTeam team = getPlayerTeam(player);
        if (team != null && team.isBedAlive()) {
            broadcast(team.getChatColor() + player.getName() + " §7fell into the void. §aRespawning in 5s...");
            startRespawnCountdown(player, team);
        } else {
            broadcast("§c§lFINAL KILL! " + (team != null ? team.getChatColor() : "§7") + player.getName() + " §7fell into the void!");
            FPixelUser pUser = plugin.getUserManager().getUser(player);
            pUser.addBedwarsDeath();
            eliminatePlayer(player);
        }
    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setKeepInventory(false);
        Player player = event.getEntity();
        Player killer = player.getKiller();
        BedWarsTeam team = getPlayerTeam(player);

        if (killer != null && killer != player) {
            kills.put(killer.getUniqueId(), kills.getOrDefault(killer.getUniqueId(), 0) + 1);
            FPixelUser kUser = plugin.getUserManager().getUser(killer);
            kUser.addBedwarsKill();
        }

        FPixelUser pUser = plugin.getUserManager().getUser(player);
        pUser.addBedwarsDeath();

        if (team != null && team.isBedAlive()) {
            // Normal death -> 5s Respawn
            broadcast(team.getChatColor() + player.getName() + " §7died. §aRespawning in 5s...");
            startRespawnCountdown(player, team);
        } else {
            // Final Kill -> Eliminated!
            if (killer != null && killer != player) {
                finalKills.put(killer.getUniqueId(), finalKills.getOrDefault(killer.getUniqueId(), 0) + 1);
                FPixelUser kUser = plugin.getUserManager().getUser(killer);
                kUser.addBedwarsFinalKill();
            }
            broadcast("§c§lFINAL KILL! " + (team != null ? team.getChatColor() : "§7") + player.getName() + " §7was eliminated!");
            eliminatePlayer(player);
        }
    }

    private void startRespawnCountdown(Player player, BedWarsTeam team) {
        plugin.getSpectatorManager().makeSpectator(player, this);
        if (team.getSpawnLocation() != null) {
            player.teleport(team.getSpawnLocation().clone().add(0, 10, 0));
        }

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            int seconds = 5;
            @Override
            public void run() {
                if (state != GameState.IN_GAME || !player.isOnline()) return;

                if (seconds > 0) {
                    ChatUtil.sendTitle(player, "§cYOU DIED!", "§eRespawning in §c" + seconds + " §eseconds...", 0, 25, 0);
                    ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    seconds--;
                    Bukkit.getScheduler().runTaskLater(plugin, this, 20L);
                } else {
                    plugin.getSpectatorManager().removeSpectator(player);
                    player.setGameMode(GameMode.SURVIVAL);
                    if (team.getSpawnLocation() != null) {
                        Location spawn = team.getSpawnLocation().clone().add(0, 1.5, 0);
                        spawn.getBlock().setType(Material.AIR, false);
                        spawn.clone().add(0, 1, 0).getBlock().setType(Material.AIR, false);
                        player.setFallDistance(0);
                        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                        player.teleport(spawn);
                    }
                    giveTeamKit(player, team);
                    ChatUtil.sendTitle(player, "§a§lRESPAWNED!", "§7Go back into the battle!", 5, 20, 5);
                    ChatUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                }
            }
        }, 1L);
    }

    @Override
    public void checkWinCondition() {
        if (state != GameState.IN_GAME) return;

        List<BedWarsTeam> aliveTeams = new ArrayList<>();
        for (BedWarsTeam team : teams.values()) {
            boolean hasAliveMembers = false;
            for (UUID uuid : team.getMembers()) {
                if (players.contains(uuid) || simulatedPlayers.containsKey(uuid)) {
                    hasAliveMembers = true;
                    break;
                }
            }
            if (hasAliveMembers || team.isBedAlive()) {
                aliveTeams.add(team);
            }
        }

        if (aliveTeams.size() <= 1) {
            BedWarsTeam winningTeam = aliveTeams.isEmpty() ? null : aliveTeams.get(0);
            Player winner = null;
            if (winningTeam != null) {
                for (UUID uuid : winningTeam.getMembers()) {
                    if (players.contains(uuid)) {
                        winner = Bukkit.getPlayer(uuid);
                        break;
                    }
                }
            }
            endGame(winner);
        }
    }

    @Override
    public void onGameEnd(Player winner) {
        for (UUID uuid : players) {
            FPixelUser user = plugin.getUserManager().getUser(uuid, "");
            if (winner != null && winner.getUniqueId().equals(uuid)) {
                user.addBedwarsWin();
            } else {
                user.addBedwarsLoss();
            }
        }
    }

    @Override
    public void onArenaReset() {
        for (BedWarsTeam team : teams.values()) {
            team.setBedAlive(true);
            team.getMembers().clear();
            team.resetUpgrades();

            // Re-place full 2-block bed for the next match
            if (team.getBedLocation() != null && team.getBedLocation().getWorld() != null) {
                Location bl = team.getBedLocation();
                net.fpixel.core.map.MapBuilder.placeBed(bl.getWorld(), bl.getBlockX(), bl.getBlockY(), bl.getBlockZ(), team.getBedFacing(), team.getBedMaterial());
            }
        }
        for (org.bukkit.entity.Entity e : shopkeepers) {
            if (e != null && e.isValid()) e.remove();
        }
        shopkeepers.clear();

        // Reset all resource spawners
        for (BedWarsSpawner spawner : spawners) {
            spawner.reset();
        }

        // Clean up any remaining dragons
        if (lobbySpawn != null && lobbySpawn.getWorld() != null) {
            for (org.bukkit.entity.Entity e : lobbySpawn.getWorld().getEntities()) {
                if (e instanceof org.bukkit.entity.EnderDragon) {
                    e.remove();
                }
            }
        }
    }

    public String getNextEventString() {
        if (gameTime < 180) {
            int rem = 180 - gameTime;
            return "§fEvent: §bDiamond II §7(" + String.format("%02d:%02d", rem / 60, rem % 60) + ")";
        } else if (gameTime < 360) {
            int rem = 360 - gameTime;
            return "§fEvent: §aEmerald II §7(" + String.format("%02d:%02d", rem / 60, rem % 60) + ")";
        } else if (gameTime < 540) {
            int rem = 540 - gameTime;
            return "§fEvent: §bDiamond III §7(" + String.format("%02d:%02d", rem / 60, rem % 60) + ")";
        } else if (gameTime < 720) {
            int rem = 720 - gameTime;
            return "§fEvent: §aEmerald III §7(" + String.format("%02d:%02d", rem / 60, rem % 60) + ")";
        } else if (gameTime < 900) {
            int rem = 900 - gameTime;
            return "§fEvent: §cBed Gone §7(" + String.format("%02d:%02d", rem / 60, rem % 60) + ")";
        } else if (gameTime < 1080) {
            int rem = 1080 - gameTime;
            return "§fEvent: §4Sudden Death §7(" + String.format("%02d:%02d", rem / 60, rem % 60) + ")";
        } else {
            int rem = Math.max(0, 1200 - gameTime);
            return "§fEvent: §cGame End §7(" + String.format("%02d:%02d", rem / 60, rem % 60) + ")";
        }
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§7BedWars");
        lines.add(getNextEventString());
        lines.add("");

        for (BedWarsTeam team : teams.values()) {
            lines.add(team.getChatColor() + team.getName().substring(0, 1) + " §f" + team.getName() + ": " + team.getStatusSymbol(this));
        }

        lines.add(" ");
        lines.add("§fKills: §a" + kills.getOrDefault(player.getUniqueId(), 0));
        lines.add("§fFinal Kills: §a" + finalKills.getOrDefault(player.getUniqueId(), 0));
        lines.add("§fBeds Broken: §a" + bedsBroken.getOrDefault(player.getUniqueId(), 0));
        lines.add("  ");
        lines.add("§emc.fpixel.net");

        return lines;
    }
}
