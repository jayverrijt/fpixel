package net.fpixel.lobby.menus;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

public class QuickMenu {
    public static final String TITLE = "§8FPixel Quick Menu";

    public static void open(FPixelPlugin plugin, Player player) {
        if (plugin.getBedrockManager().isBedrock(player)) {
            openBedrockForm(plugin, player);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, ChatUtil.component(TITLE));
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Slot 10: Instant Survival SMP
        ItemStack survival = new ItemBuilder(Material.GRASS_BLOCK)
                .name("§a§lSurvival SMP")
                .lore(
                        "§7Instant warp to the Survival World!",
                        "§7Your survival items & coords will be restored.",
                        "",
                        "§e▶ Click to play Survival SMP!"
                )
                .build();
        inv.setItem(10, survival);

        // Slot 12: Hub Spawn / Refresh
        ItemStack hub = new ItemBuilder(Material.BEACON)
                .name("§6§lMain Hub #1")
                .lore(
                        "§7Connected to: §aHub-1 (Online)",
                        "§7Click to teleport to central Plaza.",
                        "",
                        "§e▶ Click to warp to Hub Spawn!"
                )
                .build();
        inv.setItem(12, hub);

        // Slot 14: Profile & Stats
        FPixelUser user = plugin.getUserManager().getUser(player);
        ItemStack profile = new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(player.getName())
                .name("§e§lMy Profile & Stats")
                .lore(
                        "§7Rank: " + plugin.getRankManager().getRank(player).getPrefix(),
                        "§7Coins: §6" + user.getCoins(),
                        "§7Level: §b" + user.getLevel(),
                        "",
                        "§e▶ Click to view full statistics!"
                )
                .build();
        inv.setItem(14, profile);

        // Slot 16: Player Visibility Toggle
        ItemStack hider = new ItemBuilder(Material.LIME_DYE)
                .name("§b§lPlayer Visibility")
                .lore(
                        "§7Adjust which players you see in the Hub.",
                        "",
                        "§e▶ Click to change visibility!"
                )
                .build();
        inv.setItem(16, hider);

        // Slot 22: Open Game Navigator
        ItemStack nav = new ItemBuilder(Material.COMPASS)
                .name("§d§lGame Navigator")
                .lore(
                        "§7Browse all available minigames & maps.",
                        "",
                        "§e▶ Click to open Game Selector!"
                )
                .build();
        inv.setItem(22, nav);

        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private static void openBedrockForm(FPixelPlugin plugin, Player player) {
        try {
            FloodgatePlayer fp = plugin.getBedrockManager().getFloodgatePlayer(player);
            if (fp == null) return;

            SimpleForm form = SimpleForm.builder()
                    .title("§6§lFPixel Quick Menu")
                    .content("Select an action:")
                    .button("§a§lPlay Survival SMP\n§7Instant warp to survival", FormImage.Type.PATH, "textures/blocks/grass_carried")
                    .button("§6§lHub Spawn\n§7Warp to central plaza", FormImage.Type.PATH, "textures/blocks/beacon")
                    .button("§e§lMy Profile & Stats\n§7View coins, rank & stats", FormImage.Type.PATH, "textures/items/emerald")
                    .button("§b§lPlayer Visibility\n§7Show / hide players", FormImage.Type.PATH, "textures/items/dye_powder_lime")
                    .button("§d§lGame Navigator\n§7Browse all minigames", FormImage.Type.PATH, "textures/items/compass_item")
                    .validResultHandler(response -> {
                        int id = response.clickedButtonId();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            switch (id) {
                                case 0 -> plugin.getSurvivalManager().sendToSurvival(player);
                                case 1 -> plugin.getLobbyManager().teleportToLobby(player);
                                case 2 -> StatsMenu.open(plugin, player);
                                case 3 -> PlayerHiderMenu.open(player);
                                case 4 -> GameSelectorMenu.open(plugin, player);
                            }
                        });
                    })
                    .build();

            fp.sendForm(form);
        } catch (Throwable t) {
            open(plugin, player);
        }
    }

    public static void handleClick(FPixelPlugin plugin, Player player, int slot) {
        if (slot == 10) {
            player.closeInventory();
            plugin.getSurvivalManager().sendToSurvival(player);
        } else if (slot == 12) {
            player.closeInventory();
            plugin.getLobbyManager().teleportToLobby(player);
        } else if (slot == 14) {
            player.closeInventory();
            StatsMenu.open(plugin, player);
        } else if (slot == 16) {
            player.closeInventory();
            PlayerHiderMenu.open(player);
        } else if (slot == 22) {
            player.closeInventory();
            GameSelectorMenu.open(plugin, player);
        }
    }
}
