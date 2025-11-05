package com.zombielooter.factions;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.*;

public class FactionManager {
    private final ZombieLooterX plugin;
    private final Map<String, Faction> factions = new HashMap<>();
    private Config config;

    public FactionManager(ZombieLooterX plugin){
        this.plugin = plugin;
        load();
    }

    @SuppressWarnings("unchecked")
    public void load(){
        File f = new File(plugin.getDataFolder(), "factions.yml");
        if (!f.exists()) plugin.saveResource("factions.yml", false);
        config = new Config(f, Config.YAML);
        factions.clear();

        Map<String,Object> all = config.getAll();
        for (String key : all.keySet()){
            Object raw = all.get(key);
            if (!(raw instanceof Map)) continue;
            Map<String,Object> map = (Map<String,Object>) raw;
            UUID leader = UUID.fromString(String.valueOf(map.get("leader")));
            Faction fac = new Faction(key, leader);

            // members
            List<String> members = (List<String>) map.get("members");
            if (members != null) for (String id : members) fac.addMember(UUID.fromString(id));

            // NEW: Load pending invites
            List<String> invites = (List<String>) map.get("invites");
            if (invites != null) {
                for (String id : invites) {
                    try {
                        fac.invite(UUID.fromString(id));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid invite UUID in faction " + key + ": " + id);
                    }
                }
            }

            // home
            Map<String,Object> home = (Map<String,Object>) map.get("home");
            if (home != null) {
                String level = String.valueOf(home.get("level"));
                double x = toDouble(home.get("x")), y = toDouble(home.get("y")), z = toDouble(home.get("z"));
                float yaw = (float) toDouble(home.getOrDefault("yaw", 0));
                float pitch = (float) toDouble(home.getOrDefault("pitch", 0));
                if (plugin.getServer().getLevelByName(level) != null) {
                    fac.setHome(new Location(x, y, z, yaw, pitch, plugin.getServer().getLevelByName(level)));
                }
            }

            // power
            int power = ((Number) map.getOrDefault("power", 10)).intValue();
            fac.setPower(power);

            factions.put(key.toLowerCase(), fac);
        }
        plugin.getLogger().info("Loaded "+factions.size()+" factions.");
    }

    public void save(){
    try {
        Map<String,Object> out = new LinkedHashMap<>();
        for (Faction f : factions.values()){
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("leader", f.getLeader().toString());
            
            List<String> memberList = new ArrayList<>();
            f.getMembers().forEach(u -> memberList.add(u.toString()));
            m.put("members", memberList);
            
            // NEW: Save pending invites
            List<String> inviteList = new ArrayList<>();
            f.getPendingInvites().forEach(u -> inviteList.add(u.toString()));
            if (!inviteList.isEmpty()) {
                m.put("invites", inviteList);
            }
            
            if (f.getHome() != null) {
                Location h = f.getHome();
                Map<String,Object> hm = new LinkedHashMap<>();
                hm.put("level", h.getLevel().getFolderName());
                hm.put("x", h.getX()); hm.put("y", h.getY()); hm.put("z", h.getZ());
                hm.put("yaw", h.getYaw()); hm.put("pitch", h.getPitch());
                m.put("home", hm);
            }
            m.put("power", f.getPower());
            out.put(f.getName(), m);
        }
        config.setAll((LinkedHashMap<String, Object>) out);
        config.save();
    } catch (Exception e) {
        plugin.getLogger().error("Failed to save factions!", e);
    }
}

    public boolean createFaction(Player leader, String name){
        String key = name.toLowerCase();
        if (factions.containsKey(key)) return false;
        Faction f = new Faction(name, leader.getUniqueId());
        factions.put(key, f);
        save();
        return true;
    }

    public Faction getFaction(String name){ return factions.get(name.toLowerCase()); }

    public Faction getFactionByPlayer(UUID id){
        for (Faction f : factions.values()) if (f.isMember(id)) return f;
        return null;
    }

    public void disband(String name){
        factions.remove(name.toLowerCase());
        save();
    }

    public void addMember(String name, Player p){
        Faction f = getFaction(name);
        if (f != null){ f.addMember(p.getUniqueId()); save(); }
    }

    public Collection<Faction> getFactions(){ return factions.values(); }

    private double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return Double.parseDouble(String.valueOf(o));
    }
}
