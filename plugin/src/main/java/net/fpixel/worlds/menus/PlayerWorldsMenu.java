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

        // Create New World item at slot 48
        ItemStack createItem = new ItemBuilder(Material.NETHER_STAR)
                .name("§a§l+ Nieuwe Wereld Aanmaken")
                .lore(
                        "§7Start direct een nieuwe persoonlijke wereld!",
                        "§7Keuze uit:",
                        "§8 • §fNormal World Generation",
                        "§8 • §bSkyBlock Island",
                        "§8 • §dOneBlock Challenge",
                        "",
                        "§e▶ Klik om de wizard te openen!"
                )
                .build();
        inv.setItem(48, createItem);

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
                    .content("Kies een bestaande spelerwereld of maak een nieuwe aan:");

            builder.button("§2§l+ Nieuwe Wereld Aanmaken\n§7Normal, Skyblock of OneBlock", FormImage.Type.PATH, "textures/ui/anvil_icon");

            for (PlayerWorld pw : accessibleWorlds) {
                String sub = "Door " + pw.getCreatorName() + (pw.isCheatsEnabled() ? " | Cheats: AAN" : "") + " | " + pw.getGeneratorType();
                builder.button("§a§l" + pw.getName() + "\n§7" + sub, FormImage.Type.PATH, "textures/blocks/grass_carried");
            }

            builder.validResultHandler(response -> {
                int clicked = response.clickedButtonId();
                if (clicked == 0) {
                    Bukkit.getScheduler().runTask(plugin, this::openBedrockCreateWizard);
                } else if (clicked - 1 >= 0 && clicked - 1 < accessibleWorlds.size()) {
                    PlayerWorld target = accessibleWorlds.get(clicked - 1);
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

    private void openBedrockCreateWizard() {
        try {
            FloodgatePlayer fp = plugin.getBedrockManager().getFloodgatePlayer(player);
            if (fp == null) return;

            org.geysermc.cumulus.form.CustomForm.Builder builder = org.geysermc.cumulus.form.CustomForm.builder()
                    .title("§2§lWereld Aanmaken")
                    .input("Wereldnaam", "Mijn Wereld", player.getName() + "'s Wereld")
                    .dropdown("Generatietype", java.util.List.of("Normal World", "SkyBlock", "OneBlock"), 0)
                    .dropdown("Gamemode", java.util.List.of("Survival", "Creative", "Hardcore"), 0)
                    .toggle("Cheats Inschakelen", false)
                    .dropdown("Zichtbaarheid / Delen", java.util.List.of("Iedereen (Openbaar)", "Alleen Ik (Privé)"), 1);

            builder.validResultHandler(resp -> {
                String rawName = resp.asInput(0);
                String finalName = (rawName == null || rawName.isBlank()) ? player.getName() + "'s Wereld" : rawName;
                int genIdx = resp.asDropdown(1);
                int gmIdx = resp.asDropdown(2);
                boolean cheats = resp.asToggle(3);
                int shareIdx = resp.asDropdown(4);

                String genType = genIdx == 1 ? "SKYBLOCK" : (genIdx == 2 ? "ONEBLOCK" : "NORMAL");
                String gm = gmIdx == 1 ? "CREATIVE" : (gmIdx == 2 ? "HARDCORE" : "SURVIVAL");
                boolean isPublic = shareIdx == 0;

                String baseId = (player.getName() + "_" + genType).toLowerCase().replaceAll("[^a-z0-9_]", "");
                String uid = baseId + "_" + (System.currentTimeMillis() % 10000);
                long seed = new java.util.Random().nextLong();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§a§lBezig met genereren van je " + genType + " wereld...");
                    PlayerWorld pw = worldManager.createWorld(uid, finalName, player.getName(), player.getUniqueId(), seed, cheats, gm, genType);
                    pw.setPublicWorld(isPublic);
                    worldManager.saveWorlds();
                    worldManager.teleportPlayerToWorld(player, pw.getUid());
                });
            });

            fp.sendForm(builder.build());
        } catch (Throwable t) {
            player.sendMessage("§cKon wizard niet openen: " + t.getMessage());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inv)) return;
        event.setCancelled(true);

        if (event.getWhoClicked() instanceof Player clicker && clicker.equals(player)) {
            int slot = event.getRawSlot();
            if (slot == 48) {
                player.closeInventory();
                new WorldCreationWizardMenu(plugin, worldManager, player).open();
                return;
            }
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
