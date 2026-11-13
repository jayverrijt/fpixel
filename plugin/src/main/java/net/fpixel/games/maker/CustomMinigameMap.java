package net.fpixel.games.maker;

import java.util.*;

public class CustomMinigameMap {
    public enum Status {
        DRAFT,
        PENDING_REVIEW,
        APPROVED,
        REJECTED,
        ARCHIVED
    }

    public enum GameType {
        PARKOUR("Parkour Studio"),
        BEDWARS("BedWars"),
        SKYWARS("SkyWars"),
        TNTRUN("TNT Run"),
        MURDER_MYSTERY("Murder Mystery"),
        PROP_HUNT("Prop Hunt"),
        THE_BRIDGE("The Bridge 1v1"),
        SUMO("Sumo 1v1");

        private final String displayName;
        GameType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }

        public static GameType fromString(String str) {
            if (str == null) return PARKOUR;
            String clean = str.toUpperCase().replace("-", "_").replace(" ", "_");
            for (GameType gt : values()) {
                if (gt.name().equalsIgnoreCase(clean)) return gt;
            }
            if (clean.contains("PARKOUR")) return PARKOUR;
            if (clean.contains("BED")) return BEDWARS;
            if (clean.contains("SKY")) return SKYWARS;
            if (clean.contains("TNT")) return TNTRUN;
            if (clean.contains("MURDER")) return MURDER_MYSTERY;
            if (clean.contains("PROP")) return PROP_HUNT;
            if (clean.contains("BRIDGE")) return THE_BRIDGE;
            if (clean.contains("SUMO")) return SUMO;
            return PARKOUR;
        }
    }

    private String id;
    private String name;
    private GameType gameType;
    private UUID creatorUuid;
    private String creatorName;
    private int teamCount; // 2 to 8
    private Status status = Status.DRAFT;
    private String rejectionReason;
    private String worldName;
    private long createdAt;
    private long updatedAt;

    // Config points (stored as "world,x,y,z,yaw,pitch")
    private List<String> teamSpawns = new ArrayList<>();
    private List<String> teamBeds = new ArrayList<>();
    private List<String> teamGenerators = new ArrayList<>();
    private List<String> midChests = new ArrayList<>();
    private List<String> midGenerators = new ArrayList<>();
    private List<String> parkourCheckpoints = new ArrayList<>();
    private String spectatorSpawn;

    public CustomMinigameMap() {}

    public CustomMinigameMap(String id, String name, GameType gameType, UUID creatorUuid, String creatorName, int teamCount) {
        this.id = id;
        this.name = name;
        this.gameType = gameType;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.teamCount = Math.max(2, Math.min(8, teamCount));
        this.status = Status.DRAFT;
        this.worldName = "map_build_" + id.toLowerCase();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public GameType getGameType() { return gameType; }
    public void setGameType(GameType gameType) { this.gameType = gameType; }

    public UUID getCreatorUuid() { return creatorUuid; }
    public void setCreatorUuid(UUID creatorUuid) { this.creatorUuid = creatorUuid; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public int getTeamCount() { return teamCount; }
    public void setTeamCount(int teamCount) { this.teamCount = teamCount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public List<String> getTeamSpawns() { return teamSpawns; }
    public void setTeamSpawns(List<String> teamSpawns) { this.teamSpawns = teamSpawns; }

    public List<String> getTeamBeds() { return teamBeds; }
    public void setTeamBeds(List<String> teamBeds) { this.teamBeds = teamBeds; }

    public List<String> getTeamGenerators() { return teamGenerators; }
    public void setTeamGenerators(List<String> teamGenerators) { this.teamGenerators = teamGenerators; }

    public List<String> getMidChests() { return midChests; }
    public void setMidChests(List<String> midChests) { this.midChests = midChests; }

    public List<String> getMidGenerators() { return midGenerators; }
    public void setMidGenerators(List<String> midGenerators) { this.midGenerators = midGenerators; }

    public List<String> getParkourCheckpoints() { return parkourCheckpoints; }
    public void setParkourCheckpoints(List<String> parkourCheckpoints) { this.parkourCheckpoints = parkourCheckpoints; }

    public String getSpectatorSpawn() { return spectatorSpawn; }
    public void setSpectatorSpawn(String spectatorSpawn) { this.spectatorSpawn = spectatorSpawn; }
}
