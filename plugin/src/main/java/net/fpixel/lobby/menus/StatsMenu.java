package net.fpixel.lobby.menus;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class StatsMenu {
    public static final String TITLE = "§8My Profile & Stats";

    public static void open(FPixelPlugin plugin, Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, ChatUtil.component(TITLE));
        FPixelUser user = plugin.getUserManager().getUser(player);
        Rank rank = plugin.getRankManager().getRank(player);

        // Fillers
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        // Player Head
        ItemStack head = new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(player.getName())
                .name("§a§l" + player.getName())
                .lore(
                        "§7Rank: " + rank.getColor() + rank.name(),
                        "§7Coins: §6" + String.format("%,d", user.getCoins()),
                        "§7Network Level: §b" + user.getLevel(),
                        "§7Experience: §e" + user.getExperience() + "§7/§e" + (user.getLevel() * 100)
                )
                .build();
        inv.setItem(13, head);

        // BedWars Stats
        ItemStack bedwarsStats = new ItemBuilder(Material.RED_BED)
                .name("§c§lBedWars Stats")
                .lore(
                        "§7Wins: §a" + user.getBedwarsWins(),
                        "§7Losses: §c" + user.getBedwarsLosses(),
                        "§7Kills: §e" + user.getBedwarsKills(),
                        "§7Final Kills: §6" + user.getBedwarsFinalKills(),
                        "§7Beds Broken: §b" + user.getBedwarsBedsBroken()
                )
                .build();
        inv.setItem(20, bedwarsStats);

        // TNT Run Stats
        ItemStack tntStats = new ItemBuilder(Material.TNT)
                .name("§c§lTNT Run Stats")
                .lore(
                        "§7Wins: §a" + user.getTntrunWins(),
                        "§7Losses: §c" + user.getTntrunLosses()
                )
                .build();
        inv.setItem(22, tntStats);

        // Sumo Stats
        ItemStack sumoStats = new ItemBuilder(Material.STICK)
                .name("§6§lSumo Duels Stats")
                .lore(
                        "§7Wins: §a" + user.getSumoWins(),
                        "§7Losses: §c" + user.getSumoLosses(),
                        "§7Current Streak: §b" + user.getSumoStreak()
                )
                .build();
        inv.setItem(24, sumoStats);

        // Murder Mystery Stats
        ItemStack mmStats = new ItemBuilder(Material.IRON_SWORD)
                .name("§c§lMurder Mystery Stats")
                .lore(
                        "§7Wins: §a" + user.getMurderMysteryWins(),
                        "§7Survive as Innocent/Detective or Slay as Murderer!"
                )
                .build();
        inv.setItem(29, mmStats);

        // Prop Hunt Stats
        ItemStack phStats = new ItemBuilder(Material.CRAFTING_TABLE)
                .name("§6§lProp Hunt Stats")
                .lore(
                        "§7Wins: §a" + user.getPropHuntWins(),
                        "§7Fool hunters as Props or Seek them as Hunters!"
                )
                .build();
        inv.setItem(33, phStats);

        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }
}
