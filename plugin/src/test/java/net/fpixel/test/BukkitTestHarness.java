package net.fpixel.test;

import net.fpixel.FPixelPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.logging.Logger;

/**
 * Lightweight JUnit test harness providing dynamic-proxy Bukkit/Paper test doubles
 * without requiring a running CraftBukkit/Paper server.
 */
public final class BukkitTestHarness {

    private BukkitTestHarness() {}

    public static void ensureBukkitServer() {
        try {
            if (Bukkit.getServer() != null) return;
        } catch (Throwable ignored) {
        }

        PluginManager pm = proxy(PluginManager.class, (m, args) -> null);
        BukkitTask dummyTask = proxy(BukkitTask.class, (m, args) -> {
            if (m.getName().equals("getTaskId")) return 1;
            if (m.getName().equals("isCancelled")) return false;
            return null;
        });
        BukkitScheduler scheduler = proxy(BukkitScheduler.class, (m, args) -> {
            if (m.getReturnType() == BukkitTask.class) return dummyTask;
            if (m.getReturnType() == int.class) return 1;
            return null;
        });
        Logger logger = Logger.getLogger("FPixelTest");

        ItemFactory itemFactory = proxy(ItemFactory.class, (m, args) -> {
            if (m.getReturnType() == boolean.class) return false;
            return null;
        });

        Server server = proxy(Server.class, (m, args) -> switch (m.getName()) {
            case "getPluginManager" -> pm;
            case "getScheduler" -> scheduler;
            case "getLogger" -> logger;
            case "getName" -> "FPixelTestServer";
            case "getVersion" -> "1.0-TEST";
            case "getBukkitVersion" -> "1.21.4-R0.1-SNAPSHOT";
            case "getOnlinePlayers" -> Collections.emptyList();
            case "getItemFactory" -> itemFactory;
            case "isPrimaryThread" -> true;
            default -> defaultReturn(m.getReturnType());
        });

        try {
            var serverField = Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, server);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set Bukkit.server for tests", e);
        }
    }

    public static FPixelPlugin createStubPlugin() {
        ensureBukkitServer();
        try {
            // Allocate without running JavaPlugin constructor (which requires PluginLoader)
            var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            FPixelPlugin stub = (FPixelPlugin) unsafe.allocateInstance(FPixelPlugin.class);

            var jpClass = org.bukkit.plugin.java.JavaPlugin.class;
            var loggerField = jpClass.getDeclaredField("logger");
            loggerField.setAccessible(true);
            loggerField.set(stub, Logger.getLogger("FPixelTest"));

            var dataFolderField = jpClass.getDeclaredField("dataFolder");
            dataFolderField.setAccessible(true);
            dataFolderField.set(stub, new java.io.File(System.getProperty("java.io.tmpdir"), "fpixel-test"));

            return stub;
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate FPixelPlugin stub", e);
        }
    }

    public static World createWorld(String name) {
        Map<String, Block> blocks = new HashMap<>();
        World[] self = new World[1];
        self[0] = proxy(World.class, (m, args) -> switch (m.getName()) {
            case "getName" -> name;
            case "isChunkLoaded" -> true;
            case "loadChunk" -> {
                if (m.getReturnType() == boolean.class) yield true;
                yield null;
            }
            case "getLoadedChunks" -> new org.bukkit.Chunk[0];
            case "getEntities" -> Collections.emptyList();
            case "getBlockAt" -> {
                int x, y, z;
                if (args.length == 1 && args[0] instanceof Location loc) {
                    x = loc.getBlockX();
                    y = loc.getBlockY();
                    z = loc.getBlockZ();
                } else {
                    x = (int) args[0];
                    y = (int) args[1];
                    z = (int) args[2];
                }
                String key = x + "," + y + "," + z;
                yield blocks.computeIfAbsent(key, k -> createBlock(self[0], x, y, z, Material.AIR));
            }
            default -> defaultReturn(m.getReturnType());
        });
        return self[0];
    }

    public static Block createBlock(World world, int x, int y, int z, Material initialType) {
        final Material[] type = {initialType};
        final BlockState[] customState = {null};
        final BlockData[] blockData = {null};
        blockData[0] = createBlockData(type[0]);
        return proxy(Block.class, (m, args) -> switch (m.getName()) {
            case "getWorld" -> world;
            case "getX" -> x;
            case "getY" -> y;
            case "getZ" -> z;
            case "getLocation" -> new Location(world, x, y, z);
            case "getType" -> type[0];
            case "setType" -> {
                type[0] = (Material) args[0];
                blockData[0] = createBlockData(type[0]);
                yield null;
            }
            case "getState" -> customState[0] != null ? customState[0] : proxy(BlockState.class, (bm, ba) -> {
                if (bm.getName().equals("getType")) return type[0];
                if (bm.getName().equals("getLocation")) return new Location(world, x, y, z);
                return defaultReturn(bm.getReturnType());
            });
            case "setBlockData" -> {
                if (args[0] instanceof BlockData bd) {
                    blockData[0] = bd;
                    type[0] = bd.getMaterial();
                }
                yield null;
            }
            case "getBlockData" -> blockData[0];
            default -> defaultReturn(m.getReturnType());
        });
    }

    private static BlockData createBlockData(Material mat) {
        return proxy(BlockData.class, (m, args) -> {
            if (m.getName().equals("getMaterial")) return mat;
            if (m.getName().equals("clone")) return createBlockData(mat);
            return defaultReturn(m.getReturnType());
        });
    }

    public static Inventory createInventory(int size) {
        ItemStack[] contents = new ItemStack[size];
        return proxy(Inventory.class, (m, args) -> switch (m.getName()) {
            case "getSize" -> size;
            case "getContents" -> contents;
            case "getItem" -> contents[(int) args[0]];
            case "setItem" -> {
                contents[(int) args[0]] = (ItemStack) args[1];
                yield null;
            }
            case "addItem" -> {
                ItemStack[] items = (ItemStack[]) args[0];
                for (ItemStack item : items) {
                    if (item == null) continue;
                    for (int i = 0; i < contents.length; i++) {
                        if (contents[i] == null) {
                            contents[i] = item;
                            break;
                        }
                    }
                }
                yield Collections.emptyMap();
            }
            case "clear" -> {
                Arrays.fill(contents, null);
                yield null;
            }
            case "isEmpty" -> {
                for (ItemStack item : contents) {
                    if (item != null) yield false;
                }
                yield true;
            }
            default -> defaultReturn(m.getReturnType());
        });
    }

    public static Chest createChest(World world, int x, int y, int z, Inventory inv) {
        Location loc = new Location(world, x, y, z);
        return proxy(Chest.class, (m, args) -> switch (m.getName()) {
            case "getLocation" -> loc;
            case "getWorld" -> world;
            case "getX" -> x;
            case "getY" -> y;
            case "getZ" -> z;
            case "getType" -> Material.CHEST;
            case "getBlockInventory", "getInventory" -> inv;
            case "update" -> true;
            default -> defaultReturn(m.getReturnType());
        });
    }

    public static Player createPlayer(String name, UUID uuid, World world) {
        org.bukkit.inventory.PlayerInventory inv = proxy(org.bukkit.inventory.PlayerInventory.class, (m, args) -> {
            if (m.getReturnType() == int.class) return 41;
            if (m.getReturnType() == boolean.class) return true;
            return null;
        });
        return proxy(Player.class, (m, args) -> switch (m.getName()) {
            case "getName" -> name;
            case "getUniqueId" -> uuid;
            case "getWorld" -> world;
            case "getLocation" -> new Location(world, 0, 64, 0);
            case "isOnline" -> true;
            case "getActivePotionEffects" -> Collections.emptyList();
            case "getInventory" -> inv;
            default -> defaultReturn(m.getReturnType());
        });
    }

    public static Hanging createHanging(World world) {
        return proxy(Hanging.class, (m, args) -> switch (m.getName()) {
            case "getWorld" -> world;
            case "getLocation" -> new Location(world, 0, 65, 0);
            default -> defaultReturn(m.getReturnType());
        });
    }

    public static Projectile createProjectile(Player shooter, World world) {
        return proxy(Projectile.class, (m, args) -> switch (m.getName()) {
            case "getShooter" -> shooter;
            case "getWorld" -> world;
            default -> defaultReturn(m.getReturnType());
        });
    }

    @FunctionalInterface
    public interface Handler {
        Object handle(java.lang.reflect.Method method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    public static <T> T proxy(Class<T> iface, Handler handler) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                (proxyObj, method, args) -> {
                    if (method.getName().equals("equals") && method.getParameterCount() == 1) {
                        return proxyObj == args[0];
                    }
                    if (method.getName().equals("hashCode") && method.getParameterCount() == 0) {
                        return System.identityHashCode(proxyObj);
                    }
                    if (method.getName().equals("toString") && method.getParameterCount() == 0) {
                        return iface.getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(proxyObj));
                    }
                    return handler.handle(method, args == null ? new Object[0] : args);
                }
        );
    }

    private static Object defaultReturn(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }
}
