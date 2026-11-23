package net.fpixel.games.skywars;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.ArrayList;
import java.util.List;

public class SkyWarsKitMenu {
    public static final String TITLE = "§8Select SkyWars Kit";

    public static void open(FPixelPlugin plugin, Player player, SkyWarsGame game) {
        if (plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrock(player)) {
            openBedrockForm(plugin, player, game);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 36, ChatUtil.component(TITLE));
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) inv.setItem(i, filler);

        SkyWarsKit currentKit = game.getSelectedKit(player.getUniqueId());

        // Slots 10 to 16, and 19 to 21 (Total 10 slots)
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        SkyWarsKit[] kits = SkyWarsKit.values();

        for (int i = 0; i < kits.length && i < slots.length; i++) {
            SkyWarsKit kit = kits[i];
            boolean selected = (currentKit == kit);

            List<String> lore = new ArrayList<>(kit.getLore());
            if (selected) {
                lore.add(0, "§a§l✔ CURRENTLY SELECTED");
                lore.add(1, "");
            }

            ItemBuilder builder = new ItemBuilder(kit.getIcon())
                    .name(kit.getDisplayName())
                    .lore(lore);

            if (selected) {
                builder.hideFlags();
            }

            inv.setItem(slots[i], builder.build());
        }

        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private static void openBedrockForm(FPixelPlugin plugin, Player player, SkyWarsGame game) {
        try {
            FloodgatePlayer fp = plugin.getBedrockManager().getFloodgatePlayer(player);
            if (fp == null) return;

            SkyWarsKit currentKit = game.getSelectedKit(player.getUniqueId());

            SimpleForm.Builder builder = SimpleForm.builder()
                    .title("§b§lSkyWars Kits")
                    .content("Choose your SkyWars combat kit:");

            for (SkyWarsKit kit : SkyWarsKit.values()) {
                String prefix = (currentKit == kit) ? "§a✔ " : "";
                builder.button(prefix + kit.getDisplayName() + "\n§7" + kit.getId());
            }

            builder.validResultHandler(response -> {
                int id = response.clickedButtonId();
                if (id >= 0 && id < SkyWarsKit.values().length) {
                    SkyWarsKit chosen = SkyWarsKit.values()[id];
                    game.setSelectedKit(player.getUniqueId(), chosen);
                    ChatUtil.sendMessage(player, "§aYou selected the " + chosen.getDisplayName() + " §akit!");
                    ChatUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                }
            });

            fp.sendForm(builder.build());
        } catch (Throwable ignored) {
            open(plugin, player, game);
        }
    }

    public static void handleClick(FPixelPlugin plugin, Player player, int slot) {
        Arena arena = plugin.getGameManager().getPlayerArena(player);
        if (!(arena instanceof SkyWarsGame game)) {
            player.closeInventory();
            return;
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        SkyWarsKit[] kits = SkyWarsKit.values();

        for (int i = 0; i < slots.length && i < kits.length; i++) {
            if (slots[i] == slot) {
                SkyWarsKit chosen = kits[i];
                game.setSelectedKit(player.getUniqueId(), chosen);
                ChatUtil.sendMessage(player, "§aYou selected the " + chosen.getDisplayName() + " §akit!");
                ChatUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                player.closeInventory();
                return;
            }
        }
    }
}
