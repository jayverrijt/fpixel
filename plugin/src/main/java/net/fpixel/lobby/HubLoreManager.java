package net.fpixel.lobby;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.*;

public class HubLoreManager implements Listener, CommandExecutor {
    private final FPixelPlugin plugin;
    public static final String RELIC_TAG = "fpixel_relic";

    public record HubRelic(
            String id,
            String name,
            String locationName,
            String clue,
            String story,
            Material icon,
            int x,
            int y,
            int z
    ) {
        public Location toLocation(World world) {
            return new Location(world, x + 0.5, y, z + 0.5);
        }
    }

    private final List<HubRelic> relics = new ArrayList<>();

    public HubLoreManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        initRelics();
        startParticleTicker();
    }

    private void initRelics() {
        int cy = 100;
        relics.clear();

        // 1. Manor Library: The Chronicler's First Tome
        relics.add(new HubRelic(
                "relic_archivist_journal",
                "§6§lThe Chronicler's First Tome",
                "Medieval Village - Autumn Townhouse",
                "§7Hidden amongst the dusty, enchanted bookshelves of the village townhouse.",
                "§eIn the Year of the Shattering, when the Void tore open the mortal world, the First Builders carved this floating sanctuary from the mountain peaks so that humanity's spirit would never fade.",
                Material.WRITTEN_BOOK,
                -74, cy + 1, -10
        ));

        // 2. Crystal Fountain: The Sunken Chalice of Aetheria
        relics.add(new HubRelic(
                "relic_sunken_chalice",
                "§b§lThe Sunken Chalice of Aetheria",
                "Grand Central Fountain",
                "§7Resting within the crystal waters of the Grand Marble Fountain.",
                "§eA ceremonial goblet of pure electrum, dropped during the Great Crossing. The fountain waters have kept it gleaming for centuries.",
                Material.PRISMARINE_CRYSTALS,
                0, cy + 2, 0
        ));

        // 3. BedWars Smithy: The Forge-Master's Ember
        relics.add(new HubRelic(
                "relic_blacksmith_hammer",
                "§c§lThe Forge-Master's Ember",
                "BedWars Armory - Secret Cellar",
                "§7Tucked in a hidden alcove behind the smoking anvils of the BedWars forge.",
                "§eAn eternal flame pulled from the Nether core. The ancient blacksmiths used this very ember to forge the magical beds that tether a champion's soul to their team.",
                Material.BLAZE_POWDER,
                -64, cy + 1, -12
        ));

        // 4. Golden Windmill: The Stargazer's Celestial Lens
        relics.add(new HubRelic(
                "relic_stargazer_lens",
                "§e§lThe Stargazer's Celestial Lens",
                "The Golden Windmill - High Loft",
                "§7High above the parkour ruins, on the windy wooden rafters of the windmill.",
                "§eThrough this crystal lens, the ancient astrologers tracked the alignment of the Phoenix constellation, foretelling the rise of a new generation of champions.",
                Material.SPYGLASS,
                -66, cy + 14, -62
        ));

        // 5. Sumo Zen Garden: The Sacred Torii Blossom
        relics.add(new HubRelic(
                "relic_torii_offering",
                "§d§lThe Sacred Torii Blossom",
                "Sumo Dojo - Great Cherry Blossom",
                "§7Resting within the ancient hollow of the giant pink cherry tree by the Torii.",
                "§ePlanted by Master Shen, founder of the Sumo Ring. Its blossoms remind all who duel that stillness and balance will overcome brute force every time.",
                Material.CHERRY_SAPLING,
                -60, cy + 1, 70
        ));

        // 6. TNT Colosseum: The Volcanic Core
        relics.add(new HubRelic(
                "relic_alchemist_core",
                "§4§lThe Volcanic Core",
                "TNT Colosseum - Fissure Rocks",
                "§7Concealed in a smoking sulfur crack behind the towering volcanic pillars.",
                "§eA volatile sphere of crystallized magma. The pyromancers of Aetheria discovered that this stone could dissolve solid ground in the blink of an eye.",
                Material.MAGMA_CREAM,
                63, cy + 1, -48
        ));

        // 7. Observatory Dome: The Cloudrunner's Feather
        relics.add(new HubRelic(
                "relic_cloudrunner_feather",
                "§a§lThe Cloudrunner's Feather",
                "Airship Harbor - Observatory Dome",
                "§7Perched atop the glass celestial observatory dome overlooking the airship harbor.",
                "§eLeft behind by Zephyr, the first acrobat who leaped across the void from island to island without ever touching the ground.",
                Material.FEATHER,
                48, cy + 8, 56
        ));
    }

    public void spawnAllRelics(World world) {
        if (world == null) return;
        // Purge old relic stands
        for (Entity e : world.getEntities()) {
            if (e.getScoreboardTags().contains(RELIC_TAG)) {
                e.remove();
            }
        }

        for (HubRelic relic : relics) {
            Location loc = relic.toLocation(world).clone();

            // Ground detection for relic stands if placed on or near the surface
            int bx = loc.getBlockX();
            int bz = loc.getBlockZ();
            int groundY = -1;
            for (int checkY = Math.min(115, loc.getBlockY() + 4); checkY >= Math.max(90, loc.getBlockY() - 4); checkY--) {
                org.bukkit.block.Block b = world.getBlockAt(bx, checkY, bz);
                if (b.getType().isSolid()) {
                    groundY = checkY;
                    break;
                }
            }
            if (groundY != -1 && Math.abs(loc.getBlockY() - groundY) <= 3) {
                loc.setY(groundY + 1.0);
            }

            // Relic floating armor stand (small, invisible, item floating smoothly above ground)
            ArmorStand stand = world.spawn(loc.clone().add(0, 0.2, 0), ArmorStand.class, as -> {
                as.setVisible(false);
                as.setGravity(false);
                as.setInvulnerable(true);
                as.setMarker(false);
                as.setSmall(true);
                as.setCustomNameVisible(true);
                as.customName(ChatUtil.component("§6★ §e§lANCIENT RELIC §6★"));
                as.addScoreboardTag(RELIC_TAG);
                as.addScoreboardTag("relic_" + relic.id());
                as.getEquipment().setHelmet(new ItemStack(relic.icon()));
            });

            // Subtitle stand
            world.spawn(loc.clone().subtract(0, 0.1, 0), ArmorStand.class, sub -> {
                sub.setVisible(false);
                sub.setGravity(false);
                sub.setInvulnerable(true);
                sub.setMarker(true);
                sub.setSmall(true);
                sub.setCustomNameVisible(true);
                sub.customName(ChatUtil.component("§eRight Click to Discover"));
                sub.addScoreboardTag(RELIC_TAG);
                sub.addScoreboardTag("relic_" + relic.id());
            });
        }
        plugin.getLogger().info("Successfully spawned " + relics.size() + " Ancient Relics with floating displays across the Hub!");
    }

    private void startParticleTicker() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            World world = Bukkit.getWorlds().get(0);
            if (world == null) return;

            for (HubRelic relic : relics) {
                Location loc = relic.toLocation(world).add(0, 0.6, 0);
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 4, 0.2, 0.2, 0.2, 0.05);
                world.spawnParticle(Particle.PORTAL, loc, 6, 0.2, 0.2, 0.2, 0.05);
            }
        }, 40L, 40L);
    }

    public void handleRelicClick(Player player, String relicId) {
        HubRelic relic = getRelicById(relicId);
        if (relic == null) return;

        FPixelUser user = plugin.getUserManager().getUser(player);
        if (user == null) return;

        if (user.hasDiscoveredSecret(relic.id())) {
            ChatUtil.sendMessage(player, " ");
            ChatUtil.sendMessage(player, "§6§l★ ALREADY DISCOVERED! ★");
            ChatUtil.sendMessage(player, "§7You have already collected " + relic.name() + "§7!");
            ChatUtil.sendMessage(player, "§eProgress: §a" + user.getDiscoveredSecretsCount() + "§7/§e" + relics.size() + " Ancient Relics found.");
            ChatUtil.sendMessage(player, "§7Type §b/secrets §7to review your clues.");
            ChatUtil.sendMessage(player, " ");
            ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            return;
        }

        // New Discovery!
        user.addDiscoveredSecret(relic.id());
        user.addCoins(250);
        plugin.getUserManager().saveUser(user);

        int count = user.getDiscoveredSecretsCount();

        // Celebration Effects
        spawnFirework(relic.toLocation(player.getWorld()).add(0, 1, 0));
        ChatUtil.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        ChatUtil.sendTitle(player, "§6★ RELIC DISCOVERED! ★", relic.name() + " §7(" + count + "/" + relics.size() + ")", 5, 45, 10);

        // Story Chat Box
        ChatUtil.sendMessage(player, " ");
        ChatUtil.sendMessage(player, "§6§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatUtil.sendMessage(player, "§e§l              ANCIENT RELIC DISCOVERED!");
        ChatUtil.sendMessage(player, "§6               " + relic.name() + " §7[" + count + "/" + relics.size() + "]");
        ChatUtil.sendMessage(player, "§7Location: §f" + relic.locationName());
        ChatUtil.sendMessage(player, " ");
        ChatUtil.sendMessage(player, relic.story());
        ChatUtil.sendMessage(player, " ");
        ChatUtil.sendMessage(player, "§a✔ +250 FPixel Coins awarded!");
        ChatUtil.sendMessage(player, "§7Type §b/secrets §7to see your remaining clues.");
        ChatUtil.sendMessage(player, "§6§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatUtil.sendMessage(player, " ");

        // Check completion of all 7 relics
        if (count >= relics.size()) {
            user.addCoins(1000);
            plugin.getUserManager().saveUser(user);

            ChatUtil.sendTitle(player, "§b§lEXPLORER MASTER!", "§6All 7 Relics Found! §a+1,000 Coins!", 10, 60, 15);
            ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

            ChatUtil.broadcast(" ");
            ChatUtil.broadcast("§6§l★ EXPLORER MASTER ★ §e" + player.getName() + " §7has uncovered all §67 Ancient Relics of FPixel§7!");
            ChatUtil.broadcast("§a" + player.getName() + " §7has earned the master explorer reward!");
            ChatUtil.broadcast(" ");

            for (int i = 0; i < 3; i++) {
                final int delay = i * 15;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        spawnFirework(player.getLocation().add(0, 1, 0));
                    }
                }, delay);
            }
        }
    }

    public void openSecretsGUI(Player player) {
        FPixelUser user = plugin.getUserManager().getUser(player);
        if (user == null) return;

        if (plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrock(player)) {
            openBedrockSecrets(player, user);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, ChatUtil.component("§8The 7 Ancient Relics"));

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        int[] slots = {10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < relics.size(); i++) {
            HubRelic relic = relics.get(i);
            boolean found = user.hasDiscoveredSecret(relic.id());

            ItemStack item;
            if (found) {
                item = new ItemBuilder(relic.icon())
                        .name(relic.name() + " §a✔ DISCOVERED")
                        .lore(
                                "§7Location: §f" + relic.locationName(),
                                "",
                                "§eLore:",
                                relic.story(),
                                "",
                                "§a✔ Found & Claimed (+250 Coins)"
                        )
                        .build();
            } else {
                item = new ItemBuilder(Material.BARRIER)
                        .name("§c§l??? §7(Undiscovered Relic #" + (i + 1) + ")")
                        .lore(
                                "§7Status: §cNot yet found",
                                "",
                                "§eClue / Riddle:",
                                relic.clue(),
                                "",
                                "§6Reward upon finding: §a+250 Coins"
                        )
                        .build();
            }
            inv.setItem(slots[i], item);
        }

        // Summary item in slot 22
        int count = user.getDiscoveredSecretsCount();
        ItemStack summary = new ItemBuilder(Material.NETHER_STAR)
                .name("§6§lRelic Hunt Progress: §e" + count + "§7/§e" + relics.size())
                .lore(
                        "§7Explore the Grand Hub to discover all",
                        "§77 ancient relics left by the First Builders!",
                        "",
                        "§fTotal Coins Earned: §6" + (count * 250) + " Coins",
                        "§fGrand Prize: §a+1,000 Coins §7for finding all 7!",
                        "",
                        count >= relics.size() ? "§a§l★ ALL RELICS COMPLETED! ★" : "§eKeep exploring the Hub!"
                )
                .build();
        inv.setItem(22, summary);

        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private void openBedrockSecrets(Player player, FPixelUser user) {
        try {
            FloodgatePlayer fp = plugin.getBedrockManager().getFloodgatePlayer(player);
            if (fp == null) return;

            int count = user.getDiscoveredSecretsCount();
            StringBuilder sb = new StringBuilder();
            sb.append("§6Ancient Relics Found: §e").append(count).append("/").append(relics.size()).append("\n\n");

            var builder = SimpleForm.builder()
                    .title("§6§lThe 7 Ancient Relics")
                    .content("Explore the Hub and find all 7 ancient treasures to unlock secret lore and +2,750 Coins!\n\nProgress: " + count + "/" + relics.size());

            for (HubRelic relic : relics) {
                boolean found = user.hasDiscoveredSecret(relic.id());
                if (found) {
                    builder.button(relic.name() + "\n§a✔ Found (" + relic.locationName() + ")");
                } else {
                    builder.button("§7Undiscovered Relic\n§eClue: " + relic.clue());
                }
            }
            builder.button("§c« Close");

            fp.sendForm(builder.build());
        } catch (Throwable ignored) {}
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can view relics.");
            return true;
        }
        openSecretsGUI(player);
        return true;
    }

    private HubRelic getRelicById(String id) {
        for (HubRelic r : relics) {
            if (r.id().equalsIgnoreCase(id)) return r;
        }
        return null;
    }

    private String getRelicIdFromEntity(Entity entity) {
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith("relic_")) {
                return tag.substring(6);
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity.getScoreboardTags().contains(RELIC_TAG)) {
            event.setCancelled(true);
            String id = getRelicIdFromEntity(entity);
            if (id != null) {
                handleRelicClick(event.getPlayer(), id);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (event.getRightClicked().getScoreboardTags().contains(RELIC_TAG)) {
            event.setCancelled(true);
            String id = getRelicIdFromEntity(event.getRightClicked());
            if (id != null) {
                handleRelicClick(event.getPlayer(), id);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Location bLoc = event.getClickedBlock().getLocation();
        for (HubRelic relic : relics) {
            if (relic.x() == bLoc.getBlockX() && relic.y() == bLoc.getBlockY() && relic.z() == bLoc.getBlockZ()) {
                event.setCancelled(true);
                handleRelicClick(event.getPlayer(), relic.id());
                return;
            }
        }
    }

    private void spawnFirework(Location loc) {
        if (loc.getWorld() == null) return;
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.ORANGE, Color.YELLOW, Color.WHITE)
                .withFade(Color.FUCHSIA)
                .with(FireworkEffect.Type.BALL_LARGE)
                .flicker(true)
                .trail(true)
                .build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);
    }

    public List<HubRelic> getRelics() { return relics; }
}
