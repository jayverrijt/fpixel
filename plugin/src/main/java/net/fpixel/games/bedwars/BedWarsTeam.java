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

    private org.bukkit.block.BlockFace bedFacing = org.bukkit.block.BlockFace.WEST;
    private int sharpness = 0;
    private int protection = 0;
    private int haste = 0;
    private int forgeTier = 0;
    private boolean healPool = false;
    private boolean trap = false;

    public org.bukkit.block.BlockFace getBedFacing() { return bedFacing; }
    public void setBedFacing(org.bukkit.block.BlockFace bedFacing) { this.bedFacing = bedFacing; }

    public int getSharpness() { return sharpness; }
    public void setSharpness(int sharpness) { this.sharpness = sharpness; }

    public int getProtection() { return protection; }
    public void setProtection(int protection) { this.protection = protection; }

    public int getHaste() { return haste; }
    public void setHaste(int haste) { this.haste = haste; }

    public int getForgeTier() { return forgeTier; }
    public void setForgeTier(int forgeTier) { this.forgeTier = forgeTier; }

    public boolean hasHealPool() { return healPool; }
    public void setHealPool(boolean healPool) { this.healPool = healPool; }

    public boolean hasTrap() { return trap; }
    public void setTrap(boolean trap) { this.trap = trap; }

    public void resetUpgrades() {
        this.sharpness = 0;
        this.protection = 0;
        this.haste = 0;
        this.forgeTier = 0;
        this.healPool = false;
        this.trap = false;
    }

    public String getStatusSymbol() {
        return bedAlive ? "§a✔" : (members.isEmpty() ? "§c✖" : "§e" + members.size());
    }
}
