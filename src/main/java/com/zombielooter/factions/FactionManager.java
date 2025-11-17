package com.zombielooter.factions;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.HUDManager;

import java.io.File;
import java.util.*;

public class FactionManager {

    private final ZombieLooterX plugin;
    private final Config factionsConfig;
    private final Map<String, Faction> factions = new HashMap<>();

    public FactionManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.factionsConfig = new Config(new File(plugin.getDataFolder(), "factions.yml"), Config.YAML);
        load();
    }

    private void load() {
        for (String name : factionsConfig.getKeys(false)) {
            try {
                UUID leader = UUID.fromString(factionsConfig.getString(name + ".leader"));
                Faction faction = new Faction(name, leader);

                List<String> memberUUIDs = factionsConfig.getStringList(name + ".members");
                for (String uuidStr : memberUUIDs) {
                    faction.addMember(UUID.fromString(uuidStr));
                }

                if (factionsConfig.exists(name + ".home")) {
                    String[] homeData = factionsConfig.getString(name + ".home").split(":");
                    faction.setHome(new Location(
                        Double.parseDouble(homeData[1]),
                        Double.parseDouble(homeData[2]),
                        Double.parseDouble(homeData[3]),
                        plugin.getServer().getLevelByName(homeData[0])
                    ));
                }
                faction.setPower(factionsConfig.getInt(name + ".power", 0));
                faction.setBankBalance(factionsConfig.getDouble(name + ".bank", 0)); // Load bank balance

                factions.put(name, faction);
            } catch (Exception e) {
                plugin.getLogger().error("Failed to load faction: " + name, e);
            }
        }
    }

    public void save() {
        for (Faction faction : factions.values()) {
            String name = faction.getName();
            factionsConfig.set(name + ".leader", faction.getLeader().toString());

            List<String> memberUUIDs = new ArrayList<>();
            for (UUID uuid : faction.getMembers()) {
                memberUUIDs.add(uuid.toString());
            }
            factionsConfig.set(name + ".members", memberUUIDs);

            if (faction.getHome() != null) {
                Location home = faction.getHome();
                factionsConfig.set(name + ".home", home.getLevel().getName() + ":" + home.getX() + ":" + home.getY() + ":" + home.getZ());
            }
            factionsConfig.set(name + ".power", faction.getPower());
            factionsConfig.set(name + ".bank", faction.getBankBalance()); // Save bank balance
        }
        factionsConfig.save();
    }

    public boolean createFaction(Player player, String name) {
        if (factions.containsKey(name)) {
            return false;
        }
        Faction faction = new Faction(name, player.getUniqueId());
        factions.put(name, faction);
        save();
        return true;
    }

    public Faction getFaction(String name) {
        return factions.get(name);
    }

    public Faction getFactionByPlayer(UUID uuid) {
        for (Faction faction : factions.values()) {
            if (faction.isMember(uuid)) {
                return faction;
            }
        }
        return null;
    }

    public void disband(String name) {
        Set<UUID> set = new HashSet<>(factions.get(name).getMembers());
        factions.remove(name);
        factionsConfig.remove(name);
        save();
        for (UUID uuid : set) {
            Optional<Player> p = plugin.getServer().getPlayer(uuid);
            if(p.isPresent()) HUDManager.refreshHud(plugin, p.orElse(null));;
        }
    }

    public List<Faction> getFactions() {
        return new ArrayList<>(factions.values());
    }
}
