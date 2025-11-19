package com.zombielooter.factions;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.particle.RedstoneParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.GUITextManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import cn.nukkit.utils.TextFormat;

public class ClaimManager {

    private final ZombieLooterX plugin;
    private final GUITextManager text;
    private final Config claimsConfig;
    private final Map<String, String> chunkToFaction = new HashMap<>(); // chunk hash -> faction name

    public ClaimManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.text = plugin.getGUITextManager();
        this.claimsConfig = new Config(new File(plugin.getDataFolder(), "claims.yml"), Config.YAML);
        loadClaims();
    }

    private void loadClaims() {
        for (String factionName : claimsConfig.getKeys(false)) {
            for (String chunkHash : claimsConfig.getStringList(factionName)) {
                chunkToFaction.put(chunkHash, factionName);
            }
        }
    }

    public void save() {
        Map<String, List<String>> factionToChunks = new HashMap<>();
        for (Map.Entry<String, String> entry : chunkToFaction.entrySet()) {
            factionToChunks.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        claimsConfig.setAll(new LinkedHashMap<String, Object>(factionToChunks));
        claimsConfig.save();
    }

    public int getClaimCount(String factionName) {
        return (int) chunkToFaction.values().stream().filter(name -> name.equals(factionName)).count();
    }

    public void claimChunk(Faction faction, Player player) {
        if (faction == null) return;

        int claimsHeld = getClaimCount(faction.getName());
        int requiredPower = (claimsHeld + 1) * plugin.getPowerManager().getPowerPerClaim();
        if (faction.getPower() < requiredPower) {
            player.sendMessage(TextFormat.colorize('&', text.getText("commands.claim.no_power", "&cYour faction doesn't have enough power to claim more land!")));
            player.sendMessage(TextFormat.colorize('&', String.format(text.getText("commands.claim.required_power", "&cRequired: %d | Current: %d"), requiredPower, faction.getPower())));
            return;
        }

        String chunkHash = String.valueOf(Level.chunkHash(player.getChunkX(), player.getChunkZ()));
        if (chunkToFaction.containsKey(chunkHash)) {
            player.sendMessage(TextFormat.colorize('&', text.getText("commands.claim.already_claimed", "&cThis land is already claimed!")));
            return;
        }

        chunkToFaction.put(chunkHash, faction.getName());
        save();
        player.sendMessage(TextFormat.colorize('&', text.getText("commands.claim.claim_success", "&aLand claimed successfully!")));
    }

    public void forceClaimChunk(Faction faction, int chunkX, int chunkZ) {
        String chunkHash = String.valueOf(Level.chunkHash(chunkX, chunkZ));
        chunkToFaction.put(chunkHash, faction.getName());
        save();
    }

    public void unclaimChunk(Faction faction, Player player) {
        if (faction == null) return;
        String chunkHash = String.valueOf(Level.chunkHash(player.getChunkX(), player.getChunkZ()));
        if (!faction.getName().equals(chunkToFaction.get(chunkHash))) {
            player.sendMessage(TextFormat.colorize('&', text.getText("commands.claim.not_your_land", "&cYour faction doesn't own this land!")));
            return;
        }

        chunkToFaction.remove(chunkHash);
        save();
        player.sendMessage(TextFormat.colorize('&', text.getText("commands.claim.unclaim_success", "&aLand unclaimed successfully!")));
    }

    public Faction getFactionForChunk(int chunkX, int chunkZ) {
        String chunkHash = String.valueOf(Level.chunkHash(chunkX, chunkZ));
        String factionName = chunkToFaction.get(chunkHash);
        return factionName != null ? plugin.getFactionManager().getFaction(factionName) : null;
    }

    public void previewClaimChunk(Player player) {
        int chunkX = player.getChunkX();
        int chunkZ = player.getChunkZ();
        Level level = player.getLevel();

        for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) {
            for (int z = chunkZ * 16; z < chunkZ * 16 + 16; z++) {
                if (x % 2 == 0 || z % 2 == 0) { // Don't render every single block
                    int y = level.getHighestBlockAt(x, z) + 1;
                    level.addParticle(new RedstoneParticle(new Vector3(x, y, z)));
                }
            }
        }
        player.sendMessage(TextFormat.colorize('&', text.getText("commands.claim.preview", "&eDisplaying chunk border for your current location.")));
    }
}
