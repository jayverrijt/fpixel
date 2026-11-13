package net.fpixel.core.rank;

public enum Rank {
    OWNER("§6[👑] ", "§6", 100, "fpixel.rank.owner"),
    ADMIN("§c[⚔] ", "§c", 90, "fpixel.rank.admin"),
    MODERATOR("§a[🎩] ", "§a", 80, "fpixel.rank.moderator"),
    DONOR("§d[❤] ", "§d", 50, "fpixel.rank.donor"),
    MEMBER("", "§f", 10, "fpixel.rank.member");

    private final String prefix;
    private final String color;
    private final int weight;
    private final String permission;

    Rank(String prefix, String color, int weight, String permission) {
        this.prefix = prefix;
        this.color = color;
        this.weight = weight;
        this.permission = permission;
    }

    public String getPrefix() { return prefix; }
    public String getColor() { return color; }
    public int getWeight() { return weight; }
    public String getPermission() { return permission; }

    public static Rank fromString(String raw) {
        if (raw == null) return MEMBER;
        String clean = raw.trim().toUpperCase();
        switch (clean) {
            case "OWNER":
                return OWNER;
            case "ADMIN":
                return ADMIN;
            case "MODERATOR":
            case "MOD":
                return MODERATOR;
            case "DONOR":
            case "VIP":
            case "VIP_PLUS":
            case "MVP":
            case "MVP_PLUS":
            case "YOUTUBE":
                return DONOR;
            case "MEMBER":
            default:
                return MEMBER;
        }
    }
}
