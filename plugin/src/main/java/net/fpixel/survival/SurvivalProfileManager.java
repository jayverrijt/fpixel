package net.fpixel.survival;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fpixel.FPixelPlugin;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SurvivalProfileManager {
    private final FPixelPlugin plugin;
    private final File dataDir;
    private final File importedDir;

    public SurvivalProfileManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        this.dataDir = new File(plugin.getDataFolder(), "survival_data");
        this.importedDir = new File(dataDir, "imported");
        this.dataDir.mkdirs();
        this.importedDir.mkdirs();
    }

    /**
     * Checks whether an item is a Lobby item that should never exist or be saved in Survival.
     */
    public static boolean isLobbyItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        Material mat = item.getType();
        if (mat != Material.COMPASS && mat != Material.PLAYER_HEAD &&
            mat != Material.LIME_DYE && mat != Material.GRAY_DYE &&
            mat != Material.NETHER_STAR) {
            return false;
        }
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String name = "";
        try {
            if (meta.hasDisplayName() && meta.displayName() != null) {
                name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            }
        } catch (Throwable ignored) {}
        if (name.isEmpty() && meta.hasDisplayName()) {
            name = meta.getDisplayName();
        }
        if (name == null) name = "";
        return name.contains("Game Navigator") ||
               name.contains("My Profile") ||
               name.contains("Players:") ||
               name.contains("Quick Menu");
    }

    /**
     * Checks if a YAML config has any actual non-lobby survival items.
     */
    private boolean hasSurvivalItems(YamlConfiguration config) {
        List<?> invList = config.getList("inventory");
        if (invList != null) {
            for (Object obj : invList) {
                if (obj instanceof ItemStack item && item.getType() != Material.AIR && !isLobbyItem(item)) {
                    return true;
                }
            }
        }
        List<?> armorList = config.getList("armor");
        if (armorList != null) {
            for (Object obj : armorList) {
                if (obj instanceof ItemStack item && item.getType() != Material.AIR && !isLobbyItem(item)) {
                    return true;
                }
            }
        }
        ItemStack offhand = config.getItemStack("offhand");
        if (offhand != null && offhand.getType() != Material.AIR && !isLobbyItem(offhand)) {
            return true;
        }
        return false;
    }

    /**
     * Loads the survival profile (location, inventory, armor, offhand, XP, health) for the player.
     * Teleports the player to their exact saved survival position.
     */
    public void loadProfile(Player player, World survivalWorld) {
        File ymlFile = new File(dataDir, player.getUniqueId().toString() + ".yml");

        // Clear any leftover items completely before loading
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        File importedFile = findImportedFile(player);

        if (ymlFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(ymlFile);
            boolean hasItems = hasSurvivalItems(config);

            // If YAML has no survival items but an imported JSON file exists, prioritize the imported file!
            if (!hasItems && importedFile != null && importedFile.exists()) {
                plugin.getLogger().info("YAML profile for " + player.getName() + " had no items; restoring from imported profile: " + importedFile.getName());
                ymlFile.delete();
                loadFromJson(player, importedFile, survivalWorld);
                saveProfile(player);
                return;
            }

            loadFromYaml(player, config, survivalWorld);
            return;
        }

        // Check if an imported JSON file exists from the user's world zip
        if (importedFile != null && importedFile.exists()) {
            plugin.getLogger().info("Found imported survival profile for " + player.getName() + " in " + importedFile.getName());
            loadFromJson(player, importedFile, survivalWorld);
            // Immediately save to YAML so it becomes their permanent profile
            saveProfile(player);
            return;
        }

        // New player with no prior data: teleport to survival spawn with clean state
        Location spawn = survivalWorld.getSpawnLocation();
        player.teleport(spawn);
        setPlayerHealth(player, 20.0);
        player.setFoodLevel(20);
        player.setLevel(0);
        player.setExp(0);
        plugin.getLogger().info("New player " + player.getName() + " initialized at survival spawn: " + spawn);
    }

    private boolean fileHasItems(File file) {
        if (file == null || !file.exists()) return false;
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("inventory") && json.get("inventory").isJsonArray()) {
                JsonArray inv = json.getAsJsonArray("inventory");
                if (inv.size() > 0) return true;
            }
            if (json.has("equipment") && json.get("equipment").isJsonObject()) {
                JsonObject eq = json.getAsJsonObject("equipment");
                if (eq.size() > 0) return true;
            }
            if (json.has("level") && json.get("level").getAsInt() > 0) return true;
        } catch (Exception ignored) {}
        return false;
    }

    public File findImportedFile(Player player) {
        String name = player.getName().toLowerCase();
        UUID uuid = player.getUniqueId();

        // 1. Check exact session UUID with items
        File byUuid = new File(importedDir, uuid.toString() + ".json");
        if (byUuid.exists() && fileHasItems(byUuid)) return byUuid;

        // 2. Check offline UUID with items
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + player.getName()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        File byOfflineUuid = new File(importedDir, offlineUuid.toString() + ".json");
        if (byOfflineUuid.exists() && fileHasItems(byOfflineUuid)) return byOfflineUuid;

        // 3. Check exact lowercase username with items
        File byName = new File(importedDir, name + ".json");
        if (byName.exists() && fileHasItems(byName)) return byName;

        // 4. Strip Bedrock / Floodgate prefix if present (e.g. .username or *username or _username)
        if (name.startsWith(".") || name.startsWith("*") || name.startsWith("_")) {
            File stripped = new File(importedDir, name.substring(1) + ".json");
            if (stripped.exists() && fileHasItems(stripped)) return stripped;
        }

        // 5. Check with dot prefix for Bedrock players only if it has items
        File dotted = new File(importedDir, "." + name + ".json");
        if (dotted.exists() && fileHasItems(dotted)) return dotted;

        // Fallbacks if no file had items yet:
        if (byUuid.exists()) return byUuid;
        if (byOfflineUuid.exists()) return byOfflineUuid;
        if (byName.exists()) return byName;
        if (dotted.exists()) return dotted;

        // 6. Case-insensitive scan of imported directory
        File[] files = importedDir.listFiles();
        if (files != null) {
            File fallback = null;
            for (File f : files) {
                if (f.getName().endsWith(".json")) {
                    String base = f.getName().substring(0, f.getName().length() - 5).toLowerCase();
                    if (base.equals(name) || base.equals(player.getName().toLowerCase())
                            || (base.startsWith(".") && base.substring(1).equals(name))
                            || (name.startsWith(".") && base.equals(name.substring(1)))) {
                        if (fileHasItems(f)) return f;
                        if (fallback == null) fallback = f;
                    }
                }
            }
            if (fallback != null) return fallback;
        }

        return null;
    }

    private void setPlayerHealth(Player player, double health) {
        try {
            Attribute attrType = Attribute.valueOf("MAX_HEALTH");
            AttributeInstance attr = player.getAttribute(attrType);
            double maxHealth = attr != null ? attr.getValue() : 20.0;
            player.setHealth(Math.min(maxHealth, Math.max(1.0, health)));
        } catch (Throwable t) {
            try {
                player.setHealth(Math.min(20.0, Math.max(1.0, health)));
            } catch (Throwable ignored) {}
        }
    }

    private void loadFromYaml(Player player, YamlConfiguration config, World survivalWorld) {
        try {
            // 1. Location
            if (config.contains("location.x")) {
                String worldName = config.getString("location.world", "survival");
                World targetWorld = Bukkit.getWorld(worldName);
                if (targetWorld == null || (plugin.getSurvivalManager() != null && !plugin.getSurvivalManager().isSurvivalWorld(targetWorld))) {
                    targetWorld = survivalWorld;
                }
                double x = config.getDouble("location.x");
                double y = config.getDouble("location.y");
                double z = config.getDouble("location.z");
                float yaw = (float) config.getDouble("location.yaw");
                float pitch = (float) config.getDouble("location.pitch");
                Location loc = new Location(targetWorld, x, y, z, yaw, pitch);
                player.teleport(loc);
            } else {
                player.teleport(survivalWorld.getSpawnLocation());
            }

            // 2. Stats
            if (config.contains("level")) player.setLevel(config.getInt("level"));
            if (config.contains("exp")) player.setExp((float) config.getDouble("exp"));
            if (config.contains("health")) setPlayerHealth(player, config.getDouble("health"));
            if (config.contains("food")) player.setFoodLevel(config.getInt("food"));

            // 3. Inventory (strip any lobby items)
            List<?> invList = config.getList("inventory");
            if (invList != null) {
                ItemStack[] items = invList.toArray(new ItemStack[0]);
                for (int i = 0; i < items.length; i++) {
                    if (isLobbyItem(items[i])) {
                        items[i] = null;
                    }
                }
                player.getInventory().setContents(items);
            }

            // 4. Armor
            List<?> armorList = config.getList("armor");
            if (armorList != null) {
                ItemStack[] armor = armorList.toArray(new ItemStack[0]);
                for (int i = 0; i < armor.length; i++) {
                    if (isLobbyItem(armor[i])) {
                        armor[i] = null;
                    }
                }
                player.getInventory().setArmorContents(armor);
            }

            // 5. Offhand
            if (config.contains("offhand")) {
                ItemStack offhand = config.getItemStack("offhand");
                if (offhand != null && !isLobbyItem(offhand)) {
                    player.getInventory().setItemInOffHand(offhand);
                }
            }

            // 6. Ender Chest
            List<?> enderList = config.getList("enderchest");
            if (enderList != null) {
                ItemStack[] ender = enderList.toArray(new ItemStack[0]);
                player.getEnderChest().setContents(ender);
            }

            plugin.getLogger().info("Successfully loaded survival profile for " + player.getName() + " at " + player.getLocation());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load survival YAML for " + player.getName() + ": " + e.getMessage());
            player.teleport(survivalWorld.getSpawnLocation());
        }
    }

    private void loadFromJson(Player player, File file, World survivalWorld) {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            // 1. Location
            double x = json.get("x").getAsDouble();
            double y = json.get("y").getAsDouble();
            double z = json.get("z").getAsDouble();
            float yaw = json.has("yaw") ? json.get("yaw").getAsFloat() : 0.0f;
            float pitch = json.has("pitch") ? json.get("pitch").getAsFloat() : 0.0f;
            Location loc = new Location(survivalWorld, x, y, z, yaw, pitch);
            player.teleport(loc);

            // 2. Stats
            if (json.has("level")) player.setLevel(json.get("level").getAsInt());
            if (json.has("exp")) player.setExp(json.get("exp").getAsFloat());
            if (json.has("health")) setPlayerHealth(player, json.get("health").getAsDouble());
            if (json.has("food")) player.setFoodLevel(json.get("food").getAsInt());

            // 3. Inventory Items
            if (json.has("inventory") && json.get("inventory").isJsonArray()) {
                JsonArray invArr = json.getAsJsonArray("inventory");
                for (JsonElement el : invArr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    int slot = obj.get("slot").getAsInt();
                    ItemStack stack = buildItemStackFromJson(obj);
                    if (stack != null && !isLobbyItem(stack) && slot >= 0 && slot < player.getInventory().getSize()) {
                        player.getInventory().setItem(slot, stack);
                    }
                }
            }

            // 4. Equipment (Armor & Offhand)
            if (json.has("equipment") && json.get("equipment").isJsonObject()) {
                JsonObject eq = json.getAsJsonObject("equipment");
                if (eq.has("head")) {
                    ItemStack head = buildItemStackFromJson(eq.getAsJsonObject("head"));
                    if (head != null && !isLobbyItem(head)) player.getInventory().setHelmet(head);
                }
                if (eq.has("chest")) {
                    ItemStack chest = buildItemStackFromJson(eq.getAsJsonObject("chest"));
                    if (chest != null && !isLobbyItem(chest)) player.getInventory().setChestplate(chest);
                }
                if (eq.has("legs")) {
                    ItemStack legs = buildItemStackFromJson(eq.getAsJsonObject("legs"));
                    if (legs != null && !isLobbyItem(legs)) player.getInventory().setLeggings(legs);
                }
                if (eq.has("feet")) {
                    ItemStack feet = buildItemStackFromJson(eq.getAsJsonObject("feet"));
                    if (feet != null && !isLobbyItem(feet)) player.getInventory().setBoots(feet);
                }
                if (eq.has("offhand")) {
                    ItemStack off = buildItemStackFromJson(eq.getAsJsonObject("offhand"));
                    if (off != null && !isLobbyItem(off)) player.getInventory().setItemInOffHand(off);
                }
            }

            plugin.getLogger().info("Successfully imported survival JSON data for " + player.getName() + " at " + loc);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load survival JSON for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.teleport(survivalWorld.getSpawnLocation());
        }
    }

    private Enchantment findEnchantment(String key) {
        key = key.toLowerCase().replace("minecraft:", "").trim();
        try {
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
            if (ench != null) return ench;
        } catch (Throwable ignored) {}
        try {
            Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(key));
            if (ench != null) return ench;
        } catch (Throwable ignored) {}
        try {
            Enchantment ench = Enchantment.getByName(key.toUpperCase());
            if (ench != null) return ench;
        } catch (Throwable ignored) {}
        if (key.equals("sweeping")) return findEnchantment("sweeping_edge");
        return null;
    }

    private ItemStack buildItemStackFromJson(JsonObject obj) {
        try {
            String matName = obj.get("material").getAsString().toUpperCase();
            Material mat = Material.matchMaterial(matName);
            if (mat == null) {
                plugin.getLogger().warning("Unknown material from JSON: " + matName);
                return null;
            }

            int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
            ItemStack stack = new ItemStack(mat, Math.max(1, count));
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return stack;

            // Damage / Durability
            if (obj.has("damage") && meta instanceof Damageable d) {
                d.setDamage(obj.get("damage").getAsInt());
            }

            // Repair cost
            if (obj.has("repair_cost") && meta instanceof Repairable r) {
                r.setRepairCost(obj.get("repair_cost").getAsInt());
            }

            // Enchantments
            if (obj.has("enchantments") && obj.get("enchantments").isJsonObject()) {
                JsonObject enchs = obj.getAsJsonObject("enchantments");
                for (Map.Entry<String, JsonElement> entry : enchs.entrySet()) {
                    String enchKey = entry.getKey().toLowerCase();
                    int enchLevel = entry.getValue().getAsInt();
                    Enchantment ench = findEnchantment(enchKey);
                    if (ench != null) {
                        meta.addEnchant(ench, enchLevel, true);
                    } else {
                        plugin.getLogger().warning("Could not find enchantment: " + enchKey);
                    }
                }
            }

            stack.setItemMeta(meta);
            return stack;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to build item from json: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves the current survival profile (location, inventory, armor, offhand, XP, health, food) to disk.
     */
    public void saveProfile(Player player) {
        if (plugin.getSurvivalManager() != null) {
            if (!plugin.getSurvivalManager().isPlayerInSurvival(player)) {
                return;
            }
        } else if (!player.getWorld().getName().equalsIgnoreCase("survival")) {
            return;
        }

        File ymlFile = new File(dataDir, player.getUniqueId().toString() + ".yml");
        try {
            YamlConfiguration config = new YamlConfiguration();
            Location loc = player.getLocation();
            config.set("location.world", loc.getWorld().getName());
            config.set("location.x", loc.getX());
            config.set("location.y", loc.getY());
            config.set("location.z", loc.getZ());
            config.set("location.yaw", loc.getYaw());
            config.set("location.pitch", loc.getPitch());

            config.set("level", player.getLevel());
            config.set("exp", player.getExp());
            config.set("health", player.getHealth());
            config.set("food", player.getFoodLevel());

            // Filter out any lobby items before saving
            ItemStack[] inv = player.getInventory().getContents();
            for (int i = 0; i < inv.length; i++) {
                if (isLobbyItem(inv[i])) {
                    inv[i] = null;
                }
            }
            config.set("inventory", inv);

            ItemStack[] armor = player.getInventory().getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                if (isLobbyItem(armor[i])) {
                    armor[i] = null;
                }
            }
            config.set("armor", armor);

            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (isLobbyItem(offhand)) {
                offhand = null;
            }
            config.set("offhand", offhand);

            config.set("enderchest", player.getEnderChest().getContents());

            config.save(ymlFile);
            plugin.getLogger().info("Saved survival profile for " + player.getName() + " at " + loc);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save survival YAML for " + player.getName() + ": " + e.getMessage());
        }
    }
}
