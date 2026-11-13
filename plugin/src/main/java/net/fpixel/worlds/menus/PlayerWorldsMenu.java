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
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.ArrayList;
import java.util.List;

public class PlayerWorldsMenu implements Listener {
    public static final String TITLE = "§8» §2§lPlayer Worlds §8«";
    private final FPixelPlugin plugin;
    private final PlayerWorldManager worldManager;
    private final Player player;
    private final List<PlayerWorld> accessibleWorlds;
    private Inventory inv;

    public PlayerWorldsMenu(FPixelPlugin plugin, PlayerWorldManager worldManager, Player player) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.player = player;
        this.accessibleWorlds = worldManager.getAccessibleWorlds(player);
    }

    public void open() {
        if (plugin.getBedrockManager().isBedrock(player)) {
            openBedrock();
            return;
        }

        inv = Bukkit.createInventory(null, 54, ChatUtil.component(TITLE));

        // Background filler
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        if (accessibleWorlds.isEmpty()) {
            ItemStack emptyItem = new ItemBuilder(Material.BARRIER)
                    .name("§c§lGeen Werelden Gevonden")
                    .lore(
                            "§7Je hebt nog geen eigen werelden aangemaakt",
                            "§7en er zijn nog geen werelden met jou gedeeld.",
                            "",
                            "§eGa naar §bhttps://mc.fpixel.nl §eom direct",
                            "§eeen gratis Survival wereld te starten!"
                    )
                    .build();
            inv.setItem(22, emptyItem);
        } else {
            int slot = 10;
            for (PlayerWorld pw : accessibleWorlds) {
                if (slot % 9 == 8) slot += 2;
                if (slot >= 44) break;

                Material iconMat = pw.isPrimarySmp() ? Material.BEACON :
                        (pw.getCreatorName() != null && pw.getCreatorName().equalsIgnoreCase(player.getName()) ? Material.EMERALD_BLOCK : Material.GRASS_BLOCK);

                List<String> lore = new ArrayList<>();
                lore.add("§7UID: §f" + pw.getUid());
                lore.add("§7Maker: §e" + (pw.getCreatorName() != null ? pw.getCreatorName() : "Server"));
                lore.add("§7Type: " + (pw.isPrimarySmp() ? "§6Hoofd Survival SMP" : "§bPersoonlijke Wereld"));
                lore.add("§7Cheats: " + (pw.isCheatsEnabled() ? "§aIngeschakeld" : "§cUitgeschakeld"));
                if (pw.isCheatsEnabled()) {
                    lore.add("§7Jouw Cheat Status: " + (pw.canCheat(player) ? "§aToegestaan" : "§cGeen Toegang"));
                }
                lore.add("");
                lore.add("§e▶ Klik om direct te verbinden!");

                ItemStack item = new ItemBuilder(iconMat)
                        .name("§a§l" + pw.getName())
                        .lore(lore)
                        .build();

                inv.setItem(slot++, item);
            }
        }

        // Info / Web account item at bottom center (slot 49)
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name("§6§lBeheer op mc.fpixel.nl")
                .lore(
                        "§7Nieuwe werelden aanmaken, vrienden uitnodigen,",
                        "§7cheats instellen en git-backups beheren doe",
                        "§7je eenvoudig in je browser op §bmc.fpixel.nl§7!"
                )
                .build();
        inv.setItem(49, infoItem);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private void openBedrock() {
        try {
            FloodgatePlayer fp = plugin.getBedrockManager().getFloodgatePlayer(player);
            if (fp == null) return;

            SimpleForm.Builder builder = SimpleForm.builder()
                    .title("§2§lPlayer Worlds")
                    .content(accessibleWorlds.isEmpty() ?
                            "Geen werelden beschikbaar. Bezoek mc.fpixel.nl om een wereld aan te maken!" :
                            "Kies een spelerwereld om naar toe te teleporteren:");

            for (PlayerWorld pw : accessibleWorlds) {
                String sub = "Door " + pw.getCreatorName() + (pw.isCheatsEnabled() ? " | Cheats: AAN" : "");
                builder.button("§a§l" + pw.getName() + "\n§7" + sub, FormImage.Type.PATH, "textures/blocks/grass_carried");
            }

            builder.validResultHandler(response -> {
                int clicked = response.clickedButtonId();
                if (clicked >= 0 && clicked < accessibleWorlds.size()) {
                    PlayerWorld target = accessibleWorlds.get(clicked);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        worldManager.teleportPlayerToWorld(player, target.getUid());
                    });
                }
            });

            fp.sendForm(builder.build());
        } catch (Throwable t) {
            // Fallback to java chest GUI
            player.sendMessage("§aOpenen van Player Worlds menu...");
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inv)) return;
        event.setCancelled(true);

        if (event.getWhoClicked() instanceof Player clicker && clicker.equals(player)) {
            int slot = event.getRawSlot();
            if (slot >= 10 && slot < 44) {
                int index = getIndexFromSlot(slot);
                if (index >= 0 && index < accessibleWorlds.size()) {
                    PlayerWorld target = accessibleWorlds.get(index);
                    player.closeInventory();
                    worldManager.teleportPlayerToWorld(player, target.getUid());
                }
            }
        }
    }

    private int getIndexFromSlot(int slot) {
        int index = 0;
        int s = 10;
        for (int i = 0; i < accessibleWorlds.size(); i++) {
            if (s % 9 == 8) s += 2;
            if (s == slot) return i;
            s++;
        }
        return -1;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inv)) {
            HandlerList.unregisterAll(this);
        }
    }
}
