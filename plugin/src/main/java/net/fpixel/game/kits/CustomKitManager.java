package net.fpixel.game.kits;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and applies admin-created custom kits per gamemode from plugins/FPixelCore/custom_kits.json.
 */
public class CustomKitManager implements Listener {

    public static class KitItem {
        public String material;
        public int amount = 1;
    }

    public static class CustomKit {
        public String id;
        public String name;
        public String icon = "CHEST";
        public String helmet;
        public String chestplate;
        public String leggings;
        public String boots;
        public List<KitItem> items = new ArrayList<>();
    }

    private final FPixelPlugin plugin;
    private final File kitsFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, List<CustomKit>> kitsByGame = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> selectedKits = new ConcurrentHashMap<>();
    private long lastLoadedModified = -1L;

    public CustomKitManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        this.kitsFile = new File(plugin.getDataFolder(), "custom_kits.json");
        loadKits();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public synchronized void loadKits() {
        if (!kitsFile.exists()) {
            kitsByGame.clear();
            return;
        }
        try (FileReader reader = new FileReader(kitsFile, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, List<CustomKit>>>() {}.getType();
            Map<String, List<CustomKit>> parsed = gson.fromJson(reader, type);
            kitsByGame.clear();
            if (parsed != null) {
                for (Map.Entry<String, List<CustomKit>> entry : parsed.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        kitsByGame.put(entry.getKey().toUpperCase(), new ArrayList<>(entry.getValue()));
                    }
                }
            }
            lastLoadedModified = kitsFile.lastModified();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load custom_kits.json: " + e.getMessage());
        }
    }

    public void reloadIfModified() {
        if (kitsFile.exists() && kitsFile.lastModified() != lastLoadedModified) {
            loadKits();
        }
    }

    public List<CustomKit> getKitsForGameType(String gameType) {
        reloadIfModified();
        if (gameType == null) return Collections.emptyList();
        return kitsByGame.getOrDefault(gameType.toUpperCase(), Collections.emptyList());
    }

    public void selectKit(UUID playerId, String gameType, String kitId) {
        if (playerId == null || gameType == null || kitId == null) return;
        selectedKits.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(gameType.toUpperCase(), kitId);
    }

    public CustomKit getSelectedKit(UUID playerId, String gameType) {
        if (playerId == null || gameType == null) return null;
        Map<String, String> map = selectedKits.get(playerId);
        if (map == null) return null;
        String kitId = map.get(gameType.toUpperCase());
        if (kitId == null) return null;
        for (CustomKit kit : getKitsForGameType(gameType)) {
            if (kit.id != null && kit.id.equalsIgnoreCase(kitId)) {
                return kit;
            }
        }
        return null;
    }

    /**
     * Applies the player's selected custom kit for this gamemode if one is selected.
     * @return true if a custom kit was applied.
     */
    public boolean applySelectedKit(Player player, String gameType) {
        if (player == null || gameType == null) return false;
        CustomKit kit = getSelectedKit(player.getUniqueId(), gameType);
        if (kit == null) return false;
        applyKit(player, kit);
        return true;
    }

    public void applyKit(Player player, CustomKit kit) {
        if (player == null || kit == null) return;
        player.getInventory().clear();

        if (kit.helmet != null && !kit.helmet.isBlank()) {
            Material m = Material.matchMaterial(kit.helmet.trim().toUpperCase());
            if (m != null) player.getInventory().setHelmet(new ItemStack(m));
        }
        if (kit.chestplate != null && !kit.chestplate.isBlank()) {
            Material m = Material.matchMaterial(kit.chestplate.trim().toUpperCase());
            if (m != null) player.getInventory().setChestplate(new ItemStack(m));
        }
        if (kit.leggings != null && !kit.leggings.isBlank()) {
            Material m = Material.matchMaterial(kit.leggings.trim().toUpperCase());
            if (m != null) player.getInventory().setLeggings(new ItemStack(m));
        }
        if (kit.boots != null && !kit.boots.isBlank()) {
            Material m = Material.matchMaterial(kit.boots.trim().toUpperCase());
            if (m != null) player.getInventory().setBoots(new ItemStack(m));
        }
        if (kit.items != null) {
            for (KitItem ki : kit.items) {
                if (ki == null || ki.material == null) continue;
                Material m = Material.matchMaterial(ki.material.trim().toUpperCase());
                if (m != null && m != Material.AIR) {
                    int amt = Math.max(1, Math.min(64, ki.amount));
                    player.getInventory().addItem(new ItemStack(m, amt));
                }
            }
        }
        ChatUtil.sendMessage(player, "§a§l[KIT] §eCustom kit §6" + kit.name + " §egeactiveerd!");
    }

    public void openKitSelector(Player player, String gameType) {
        List<CustomKit> kits = getKitsForGameType(gameType);
        if (kits.isEmpty()) {
            ChatUtil.sendMessage(player, "§7Er zijn nog geen custom kits ingesteld voor §e" + gameType + "§7.");
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, ChatUtil.component("§8» §6§lCustom Kits: " + gameType));
        int slot = 10;
        for (CustomKit kit : kits) {
            if (slot >= 17) break;
            Material iconMat = Material.matchMaterial(kit.icon != null ? kit.icon.toUpperCase() : "CHEST");
            if (iconMat == null || iconMat == Material.AIR) iconMat = Material.CHEST;
            List<String> lore = new ArrayList<>();
            lore.add("§7ID: §f" + kit.id);
            if (kit.items != null) {
                for (int i = 0; i < Math.min(5, kit.items.size()); i++) {
                    KitItem ki = kit.items.get(i);
                    lore.add("§8• §f" + ki.amount + "x " + ki.material);
                }
            }
            lore.add("");
            lore.add("§e▶ Klik om deze kit te selecteren!");
            inv.setItem(slot++, new ItemBuilder(iconMat).name("§a§l" + kit.name).lore(lore).build());
        }
        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    @EventHandler
    public void onKitItemInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.CHEST) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().getDisplayName().contains("Custom Kits")) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        var arena = plugin.getGameManager().getPlayerArena(player);
        if (arena != null) {
            openKitSelector(player, arena.getGameType());
        }
    }

    @EventHandler
    public void onKitInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.startsWith("» Custom Kits: ")) return;

        event.setCancelled(true);
        String gameType = title.substring("» Custom Kits: ".length()).trim();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasLore()) return;
        for (String line : clicked.getItemMeta().getLore()) {
            String stripped = line.replaceAll("§[0-9a-fk-or]", "");
            if (stripped.startsWith("ID: ")) {
                String kitId = stripped.substring(4).trim();
                selectKit(player.getUniqueId(), gameType, kitId);
                player.closeInventory();
                ChatUtil.sendMessage(player, "§a§l[KIT] §eCustom kit §6" + kitId + " §egeselecteerd voor §f" + gameType + "§e!");
                ChatUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.3f);
                return;
            }
        }
    }
}
