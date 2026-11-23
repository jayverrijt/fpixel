package net.fpixel.games.parkour;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomParkourMap {
    private String id;
    private String name;
    private String creatorName;
    private UUID creatorUuid;
    private long createdAt;
    private String worldName = "parkour_studio";
    private int slotIndex;

    private double startX;
    private double startY;
    private double startZ;
    private float startYaw;
    private float startPitch;

    private Double finishX;
    private Double finishY;
    private Double finishZ;

    private List<BlockPos> checkpoints = new ArrayList<>();
    private int voidY = 70;
    private String status = "PENDING_REVIEW"; // PENDING_REVIEW, APPROVED, REJECTED
    private String reviewer;
    private long reviewedAt;
    private String rejectReason;

    public CustomParkourMap() {}

    public CustomParkourMap(String id, String name, String creatorName, UUID creatorUuid, int slotIndex) {
        this.id = id;
        this.name = name;
        this.creatorName = creatorName;
        this.creatorUuid = creatorUuid;
        this.createdAt = System.currentTimeMillis();
        this.slotIndex = slotIndex;
        this.status = "PENDING_REVIEW";
    }

    public static class BlockPos {
        public double x;
        public double y;
        public double z;

        public BlockPos() {}

        public BlockPos(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public UUID getCreatorUuid() { return creatorUuid; }
    public void setCreatorUuid(UUID creatorUuid) { this.creatorUuid = creatorUuid; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public int getSlotIndex() { return slotIndex; }
    public void setSlotIndex(int slotIndex) { this.slotIndex = slotIndex; }

    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getStartZ() { return startZ; }
    public float getStartYaw() { return startYaw; }
    public float getStartPitch() { return startPitch; }

    public void setStart(double x, double y, double z, float yaw, float pitch) {
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.startYaw = yaw;
        this.startPitch = pitch;
    }

    public void setStart(Location loc) {
        if (loc != null) {
            setStart(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            if (loc.getWorld() != null) {
                this.worldName = loc.getWorld().getName();
            }
        }
    }

    public Location getStartLocation(World fallbackWorld) {
        World w = Bukkit.getWorld(worldName);
        if (w == null) w = fallbackWorld;
        if (w == null) return null;
        return new Location(w, startX, startY, startZ, startYaw, startPitch);
    }

    public Double getFinishX() { return finishX; }
    public Double getFinishY() { return finishY; }
    public Double getFinishZ() { return finishZ; }

    public void setFinish(double x, double y, double z) {
        this.finishX = x;
        this.finishY = y;
        this.finishZ = z;
    }

    public void setFinish(Location loc) {
        if (loc != null) {
            setFinish(loc.getX(), loc.getY(), loc.getZ());
        } else {
            this.finishX = null;
            this.finishY = null;
            this.finishZ = null;
        }
    }

    public Location getFinishLocation(World fallbackWorld) {
        if (finishX == null || finishY == null || finishZ == null) return null;
        World w = Bukkit.getWorld(worldName);
        if (w == null) w = fallbackWorld;
        if (w == null) return null;
        return new Location(w, finishX, finishY, finishZ);
    }

    public List<BlockPos> getCheckpoints() {
        if (checkpoints == null) checkpoints = new ArrayList<>();
        return checkpoints;
    }

    public void setCheckpoints(List<BlockPos> checkpoints) {
        this.checkpoints = checkpoints;
    }

    public void addCheckpoint(double x, double y, double z) {
        if (checkpoints == null) checkpoints = new ArrayList<>();
        checkpoints.add(new BlockPos(x, y, z));
    }

    public void addCheckpoint(Location loc) {
        if (loc != null) {
            addCheckpoint(loc.getX(), loc.getY(), loc.getZ());
        }
    }

    public boolean removeCheckpointNear(Location loc, double radius) {
        if (checkpoints == null || loc == null) return false;
        double rSq = radius * radius;
        return checkpoints.removeIf(cp -> {
            double dx = cp.x - loc.getX();
            double dy = cp.y - loc.getY();
            double dz = cp.z - loc.getZ();
            return (dx * dx + dy * dy + dz * dz) <= rSq;
        });
    }

    public List<Location> getCheckpointLocations(World fallbackWorld) {
        List<Location> locs = new ArrayList<>();
        World w = Bukkit.getWorld(worldName);
        if (w == null) w = fallbackWorld;
        if (w == null) return locs;

        for (BlockPos cp : getCheckpoints()) {
            locs.add(new Location(w, cp.x, cp.y, cp.z));
        }
        return locs;
    }

    public int getVoidY() { return voidY; }
    public void setVoidY(int voidY) { this.voidY = voidY; }

    public String getStatus() { return status != null ? status : "PENDING_REVIEW"; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }

    public long getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(long reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(status);
    }

    public boolean isDisabled() {
        return "DISABLED".equalsIgnoreCase(status) || "DEACTIVATED".equalsIgnoreCase(status);
    }
}
