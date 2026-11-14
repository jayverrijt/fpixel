package net.fpixel.worlds;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerWorld {
    private String uid;
    private String name;
    private UUID creatorUuid;
    private String creatorName;
    private long createdAt;
    private boolean cheatsEnabled;
    private long seed;
    private String difficulty; // EASY, NORMAL, HARD, PEACEFUL
    private String defaultGameMode; // SURVIVAL, CREATIVE, ADVENTURE
    private boolean isPrimarySmp;
    private Map<String, Boolean> sharedPlayers = new HashMap<>(); // username/UUID -> canCheat
    private boolean locked;

    public PlayerWorld() {
    }

    public PlayerWorld(String uid, String name, UUID creatorUuid, String creatorName, long seed, boolean cheatsEnabled, boolean isPrimarySmp) {
        this(uid, name, creatorUuid, creatorName, seed, cheatsEnabled, "SURVIVAL", isPrimarySmp);
    }

    public PlayerWorld(String uid, String name, UUID creatorUuid, String creatorName, long seed, boolean cheatsEnabled, String defaultGameMode, boolean isPrimarySmp) {
        this.uid = uid;
        this.name = name;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.createdAt = System.currentTimeMillis();
        this.seed = seed;
        this.cheatsEnabled = cheatsEnabled;
        this.defaultGameMode = defaultGameMode != null && !defaultGameMode.isBlank() ? defaultGameMode.toUpperCase() : "SURVIVAL";
        this.difficulty = "HARDCORE".equalsIgnoreCase(this.defaultGameMode) ? "HARD" : "NORMAL";
        this.isPrimarySmp = isPrimarySmp;
        this.locked = false;
    }

    public boolean canAccess(Player player) {
        if (player.hasPermission("fpixel.admin") || player.isOp()) return true;
        if (creatorUuid != null && creatorUuid.equals(player.getUniqueId())) return true;
        String pName = player.getName().toLowerCase();
        String pNameClean = pName.startsWith(".") ? pName.substring(1) : pName;
        if (creatorName != null) {
            String cName = creatorName.toLowerCase();
            String cNameClean = cName.startsWith(".") ? cName.substring(1) : cName;
            if (cName.equals(pName) || cNameClean.equals(pNameClean)) return true;
        }
        if (sharedPlayers.containsKey(pName) || sharedPlayers.containsKey(pNameClean) ||
            sharedPlayers.containsKey(player.getUniqueId().toString())) {
            return true;
        }
        return false;
    }

    public boolean canCheat(Player player) {
        if (!cheatsEnabled) return false;
        if (player.hasPermission("fpixel.admin") || player.isOp()) return true;
        if (creatorUuid != null && creatorUuid.equals(player.getUniqueId())) return true;
        String pName = player.getName().toLowerCase();
        String pNameClean = pName.startsWith(".") ? pName.substring(1) : pName;
        if (creatorName != null) {
            String cName = creatorName.toLowerCase();
            String cNameClean = cName.startsWith(".") ? cName.substring(1) : cName;
            if (cName.equals(pName) || cNameClean.equals(pNameClean)) return true;
        }
        Boolean b = sharedPlayers.get(pName);
        if (b == null) b = sharedPlayers.get(pNameClean);
        if (b == null) b = sharedPlayers.get(player.getUniqueId().toString());
        return b != null && b;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getCreatorUuid() { return creatorUuid; }
    public void setCreatorUuid(UUID creatorUuid) { this.creatorUuid = creatorUuid; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isCheatsEnabled() { return cheatsEnabled; }
    public void setCheatsEnabled(boolean cheatsEnabled) { this.cheatsEnabled = cheatsEnabled; }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getDefaultGameMode() { return defaultGameMode; }
    public void setDefaultGameMode(String defaultGameMode) { this.defaultGameMode = defaultGameMode; }

    public boolean isPrimarySmp() { return isPrimarySmp; }
    public void setPrimarySmp(boolean primarySmp) { isPrimarySmp = primarySmp; }

    public Map<String, Boolean> getSharedPlayers() { return sharedPlayers; }
    public void setSharedPlayers(Map<String, Boolean> sharedPlayers) { this.sharedPlayers = sharedPlayers; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
}
