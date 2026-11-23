package net.fpixel.game;

public enum GameState {
    WAITING("§aWaiting..."),
    STARTING("§eStarting..."),
    IN_GAME("§cIn Game"),
    ENDING("§6Ending"),
    RESETTING("§7Resetting");

    private final String display;

    GameState(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
