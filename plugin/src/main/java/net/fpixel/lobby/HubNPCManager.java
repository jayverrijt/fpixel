package net.fpixel.lobby;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.lobby.menus.GameSelectorMenu;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.List;

public class HubNPCManager implements Listener {
    private final FPixelPlugin plugin;
    public static final String NPC_TAG = "fpixel_hub_npc";
    public static final String HOLO_TAG = "fpixel_hologram";

    public record NPCDefinition(
            String id,
            String title,
            String subtitle,
            Location location,
            ItemStack head,
            ItemStack chest,
            ItemStack legs,
            ItemStack boots,
            ItemStack hand,
            ItemStack offhand
    ) {}

    public HubNPCManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void removeExistingNPCs(World world) {
        if (world == null) return;
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains(NPC_TAG) || entity.getScoreboardTags().contains(HOLO_TAG)) {
                entity.remove();
            }
        }
    }

    public void spawnAllNPCs(World world) {
        removeExistingNPCs(world);

        List<NPCDefinition> npcs = getDefinitions(world);
        for (NPCDefinition def : npcs) {
            spawnNPC(def);
        }
        plugin.getLogger().info("Successfully spawned " + npcs.size() + " Hub Game NPCs with holograms!");
    }

    private List<NPCDefinition> getDefinitions(World world) {
        List<NPCDefinition> list = new ArrayList<>();
        double y = 100.0;

        // 1. Central Welcome Guide (0, 100, -8) facing South (yaw 180)
        list.add(new NPCDefinition(
                "guide",
                "§6§lGAME NAVIGATOR",
                "§eClick to Browse Games",
                new Location(world, 0.5, y, -8.5, 180f, 0f),
                new ItemBuilder(Material.GOLDEN_HELMET).build(),
                new ItemBuilder(Material.GOLDEN_CHESTPLATE).build(),
                new ItemBuilder(Material.GOLDEN_LEGGINGS).build(),
                new ItemBuilder(Material.GOLDEN_BOOTS).build(),
                new ItemBuilder(Material.COMPASS).build(),
                new ItemBuilder(Material.NETHER_STAR).build()
        ));

        // 2. Survival SMP (North Gate: 0, 100, -30) facing South (yaw 180)
        list.add(new NPCDefinition(
                "survival",
                "§a§lSURVIVAL SMP",
                "§7Vanilla Survival World",
                new Location(world, 0.5, y, -30.5, 180f, 0f),
                new ItemBuilder(Material.DIAMOND_HELMET).build(),
                new ItemBuilder(Material.DIAMOND_CHESTPLATE).build(),
                new ItemBuilder(Material.DIAMOND_LEGGINGS).build(),
                new ItemBuilder(Material.DIAMOND_BOOTS).build(),
                new ItemBuilder(Material.DIAMOND_PICKAXE).build(),
                new ItemBuilder(Material.TRIDENT).build()
        ));

        // 3. BedWars (North-East Outpost: 22, 100, -22) facing South-West (yaw 225)
        list.add(new NPCDefinition(
                "bedwars",
                "§c§lBEDWARS",
                "§710 Fortified Maps",
                new Location(world, 22.5, y, -22.5, 225f, 0f),
                new ItemBuilder(Material.RED_WOOL).build(),
                new ItemBuilder(Material.IRON_CHESTPLATE).build(),
                new ItemBuilder(Material.IRON_LEGGINGS).build(),
                new ItemBuilder(Material.IRON_BOOTS).build(),
                new ItemBuilder(Material.DIAMOND_SWORD).build(),
                new ItemBuilder(Material.RED_BED).build()
        ));

        // 4. SkyWars (East Floating Shrine: 30, 100, 0) facing West (yaw 270)
        list.add(new NPCDefinition(
                "skywars",
                "§b§lSKYWARS",
                "§710 Floating Void Maps",
                new Location(world, 30.5, y, 0.5, 270f, 0f),
                new ItemBuilder(Material.DIAMOND_HELMET).build(),
                new ItemBuilder(Material.CHAINMAIL_CHESTPLATE).build(),
                new ItemBuilder(Material.CHAINMAIL_LEGGINGS).build(),
                new ItemBuilder(Material.DIAMOND_BOOTS).build(),
                new ItemBuilder(Material.BOW).build(),
                new ItemBuilder(Material.ENDER_PEARL).build()
        ));

        // 5. TNT Run (South-East Volcanic Arena: 22, 100, 22) facing North-West (yaw 315)
        list.add(new NPCDefinition(
                "tntrun",
                "§4§lTNT RUN",
                "§710 Colosseum Maps",
                new Location(world, 22.5, y, 22.5, 315f, 0f),
                new ItemBuilder(Material.TNT).build(),
                new ItemBuilder(Material.LEATHER_CHESTPLATE).build(),
                new ItemBuilder(Material.LEATHER_LEGGINGS).build(),
                new ItemBuilder(Material.GOLDEN_BOOTS).build(),
                new ItemBuilder(Material.SHEARS).build(),
                null
        ));

        // 6. Duels & PvP (South Knights Hall: 0, 100, 30) facing North (yaw 0)
        list.add(new NPCDefinition(
                "duels",
                "§d§lDUELS & PVP",
                "§71v1 Competitive Arenas",
                new Location(world, 0.5, y, 30.5, 0f, 0f),
                new ItemBuilder(Material.IRON_HELMET).build(),
                new ItemBuilder(Material.IRON_CHESTPLATE).build(),
                new ItemBuilder(Material.IRON_LEGGINGS).build(),
                new ItemBuilder(Material.IRON_BOOTS).build(),
                new ItemBuilder(Material.DIAMOND_SWORD).build(),
                new ItemBuilder(Material.SHIELD).build()
        ));

        // 7. The Bridge (South-West Water Bridge: -22, 100, 22) facing North-East (yaw 45)
        list.add(new NPCDefinition(
                "bridge",
                "§6§lTHE BRIDGE 1V1",
                "§7Score in Enemy Goal",
                new Location(world, -22.5, y, 22.5, 45f, 0f),
                new ItemBuilder(Material.GOLDEN_HELMET).build(),
                new ItemBuilder(Material.LEATHER_CHESTPLATE).build(),
                new ItemBuilder(Material.LEATHER_LEGGINGS).build(),
                new ItemBuilder(Material.GOLDEN_BOOTS).build(),
                new ItemBuilder(Material.BOW).build(),
                new ItemBuilder(Material.RED_TERRACOTTA).build()
        ));

        // 8. Sumo (West Dojo Ring: -30, 100, 0) facing East (yaw 90)
        list.add(new NPCDefinition(
                "sumo",
                "§e§lSUMO 1V1",
                "§7Knock Off The Ring",
                new Location(world, -30.5, y, 0.5, 90f, 0f),
                null,
                new ItemBuilder(Material.IRON_CHESTPLATE).build(),
                new ItemBuilder(Material.LEATHER_LEGGINGS).build(),
                null,
                new ItemBuilder(Material.STICK).build(),
                null
        ));

        // 9. Parkour Duels (North-West Agility Tower: -22, 100, -22) facing South-East (yaw 135)
        list.add(new NPCDefinition(
                "parkour",
                "§a§lPARKOUR DUELS",
                "§710 Checkpoint Courses",
                new Location(world, -22.5, y, -22.5, 135f, 0f),
                null,
                new ItemBuilder(Material.LEATHER_CHESTPLATE).build(),
                new ItemBuilder(Material.LEATHER_LEGGINGS).build(),
                new ItemBuilder(Material.DIAMOND_BOOTS).build(),
                new ItemBuilder(Material.FEATHER).build(),
                null
        ));

        return list;
    }

    private void spawnNPC(NPCDefinition def) {
        Location loc = def.location();
        World world = loc.getWorld();
        if (world == null) return;

        // 1. ArmorStand Body
        ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setArms(true);
        stand.setBasePlate(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setCollidable(false);
        stand.setCustomNameVisible(false);
        stand.addScoreboardTag(NPC_TAG);
        stand.addScoreboardTag("npc_" + def.id());

        // Pose
        stand.setRightArmPose(new EulerAngle(Math.toRadians(-15), Math.toRadians(0), Math.toRadians(10)));

        // Equipment
        if (stand.getEquipment() != null) {
            if (def.head() != null) stand.getEquipment().setHelmet(def.head());
            if (def.chest() != null) stand.getEquipment().setChestplate(def.chest());
            if (def.legs() != null) stand.getEquipment().setLeggings(def.legs());
            if (def.boots() != null) stand.getEquipment().setBoots(def.boots());
            if (def.hand() != null) stand.getEquipment().setItemInMainHand(def.hand());
            if (def.offhand() != null) stand.getEquipment().setItemInOffHand(def.offhand());
        }

        // 2. Modern TextDisplay Hologram directly above head
        Location holoLoc = loc.clone().add(0, 2.15, 0);
        try {
            TextDisplay display = (TextDisplay) world.spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
            String fullText = def.title() + "\n" + def.subtitle() + "\n§e§l▶ CLICK TO PLAY ◀";
            display.text(ChatUtil.component(fullText));
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.addScoreboardTag(HOLO_TAG);
            display.addScoreboardTag("npc_" + def.id());
        } catch (Throwable t) {
            // Fallback for environments where TextDisplay is unavailable
            stand.customName(ChatUtil.component(def.title() + " §e[CLICK]"));
            stand.setCustomNameVisible(true);
        }
    }

    public void handleNPCClick(Player player, String npcId) {
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5);

        switch (npcId) {
            case "survival" -> {
                plugin.getSurvivalManager().sendToSurvival(player);
            }
            case "bedwars" -> {
                plugin.getGameManager().joinQueue(player, "BEDWARS");
            }
            case "skywars" -> {
                plugin.getGameManager().joinQueue(player, "SKYWARS");
            }
            case "tntrun" -> {
                plugin.getGameManager().joinQueue(player, "TNTRUN");
            }
            case "bridge" -> {
                plugin.getGameManager().joinQueue(player, "BRIDGE");
            }
            case "sumo" -> {
                plugin.getGameManager().joinQueue(player, "SUMO");
            }
            case "parkour" -> {
                plugin.getGameManager().joinQueue(player, "PARKOUR");
            }
            case "duels" -> {
                plugin.getGameManager().joinQueue(player, "PVP");
            }
            case "guide" -> {
                GameSelectorMenu.open(plugin, player);
            }
            default -> {
                ChatUtil.sendMessage(player, "§aOpening " + npcId + "...");
            }
        }
    }

    private String getNPCIdFromEntity(Entity entity) {
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith("npc_")) {
                return tag.substring(4);
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity.getScoreboardTags().contains(NPC_TAG) || entity.getScoreboardTags().contains(HOLO_TAG)) {
            event.setCancelled(true);
            String id = getNPCIdFromEntity(entity);
            if (id != null) {
                handleNPCClick(event.getPlayer(), id);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity.getScoreboardTags().contains(NPC_TAG) || entity.getScoreboardTags().contains(HOLO_TAG)) {
            event.setCancelled(true);
            String id = getNPCIdFromEntity(entity);
            if (id != null) {
                handleNPCClick(event.getPlayer(), id);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (event.getRightClicked().getScoreboardTags().contains(NPC_TAG)) {
            event.setCancelled(true);
            String id = getNPCIdFromEntity(event.getRightClicked());
            if (id != null) {
                handleNPCClick(event.getPlayer(), id);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity().getScoreboardTags().contains(NPC_TAG) || event.getEntity().getScoreboardTags().contains(HOLO_TAG)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (entity.getScoreboardTags().contains(NPC_TAG) || entity.getScoreboardTags().contains(HOLO_TAG)) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player) {
                String id = getNPCIdFromEntity(entity);
                if (id != null) {
                    handleNPCClick(player, id);
                }
            }
        }
    }
}
