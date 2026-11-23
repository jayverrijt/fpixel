package net.fpixel.games.parkour;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ParkourTestSession {
    private final UUID playerUuid;
    private final CustomParkourMap map;
    private final Location previousLocation;
    private final GameMode previousGameMode;
    private final long startTime;

    private int currentCheckpoint = 0;
    private boolean completed = false;

    public ParkourTestSession(Player player, CustomParkourMap map) {
        this.playerUuid = player.getUniqueId();
        this.map = map;
        this.previousLocation = player.getLocation().clone();
        this.previousGameMode = player.getGameMode();
        this.startTime = System.currentTimeMillis();
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public CustomParkourMap getMap() { return map; }
    public Location getPreviousLocation() { return previousLocation; }
    public GameMode getPreviousGameMode() { return previousGameMode; }
    public long getStartTime() { return startTime; }

    public int getCurrentCheckpoint() { return currentCheckpoint; }
    public void setCurrentCheckpoint(int currentCheckpoint) { this.currentCheckpoint = currentCheckpoint; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public double getElapsedSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }
}
