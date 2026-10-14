package net.fpixel.survival;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fpixel.FPixelPlugin;
import org.bukkit.*;
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
     * Loads the survival profile (location, inventory, armor, offhand, XP, health) for the player.
     * Teleports the player to their exact saved survival position.
     */
    public void loadProfile(Player player, World survivalWorld) {
        File ymlFile = new File(dataDir, player.getUniqueId().toString() + ".yml");

        // Clear any leftover lobby items
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        if (ymlFile.exists()) {
            loadFromYaml(player, ymlFile, survivalWorld);
            return;
        }

        // Check if an imported JSON file exists from the user's world zip
        File importedFile = findImportedFile(player);
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
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setLevel(0);
        player.setExp(0);
    }

    private File findImportedFile(Player player) {
        // 1. By UUID
        File byUuid = new File(importedDir, player.getUniqueId().toString() + ".json");
        if (byUuid.exists()) return byUuid;

        // 2. By Username (lowercase)
        File byName = new File(importedDir, player.getName().toLowerCase() + ".json");
        if (byName.exists()) return byName;

        return null;
    }

    private void loadFromYaml(Player player, File file, World survivalWorld) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            // 1. Location
            if (config.contains("location.x")) {
                double x = config.getDouble("location.x");
                double y = config.getDouble("location.y");
                double z = config.getDouble("location.z");
                float yaw = (float) config.getDouble("location.yaw");
                float pitch = (float) config.getDouble("location.pitch");
                Location loc = new Location(survivalWorld, x, y, z, yaw, pitch);
                player.teleport(loc);
            } else {
                player.teleport(survivalWorld.getSpawnLocation());
            }

            // 2. Stats
            if (config.contains("level")) player.setLevel(config.getInt("level"));
            if (config.contains("exp")) player.setExp((float) config.getDouble("exp"));
            if (config.contains("health")) player.setHealth(Math.min(player.getMaxHealth(), config.getDouble("health")));
            if (config.contains("food")) player.setFoodLevel(config.getInt("food"));

            // 3. Inventory
            List<?> invList = config.getList("inventory");
            if (invList != null) {
                ItemStack[] items = invList.toArray(new ItemStack[0]);
                player.getInventory().setContents(items);
            }

            // 4. Armor
            List<?> armorList = config.getList("armor");
            if (armorList != null) {
                ItemStack[] armor = armorList.toArray(new ItemStack[0]);
                player.getInventory().setArmorContents(armor);
            }

            // 5. Offhand
            if (config.contains("offhand")) {
                ItemStack offhand = config.getItemStack("offhand");
                if (offhand != null) player.getInventory().setItemInOffHand(offhand);
            }

            // 6. Ender Chest
            List<?> enderList = config.getList("enderchest");
            if (enderList != null) {
                ItemStack[] ender = enderList.toArray(new ItemStack[0]);
                player.getEnderChest().setContents(ender);
            }

            plugin.getLogger().info("Successfully loaded survival profile for " + player.getName());
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
            if (json.has("health")) player.setHealth(Math.min(player.getMaxHealth(), json.get("health").getAsDouble()));
            if (json.has("food")) player.setFoodLevel(json.get("food").getAsInt());

            // 3. Inventory Items
            if (json.has("inventory") && json.get("inventory").isJsonArray()) {
                JsonArray invArr = json.getAsJsonArray("inventory");
                for (JsonElement el : invArr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    int slot = obj.get("slot").getAsInt();
                    ItemStack stack = buildItemStackFromJson(obj);
                    if (stack != null && slot >= 0 && slot < player.getInventory().getSize()) {
                        player.getInventory().setItem(slot, stack);
                    }
                }
            }

            // 4. Equipment (Armor & Offhand)
            if (json.has("equipment") && json.get("equipment").isJsonObject()) {
                JsonObject eq = json.getAsJsonObject("equipment");
                if (eq.has("head")) {
                    ItemStack head = buildItemStackFromJson(eq.getAsJsonObject("head"));
                    if (head != null) player.getInventory().setHelmet(head);
                }
                if (eq.has("chest")) {
                    ItemStack chest = buildItemStackFromJson(eq.getAsJsonObject("chest"));
                    if (chest != null) player.getInventory().setChestplate(chest);
                }
                if (eq.has("legs")) {
                    ItemStack legs = buildItemStackFromJson(eq.getAsJsonObject("legs"));
                    if (legs != null) player.getInventory().setLeggings(legs);
                }
                if (eq.has("feet")) {
                    ItemStack feet = buildItemStackFromJson(eq.getAsJsonObject("feet"));
                    if (feet != null) player.getInventory().setBoots(feet);
                }
                if (eq.has("offhand")) {
                    ItemStack off = buildItemStackFromJson(eq.getAsJsonObject("offhand"));
                    if (off != null) player.getInventory().setItemInOffHand(off);
                }
            }

            plugin.getLogger().info("Successfully imported survival JSON data for " + player.getName() + " at " + loc);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load survival JSON for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.teleport(survivalWorld.getSpawnLocation());
        }
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
                    Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchKey));
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
        if (!player.getWorld().getName().equalsIgnoreCase("survival")) {
            return;
        }

        File ymlFile = new File(dataDir, player.getUniqueId().toString() + ".yml");
        try {
            YamlConfiguration config = new YamlConfiguration();
            Location loc = player.getLocation();
            config.set("location.x", loc.getX());
            config.set("location.y", loc.getY());
            config.set("location.z", loc.getZ());
            config.set("location.yaw", loc.getYaw());
            config.set("location.pitch", loc.getPitch());

            config.set("level", player.getLevel());
            config.set("exp", player.getExp());
            config.set("health", player.getHealth());
            config.set("food", player.getFoodLevel());

            config.set("inventory", player.getInventory().getContents());
            config.set("armor", player.getInventory().getArmorContents());
            config.set("offhand", player.getInventory().getItemInOffHand());
            config.set("enderchest", player.getEnderChest().getContents());

            config.save(ymlFile);
            plugin.getLogger().info("Saved survival profile for " + player.getName() + " at " + loc);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save survival YAML for " + player.getName() + ": " + e.getMessage());
        }
    }
}
