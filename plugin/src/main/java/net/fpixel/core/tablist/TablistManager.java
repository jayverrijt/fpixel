package net.fpixel.core.tablist;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TablistManager {
    private final FPixelPlugin plugin;

    public TablistManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        startTablistTask();
    }

    private void startTablistTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int online = Bukkit.getOnlinePlayers().size();
            for (Player player : Bukkit.getOnlinePlayers()) {
                int ping = player.getPing();
                String header = "\n §6§lFPIXEL NETWORK §7[1.21] \n §7play.fpixel.net \n";
                String footer = "\n §eOnline: §a" + online + " §7| §ePing: §a" + ping + "ms \n §bStore: §fstore.fpixel.net \n";

                player.sendPlayerListHeaderAndFooter(
                        ChatUtil.component(header),
                        ChatUtil.component(footer)
                );

                updatePlayerTeam(player);
            }
        }, 40L, 40L); // Update every 2s
    }

    public void updatePlayerTeam(Player player) {
        Rank rank = plugin.getRankManager().getRank(player);
        Scoreboard board = player.getScoreboard();
        if (board == null) return;

        // Prefix team name with order weight so tab sorts correctly (01_OWNER, 02_ADMIN, etc.)
        String teamName = String.format("%02d_%s", 100 - rank.getWeight(), rank.name());
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        team.prefix(ChatUtil.component(rank.getPrefix()));
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }
}
