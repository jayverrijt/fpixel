package net.fpixel.duels;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class DuelManager {
    private final FPixelPlugin plugin;

    public static class DuelInvite {
        private final UUID sender;
        private final UUID target;
        private final String gameType;
        private final long timestamp;

        public DuelInvite(UUID sender, UUID target, String gameType) {
            this.sender = sender;
            this.target = target;
            this.gameType = gameType;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60_000; // 60s
        }

        public UUID getSender() { return sender; }
        public UUID getTarget() { return target; }
        public String getGameType() { return gameType; }
    }

    // Target UUID -> (Sender UUID -> DuelInvite)
    private final Map<UUID, Map<UUID, DuelInvite>> pendingInvites = new HashMap<>();

    public DuelManager(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendChallenge(Player sender, Player target, String gameType) {
        if (sender.equals(target)) {
            ChatUtil.sendMessage(sender, "§cYou cannot duel yourself!");
            return;
        }

        Arena senderArena = plugin.getGameManager().getPlayerArena(sender);
        Arena targetArena = plugin.getGameManager().getPlayerArena(target);
        boolean senderInSurvival = plugin.getSurvivalManager() != null && plugin.getSurvivalManager().isSurvivalWorld(sender.getWorld());
        boolean targetInSurvival = plugin.getSurvivalManager() != null && plugin.getSurvivalManager().isSurvivalWorld(target.getWorld());
        if (senderArena != null || targetArena != null || senderInSurvival || targetInSurvival) {
            ChatUtil.sendMessage(sender, "§cBoth players must be in the Lobby to duel.");
            return;
        }

        pendingInvites.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                .put(sender.getUniqueId(), new DuelInvite(sender.getUniqueId(), target.getUniqueId(), gameType));

        ChatUtil.sendMessage(sender, "§aSent a §e" + gameType + " Duel §achallenge to §e" + target.getName() + "§a!");
        ChatUtil.playSound(sender, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);

        // Clickable interactive invitation in target's chat
        Component invite = Component.text("§6§lDUELS §8» §e" + sender.getName() + " §7challenged you to a §6" + gameType + " 1v1§7!\n §7Click to respond: ")
                .append(Component.text("§a§l[ACCEPT]")
                        .clickEvent(ClickEvent.runCommand("/duel accept " + sender.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("§aClick to accept duel!"))))
                .append(Component.text("  "))
                .append(Component.text("§c§l[DECLINE]")
                        .clickEvent(ClickEvent.runCommand("/duel decline " + sender.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("§cClick to decline duel."))));

        target.sendMessage(invite);
        ChatUtil.playSound(target, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
    }

    public void acceptChallenge(Player target, String senderName) {
        Player sender = Bukkit.getPlayer(senderName);
        if (sender == null || !sender.isOnline()) {
            ChatUtil.sendMessage(target, "§cThat player is no longer online.");
            return;
        }

        Map<UUID, DuelInvite> invites = pendingInvites.get(target.getUniqueId());
        if (invites == null || !invites.containsKey(sender.getUniqueId())) {
            ChatUtil.sendMessage(target, "§cYou do not have a pending duel invite from " + senderName + ".");
            return;
        }

        DuelInvite invite = invites.remove(sender.getUniqueId());
        if (invite.isExpired()) {
            ChatUtil.sendMessage(target, "§cThat duel invite has expired!");
            return;
        }

        // Find available 1v1 duel arena
        Arena duelArena = null;
        for (Arena arena : plugin.getGameManager().getArenas()) {
            if (arena.getMaxPlayers() == 2
                    && arena.getGameType().equalsIgnoreCase(invite.getGameType())
                    && arena.getState() == GameState.WAITING
                    && arena.getPlayerCount() == 0) {
                duelArena = arena;
                break;
            }
        }

        if (duelArena == null) {
            ChatUtil.sendMessage(target, "§cNo empty arena available for " + invite.getGameType() + " right now!");
            ChatUtil.sendMessage(sender, "§cNo empty arena available for " + invite.getGameType() + " right now!");
            return;
        }

        ChatUtil.broadcast("§6§lDUELS §8» §e" + sender.getName() + " §7and §e" + target.getName() + " §7started a §6" + invite.getGameType() + " Duel§7!");

        duelArena.join(sender);
        duelArena.join(target);
        duelArena.forceStart();
    }

    public void declineChallenge(Player target, String senderName) {
        Player sender = Bukkit.getPlayer(senderName);
        if (sender != null) {
            Map<UUID, DuelInvite> invites = pendingInvites.get(target.getUniqueId());
            if (invites != null) {
                invites.remove(sender.getUniqueId());
            }
            ChatUtil.sendMessage(sender, "§c" + target.getName() + " declined your duel challenge.");
        }
        ChatUtil.sendMessage(target, "§7Declined duel challenge.");
    }
}
