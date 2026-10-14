package net.fpixel.games.skywars;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SkyWarsGame extends Arena {
    private final int voidKillY;
    private final List<Location> islandSpawns = new ArrayList<>();
    private final List<Location> islandChests = new ArrayList<>();
    private final List<Location> midChests = new ArrayList<>();
    private final List<Location> cageBlocks = new ArrayList<>();
    private final Map<UUID, Integer> kills = new HashMap<>();

    private static final Random RNG = new Random();

    public SkyWarsGame(FPixelPlugin plugin, String id, String displayName, int minPlayers, int maxPlayers, int voidKillY) {
        super(plugin, id, displayName, "SKYWARS", minPlayers, maxPlayers);
        this.voidKillY = voidKillY;
    }

    public void addIslandSpawn(Location loc) {
        islandSpawns.add(loc);
    }

    public void addIslandChest(Location loc) {
        islandChests.add(loc);
    }

    public void addMidChest(Location loc) {
        midChests.add(loc);
    }

    @Override
    public void onGameStart() {
        kills.clear();
        buildAndBreakCages();

        // Teleport players to their respective islands
        List<Player> bPlayers = getBukkitPlayers();
        for (int i = 0; i < bPlayers.size(); i++) {
            Player p = bPlayers.get(i);
            Location spawn = (i < islandSpawns.size()) ? islandSpawns.get(i) : lobbySpawn;
            p.teleport(spawn.clone().add(0.5, 1, 0.5));
            p.getInventory().clear();
            ChatUtil.sendTitle(p, "§6§lSKYWARS", "§eLoot chests and survive to the end!", 10, 40, 10);
            ChatUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.2f);
        }

        // Spawn simulated players on other islands
        int botSpawnIdx = bPlayers.size();
        for (net.fpixel.core.testing.SimulatedPlayer bot : simulatedPlayers.values()) {
            Location bSpawn = (botSpawnIdx < islandSpawns.size()) ? islandSpawns.get(botSpawnIdx) : lobbySpawn;
            if (bSpawn != null) {
                bot.spawnEntity(bSpawn.clone().add(0.5, 1, 0.5), "SKYWARS");
            }
            botSpawnIdx++;
        }

        // Fill chests with items
        populateChests();
    }

    private void buildAndBreakCages() {
        for (Location loc : cageBlocks) {
            loc.getBlock().setType(Material.AIR, false);
            loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
        }
        cageBlocks.clear();
    }

    public void spawnCage(Location loc, Material glassMat) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        var world = loc.getWorld();
        if (world == null) return;

        int[][] offsets = {
                {0, -1, 0}, {0, 3, 0},
                {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
                {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
                {1, 2, 0}, {-1, 2, 0}, {0, 2, 1}, {0, 2, -1}
        };

        for (int[] off : offsets) {
            Block b = world.getBlockAt(x + off[0], y + off[1], z + off[2]);
            b.setType(glassMat, false);
            cageBlocks.add(b.getLocation());
        }
    }

    private void populateChests() {
        for (Location loc : islandChests) {
            if (loc.getBlock().getState() instanceof Chest chest) {
                fillIslandChest(chest.getBlockInventory());
            }
        }
        for (Location loc : midChests) {
            if (loc.getBlock().getState() instanceof Chest chest) {
                fillMidChest(chest.getBlockInventory());
            }
        }
    }

    private void fillIslandChest(Inventory inv) {
        inv.clear();
        // Guaranteed weapon
        inv.setItem(RNG.nextInt(9), new ItemStack(RNG.nextBoolean() ? Material.IRON_SWORD : Material.STONE_SWORD));
        // Blocks
        inv.setItem(9 + RNG.nextInt(9), new ItemStack(Material.OAK_PLANKS, 32 + RNG.nextInt(32)));
        // Armor piece
        Material[] armors = {Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS, Material.CHAINMAIL_CHESTPLATE};
        inv.setItem(18 + RNG.nextInt(9), new ItemStack(armors[RNG.nextInt(armors.length)]));

        // Random extras
        if (RNG.nextBoolean()) inv.setItem(RNG.nextInt(27), new ItemStack(Material.WATER_BUCKET));
        if (RNG.nextBoolean()) inv.setItem(RNG.nextInt(27), new ItemStack(Material.SNOWBALL, 16));
        if (RNG.nextBoolean()) inv.setItem(RNG.nextInt(27), new ItemStack(Material.GOLDEN_APPLE, 1 + RNG.nextInt(2)));
        if (RNG.nextBoolean()) inv.setItem(RNG.nextInt(27), new ItemStack(Material.COOKED_BEEF, 8));
        if (RNG.nextBoolean()) {
            inv.setItem(RNG.nextInt(27), new ItemStack(Material.BOW));
            inv.setItem(RNG.nextInt(27), new ItemStack(Material.ARROW, 16));
        }
    }

    private void fillMidChest(Inventory inv) {
        inv.clear();
        // OP Mid loot
        inv.setItem(RNG.nextInt(9), new ItemStack(Material.DIAMOND_SWORD));
        inv.setItem(9 + RNG.nextInt(9), new ItemStack(Material.ENDER_PEARL, 2 + RNG.nextInt(3)));

        Material[] opArmor = {Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, Material.DIAMOND_HELMET};
        inv.setItem(18 + RNG.nextInt(9), new ItemStack(opArmor[RNG.nextInt(opArmor.length)]));

        inv.setItem(RNG.nextInt(27), new ItemStack(Material.GOLDEN_APPLE, 2 + RNG.nextInt(4)));
        if (RNG.nextBoolean()) inv.setItem(RNG.nextInt(27), new ItemStack(Material.LAVA_BUCKET));
        if (RNG.nextBoolean()) inv.setItem(RNG.nextInt(27), new ItemStack(Material.TNT, 8));
        if (RNG.nextBoolean()) inv.setItem(RNG.nextInt(27), new ItemStack(Material.FLINT_AND_STEEL));
    }

    @Override
    public void handleGameTick() {
        // Void check
        for (Player p : getBukkitPlayers()) {
            if (p.getLocation().getY() < voidKillY) {
                broadcast("§c" + p.getName() + " §7fell into the void.");
                eliminatePlayer(p);
            }
        }
        for (net.fpixel.core.testing.SimulatedPlayer bot : new ArrayList<>(simulatedPlayers.values())) {
            if (bot.getEntity() != null && bot.getEntity().isValid()) {
                if (bot.getEntity().getLocation().getY() < voidKillY) {
                    eliminateSimulatedPlayer(bot);
                }
            }
        }

        // Timed SkyWars Events
        if (gameTime == 120) {
            populateChests();
            broadcast("§a§lCHEST REFILL §8» §eAll chests have been refilled!");
            for (Player p : getAllBukkitPlayers()) {
                ChatUtil.sendTitle(p, "§a§lCHEST REFILL!", "§7Chests have been replenished!", 5, 30, 10);
                ChatUtil.playSound(p, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.2f);
            }
        } else if (gameTime == 240) {
            populateChests();
            for (Location loc : midChests) {
                if (loc.getBlock().getState() instanceof Chest chest) {
                    fillMidChest(chest.getBlockInventory());
                    chest.getBlockInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
                    chest.getBlockInventory().addItem(new ItemStack(Material.ENDER_PEARL, 4));
                }
            }
            broadcast("§c§lOP CHEST REFILL §8» §6Mid chests filled with overpowered items!");
            for (Player p : getAllBukkitPlayers()) {
                ChatUtil.sendTitle(p, "§c§lOP REFILL!", "§6Go to mid for overpowered loot!", 5, 30, 10);
                ChatUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
        } else if (gameTime == 360) {
            broadcast("§4§lDOOM DRAGON §8» §cSudden Death! An Ender Dragon has awakened!");
            if (lobbySpawn != null && lobbySpawn.getWorld() != null) {
                Location dragonSpawn = lobbySpawn.clone().subtract(0, 20, 0);
                try {
                    lobbySpawn.getWorld().spawn(dragonSpawn, org.bukkit.entity.EnderDragon.class);
                } catch (Exception ignored) {}
            }
            for (Player p : getAllBukkitPlayers()) {
                ChatUtil.sendTitle(p, "§4§lDOOM DRAGON!", "§cWatch out from above!", 5, 40, 10);
                ChatUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.7f);
            }
        } else if (gameTime >= 600) {
            broadcast("§c§lTIME LIMIT §8» §7Match ended in a draw!");
            endGame(null);
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        if (state != GameState.IN_GAME) {
            event.setCancelled(true);
            return;
        }
        rollbackEngine.recordBreak(event.getBlock());
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        if (state != GameState.IN_GAME) {
            event.setCancelled(true);
            return;
        }
        if (lobbySpawn != null && event.getBlockPlaced().getY() >= lobbySpawn.getBlockY() - 3) {
            event.setCancelled(true);
            ChatUtil.sendMessage(event.getPlayer(), "§cYou cannot build that high!");
            return;
        }
        rollbackEngine.recordPlace(event.getBlockPlaced());
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (state != GameState.IN_GAME) return;
        Player player = event.getPlayer();
        if (!players.contains(player.getUniqueId())) return;
        if (player.getLocation().getY() < voidKillY) {
            broadcast("§c" + player.getName() + " §7fell into the void.");
            eliminatePlayer(player);
        }
    }

    @Override
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (state != GameState.IN_GAME) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player victim && players.contains(victim.getUniqueId())) {
            if (victim.getHealth() - event.getFinalDamage() <= 0) {
                event.setCancelled(true);
                victim.setHealth(20.0);
                victim.setFoodLevel(20);
                victim.setFireTicks(0);
                victim.setFallDistance(0);
                victim.setVelocity(new org.bukkit.util.Vector(0, 0, 0));

                if (event.getDamager() instanceof Player killer) {
                    kills.put(killer.getUniqueId(), kills.getOrDefault(killer.getUniqueId(), 0) + 1);
                    broadcast("§c" + victim.getName() + " §7was slain by §a" + killer.getName() + "§7!");
                    ChatUtil.playSound(killer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                } else {
                    broadcast("§c" + victim.getName() + " §7died.");
                }

                eliminatePlayer(victim);
            }
        }
    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        event.getDrops().clear();
        event.setCancelled(true);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));

        if (player.getKiller() != null) {
            Player killer = player.getKiller();
            kills.put(killer.getUniqueId(), kills.getOrDefault(killer.getUniqueId(), 0) + 1);
            broadcast("§c" + player.getName() + " §7was slain by §a" + killer.getName() + "§7!");
            ChatUtil.playSound(killer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        } else {
            broadcast("§c" + player.getName() + " §7died.");
        }

        eliminatePlayer(player);
    }

    @Override
    public void checkWinCondition() {
        if (state != GameState.IN_GAME) return;
        if (getAlivePlayerCount() <= 1) {
            List<Player> alive = getBukkitPlayers();
            Player winner = alive.isEmpty() ? null : alive.get(0);
            endGame(winner);
        }
    }

    @Override
    public void onGameEnd(Player winner) {
        buildAndBreakCages();
    }

    @Override
    public void onArenaReset() {
        kills.clear();
        buildAndBreakCages();
        for (Location loc : islandChests) {
            if (loc.getBlock().getState() instanceof Chest chest) chest.getBlockInventory().clear();
        }
        for (Location loc : midChests) {
            if (loc.getBlock().getState() instanceof Chest chest) chest.getBlockInventory().clear();
        }
        if (lobbySpawn != null && lobbySpawn.getWorld() != null) {
            for (org.bukkit.entity.Entity e : lobbySpawn.getWorld().getEntities()) {
                if (e instanceof org.bukkit.entity.EnderDragon) {
                    e.remove();
                }
            }
        }
    }

    public String getNextEventString() {
        if (gameTime < 120) {
            int rem = 120 - gameTime;
            return "§fEvent: §aRefill §7(" + String.format("%02d:%02d", rem / 60, rem % 60) + ")";
        } else if (gameTime < 240) {
            int rem = 240 - gameTime;
            return "§fEvent: §cOP Refill §7(" + String.format("%02d:%02d", rem / 60, rem % 60) + ")";
        } else if (gameTime < 360) {
            int rem = 360 - gameTime;
            return "§fEvent: §4Doom Dragon §7(" + String.format("%02d:%02d", rem / 60, rem % 60) + ")";
        } else {
            int rem = Math.max(0, 600 - gameTime);
            return "§fEvent: §cGame End §7(" + String.format("%02d:%02d", rem / 60, rem % 60) + ")";
        }
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§7" + id);
        lines.add(" ");
        if (state == GameState.WAITING || state == GameState.STARTING) {
            lines.add("§fPlayers: §a" + players.size() + "/" + maxPlayers);
            lines.add("§fStarting in: §a" + countdown + "s");
        } else if (state == GameState.IN_GAME) {
            lines.add(getNextEventString());
            lines.add("§fPlayers Left: §a" + players.size());
            lines.add("§fKills: §a" + kills.getOrDefault(player.getUniqueId(), 0));
        } else {
            lines.add("§fGame: §eEnding");
        }
        lines.add(" ");
        lines.add("§emc.fpixel.net");
        return lines;
    }

    public List<Location> getIslandSpawns() { return islandSpawns; }
}
