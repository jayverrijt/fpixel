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

    public static final String UPGRADES_TITLE = "§8BedWars Team Upgrades";

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

        var arena = net.fpixel.FPixelPlugin.getInstance().getGameManager().getPlayerArena(player);
        BedWarsTeam team = (arena instanceof BedWarsGame bwGame) ? bwGame.getPlayerTeam(player) : null;
        if (team != null) {
            inv.setItem(10, new ItemBuilder(team.getWoolMaterial(), 16)
                    .name(team.getChatColor() + team.getName() + " Wool (16x)")
                    .lore("§7Cost: §f4 Iron Ingots", "", "§eClick to purchase!")
                    .build());
        }

        // Slot 49: Team Upgrades button
        ItemStack upgradesBtn = new ItemBuilder(Material.DIAMOND)
                .name("§b§lTeam Upgrades §7(Diamonds)")
                .lore(
                        "§7Click to open Team Upgrades!",
                        "§7Upgrade swords, protection, haste,",
                        "§7heal pool, forge speed & traps.",
                        "",
                        "§e▶ Click to view Team Upgrades!"
                )
                .build();
        inv.setItem(49, upgradesBtn);

        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
    }

    public static void openUpgrades(Player player) {
        var arena = net.fpixel.FPixelPlugin.getInstance().getGameManager().getPlayerArena(player);
        if (arena instanceof BedWarsGame bwGame) {
            BedWarsTeam team = bwGame.getPlayerTeam(player);
            if (team != null) {
                openTeamUpgrades(player, team);
            }
        }
    }

    public static void openTeamUpgrades(Player player, BedWarsTeam team) {
        Inventory inv = Bukkit.createInventory(null, 36, ChatUtil.component(UPGRADES_TITLE));

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) inv.setItem(i, filler);

        // 1. Sharpened Swords (Slot 10)
        boolean hasSharp = team.getSharpness() > 0;
        ItemStack sharpItem = new ItemBuilder(Material.IRON_SWORD)
                .name(hasSharp ? "§a§lSharpened Swords §7(Unlocked)" : "§b§lSharpened Swords")
                .lore(
                        "§7Grants Sharpness I to all team swords.",
                        "",
                        hasSharp ? "§aALREADY PURCHASED" : "§7Cost: §b4 Diamonds",
                        hasSharp ? "" : "§eClick to purchase!"
                )
                .build();
        inv.setItem(10, sharpItem);

        // 2. Reinforced Armor (Slot 12)
        int prot = team.getProtection();
        int[] protCosts = {2, 4, 8, 16};
        String protCostStr = prot >= 4 ? "§aMAXED" : "§7Cost: §b" + protCosts[prot] + " Diamonds";
        ItemStack protItem = new ItemBuilder(Material.IRON_CHESTPLATE)
                .name("§b§lReinforced Armor §e" + (prot > 0 ? ("Tier " + prot) : ""))
                .lore(
                        "§7Grants Protection " + (prot + 1) + " to all armor.",
                        "",
                        protCostStr,
                        prot < 4 ? "§eClick to purchase!" : "§aMAX TIER REACHED"
                )
                .build();
        inv.setItem(12, protItem);

        // 3. Manic Miner (Slot 14)
        int haste = team.getHaste();
        int[] hasteCosts = {2, 4};
        String hasteCostStr = haste >= 2 ? "§aMAXED" : "§7Cost: §b" + hasteCosts[haste] + " Diamonds";
        ItemStack hasteItem = new ItemBuilder(Material.GOLDEN_PICKAXE)
                .name("§b§lManic Miner §e" + (haste > 0 ? ("Tier " + haste) : ""))
                .lore(
                        "§7Grants permanent Haste to all teammates.",
                        "",
                        hasteCostStr,
                        haste < 2 ? "§eClick to purchase!" : "§aMAX TIER REACHED"
                )
                .build();
        inv.setItem(14, hasteItem);

        // 4. Iron / Molten Forge (Slot 16)
        int forge = team.getForgeTier();
        int[] forgeCosts = {2, 4, 6};
        String forgeCostStr = forge >= 3 ? "§aMAXED" : "§7Cost: §b" + forgeCosts[forge] + " Diamonds";
        ItemStack forgeItem = new ItemBuilder(Material.FURNACE)
                .name("§b§lIron Forge §e" + (forge > 0 ? ("Tier " + forge) : ""))
                .lore(
                        "§7Increases resource generator speed at base.",
                        "§7Tier 3 also spawns emeralds at base!",
                        "",
                        forgeCostStr,
                        forge < 3 ? "§eClick to purchase!" : "§aMAX TIER REACHED"
                )
                .build();
        inv.setItem(16, forgeItem);

        // 5. Heal Pool (Slot 20)
        boolean hasHeal = team.hasHealPool();
        ItemStack healItem = new ItemBuilder(Material.BEACON)
                .name(hasHeal ? "§a§lHeal Pool §7(Unlocked)" : "§b§lHeal Pool")
                .lore(
                        "§7Grants Regeneration I around your base island.",
                        "",
                        hasHeal ? "§aALREADY PURCHASED" : "§7Cost: §b1 Diamond",
                        hasHeal ? "" : "§eClick to purchase!"
                )
                .build();
        inv.setItem(20, healItem);

        // 6. It's a Trap! (Slot 22)
        boolean hasTrap = team.hasTrap();
        ItemStack trapItem = new ItemBuilder(Material.TRIPWIRE_HOOK)
                .name(hasTrap ? "§a§lIt's a Trap! §7(Active)" : "§b§lIt's a Trap!")
                .lore(
                        "§7Inflicts Blindness & Slowness on enemies",
                        "§7who step onto your team's base island.",
                        "",
                        hasTrap ? "§aALREADY PURCHASED" : "§7Cost: §b1 Diamond",
                        hasTrap ? "" : "§eClick to purchase!"
                )
                .build();
        inv.setItem(22, trapItem);

        // Back button (Slot 31)
        ItemStack back = new ItemBuilder(Material.ARROW)
                .name("§c§lBack to Item Shop")
                .lore("§7Click to return to the item shop.")
                .build();
        inv.setItem(31, back);

        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
    }

    public static void handleClick(Player player, int slot) {
        String title = player.getOpenInventory().getTitle();
        var arena = net.fpixel.FPixelPlugin.getInstance().getGameManager().getPlayerArena(player);
        if (!(arena instanceof BedWarsGame bwGame)) return;
        BedWarsTeam team = bwGame.getPlayerTeam(player);
        if (team == null) return;

        if (title.contains("BedWars Team Upgrades")) {
            handleUpgradeClick(player, bwGame, team, slot);
            return;
        }

        if (slot == 49) {
            openTeamUpgrades(player, team);
            return;
        }

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
            ItemStack boots = new ItemStack(Material.CHAINMAIL_BOOTS);
            ItemStack legs = new ItemStack(Material.CHAINMAIL_LEGGINGS);
            if (team.getProtection() > 0) {
                boots.addEnchantment(Enchantment.PROTECTION, team.getProtection());
                legs.addEnchantment(Enchantment.PROTECTION, team.getProtection());
            }
            player.getInventory().setLeggings(legs);
            player.getInventory().setBoots(boots);
        } else if (item.getRewardItem().getType() == Material.IRON_BOOTS) {
            ItemStack boots = new ItemStack(Material.IRON_BOOTS);
            ItemStack legs = new ItemStack(Material.IRON_LEGGINGS);
            if (team.getProtection() > 0) {
                boots.addEnchantment(Enchantment.PROTECTION, team.getProtection());
                legs.addEnchantment(Enchantment.PROTECTION, team.getProtection());
            }
            player.getInventory().setLeggings(legs);
            player.getInventory().setBoots(boots);
        } else if (item.getRewardItem().getType() == Material.DIAMOND_BOOTS) {
            ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
            ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
            if (team.getProtection() > 0) {
                boots.addEnchantment(Enchantment.PROTECTION, team.getProtection());
                legs.addEnchantment(Enchantment.PROTECTION, team.getProtection());
            }
            player.getInventory().setLeggings(legs);
            player.getInventory().setBoots(boots);
        } else {
            ItemStack reward = item.getRewardItem().clone();
            if (reward.getType() == Material.WHITE_WOOL && team != null) {
                reward.setType(team.getWoolMaterial());
            }
            if (reward.getType().name().contains("SWORD") && team.getSharpness() > 0) {
                reward.addEnchantment(Enchantment.SHARPNESS, team.getSharpness());
            }
            player.getInventory().addItem(reward);
        }

        ChatUtil.sendMessage(player, "§aPurchased §f" + item.getRewardItem().getType().name().replace("_", " ").toLowerCase() + "§a!");
        ChatUtil.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f);
    }

    private static void handleUpgradeClick(Player player, BedWarsGame game, BedWarsTeam team, int slot) {
        if (slot == 31) {
            open(player);
            return;
        }

        int diamonds = countMaterial(player, Material.DIAMOND);

        if (slot == 10) {
            // Sharpened Swords (4 Diamonds)
            if (team.getSharpness() > 0) {
                ChatUtil.sendMessage(player, "§cYour team already has Sharpened Swords!");
                return;
            }
            if (diamonds < 4) {
                ChatUtil.sendMessage(player, "§cYou need 4 Diamonds to unlock Sharpened Swords!");
                return;
            }
            removeMaterial(player, Material.DIAMOND, 4);
            team.setSharpness(1);
            applyTeamSharpness(game, team);
            game.broadcast(team.getChatColor() + player.getName() + " §7unlocked §bSharpened Swords §7for the team!");
            openTeamUpgrades(player, team);
        } else if (slot == 12) {
            // Reinforced Armor (Tier 1-4: 2, 4, 8, 16 Diamonds)
            int current = team.getProtection();
            if (current >= 4) {
                ChatUtil.sendMessage(player, "§cArmor protection is already maxed!");
                return;
            }
            int[] costs = {2, 4, 8, 16};
            int cost = costs[current];
            if (diamonds < cost) {
                ChatUtil.sendMessage(player, "§cYou need " + cost + " Diamonds for Reinforced Armor Tier " + (current + 1) + "!");
                return;
            }
            removeMaterial(player, Material.DIAMOND, cost);
            team.setProtection(current + 1);
            applyTeamProtection(game, team);
            game.broadcast(team.getChatColor() + player.getName() + " §7upgraded §bReinforced Armor §7to Tier " + (current + 1) + "!");
            openTeamUpgrades(player, team);
        } else if (slot == 14) {
            // Manic Miner (Tier 1-2: 2, 4 Diamonds)
            int current = team.getHaste();
            if (current >= 2) {
                ChatUtil.sendMessage(player, "§cManic Miner is already maxed!");
                return;
            }
            int[] costs = {2, 4};
            int cost = costs[current];
            if (diamonds < cost) {
                ChatUtil.sendMessage(player, "§cYou need " + cost + " Diamonds for Manic Miner Tier " + (current + 1) + "!");
                return;
            }
            removeMaterial(player, Material.DIAMOND, cost);
            team.setHaste(current + 1);
            game.broadcast(team.getChatColor() + player.getName() + " §7unlocked §bManic Miner Tier " + (current + 1) + "!");
            openTeamUpgrades(player, team);
        } else if (slot == 16) {
            // Forge upgrades (Tier 1-3: 2, 4, 6 Diamonds)
            int current = team.getForgeTier();
            if (current >= 3) {
                ChatUtil.sendMessage(player, "§cForge is already maxed!");
                return;
            }
            int[] costs = {2, 4, 6};
            int cost = costs[current];
            if (diamonds < cost) {
                ChatUtil.sendMessage(player, "§cYou need " + cost + " Diamonds for Forge Tier " + (current + 1) + "!");
                return;
            }
            removeMaterial(player, Material.DIAMOND, cost);
            team.setForgeTier(current + 1);
            game.broadcast(team.getChatColor() + player.getName() + " §7upgraded the §bTeam Forge §7to Tier " + (current + 1) + "!");
            openTeamUpgrades(player, team);
        } else if (slot == 20) {
            // Heal Pool (1 Diamond)
            if (team.hasHealPool()) {
                ChatUtil.sendMessage(player, "§cYour team already has Heal Pool!");
                return;
            }
            if (diamonds < 1) {
                ChatUtil.sendMessage(player, "§cYou need 1 Diamond for Heal Pool!");
                return;
            }
            removeMaterial(player, Material.DIAMOND, 1);
            team.setHealPool(true);
            game.broadcast(team.getChatColor() + player.getName() + " §7unlocked §bHeal Pool §7for the team!");
            openTeamUpgrades(player, team);
        } else if (slot == 22) {
            // It's a Trap! (1 Diamond)
            if (team.hasTrap()) {
                ChatUtil.sendMessage(player, "§cYour team already has a trap set!");
                return;
            }
            if (diamonds < 1) {
                ChatUtil.sendMessage(player, "§cYou need 1 Diamond for It's a Trap!");
                return;
            }
            removeMaterial(player, Material.DIAMOND, 1);
            team.setTrap(true);
            game.broadcast(team.getChatColor() + player.getName() + " §7purchased an §bAlarm Trap§7!");
            openTeamUpgrades(player, team);
        }
    }

    private static void applyTeamSharpness(BedWarsGame game, BedWarsTeam team) {
        for (org.bukkit.entity.Player p : game.getBukkitPlayers()) {
            if (team.hasMember(p.getUniqueId())) {
                for (ItemStack item : p.getInventory().getContents()) {
                    if (item != null && item.getType().name().contains("SWORD")) {
                        item.addEnchantment(Enchantment.SHARPNESS, team.getSharpness());
                    }
                }
            }
        }
    }

    private static void applyTeamProtection(BedWarsGame game, BedWarsTeam team) {
        for (org.bukkit.entity.Player p : game.getBukkitPlayers()) {
            if (team.hasMember(p.getUniqueId())) {
                for (ItemStack armor : p.getInventory().getArmorContents()) {
                    if (armor != null && armor.getType() != Material.AIR) {
                        armor.addEnchantment(Enchantment.PROTECTION, team.getProtection());
                    }
                }
            }
        }
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
