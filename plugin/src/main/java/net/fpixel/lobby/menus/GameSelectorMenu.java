package net.fpixel.lobby.menus;

import net.fpixel.FPixelPlugin;
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
                        "§fMode: §e1v1 / Teams",
                        "",
                        "§e▶ Click to play!"
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
                        "§fMode: §eClassic FFA",
                        "",
                        "§e▶ Click to play!"
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
                        "§fMode: §eBest of 3",
                        "",
                        "§e▶ Click to play!"
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
                    .button("§c§lBedWars\n§7Click to join", FormImage.Type.PATH, "textures/items/bed_red")
                    .button("§c§lTNT Run\n§7Click to join", FormImage.Type.PATH, "textures/blocks/tnt_side")
                    .button("§6§lSumo 1v1\n§7Click to join", FormImage.Type.PATH, "textures/items/stick")
                    .validResultHandler(response -> {
                        int id = response.clickedButtonId();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (id == 0) {
                                plugin.getGameManager().joinQueue(player, "BEDWARS");
                            } else if (id == 1) {
                                plugin.getGameManager().joinQueue(player, "TNTRUN");
                            } else if (id == 2) {
                                plugin.getGameManager().joinQueue(player, "SUMO");
                            }
                        });
                    })
                    .build();

            fp.sendForm(form);
        } catch (Throwable t) {
            // Fallback to chest GUI if Floodgate Cumulus throws
            openJavaFallback(player);
        }
    }

    private static void openJavaFallback(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatUtil.component(TITLE));
        inv.setItem(11, new ItemBuilder(Material.RED_BED).name("§c§lBedWars").build());
        inv.setItem(13, new ItemBuilder(Material.TNT).name("§c§lTNT Run").build());
        inv.setItem(15, new ItemBuilder(Material.STICK).name("§6§lSumo 1v1").build());
        player.openInventory(inv);
    }

    public static void handleClick(FPixelPlugin plugin, Player player, int slot) {
        if (slot == 11) {
            player.closeInventory();
            plugin.getGameManager().joinQueue(player, "BEDWARS");
        } else if (slot == 13) {
            player.closeInventory();
            plugin.getGameManager().joinQueue(player, "TNTRUN");
        } else if (slot == 15) {
            player.closeInventory();
            plugin.getGameManager().joinQueue(player, "SUMO");
        }
    }
}
