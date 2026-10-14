package net.fpixel.core.rank;

public enum Rank {
    OWNER("§c[OWNER] ", "§c", 100, "fpixel.rank.owner"),
    ADMIN("§c[ADMIN] ", "§c", 90, "fpixel.rank.admin"),
    MOD("§2[MOD] ", "§2", 80, "fpixel.rank.mod"),
    YOUTUBE("§c[§fYOUTUBE§c] ", "§c", 75, "fpixel.rank.youtube"),
    MVP_PLUS("§b[MVP§c+§b] ", "§b", 70, "fpixel.rank.mvpplus"),
    MVP("§b[MVP] ", "§b", 60, "fpixel.rank.mvp"),
    VIP_PLUS("§a[VIP§6+§a] ", "§a", 50, "fpixel.rank.vipplus"),
    VIP("§a[VIP] ", "§a", 40, "fpixel.rank.vip"),
    MEMBER("§7", "§7", 0, "");

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
}
