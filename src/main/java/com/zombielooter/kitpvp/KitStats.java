package com.zombielooter.kitpvp;

/**
 * Tracks persistent KitPvP stats for a single player.
 */
public class KitStats {
    private int kills;
    private int deaths;

    public void addKill() {
        kills++;
    }

    public void addDeath() {
        deaths++;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public double getKDR() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
}
