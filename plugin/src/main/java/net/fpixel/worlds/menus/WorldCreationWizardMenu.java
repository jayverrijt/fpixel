package net.fpixel.worlds.menus;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.worlds.PlayerWorld;
import net.fpixel.worlds.PlayerWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class WorldCreationWizardMenu implements Listener {

    private final FPixelPlugin plugin;
    private final PlayerWorldManager worldManager;
    private final Player player;
    private Inventory inv;

    private String generatorType = "NORMAL"; // NORMAL, SKYBLOCK, ONEBLOCK
    private String gameMode = "SURVIVAL";     // SURVIVAL, CREATIVE, HARDCORE
    private boolean cheats = false;
    private String sharing = "PUBLIC";       // PUBLIC, PRIVATE

    public WorldCreationWizardMenu(FPixelPlugin plugin, PlayerWorldManager worldManager, Player player) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.player = player;
    }

    public void open() {
        inv = Bukkit.createInventory(null, 27, ChatUtil.component("§8» §2§lWereld Aanmaken §8«"));
        render();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private void render() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Slot 10: Normal World
        boolean isNormal = "NORMAL".equalsIgnoreCase(generatorType);
        inv.setItem(10, new ItemBuilder(isNormal ? Material.GRASS_BLOCK : Material.DIRT)
                .name((isNormal ? "§a§l✔ " : "§7") + "Normal World")
                .lore("§7Klassieke Minecraft wereldomgeving", "§7met oceanen, grotten en biomen.", "", isNormal ? "§a▶ Geselecteerd" : "§e▶ Klik om te kiezen")
                .build());

        // Slot 11: SkyBlock
        boolean isSkyblock = "SKYBLOCK".equalsIgnoreCase(generatorType);
        inv.setItem(11, new ItemBuilder(isSkyblock ? Material.OAK_SAPLING : Material.STICK)
                .name((isSkyblock ? "§b§l✔ " : "§7") + "SkyBlock")
                .lore("§7Zwevend eiland in de leegte met", "§7een eikenboom en een starterkist.", "", isSkyblock ? "§a▶ Geselecteerd" : "§e▶ Klik om te kiezen")
                .build());

        // Slot 12: OneBlock
        boolean isOneblock = "ONEBLOCK".equalsIgnoreCase(generatorType);
        inv.setItem(12, new ItemBuilder(isOneblock ? Material.BEDROCK : Material.COBBLESTONE)
                .name((isOneblock ? "§d§l✔ " : "§7") + "OneBlock")
                .lore("§7Eén oneindig herhalend blok.", "§7Breek het om nieuwe fasen te ontgrendelen!", "", isOneblock ? "§a▶ Geselecteerd" : "§e▶ Klik om te kiezen")
                .build());

        // Slot 14: Gamemode
        Material gmMat = "CREATIVE".equalsIgnoreCase(gameMode) ? Material.FEATHER :
                ("HARDCORE".equalsIgnoreCase(gameMode) ? Material.WITHER_SKELETON_SKULL : Material.IRON_SWORD);
        inv.setItem(14, new ItemBuilder(gmMat)
                .name("§e§lGamemode: §f" + gameMode)
                .lore("§7Huidige modus: §a" + gameMode, "", "§e▶ Klik om te wisselen", "§7(Survival ➜ Creative ➜ Hardcore)")
                .build());

        // Slot 15: Cheats
        inv.setItem(15, new ItemBuilder(cheats ? Material.LEVER : Material.REDSTONE_TORCH)
                .name("§e§lCheats: " + (cheats ? "§a§lAAN" : "§c§lUIT"))
                .lore("§7Mogen er commando's en cheat-functies", "§7gebruikt worden in deze wereld?", "", "§e▶ Klik om te toggelen")
                .build());

        // Slot 16: Sharing
        boolean isPub = "PUBLIC".equalsIgnoreCase(sharing);
        inv.setItem(16, new ItemBuilder(isPub ? Material.BEACON : Material.IRON_DOOR)
                .name("§e§lDelen: " + (isPub ? "§a§lIedereen (Openbaar)" : "§c§lAlleen Ik (Privé)"))
                .lore("§7Wie mag jouw wereld bezoeken?", "", "§e▶ Klik om te wisselen")
                .build());

        // Slot 18: Terug
        inv.setItem(18, new ItemBuilder(Material.ARROW)
                .name("§c§l⬅ Terug")
                .lore("§7Terug naar het wereldenoverzicht")
                .build());

        // Slot 22: Bevestigen & Aanmaken
        inv.setItem(22, new ItemBuilder(Material.EMERALD_BLOCK)
                .name("§a§l✔ Start Wereld Nu!")
                .lore(
                        "§7Type: §e" + generatorType,
                        "§7Gamemode: §e" + gameMode,
                        "§7Cheats: §e" + (cheats ? "Aan" : "Uit"),
                        "§7Zichtbaarheid: §e" + (isPub ? "Openbaar" : "Privé"),
                        "",
                        "§a▶ Klik om direct aan te maken en te joinen!"
                )
                .build());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inv)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player clicker) || !clicker.equals(player)) return;

        int slot = event.getRawSlot();

        if (slot == 10) {
            generatorType = "NORMAL";
            ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            render();
        } else if (slot == 11) {
            generatorType = "SKYBLOCK";
            ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            render();
        } else if (slot == 12) {
            generatorType = "ONEBLOCK";
            ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            render();
        } else if (slot == 14) {
            if ("SURVIVAL".equalsIgnoreCase(gameMode)) gameMode = "CREATIVE";
            else if ("CREATIVE".equalsIgnoreCase(gameMode)) gameMode = "HARDCORE";
            else gameMode = "SURVIVAL";
            ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
            render();
        } else if (slot == 15) {
            cheats = !cheats;
            ChatUtil.playSound(player, Sound.BLOCK_LEVER_CLICK, 0.7f, 1.0f);
            render();
        } else if (slot == 16) {
            sharing = "PUBLIC".equalsIgnoreCase(sharing) ? "PRIVATE" : "PUBLIC";
            ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            render();
        } else if (slot == 18) {
            player.closeInventory();
            new PlayerWorldsMenu(plugin, worldManager, player).open();
        } else if (slot == 22) {
            player.closeInventory();
            player.sendMessage("§a§lBezig met genereren van je " + generatorType + " wereld...");
            ChatUtil.playSound(player, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1.5f);

            // Generate clean unique ID
            String baseId = (player.getName() + "_" + generatorType).toLowerCase().replaceAll("[^a-z0-9_]", "");
            String uid = baseId + "_" + (System.currentTimeMillis() % 10000);
            String displayName = player.getName() + "'s " + capitalize(generatorType);

            long seed = new java.util.Random().nextLong();
            PlayerWorld pw = worldManager.createWorld(uid, displayName, player.getName(), player.getUniqueId(), seed, cheats, gameMode, generatorType);
            if ("PRIVATE".equalsIgnoreCase(sharing)) {
                pw.setPublicWorld(false);
                worldManager.saveWorlds();
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                worldManager.teleportPlayerToWorld(player, pw.getUid());
            });
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inv)) {
            HandlerList.unregisterAll(this);
        }
    }
}
