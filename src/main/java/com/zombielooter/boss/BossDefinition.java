package com.zombielooter.boss;

import cn.nukkit.level.Location;

public class BossDefinition {
    public final String id, name, entityType, world;
    public final double health, x, y, z;
    public BossDefinition(String id, String name, String entityType, double health, String world, double x, double y, double z){
        this.id=id; this.name=name; this.entityType=entityType; this.health=health; this.world=world; this.x=x; this.y=y; this.z=z;
    }
    public Location toLocation(cn.nukkit.Server server){
        cn.nukkit.level.Level lvl = server.getLevelByName(world);
        if (lvl == null) return null;
        return new Location(x, y, z, lvl);
    }
}
