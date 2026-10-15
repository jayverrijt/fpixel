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
                String header = "\n §6§lFPIXEL NETWORK §7[1.21] \n §bmc.fpixel.nl \n";
                String footer = "\n §eOnline: §a" + online + " §7| §ePing: §a" + ping + "ms \n §bmc.fpixel.nl \n";

                player.sendPlayerListHeaderAndFooter(
                        ChatUtil.component(header),
                        ChatUtil.component(footer)
                );
            }
            updateAllPlayerTeams();
        }, 40L, 40L); // Update every 2s
    }

    public void updatePlayerTeam(Player player) {
        updateAllPlayerTeams();
    }

    public void updateAllPlayerTeams() {
        java.util.Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) return;

        org.bukkit.scoreboard.ScoreboardManager sm = Bukkit.getScoreboardManager();
        Scoreboard mainBoard = sm != null ? sm.getMainScoreboard() : null;

        java.util.Set<Scoreboard> boards = new java.util.HashSet<>();
        if (mainBoard != null) boards.add(mainBoard);
        for (Player p : online) {
            Scoreboard b = p.getScoreboard();
            if (b != null) boards.add(b);
        }

        for (Scoreboard board : boards) {
            // Ensure rank teams exist on this scoreboard
            for (Rank r : Rank.values()) {
                String teamName = String.format("%02d_%s", 100 - r.getWeight(), r.name().toLowerCase());
                Team team = board.getTeam(teamName);
                if (team == null) {
                    try {
                        team = board.registerNewTeam(teamName);
                    } catch (Exception ignored) {
                        team = board.getTeam(teamName);
                    }
                }
                if (team != null) {
                    team.prefix(ChatUtil.component(r.getPrefix()));
                    try {
                        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
                    } catch (Throwable ignored) {}
                }
            }

            // Add every online player to their corresponding rank team on this board
            for (Player target : online) {
                Rank r = plugin.getRankManager().getRank(target);
                String teamName = String.format("%02d_%s", 100 - r.getWeight(), r.name().toLowerCase());
                Team targetTeam = board.getTeam(teamName);
                if (targetTeam != null) {
                    // Remove from any different rank team on this scoreboard
                    for (Team other : board.getTeams()) {
                        if (other.hasEntry(target.getName()) && !other.getName().equals(teamName)) {
                            other.removeEntry(target.getName());
                        }
                    }
                    if (!targetTeam.hasEntry(target.getName())) {
                        targetTeam.addEntry(target.getName());
                    }
                }
            }
        }

        // Also update player custom names for packet/display fallback
        for (Player target : online) {
            Rank r = plugin.getRankManager().getRank(target);
            target.customName(ChatUtil.component(r.getPrefix() + r.getColor() + target.getName()));
            target.setCustomNameVisible(true);
        }
    }
}
