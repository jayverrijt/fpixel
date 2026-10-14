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
            ChatUtil.sendMessage(p, "§eRight-click the §a§lShopkeeper Villager §eor your §a§lNether Star §eto buy gear!");
        }

        // Spawn Shopkeepers at each base
        for (BedWarsTeam team : teamList) {
            if (team.getSpawnLocation() != null && team.getSpawnLocation().getWorld() != null) {
                Location shopLoc = team.getSpawnLocation().clone().add(2.0, 0, 1.5);
                org.bukkit.entity.Villager v = (org.bukkit.entity.Villager) shopLoc.getWorld().spawnEntity(shopLoc, org.bukkit.entity.EntityType.VILLAGER);
                v.setCustomName("§a§lITEM SHOP §7(Right Click)");
                v.setCustomNameVisible(true);
                v.setAI(false);
                v.setSilent(true);
                v.setInvulnerable(true);
                v.addScoreboardTag("bedwars_shopkeeper");
                shopkeepers.add(v);
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
        player.getInventory().setHelmet(new ItemBuilder(Material.LEATHER_HELMET).leatherColor(team.getLeatherColor()).unbreakable(true).build());
        player.getInventory().setChestplate(new ItemBuilder(Material.LEATHER_CHESTPLATE).leatherColor(team.getLeatherColor()).unbreakable(true).build());
        player.getInventory().setLeggings(new ItemBuilder(Material.LEATHER_LEGGINGS).leatherColor(team.getLeatherColor()).unbreakable(true).build());
        player.getInventory().setBoots(new ItemBuilder(Material.LEATHER_BOOTS).leatherColor(team.getLeatherColor()).unbreakable(true).build());

        player.getInventory().setItem(0, new ItemBuilder(Material.WOODEN_SWORD).unbreakable(true).build());
        player.getInventory().setItem(1, new ItemBuilder(Material.SHEARS).unbreakable(true).build());
        player.getInventory().setItem(8, new ItemBuilder(Material.NETHER_STAR)
                .name("§a§lItem Shop §7(Right Click)")
                .lore("§7Click to open the BedWars Item Shop.")
                .build());
    }

    @Override
    public void handleGameTick() {
        // Spawners tick
        for (BedWarsSpawner spawner : spawners) {
            spawner.tick();
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
        return blockLoc.getWorld().equals(bedLoc.getWorld()) && blockLoc.distanceSquared(bedLoc) <= 4;
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
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
        if (entity != null && (entity.getScoreboardTags().contains("bedwars_shopkeeper") || entity.getType() == org.bukkit.entity.EntityType.VILLAGER)) {
            event.setCancelled(true);
            BedWarsShop.open(event.getPlayer());
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
                        player.teleport(team.getSpawnLocation());
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
        }
        for (org.bukkit.entity.Entity e : shopkeepers) {
            if (e != null && e.isValid()) e.remove();
        }
        shopkeepers.clear();
        spawners.clear();
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§7BedWars");
        lines.add("§fTime: §a" + String.format("%02d:%02d", gameTime / 60, gameTime % 60));
        lines.add("");

        for (BedWarsTeam team : teams.values()) {
            lines.add(team.getChatColor() + team.getName().substring(0, 1) + " §f" + team.getName() + ": " + team.getStatusSymbol());
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
