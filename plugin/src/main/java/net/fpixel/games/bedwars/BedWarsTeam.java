package net.fpixel.games.bedwars;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BedWarsTeam {
    private final String name;
    private final ChatColor chatColor;
    private final Color leatherColor;
    private final Material woolMaterial;
    private final Material bedMaterial;
    private Location spawnLocation;
    private Location bedLocation;
    private boolean bedAlive = true;
    private final Set<UUID> members = new HashSet<>();

    public BedWarsTeam(String name, ChatColor chatColor, Color leatherColor, Material woolMaterial, Material bedMaterial) {
        this.name = name;
        this.chatColor = chatColor;
        this.leatherColor = leatherColor;
        this.woolMaterial = woolMaterial;
        this.bedMaterial = bedMaterial;
    }

    public String getName() { return name; }
    public ChatColor getChatColor() { return chatColor; }
    public Color getLeatherColor() { return leatherColor; }
    public Material getWoolMaterial() { return woolMaterial; }
    public Material getBedMaterial() { return bedMaterial; }

    public Location getSpawnLocation() { return spawnLocation; }
    public void setSpawnLocation(Location spawnLocation) { this.spawnLocation = spawnLocation; }

    public Location getBedLocation() { return bedLocation; }
    public void setBedLocation(Location bedLocation) { this.bedLocation = bedLocation; }

    public boolean isBedAlive() { return bedAlive; }
    public void setBedAlive(boolean bedAlive) { this.bedAlive = bedAlive; }

    public Set<UUID> getMembers() { return members; }
    public void addMember(UUID uuid) { members.add(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    public boolean hasMember(UUID uuid) { return members.contains(uuid); }

    public String getStatusSymbol() {
        return bedAlive ? "§a✔" : (members.isEmpty() ? "§c✖" : "§e" + members.size());
    }
}
