package net.fpixel.core.user;

import java.util.UUID;

public class FPixelUser {
    private final UUID uuid;
    private String name;
    private int coins;
    private int level;
    private int experience;

    // BedWars
    private int bedwarsWins;
    private int bedwarsLosses;
    private int bedwarsKills;
    private int bedwarsFinalKills;
    private int bedwarsDeaths;
    private int bedwarsBedsBroken;

    // TNT Run
    private int tntrunWins;
    private int tntrunLosses;

    // Sumo
    private int sumoWins;
    private int sumoLosses;
    private int sumoStreak;

    // Murder Mystery
    private int murderMysteryWins;

    // Prop Hunt
    private int propHuntWins;

    // Settings
    private String visibility = "ALL"; // ALL, VIPS, NONE
    private final java.util.Set<String> discoveredSecrets = new java.util.HashSet<>();

    public FPixelUser(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.coins = 100; // Starter coins
        this.level = 1;
        this.experience = 0;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCoins() { return coins; }
    public void setCoins(int amount) { this.coins = Math.max(0, amount); }
    public void addCoins(int amount) { this.coins += amount; }
    public void removeCoins(int amount) { this.coins = Math.max(0, this.coins - amount); }

    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public void addExperience(int amount) {
        this.experience += amount;
        if (this.experience >= level * 100) {
            this.experience -= level * 100;
            this.level++;
        }
    }

    public int getBedwarsWins() { return bedwarsWins; }
    public void addBedwarsWin() { this.bedwarsWins++; }
    public int getBedwarsLosses() { return bedwarsLosses; }
    public void addBedwarsLoss() { this.bedwarsLosses++; }
    public int getBedwarsKills() { return bedwarsKills; }
    public void addBedwarsKill() { this.bedwarsKills++; }
    public int getBedwarsFinalKills() { return bedwarsFinalKills; }
    public void addBedwarsFinalKill() { this.bedwarsFinalKills++; }
    public int getBedwarsDeaths() { return bedwarsDeaths; }
    public void addBedwarsDeath() { this.bedwarsDeaths++; }
    public int getBedwarsBedsBroken() { return bedwarsBedsBroken; }
    public void addBedwarsBedBroken() { this.bedwarsBedsBroken++; }

    public int getTntrunWins() { return tntrunWins; }
    public void addTntrunWin() { this.tntrunWins++; }
    public int getTntrunLosses() { return tntrunLosses; }
    public void addTntrunLoss() { this.tntrunLosses++; }

    public int getSumoWins() { return sumoWins; }
    public void addSumoWin() { this.sumoWins++; this.sumoStreak++; }
    public int getSumoLosses() { return sumoLosses; }
    public void addSumoLoss() { this.sumoLosses++; this.sumoStreak = 0; }
    public int getSumoStreak() { return sumoStreak; }

    public int getMurderMysteryWins() { return murderMysteryWins; }
    public void addMurderMysteryWin() { this.murderMysteryWins++; }

    public int getPropHuntWins() { return propHuntWins; }
    public void addPropHuntWin() { this.propHuntWins++; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public java.util.Set<String> getDiscoveredSecrets() { return discoveredSecrets; }
    public boolean hasDiscoveredSecret(String id) { return discoveredSecrets.contains(id); }
    public void addDiscoveredSecret(String id) { discoveredSecrets.add(id); }
    public int getDiscoveredSecretsCount() { return discoveredSecrets.size(); }
}
