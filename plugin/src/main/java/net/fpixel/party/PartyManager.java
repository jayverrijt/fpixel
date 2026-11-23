package net.fpixel.party;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager {
    private final FPixelPlugin plugin;
    private final List<Party> parties = new ArrayList<>();
    // Target UUID -> (Leader UUID -> Timestamp)
    private final Map<UUID, Map<UUID, Long>> invites = new HashMap<>();

    public PartyManager(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    public Party getParty(Player player) {
        for (Party party : parties) {
            if (party.isMember(player)) {
                return party;
            }
        }
        return null;
    }

    public void invite(Player leader, Player target) {
        if (leader.equals(target)) {
            ChatUtil.sendMessage(leader, "§cYou cannot invite yourself to a party.");
            return;
        }

        Party targetParty = getParty(target);
        if (targetParty != null) {
            ChatUtil.sendMessage(leader, "§cThat player is already in a party.");
            return;
        }

        Party party = getParty(leader);
        if (party == null) {
            party = new Party(leader);
            parties.add(party);
            ChatUtil.sendMessage(leader, "§aCreated a new party!");
        } else if (!party.isLeader(leader)) {
            ChatUtil.sendMessage(leader, "§cOnly the party leader can invite players.");
            return;
        }

        invites.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>()).put(leader.getUniqueId(), System.currentTimeMillis());

        party.broadcast("§e" + leader.getName() + " §7invited §e" + target.getName() + " §7to the party.");
        ChatUtil.playSound(leader, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);

        // Clickable interactive invitation in target's chat
        Component invite = Component.text("§9§lPARTY §8» §e" + leader.getName() + " §7has invited you to their party!\n §7Click to respond: ")
                .append(Component.text("§a§l[JOIN]")
                        .clickEvent(ClickEvent.runCommand("/party accept " + leader.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("§aClick to join party!"))))
                .append(Component.text("  "))
                .append(Component.text("§c§l[IGNORE]")
                        .clickEvent(ClickEvent.runCommand("/party decline " + leader.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("§cClick to ignore invite."))));

        target.sendMessage(invite);
        ChatUtil.playSound(target, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
    }

    public void accept(Player target, String leaderName) {
        Player leader = Bukkit.getPlayer(leaderName);
        if (leader == null || !leader.isOnline()) {
            ChatUtil.sendMessage(target, "§cThat player is no longer online.");
            return;
        }

        Map<UUID, Long> playerInvites = invites.get(target.getUniqueId());
        if (playerInvites == null || !playerInvites.containsKey(leader.getUniqueId())) {
            ChatUtil.sendMessage(target, "§cYou do not have a pending party invite from " + leaderName + ".");
            return;
        }

        long time = playerInvites.remove(leader.getUniqueId());
        if (System.currentTimeMillis() - time > 300_000) { // 5 minutes
            ChatUtil.sendMessage(target, "§cThat party invite has expired.");
            return;
        }

        Party party = getParty(leader);
        if (party == null) {
            ChatUtil.sendMessage(target, "§cThat party no longer exists.");
            return;
        }

        Party existing = getParty(target);
        if (existing != null) {
            leave(target);
        }

        party.addMember(target);
        party.broadcast("§a" + target.getName() + " §7joined the party!");
        ChatUtil.playSound(target, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
    }

    public void decline(Player target, String leaderName) {
        Player leader = Bukkit.getPlayer(leaderName);
        if (leader != null) {
            Map<UUID, Long> playerInvites = invites.get(target.getUniqueId());
            if (playerInvites != null) {
                playerInvites.remove(leader.getUniqueId());
            }
            ChatUtil.sendMessage(leader, "§c" + target.getName() + " declined your party invite.");
        }
        ChatUtil.sendMessage(target, "§7Declined party invite.");
    }

    public void leave(Player player) {
        Party party = getParty(player);
        if (party == null) {
            ChatUtil.sendMessage(player, "§cYou are not in a party.");
            return;
        }

        if (party.isLeader(player)) {
            disband(player);
            return;
        }

        party.removeMember(player);
        party.broadcast("§c" + player.getName() + " §7left the party.");
        ChatUtil.sendMessage(player, "§7You left the party.");
    }

    public void kick(Player leader, String targetName) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader)) {
            ChatUtil.sendMessage(leader, "§cYou must be the party leader to kick members.");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !party.isMember(target)) {
            ChatUtil.sendMessage(leader, "§cThat player is not in your party.");
            return;
        }

        if (target.equals(leader)) {
            ChatUtil.sendMessage(leader, "§cYou cannot kick yourself! Use /party disband instead.");
            return;
        }

        party.removeMember(target);
        party.broadcast("§c" + target.getName() + " §7was kicked from the party.");
        ChatUtil.sendMessage(target, "§cYou were kicked from the party by " + leader.getName() + ".");
    }

    public void disband(Player leader) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader)) {
            ChatUtil.sendMessage(leader, "§cYou must be the party leader to disband the party.");
            return;
        }

        party.broadcast("§cThe party was disbanded by " + leader.getName() + ".");
        parties.remove(party);
    }

    public void warpParty(Player leader) {
        Party party = getParty(leader);
        if (party == null) {
            ChatUtil.sendMessage(leader, "§cYou are not in a party.");
            return;
        }
        if (!party.isLeader(leader)) {
            ChatUtil.sendMessage(leader, "§cOnly the party leader can warp the party!");
            return;
        }

        var arena = plugin.getGameManager().getPlayerArena(leader);
        if (arena != null) {
            party.warpToGame(arena);
            ChatUtil.sendMessage(leader, "§aWarped your party into the game!");
            return;
        }

        org.bukkit.Location dest = leader.getLocation();
        for (Player member : party.getOnlineMembers()) {
            if (!member.equals(leader)) {
                if (plugin.getSurvivalManager() != null && plugin.getSurvivalManager().isSurvivalWorld(dest.getWorld())) {
                    if (plugin.getSurvivalManager().getProfileManager() != null) {
                        plugin.getSurvivalManager().getProfileManager().loadProfile(member, dest.getWorld());
                    }
                    member.teleport(dest);
                } else if (plugin.getSurvivalManager() != null && plugin.getSurvivalManager().isSurvivalWorld(member.getWorld())) {
                    plugin.getSurvivalManager().getProfileManager().saveProfile(member);
                    plugin.getSurvivalManager().removeSurvivalPlayer(member);
                    member.teleport(dest);
                    plugin.getLobbyManager().giveLobbyItems(member);
                } else {
                    member.teleport(dest);
                }
                ChatUtil.sendMessage(member, "§aYou were warped to your party leader!");
            }
        }
        ChatUtil.sendMessage(leader, "§aWarped all party members to your location!");
    }
}
