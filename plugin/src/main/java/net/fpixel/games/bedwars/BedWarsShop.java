package net.fpixel.games.bedwars;

import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class BedWarsShop {
    public static final String SHOP_TITLE = "§8BedWars Item Shop";

    public static class ShopItem {
        private final ItemStack displayItem;
        private final ItemStack rewardItem;
        private final Material currency;
        private final int cost;

        public ShopItem(ItemStack displayItem, ItemStack rewardItem, Material currency, int cost) {
            this.displayItem = displayItem;
            this.rewardItem = rewardItem;
            this.currency = currency;
            this.cost = cost;
        }

        public ItemStack getDisplayItem() { return displayItem; }
        public ItemStack getRewardItem() { return rewardItem; }
        public Material getCurrency() { return currency; }
        public int getCost() { return cost; }
    }

    private static final Map<Integer, ShopItem> ITEMS = new HashMap<>();

    static {
        // Blocks
        addItem(10, new ItemBuilder(Material.WHITE_WOOL, 16).name("§fWool (16x)").lore("§7Cost: §f4 Iron Ingots", "", "§eClick to purchase!").build(),
                new ItemStack(Material.WHITE_WOOL, 16), Material.IRON_INGOT, 4);

        addItem(11, new ItemBuilder(Material.TERRACOTTA, 12).name("§6Terracotta (12x)").lore("§7Cost: §f12 Iron Ingots", "", "§eClick to purchase!").build(),
                new ItemStack(Material.TERRACOTTA, 12), Material.IRON_INGOT, 12);

        addItem(12, new ItemBuilder(Material.END_STONE, 4).name("§eEnd Stone (4x)").lore("§7Cost: §f24 Iron Ingots", "", "§eClick to purchase!").build(),
                new ItemStack(Material.END_STONE, 4), Material.IRON_INGOT, 24);

        addItem(13, new ItemBuilder(Material.OAK_PLANKS, 4).name("§6Wood Planks (4x)").lore("§7Cost: §64 Gold Ingots", "", "§eClick to purchase!").build(),
                new ItemStack(Material.OAK_PLANKS, 4), Material.GOLD_INGOT, 4);

        addItem(14, new ItemBuilder(Material.OBSIDIAN, 2).name("§5Obsidian (2x)").lore("§7Cost: §a4 Emeralds", "", "§eClick to purchase!").build(),
                new ItemStack(Material.OBSIDIAN, 2), Material.EMERALD, 4);

        // Weapons
        addItem(19, new ItemBuilder(Material.STONE_SWORD).name("§7Stone Sword").lore("§7Cost: §f10 Iron Ingots", "", "§eClick to purchase!").build(),
                new ItemStack(Material.STONE_SWORD), Material.IRON_INGOT, 10);

        addItem(20, new ItemBuilder(Material.IRON_SWORD).name("§fIron Sword").lore("§7Cost: §67 Gold Ingots", "", "§eClick to purchase!").build(),
                new ItemStack(Material.IRON_SWORD), Material.GOLD_INGOT, 7);

        addItem(21, new ItemBuilder(Material.DIAMOND_SWORD).name("§bDiamond Sword").lore("§7Cost: §a4 Emeralds", "", "§eClick to purchase!").build(),
                new ItemStack(Material.DIAMOND_SWORD), Material.EMERALD, 4);

        addItem(22, new ItemBuilder(Material.STICK).name("§dKnockback Stick").addEnchant(Enchantment.KNOCKBACK, 1).lore("§7Cost: §65 Gold Ingots", "", "§eClick to purchase!").build(),
                new ItemBuilder(Material.STICK).name("§dKnockback Stick").addEnchant(Enchantment.KNOCKBACK, 1).build(), Material.GOLD_INGOT, 5);

        // Armor
        addItem(28, new ItemBuilder(Material.CHAINMAIL_BOOTS).name("§7Chainmail Armor").lore("§7Permanent boots & leggings.", "§7Cost: §f40 Iron Ingots", "", "§eClick to purchase!").build(),
                new ItemStack(Material.CHAINMAIL_BOOTS), Material.IRON_INGOT, 40);

        addItem(29, new ItemBuilder(Material.IRON_BOOTS).name("§fIron Armor").lore("§7Permanent boots & leggings.", "§7Cost: §612 Gold Ingots", "", "§eClick to purchase!").build(),
                new ItemStack(Material.IRON_BOOTS), Material.GOLD_INGOT, 12);

        addItem(30, new ItemBuilder(Material.DIAMOND_BOOTS).name("§bDiamond Armor").lore("§7Permanent boots & leggings.", "§7Cost: §a6 Emeralds", "", "§eClick to purchase!").build(),
                new ItemStack(Material.DIAMOND_BOOTS), Material.EMERALD, 6);

        // Tools
        addItem(31, new ItemBuilder(Material.SHEARS).name("§fPermanent Shears").lore("§7Never lose shears on death.", "§7Cost: §f20 Iron Ingots", "", "§eClick to purchase!").build(),
                new ItemStack(Material.SHEARS), Material.IRON_INGOT, 20);

        addItem(32, new ItemBuilder(Material.IRON_PICKAXE).name("§fIron Pickaxe").lore("§7Cost: §63 Gold Ingots", "", "§eClick to purchase!").build(),
                new ItemStack(Material.IRON_PICKAXE), Material.GOLD_INGOT, 3);

        // Specials
        addItem(37, new ItemBuilder(Material.GOLDEN_APPLE).name("§6Golden Apple").lore("§7Cost: §63 Gold Ingots", "", "§eClick to purchase!").build(),
                new ItemStack(Material.GOLDEN_APPLE), Material.GOLD_INGOT, 3);

        addItem(38, new ItemBuilder(Material.FIRE_CHARGE).name("§cFireball").lore("§7Right click to launch!", "§7Cost: §f40 Iron Ingots", "", "§eClick to purchase!").build(),
                new ItemBuilder(Material.FIRE_CHARGE).name("§cFireball").build(), Material.IRON_INGOT, 40);

        addItem(39, new ItemBuilder(Material.TNT).name("§cInstant TNT").lore("§7Auto-primes when placed!", "§7Cost: §64 Gold Ingots", "", "§eClick to purchase!").build(),
                new ItemStack(Material.TNT), Material.GOLD_INGOT, 4);

        addItem(40, new ItemBuilder(Material.ENDER_PEARL).name("§3Ender Pearl").lore("§7Cost: §a4 Emeralds", "", "§eClick to purchase!").build(),
                new ItemStack(Material.ENDER_PEARL), Material.EMERALD, 4);
    }

    private static void addItem(int slot, ItemStack display, ItemStack reward, Material currency, int cost) {
        ITEMS.put(slot, new ShopItem(display, reward, currency, cost));
    }

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatUtil.component(SHOP_TITLE));

        // Glass borders
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        inv.setItem(9, filler);
        inv.setItem(18, filler);
        inv.setItem(27, filler);
        inv.setItem(36, filler);
        inv.setItem(17, filler);
        inv.setItem(26, filler);
        inv.setItem(35, filler);
        inv.setItem(44, filler);

        for (Map.Entry<Integer, ShopItem> entry : ITEMS.entrySet()) {
            inv.setItem(entry.getKey(), entry.getValue().getDisplayItem());
        }

        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
    }

    public static void handleClick(Player player, int slot) {
        ShopItem item = ITEMS.get(slot);
        if (item == null) return;

        int playerCurrency = countMaterial(player, item.getCurrency());
        if (playerCurrency < item.getCost()) {
            ChatUtil.sendMessage(player, "§cYou don't have enough " + item.getCurrency().name().replace("_", " ").toLowerCase() + "s!");
            ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
            return;
        }

        // Deduct
        removeMaterial(player, item.getCurrency(), item.getCost());

        // Handle Armor vs normal item
        if (item.getRewardItem().getType() == Material.CHAINMAIL_BOOTS) {
            player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
        } else if (item.getRewardItem().getType() == Material.IRON_BOOTS) {
            player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
        } else if (item.getRewardItem().getType() == Material.DIAMOND_BOOTS) {
            player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        } else {
            player.getInventory().addItem(item.getRewardItem().clone());
        }

        ChatUtil.sendMessage(player, "§aPurchased §f" + item.getRewardItem().getType().name().replace("_", " ").toLowerCase() + "§a!");
        ChatUtil.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f);
    }

    private static int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private static void removeMaterial(Player player, Material material, int amount) {
        int left = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= left) {
                    left -= item.getAmount();
                    item.setAmount(0);
                } else {
                    item.setAmount(item.getAmount() - left);
                    left = 0;
                }
                if (left <= 0) break;
            }
        }
        player.updateInventory();
    }
}
