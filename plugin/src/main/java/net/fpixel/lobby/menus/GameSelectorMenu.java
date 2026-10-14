package net.fpixel.lobby.menus;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

public class GameSelectorMenu {
    public static final String TITLE = "§8Game Navigator";

    public static void open(FPixelPlugin plugin, Player player) {
        // If Bedrock player, open native Bedrock SimpleForm
        if (plugin.getBedrockManager().isBedrock(player)) {
            openBedrockForm(plugin, player);
            return;
        }

        // Java player: Open custom Chest GUI
        Inventory inv = Bukkit.createInventory(null, 36, ChatUtil.component(TITLE));

        // Fillers
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, filler);
        }

        // BedWars (Slot 11)
        ItemStack bedwars = new ItemBuilder(Material.RED_BED)
                .name("§c§lBedWars")
                .lore(
                        "§7Defend your bed and destroy enemy beds!",
                        "§7Upgrade equipment with iron, gold & emeralds.",
                        "",
                        "§fMaps: §a10 Themed Arenas",
                        "",
                        "§e▶ Left Click: §fQuick Play",
                        "§e▶ Right Click: §fSelect Map"
                )
                .build();
        inv.setItem(11, bedwars);

        // TNT Run (Slot 13)
        ItemStack tntrun = new ItemBuilder(Material.TNT)
                .name("§c§lTNT Run")
                .lore(
                        "§7The blocks beneath your feet disappear!",
                        "§7Keep sprinting and use double jumps to survive.",
                        "",
                        "§fMaps: §a10 Multi-Tier Arenas",
                        "",
                        "§e▶ Left Click: §fQuick Play",
                        "§e▶ Right Click: §fSelect Map"
                )
                .build();
        inv.setItem(13, tntrun);

        // Sumo (Slot 15)
        ItemStack sumo = new ItemBuilder(Material.STICK)
                .name("§6§lSumo 1v1")
                .lore(
                        "§7Fast-paced knockback duels on a floating arena.",
                        "§7First to knock opponent into void wins the round!",
                        "",
                        "§fMaps: §a10 Dueling Arenas",
                        "",
                        "§e▶ Left Click: §fQuick Play",
                        "§e▶ Right Click: §fSelect Map"
                )
                .build();
        inv.setItem(15, sumo);

        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private static void openBedrockForm(FPixelPlugin plugin, Player player) {
        try {
            FloodgatePlayer fp = plugin.getBedrockManager().getFloodgatePlayer(player);
            if (fp == null) return;

            SimpleForm form = SimpleForm.builder()
                    .title("§6§lFPixel Minigames")
                    .content("Select a minigame to play:")
                    .button("§c§lBedWars (Quick Join)\n§7Join open match", FormImage.Type.PATH, "textures/items/bed_red")
                    .button("§c§lBedWars (Pick Map)\n§7Choose from 10 maps", FormImage.Type.PATH, "textures/items/bed_red")
                    .button("§c§lTNT Run (Quick Join)\n§7Join open match", FormImage.Type.PATH, "textures/blocks/tnt_side")
                    .button("§c§lTNT Run (Pick Map)\n§7Choose from 10 maps", FormImage.Type.PATH, "textures/blocks/tnt_side")
                    .button("§6§lSumo 1v1 (Quick Join)\n§7Join open match", FormImage.Type.PATH, "textures/items/stick")
                    .button("§6§lSumo 1v1 (Pick Map)\n§7Choose from 10 maps", FormImage.Type.PATH, "textures/items/stick")
                    .validResultHandler(response -> {
                        int id = response.clickedButtonId();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (id == 0) plugin.getGameManager().joinQueue(player, "BEDWARS");
                            else if (id == 1) MapSelectMenu.open(plugin, player, "BEDWARS");
                            else if (id == 2) plugin.getGameManager().joinQueue(player, "TNTRUN");
                            else if (id == 3) MapSelectMenu.open(plugin, player, "TNTRUN");
                            else if (id == 4) plugin.getGameManager().joinQueue(player, "SUMO");
                            else if (id == 5) MapSelectMenu.open(plugin, player, "SUMO");
                        });
                    })
                    .build();

            fp.sendForm(form);
        } catch (Throwable t) {
            open(plugin, player);
        }
    }

    public static void handleClick(FPixelPlugin plugin, Player player, int slot, ClickType clickType) {
        if (slot == 11) {
            if (clickType.isRightClick()) {
                MapSelectMenu.open(plugin, player, "BEDWARS");
            } else {
                player.closeInventory();
                plugin.getGameManager().joinQueue(player, "BEDWARS");
            }
        } else if (slot == 13) {
            if (clickType.isRightClick()) {
                MapSelectMenu.open(plugin, player, "TNTRUN");
            } else {
                player.closeInventory();
                plugin.getGameManager().joinQueue(player, "TNTRUN");
            }
        } else if (slot == 15) {
            if (clickType.isRightClick()) {
                MapSelectMenu.open(plugin, player, "SUMO");
            } else {
                player.closeInventory();
                plugin.getGameManager().joinQueue(player, "SUMO");
            }
        }
    }
}
