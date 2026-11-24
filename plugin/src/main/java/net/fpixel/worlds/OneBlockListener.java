package net.fpixel.worlds;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the regenerating block mechanics and tier-based progression for OneBlock player worlds.
 */
public class OneBlockListener implements Listener {
    private final FPixelPlugin plugin;
    private final Map<String, Integer> blockCounts = new ConcurrentHashMap<>();
    private final File saveFile;
    private static final Random RNG = new Random();

    public OneBlockListener(FPixelPlugin plugin) {
        this.plugin = plugin;
        this.saveFile = new File(plugin.getDataFolder(), "oneblock_progress.json");
        loadProgress();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOneBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getX() != 0 || block.getY() != 64 || block.getZ() != 0) return;

        World world = block.getWorld();
        if (!world.getName().startsWith("player_worlds/")) return;

        String uid = world.getName().substring("player_worlds/".length());
        PlayerWorld pw = plugin.getPlayerWorldManager().getWorld(uid);
        if (pw == null || !"ONEBLOCK".equalsIgnoreCase(pw.getGeneratorType())) return;

        int currentCount = blockCounts.getOrDefault(uid, 0) + 1;
        blockCounts.put(uid, currentCount);

        int oldPhase = getPhase(currentCount - 1);
        int newPhase = getPhase(currentCount);

        Player player = event.getPlayer();

        // Level Up to new phase!
        if (newPhase > oldPhase) {
            String phaseName = getPhaseName(newPhase);
            for (Player p : world.getPlayers()) {
                ChatUtil.sendTitle(p, "§a§lONEBLOCK LEVEL UP!", "§eFase " + newPhase + ": §f" + phaseName, 10, 50, 15);
                ChatUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                ChatUtil.sendMessage(p, "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                ChatUtil.sendMessage(p, "§a§l[OneBlock] §eLevel Up! §7Je hebt §f" + currentCount + " §7blokken gemined.");
                ChatUtil.sendMessage(p, "§7Nieuwe fase ontgrendeld: §a§l" + phaseName);
                ChatUtil.sendMessage(p, "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }
        }

        // Schedule next block regeneration on tick + 1
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!world.isChunkLoaded(0, 0)) {
                world.loadChunk(0, 0, true);
            }

            // Ensure no bedrock block underneath the OneBlock
            if (world.getBlockAt(0, 63, 0).getType() == Material.BEDROCK) {
                world.getBlockAt(0, 63, 0).setType(Material.AIR, false);
            }

            Block target = world.getBlockAt(0, 64, 0);

            // 5% chance for bonus chest every 25+ blocks
            if (currentCount % 25 == 0 && RNG.nextInt(100) < 50) {
                target.setType(Material.CHEST, false);
                if (target.getState() instanceof Chest chest) {
                    fillBonusChest(chest, newPhase);
                }
                world.spawnParticle(Particle.HAPPY_VILLAGER, new Location(world, 0.5, 65.5, 0.5), 15, 0.3, 0.3, 0.3);
                world.playSound(new Location(world, 0.5, 64.5, 0.5), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.2f);
                ChatUtil.sendActionBar(player, "§6§l★ BONUS CHEST! §eOpen de kist voor zeldzame loot!");
            } else if (currentCount % 40 == 0 && RNG.nextInt(100) < 35) {
                // Rare friendly mob spawn
                target.setType(getPhaseMaterial(newPhase), false);
                spawnPhaseMob(world, newPhase);
            } else {
                Material nextMat = getPhaseMaterial(newPhase);
                target.setType(nextMat, false);
                world.spawnParticle(Particle.COMPOSTER, new Location(world, 0.5, 65.0, 0.5), 10, 0.2, 0.2, 0.2);
                world.playSound(new Location(world, 0.5, 64.5, 0.5), Sound.BLOCK_STONE_PLACE, 0.7f, 1.2f);
            }

            if (currentCount % 10 == 0) {
                saveProgress();
            }
        }, 1L);
    }

    private int getPhase(int count) {
        if (count < 50) return 1;    // Vlakte / Bos
        if (count < 150) return 2;   // Ondergronds / Grot
        if (count < 300) return 3;   // Moeras & Klei
        if (count < 500) return 4;   // Winter / Sneeuw
        if (count < 750) return 5;   // Woestijn
        if (count < 1050) return 6;  // Jungle
        if (count < 1500) return 7;  // Deep Dark / Diamanten
        return 8;                    // Nether & The End
    }

    private String getPhaseName(int phase) {
        return switch (phase) {
            case 1 -> "Vlakte & Bos (Plains)";
            case 2 -> "Ondergrondse Mijnen (Mines)";
            case 3 -> "Moeras & Modder (Swamp)";
            case 4 -> "Winter & IJs (Winter)";
            case 5 -> "Woestijn & Zand (Desert)";
            case 6 -> "Jungle & Bamboe (Jungle)";
            case 7 -> "Deepslate & Diamanten (Deep Dark)";
            default -> "Nether & The End (Extreem)";
        };
    }

    private Material getPhaseMaterial(int phase) {
        return switch (phase) {
            case 1 -> choose(Material.GRASS_BLOCK, Material.DIRT, Material.OAK_LOG, Material.OAK_LEAVES, Material.CLAY, Material.COARSE_DIRT);
            case 2 -> choose(Material.STONE, Material.COBBLESTONE, Material.COAL_ORE, Material.IRON_ORE, Material.COPPER_ORE, Material.GRAVEL, Material.ANDESITE, Material.DIORITE);
            case 3 -> choose(Material.MUD, Material.CLAY, Material.BIRCH_LOG, Material.SLIME_BLOCK, Material.MOSS_BLOCK, Material.WATER);
            case 4 -> choose(Material.SNOW_BLOCK, Material.PACKED_ICE, Material.SPRUCE_LOG, Material.BLUE_ICE, Material.LAPIS_ORE);
            case 5 -> choose(Material.SAND, Material.SANDSTONE, Material.RED_SAND, Material.TERRACOTTA, Material.GOLD_ORE, Material.CACTUS);
            case 6 -> choose(Material.JUNGLE_LOG, Material.MELON, Material.BAMBOO_BLOCK, Material.REDSTONE_ORE, Material.COCOA);
            case 7 -> choose(Material.DEEPSLATE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.OBSIDIAN, Material.AMETHYST_BLOCK, Material.DEEPSLATE_GOLD_ORE);
            default -> choose(Material.NETHERRACK, Material.SOUL_SAND, Material.NETHER_QUARTZ_ORE, Material.GLOWSTONE, Material.END_STONE, Material.CRYING_OBSIDIAN, Material.ANCIENT_DEBRIS);
        };
    }

    private Material choose(Material... materials) {
        return materials[RNG.nextInt(materials.length)];
    }

    private void fillBonusChest(Chest chest, int phase) {
        var inv = chest.getBlockInventory();
        inv.clear();
        inv.setItem(RNG.nextInt(9), new ItemStack(Material.BREAD, 2 + RNG.nextInt(4)));
        if (phase == 1) {
            inv.setItem(9 + RNG.nextInt(9), new ItemStack(Material.WHEAT_SEEDS, 2 + RNG.nextInt(3)));
            inv.setItem(18 + RNG.nextInt(9), new ItemStack(Material.OAK_SAPLING, 1 + RNG.nextInt(2)));
        } else if (phase == 2) {
            inv.setItem(9 + RNG.nextInt(9), new ItemStack(Material.IRON_INGOT, 1 + RNG.nextInt(3)));
            inv.setItem(18 + RNG.nextInt(9), new ItemStack(Material.TORCH, 4));
        } else if (phase == 3) {
            inv.setItem(9 + RNG.nextInt(9), new ItemStack(Material.WATER_BUCKET, 1));
            inv.setItem(18 + RNG.nextInt(9), new ItemStack(Material.SLIME_BALL, 2));
        } else if (phase == 4) {
            inv.setItem(9 + RNG.nextInt(9), new ItemStack(Material.SPRUCE_SAPLING, 2));
            inv.setItem(18 + RNG.nextInt(9), new ItemStack(Material.SWEET_BERRIES, 4));
        } else if (phase == 5) {
            inv.setItem(9 + RNG.nextInt(9), new ItemStack(Material.LAVA_BUCKET, 1));
            inv.setItem(18 + RNG.nextInt(9), new ItemStack(Material.GOLD_INGOT, 2));
        } else if (phase == 6) {
            inv.setItem(9 + RNG.nextInt(9), new ItemStack(Material.BAMBOO, 3));
            inv.setItem(18 + RNG.nextInt(9), new ItemStack(Material.MELON_SEEDS, 2));
        } else if (phase == 7) {
            inv.setItem(9 + RNG.nextInt(9), new ItemStack(Material.DIAMOND, 1 + RNG.nextInt(2)));
            inv.setItem(18 + RNG.nextInt(9), new ItemStack(Material.GOLDEN_APPLE, 1));
        } else {
            inv.setItem(9 + RNG.nextInt(9), new ItemStack(Material.NETHERITE_SCRAP, 1));
            inv.setItem(18 + RNG.nextInt(9), new ItemStack(Material.ENDER_PEARL, 2));
        }
        chest.update(true, false);
    }

    private void spawnPhaseMob(World world, int phase) {
        Location spawn = new Location(world, 0.5, 65.0, 0.5);
        EntityType type = switch (phase) {
            case 1 -> RNG.nextBoolean() ? EntityType.CHICKEN : EntityType.PIG;
            case 2 -> EntityType.SHEEP;
            case 3 -> EntityType.COW;
            case 4 -> EntityType.FOX;
            case 5 -> EntityType.RABBIT;
            case 6 -> EntityType.PARROT;
            case 7 -> EntityType.BAT;
            default -> EntityType.STRIDER;
        };
        try {
            world.spawnEntity(spawn, type);
        } catch (Exception ignored) {}
    }

    public int getBlockCount(String uid) {
        return blockCounts.getOrDefault(uid, 0);
    }

    private void loadProgress() {
        if (!saveFile.exists()) return;
        try (FileReader reader = new FileReader(saveFile, StandardCharsets.UTF_8)) {
            var map = new com.google.gson.Gson().fromJson(reader, Map.class);
            if (map != null) {
                for (Object key : map.keySet()) {
                    Object val = map.get(key);
                    if (val instanceof Number num) {
                        blockCounts.put(key.toString(), num.intValue());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public void saveProgress() {
        try {
            if (!saveFile.getParentFile().exists()) saveFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(saveFile, StandardCharsets.UTF_8)) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(blockCounts, writer);
            }
        } catch (IOException ignored) {}
    }
}
