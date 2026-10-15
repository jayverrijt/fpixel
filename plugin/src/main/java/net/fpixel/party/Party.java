package net.fpixel.party;

import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class Party {
    private UUID leader;
    private final Set<UUID> members = new HashSet<>();

    public Party(Player leader) {
        this.leader = leader.getUniqueId();
        this.members.add(leader.getUniqueId());
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public boolean isLeader(Player player) {
        return leader.equals(player.getUniqueId());
    }

    public boolean isMember(Player player) {
        return members.contains(player.getUniqueId());
    }

    public void addMember(Player player) {
        members.add(player.getUniqueId());
    }

    public void removeMember(Player player) {
        members.remove(player.getUniqueId());
    }

    public void broadcast(String message) {
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                ChatUtil.sendRawMessage(p, "§9Party §8» §f" + message);
            }
        }
    }

    public void chat(Player sender, String message) {
        broadcast("§e" + sender.getName() + "§8: §f" + message);
    }

    public List<Player> getOnlineMembers() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) list.add(p);
        }
        return list;
    }

    public void warpToGame(Arena arena) {
        for (Player member : getOnlineMembers()) {
            if (!arena.contains(member)) {
                arena.join(member);
                if (!member.getUniqueId().equals(leader)) {
                    ChatUtil.sendMessage(member, "§aYou were warped into §e" + arena.getDisplayName() + " §aby your party leader!");
                }
            }
        }
    }
}
