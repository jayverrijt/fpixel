package net.fpixel.games.pvp;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

public class PvPGame extends Arena {
    private Location spawn1;
    private Location spawn2;

    public PvPGame(FPixelPlugin plugin, String id, String displayName) {
        super(plugin, id, displayName, "PVP", 2, 2);
    }

    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }

    @Override
    public void onGameStart() {
        List<Player> bPlayers = getBukkitPlayers();
        if (bPlayers.size() >= 2) {
            bPlayers.get(0).teleport(spawn1);
            bPlayers.get(1).teleport(spawn2);
        }

        for (Player p : bPlayers) {
            p.setGameMode(GameMode.SURVIVAL);
            p.setHealth(20.0);
            p.setFoodLevel(20);
            giveDuelKit(p);
            ChatUtil.sendTitle(p, "§c§lPVP 1v1", "§eGood luck and fight hard!", 5, 25, 5);
            ChatUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.2f);
        }
    }

    private void giveDuelKit(Player p) {
        p.getInventory().clear();

        // Armor
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        helmet.addEnchantment(Enchantment.PROTECTION, 2);
        p.getInventory().setHelmet(helmet);

        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        chestplate.addEnchantment(Enchantment.PROTECTION, 2);
        p.getInventory().setChestplate(chestplate);

        ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
        leggings.addEnchantment(Enchantment.PROTECTION, 2);
        p.getInventory().setLeggings(leggings);

        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantment.PROTECTION, 2);
        p.getInventory().setBoots(boots);

        // Weapon
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 3);
        p.getInventory().setItem(0, sword);

        // Bow & Arrows
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.POWER, 2);
        p.getInventory().setItem(1, bow);
        p.getInventory().setItem(8, new ItemStack(Material.ARROW, 32));

        // Consumables
        p.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 12));
        p.getInventory().setItem(3, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
        p.getInventory().setItem(4, new ItemStack(Material.WATER_BUCKET));
        p.getInventory().setItem(5, new ItemStack(Material.COOKED_BEEF, 32));

        // Splash Healing Potions
        ItemStack healPot = new ItemStack(Material.SPLASH_POTION, 1);
        if (healPot.getItemMeta() instanceof PotionMeta meta) {
            meta.setBasePotionType(PotionType.STRONG_HEALING);
            healPot.setItemMeta(meta);
        }
        p.getInventory().setItem(6, healPot);
        p.getInventory().setItem(7, healPot.clone());
    }

    @Override
    public void handleGameTick() {
        // Void check
        for (Player p : getBukkitPlayers()) {
            if (p.getLocation().getY() < 70) {
                broadcast("§c" + p.getName() + " §7fell out of the arena.");
                eliminatePlayer(p);
            }
        }
    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        event.getDrops().clear();
        event.setCancelled(true);

        if (player.getKiller() != null) {
            broadcast("§c" + player.getName() + " §7was slain by §a" + player.getKiller().getName() + "§7!");
        } else {
            broadcast("§c" + player.getName() + " §7died.");
        }

        eliminatePlayer(player);
    }

    @Override
    public void checkWinCondition() {
        if (state != GameState.IN_GAME) return;
        List<Player> alive = getBukkitPlayers();
        if (alive.size() <= 1) {
            endGame(alive.isEmpty() ? null : alive.get(0));
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (state != GameState.IN_GAME) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onGameEnd(Player winner) {}

    @Override
    public void onArenaReset() {}

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§7" + id);
        lines.add(" ");
        if (state == GameState.WAITING || state == GameState.STARTING) {
            lines.add("§fPlayers: §a" + players.size() + "/" + maxPlayers);
            lines.add("§fStarting in: §a" + countdown + "s");
        } else if (state == GameState.IN_GAME) {
            Player opp = null;
            for (Player p : getBukkitPlayers()) {
                if (!p.equals(player)) {
                    opp = p;
                    break;
                }
            }
            lines.add("§fOpponent: §c" + (opp != null ? opp.getName() : "None"));
            if (opp != null) {
                lines.add("§fEnemy HP: §a" + String.format("%.1f", opp.getHealth()) + " §c❤");
            }
            lines.add("§fTime: §e" + (gameTime / 60) + ":" + String.format("%02d", gameTime % 60));
        } else {
            lines.add("§fGame: §eEnding");
        }
        lines.add(" ");
        lines.add("§emc.fpixel.net");
        return lines;
    }
}
