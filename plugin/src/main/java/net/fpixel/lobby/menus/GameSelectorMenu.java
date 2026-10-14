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

        // Java player: Open custom 45-slot Chest GUI
        Inventory inv = Bukkit.createInventory(null, 45, ChatUtil.component(TITLE));

        // Glass fillers
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        // 1. BedWars (Slot 11)
        ItemStack bedwars = new ItemBuilder(Material.RED_BED)
                .name("§c§lBedWars")
                .lore(
                        "§7Defend your bed and destroy enemy beds!",
                        "§7Upgrade equipment with iron, gold & emeralds.",
                        "",
                        "§fMaps: §a10 Fortified Arenas",
                        "",
                        "§e▶ Left Click: §fQuick Play",
                        "§e▶ Right Click: §fSelect Map"
                )
                .build();
        inv.setItem(11, bedwars);

        // 2. SkyWars (Slot 13)
        ItemStack skywars = new ItemBuilder(Material.ENDER_EYE)
                .name("§b§lSkyWars")
                .lore(
                        "§7Spawn on your island, loot chests, and",
                        "§7battle your opponents above the void!",
                        "",
                        "§fMaps: §a10 Floating Islands",
                        "",
                        "§e▶ Left Click: §fQuick Play",
                        "§e▶ Right Click: §fSelect Map"
                )
                .build();
        inv.setItem(13, skywars);

        // 3. TNT Run (Slot 15)
        ItemStack tntrun = new ItemBuilder(Material.TNT)
                .name("§c§lTNT Run")
                .lore(
                        "§7The blocks beneath your feet disappear!",
                        "§7Keep sprinting and use double jumps to survive.",
                        "",
                        "§fMaps: §a10 Roman Colosseums",
                        "",
                        "§e▶ Left Click: §fQuick Play",
                        "§e▶ Right Click: §fSelect Map"
                )
                .build();
        inv.setItem(15, tntrun);

        // 4. Survival SMP (Slot 22 - Centerpiece)
        ItemStack survival = new ItemBuilder(Material.GRASS_BLOCK)
                .name("§a§lSurvival SMP")
                .lore(
                        "§7Normal Vanilla Survival World!",
                        "§7Build your base, mine ores, trade & survive.",
                        "",
                        "§a✓ Mobs & Animals enabled",
                        "§a✓ Free Building & Mining",
                        "§a✓ Vanilla Mechanics",
                        "",
                        "§e▶ Click to Play Survival SMP!"
                )
                .build();
        inv.setItem(22, survival);

        // 5. The Bridge 1v1 (Slot 29)
        ItemStack bridge = new ItemBuilder(Material.RED_TERRACOTTA)
                .name("§6§lThe Bridge 1v1")
                .lore(
                        "§7Cross the narrow bridge over the void",
                        "§7and score 5 goals in the enemy castle portal!",
                        "",
                        "§fMaps: §a10 Bridge Arenas",
                        "",
                        "§e▶ Left Click: §fQuick Play",
                        "§e▶ Right Click: §fSelect Map"
                )
                .build();
        inv.setItem(29, bridge);

        // 6. Sumo 1v1 (Slot 31)
        ItemStack sumo = new ItemBuilder(Material.STICK)
                .name("§e§lSumo 1v1")
                .lore(
                        "§7Fast-paced knockback duels on a floating arena.",
                        "§7First to knock opponent into void wins the round!",
                        "",
                        "§fMaps: §a10 Dojo Temples",
                        "",
                        "§e▶ Left Click: §fQuick Play",
                        "§e▶ Right Click: §fSelect Map"
                )
                .build();
        inv.setItem(31, sumo);

        // 7. Parkour Duels (Slot 33)
        ItemStack parkour = new ItemBuilder(Material.DIAMOND_BOOTS)
                .name("§a§lParkour Duels")
                .lore(
                        "§7Race against your opponent across",
                        "§7perilous checkpoints to reach the finish line first!",
                        "",
                        "§fMaps: §a10 Obstacle Courses",
                        "",
                        "§e▶ Left Click: §fQuick Play",
                        "§e▶ Right Click: §fSelect Map"
                )
                .build();
        inv.setItem(33, parkour);

        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private static void openBedrockForm(FPixelPlugin plugin, Player player) {
        try {
            FloodgatePlayer fp = plugin.getBedrockManager().getFloodgatePlayer(player);
            if (fp == null) return;

            SimpleForm form = SimpleForm.builder()
                    .title("§6§lFPixel Minigames")
                    .content("Select a minigame or world to play:")
                    .button("§a§lSurvival SMP\n§7Vanilla survival world", FormImage.Type.PATH, "textures/blocks/grass_carried")
                    .button("§c§lBedWars (Quick Join)\n§7Join open match", FormImage.Type.PATH, "textures/items/bed_red")
                    .button("§c§lBedWars (Pick Map)\n§7Choose from 10 maps", FormImage.Type.PATH, "textures/items/bed_red")
                    .button("§b§lSkyWars (Quick Join)\n§7Join open match", FormImage.Type.PATH, "textures/items/ender_eye")
                    .button("§b§lSkyWars (Pick Map)\n§7Choose from 10 maps", FormImage.Type.PATH, "textures/items/ender_eye")
                    .button("§c§lTNT Run (Quick Join)\n§7Join open match", FormImage.Type.PATH, "textures/blocks/tnt_side")
                    .button("§c§lTNT Run (Pick Map)\n§7Choose from 10 maps", FormImage.Type.PATH, "textures/blocks/tnt_side")
                    .button("§6§lThe Bridge (Quick Join)\n§7Join 1v1 duel", FormImage.Type.PATH, "textures/blocks/hardened_clay_stained_red")
                    .button("§6§lThe Bridge (Pick Map)\n§7Choose from 10 maps", FormImage.Type.PATH, "textures/blocks/hardened_clay_stained_red")
                    .button("§e§lSumo 1v1 (Quick Join)\n§7Join open match", FormImage.Type.PATH, "textures/items/stick")
                    .button("§e§lSumo 1v1 (Pick Map)\n§7Choose from 10 maps", FormImage.Type.PATH, "textures/items/stick")
                    .button("§a§lParkour Duels (Quick Join)\n§7Join race match", FormImage.Type.PATH, "textures/items/diamond_boots")
                    .button("§a§lParkour Duels (Pick Map)\n§7Choose from 10 maps", FormImage.Type.PATH, "textures/items/diamond_boots")
                    .validResultHandler(response -> {
                        int id = response.clickedButtonId();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            switch (id) {
                                case 0 -> plugin.getSurvivalManager().sendToSurvival(player);
                                case 1 -> plugin.getGameManager().joinQueue(player, "BEDWARS");
                                case 2 -> MapSelectMenu.open(plugin, player, "BEDWARS");
                                case 3 -> plugin.getGameManager().joinQueue(player, "SKYWARS");
                                case 4 -> MapSelectMenu.open(plugin, player, "SKYWARS");
                                case 5 -> plugin.getGameManager().joinQueue(player, "TNTRUN");
                                case 6 -> MapSelectMenu.open(plugin, player, "TNTRUN");
                                case 7 -> plugin.getGameManager().joinQueue(player, "BRIDGE");
                                case 8 -> MapSelectMenu.open(plugin, player, "BRIDGE");
                                case 9 -> plugin.getGameManager().joinQueue(player, "SUMO");
                                case 10 -> MapSelectMenu.open(plugin, player, "SUMO");
                                case 11 -> plugin.getGameManager().joinQueue(player, "PARKOUR");
                                case 12 -> MapSelectMenu.open(plugin, player, "PARKOUR");
                            }
                        });
                    })
                    .build();

            fp.sendForm(form);
        } catch (Throwable t) {
            open(plugin, player);
        }
    }

    public static void handleClick(FPixelPlugin plugin, Player player, int slot, ClickType clickType) {
        if (slot == 22) {
            player.closeInventory();
            plugin.getSurvivalManager().sendToSurvival(player);
        } else if (slot == 11) {
            handleOption(plugin, player, "BEDWARS", clickType);
        } else if (slot == 13) {
            handleOption(plugin, player, "SKYWARS", clickType);
        } else if (slot == 15) {
            handleOption(plugin, player, "TNTRUN", clickType);
        } else if (slot == 29) {
            handleOption(plugin, player, "BRIDGE", clickType);
        } else if (slot == 31) {
            handleOption(plugin, player, "SUMO", clickType);
        } else if (slot == 33) {
            handleOption(plugin, player, "PARKOUR", clickType);
        }
    }

    private static void handleOption(FPixelPlugin plugin, Player player, String gameType, ClickType clickType) {
        if (clickType.isRightClick()) {
            MapSelectMenu.open(plugin, player, gameType);
        } else {
            player.closeInventory();
            plugin.getGameManager().joinQueue(player, gameType);
        }
    }
}
