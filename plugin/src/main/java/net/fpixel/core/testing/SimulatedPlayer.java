package net.fpixel.core.testing;

import net.fpixel.core.util.ItemBuilder;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SimulatedPlayer {
    private final UUID uuid;
    private final String name;
    private LivingEntity entity;
    private Location spawnLocation;

    public SimulatedPlayer(String name) {
        this.uuid = UUID.randomUUID();
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void removeEntity() {
        if (entity != null) {
            if (entity.isValid()) {
                entity.remove();
            }
            entity = null;
        }
    }

    public LivingEntity spawnEntity(Location loc, String gameType) {
        if (loc == null || loc.getWorld() == null) return null;
        removeEntity();

        this.spawnLocation = loc.clone();
        Zombie zombie = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
        zombie.setAdult();
        zombie.setSilent(true);
        zombie.setCustomName(name);
        zombie.setCustomNameVisible(true);
        zombie.setRemoveWhenFarAway(false);
        zombie.setShouldBurnInDay(false);
        zombie.setTarget(null);
        zombie.addScoreboardTag("fpixel_bot");
        zombie.addScoreboardTag("bot_" + uuid.toString());

        // Zero movement speed so bot stands in place
        Attribute speedAttrType = resolveAttribute("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED");
        if (speedAttrType != null) {
            var speedAttr = zombie.getAttribute(speedAttrType);
            if (speedAttr != null) speedAttr.setBaseValue(0.0);
        }

        // Zero attack damage so bot never hurts player
        Attribute dmgAttrType = resolveAttribute("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE");
        if (dmgAttrType != null) {
            var dmgAttr = zombie.getAttribute(dmgAttrType);
            if (dmgAttr != null) dmgAttr.setBaseValue(0.0);
        }

        // Equip the bot with gear appropriate for the minigame
        var eq = zombie.getEquipment();
        if (eq != null) {
            eq.setHelmet(new ItemBuilder(Material.PLAYER_HEAD).build());
            switch (gameType.toUpperCase()) {
                case "SUMO" -> {
                    eq.setChestplate(new ItemBuilder(Material.LEATHER_CHESTPLATE).build());
                    Attribute maxHealthType = resolveAttribute("GENERIC_MAX_HEALTH", "MAX_HEALTH");
                    if (maxHealthType != null) {
                        var maxHealth = zombie.getAttribute(maxHealthType);
                        if (maxHealth != null) maxHealth.setBaseValue(100.0);
                    }
                    zombie.setHealth(100.0);
                }
                case "PVP" -> {
                    eq.setChestplate(new ItemBuilder(Material.DIAMOND_CHESTPLATE).build());
                    eq.setLeggings(new ItemBuilder(Material.DIAMOND_LEGGINGS).build());
                    eq.setBoots(new ItemBuilder(Material.DIAMOND_BOOTS).build());
                    eq.setItemInMainHand(new ItemBuilder(Material.DIAMOND_SWORD).build());
                    eq.setItemInOffHand(new ItemBuilder(Material.SHIELD).build());
                    zombie.setHealth(20.0);
                }
                case "BEDWARS" -> {
                    eq.setChestplate(new ItemBuilder(Material.LEATHER_CHESTPLATE).leatherColor(Color.BLUE).build());
                    eq.setLeggings(new ItemBuilder(Material.LEATHER_LEGGINGS).leatherColor(Color.BLUE).build());
                    eq.setBoots(new ItemBuilder(Material.LEATHER_BOOTS).leatherColor(Color.BLUE).build());
                    eq.setItemInMainHand(new ItemBuilder(Material.WOODEN_SWORD).build());
                    zombie.setHealth(20.0);
                }
                case "BRIDGE" -> {
                    eq.setChestplate(new ItemBuilder(Material.CHAINMAIL_CHESTPLATE).build());
                    eq.setLeggings(new ItemBuilder(Material.IRON_LEGGINGS).build());
                    eq.setBoots(new ItemBuilder(Material.IRON_BOOTS).build());
                    eq.setItemInMainHand(new ItemBuilder(Material.IRON_SWORD).build());
                    zombie.setHealth(20.0);
                }
                case "SKYWARS" -> {
                    eq.setChestplate(new ItemBuilder(Material.IRON_CHESTPLATE).build());
                    eq.setLeggings(new ItemBuilder(Material.CHAINMAIL_LEGGINGS).build());
                    eq.setBoots(new ItemBuilder(Material.IRON_BOOTS).build());
                    eq.setItemInMainHand(new ItemBuilder(Material.IRON_SWORD).build());
                    zombie.setHealth(20.0);
                }
                default -> {
                    eq.setChestplate(new ItemBuilder(Material.IRON_CHESTPLATE).build());
                    zombie.setHealth(20.0);
                }
            }
        }

        this.entity = zombie;
        return zombie;
    }

    private static Attribute resolveAttribute(String... names) {
        for (String name : names) {
            try {
                return Attribute.valueOf(name);
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }
}

