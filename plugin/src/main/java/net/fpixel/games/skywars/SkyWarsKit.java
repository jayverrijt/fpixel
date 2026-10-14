package net.fpixel.games.skywars;

import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public enum SkyWarsKit {
    KNIGHT(
            "Knight",
            "§a§lKnight",
            Material.IRON_SWORD,
            List.of("§7Spawn with an Iron Sword and", "§7Golden Helmet (Protection I).", "", "§e▶ Click to select Knight kit!")
    ) {
        @Override
        public void apply(Player player) {
            player.getInventory().addItem(new ItemBuilder(Material.IRON_SWORD).name("§aKnight's Sword").build());
            player.getInventory().setHelmet(new ItemBuilder(Material.GOLDEN_HELMET).name("§aKnight's Helmet").addEnchant(Enchantment.PROTECTION, 1).build());
        }
    },

    ARCHER(
            "Archer",
            "§b§lArcher",
            Material.BOW,
            List.of("§7Spawn with a Bow (Power I),", "§716 Arrows and a Leather Cap.", "", "§e▶ Click to select Archer kit!")
    ) {
        @Override
        public void apply(Player player) {
            player.getInventory().addItem(new ItemBuilder(Material.BOW).name("§bArcher's Bow").addEnchant(Enchantment.POWER, 1).build());
            player.getInventory().addItem(new ItemStack(Material.ARROW, 16));
            player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        }
    },

    ARMORER(
            "Armorer",
            "§9§lArmorer",
            Material.IRON_CHESTPLATE,
            List.of("§7Spawn with an Iron Chestplate (Prot I)", "§7and Iron Boots.", "", "§e▶ Click to select Armorer kit!")
    ) {
        @Override
        public void apply(Player player) {
            player.getInventory().setChestplate(new ItemBuilder(Material.IRON_CHESTPLATE).name("§9Reinforced Chestplate").addEnchant(Enchantment.PROTECTION, 1).build());
            player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
        }
    },

    SCOUT(
            "Scout",
            "§e§lScout",
            Material.FEATHER,
            List.of("§7Spawn with 3x Splash Potions of", "§7Speed II for quick escapes.", "", "§e▶ Click to select Scout kit!")
    ) {
        @Override
        public void apply(Player player) {
            ItemStack speedPot = new ItemStack(Material.SPLASH_POTION, 3);
            if (speedPot.getItemMeta() instanceof PotionMeta pm) {
                pm.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 15, 1), true);
                pm.displayName(ChatUtil.component("§eSpeed II Splash Potion (15s)"));
                speedPot.setItemMeta(pm);
            }
            player.getInventory().addItem(speedPot);
        }
    },

    BUILDER(
            "Builder",
            "§6§lBuilder",
            Material.BRICKS,
            List.of("§7Spawn with 64x Stone Bricks and", "§764x Oak Planks to bridge anywhere.", "", "§e▶ Click to select Builder kit!")
    ) {
        @Override
        public void apply(Player player) {
            player.getInventory().addItem(new ItemStack(Material.STONE_BRICKS, 64));
            player.getInventory().addItem(new ItemStack(Material.OAK_PLANKS, 64));
        }
    },

    HEALER(
            "Healer",
            "§d§lHealer",
            Material.GOLDEN_APPLE,
            List.of("§7Spawn with 3x Splash Health Potions", "§7and a Golden Apple.", "", "§e▶ Click to select Healer kit!")
    ) {
        @Override
        public void apply(Player player) {
            ItemStack healPot = new ItemStack(Material.SPLASH_POTION, 3);
            if (healPot.getItemMeta() instanceof PotionMeta pm) {
                pm.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1), true);
                pm.displayName(ChatUtil.component("§dSplash Potion of Healing II"));
                healPot.setItemMeta(pm);
            }
            player.getInventory().addItem(healPot);
            player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
        }
    },

    CANNONEER(
            "Cannoneer",
            "§c§lCannoneer",
            Material.TNT,
            List.of("§7Spawn with 16x TNT, 4x Redstone,", "§7Water Bucket and Blast Boots (Prot IV).", "", "§e▶ Click to select Cannoneer kit!")
    ) {
        @Override
        public void apply(Player player) {
            player.getInventory().addItem(new ItemStack(Material.TNT, 16));
            player.getInventory().addItem(new ItemStack(Material.REDSTONE_BLOCK, 4));
            player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
            player.getInventory().setBoots(new ItemBuilder(Material.IRON_BOOTS).name("§cBlast Boots").addEnchant(Enchantment.BLAST_PROTECTION, 4).build());
        }
    },

    PYRO(
            "Pyro",
            "§4§lPyro",
            Material.FLINT_AND_STEEL,
            List.of("§7Spawn with Flint & Steel, Lava Bucket", "§7and Fire Resistance.", "", "§e▶ Click to select Pyro kit!")
    ) {
        @Override
        public void apply(Player player) {
            player.getInventory().addItem(new ItemStack(Material.FLINT_AND_STEEL, 1));
            player.getInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
            player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
            ItemStack firePot = new ItemStack(Material.SPLASH_POTION, 1);
            if (firePot.getItemMeta() instanceof PotionMeta pm) {
                pm.addCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 30, 0), true);
                pm.displayName(ChatUtil.component("§6Fire Resistance Splash Potion (30s)"));
                firePot.setItemMeta(pm);
            }
            player.getInventory().addItem(firePot);
        }
    },

    FARMER(
            "Farmer",
            "§f§lFarmer",
            Material.EGG,
            List.of("§7Spawn with Iron Leggings, a Golden Apple", "§7and 64x Projectile Eggs for void knocking.", "", "§e▶ Click to select Farmer kit!")
    ) {
        @Override
        public void apply(Player player) {
            player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            player.getInventory().addItem(new ItemStack(Material.EGG, 64));
            player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
        }
    },

    FISHERMAN(
            "Fisherman",
            "§3§lFisherman",
            Material.FISHING_ROD,
            List.of("§7Spawn with a Knockback II Fishing Rod", "§7and 16x Cooked Salmon.", "", "§e▶ Click to select Fisherman kit!")
    ) {
        @Override
        public void apply(Player player) {
            player.getInventory().addItem(new ItemBuilder(Material.FISHING_ROD).name("§3Fisherman's Rod").addEnchant(Enchantment.KNOCKBACK, 2).addEnchant(Enchantment.UNBREAKING, 5).build());
            player.getInventory().addItem(new ItemStack(Material.COOKED_SALMON, 16));
        }
    };

    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<String> lore;

    SkyWarsKit(String id, String displayName, Material icon, List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.lore = lore;
    }

    public abstract void apply(Player player);

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public List<String> getLore() { return lore; }
}
