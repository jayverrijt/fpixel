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

        // Chest refill every 180 seconds (3 min)
        if (gameTime > 0 && gameTime % 180 == 0) {
            populateChests();
            broadcast("§a§lCHEST REFILL §8» §eAll chests have been refilled with loot!");
            for (Player p : getAllBukkitPlayers()) {
                ChatUtil.playSound(p, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.2f);
            }
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
        rollbackEngine.recordPlace(event.getBlockPlaced());
    }

    @Override
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (state != GameState.IN_GAME) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        event.getDrops().clear();
        event.setCancelled(true);

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
        List<Player> alive = getBukkitPlayers();
        if (alive.size() <= 1) {
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
            int refill = Math.max(0, 180 - (gameTime % 180));
            lines.add("§fPlayers Left: §a" + players.size());
            lines.add("§fKills: §a" + kills.getOrDefault(player.getUniqueId(), 0));
            lines.add("§fRefill in: §e" + (refill / 60) + ":" + String.format("%02d", refill % 60));
        } else {
            lines.add("§fGame: §eEnding");
        }
        lines.add(" ");
        lines.add("§emc.fpixel.net");
        return lines;
    }
}
