package net.fpixel.core.scoreboard;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ScoreboardManager {
    private final FPixelPlugin plugin;
    private final Map<UUID, FastScoreboard> scoreboards = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
    private int titleStep = 0;
    private final String[] animatedTitles = {
            "§6§lFPIXEL",
            "§e§lFPIXEL",
            "§f§lFPIXEL",
            "§e§lFPIXEL",
            "§6§lFPIXEL"
    };

    public ScoreboardManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    private void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            titleStep = (titleStep + 1) % animatedTitles.length;
            String title = animatedTitles[titleStep];

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().getName().equalsIgnoreCase("survival") ||
                        (plugin.getSurvivalManager() != null && plugin.getSurvivalManager().isPlayerInSurvival(player))) {
                    remove(player);
                    continue;
                }

                FastScoreboard board = scoreboards.computeIfAbsent(player.getUniqueId(), id -> new FastScoreboard(player));
                board.updateTitle(title);
                updateLines(player, board);
            }
        }, 20L, 20L); // Update every 1 second
    }

    private void updateLines(Player player, FastScoreboard board) {
        Arena arena = plugin.getGameManager().getPlayerArena(player);
        if (arena == null) {
            // Lobby scoreboard
            FPixelUser user = plugin.getUserManager().getUser(player);
            Rank rank = plugin.getRankManager().getRank(player);

            List<String> lines = new ArrayList<>();
            lines.add("§7" + dateFormat.format(new Date()) + " §8m102A");
            lines.add("");
            lines.add("§fRank: " + rank.getColor() + rank.name());
            lines.add("§fCoins: §6" + String.format("%,d", user.getCoins()));
            lines.add("§fLevel: §b" + user.getLevel());
            lines.add(" ");
            lines.add("§fServer: §aLobby-1");
            lines.add("§fOnline: §a" + Bukkit.getOnlinePlayers().size());
            lines.add("  ");
            lines.add("§emc.fpixel.nl");

            board.updateLines(lines);
        } else {
            // Game scoreboard delegates to the active arena
            List<String> lines = arena.getScoreboardLines(player);
            board.updateLines(lines);
        }
    }

    public void remove(Player player) {
        FastScoreboard board = scoreboards.remove(player.getUniqueId());
        if (board != null) {
            board.cleanup();
        }
        if (player.isOnline()) {
            org.bukkit.scoreboard.ScoreboardManager sm = Bukkit.getScoreboardManager();
            if (sm != null && player.getScoreboard() != sm.getMainScoreboard()) {
                player.setScoreboard(sm.getMainScoreboard());
            }
        }
    }

    public static class FastScoreboard {
        private final Player player;
        private final Scoreboard scoreboard;
        private final Objective objective;
        private final Map<Integer, Team> lineTeams = new HashMap<>();
        private final Map<Integer, String> entryNames = new HashMap<>();

        public FastScoreboard(Player player) {
            this.player = player;
            org.bukkit.scoreboard.ScoreboardManager sm = Bukkit.getScoreboardManager();
            this.scoreboard = sm != null ? sm.getNewScoreboard() : null;
            if (this.scoreboard != null) {
                this.objective = scoreboard.registerNewObjective("fpixel_sb", Criteria.DUMMY, ChatUtil.component("§6§lFPIXEL"));
                this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                player.setScoreboard(scoreboard);
                if (FPixelPlugin.getInstance().getTablistManager() != null) {
                    FPixelPlugin.getInstance().getTablistManager().updateAllPlayerTeams();
                }
            } else {
                this.objective = null;
            }
        }

        public void updateTitle(String title) {
            if (objective != null) {
                objective.displayName(ChatUtil.component(title));
            }
        }

        public void updateLines(List<String> lines) {
            if (objective == null || scoreboard == null) return;

            int score = lines.size();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int lineIndex = score - i;
                Team team = lineTeams.computeIfAbsent(lineIndex, idx -> {
                    String entry = ChatColor.values()[idx % 16].toString() + ChatColor.COLOR_CHAR + (char) ('a' + idx);
                    entryNames.put(idx, entry);
                    Team t = scoreboard.registerNewTeam("line_" + idx);
                    t.addEntry(entry);
                    objective.getScore(entry).setScore(idx);
                    return t;
                });

                team.prefix(ChatUtil.component(line));
            }

            // Remove extra lines if list shrank
            for (int idx = lines.size() + 1; idx <= 15; idx++) {
                if (lineTeams.containsKey(idx)) {
                    String entry = entryNames.remove(idx);
                    if (entry != null) {
                        scoreboard.resetScores(entry);
                    }
                    Team t = lineTeams.remove(idx);
                    if (t != null) {
                        t.unregister();
                    }
                }
            }
        }

        public void cleanup() {
            if (scoreboard != null) {
                for (Team t : lineTeams.values()) {
                    try { t.unregister(); } catch (Exception ignored) {}
                }
                lineTeams.clear();
                entryNames.clear();
            }
        }
    }
}
