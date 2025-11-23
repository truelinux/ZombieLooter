package com.zombielooter.npc;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.format.IChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.economy.Vendor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import cn.nukkit.utils.TextFormat;

public class NPCManager {

    private final ZombieLooterX plugin;
    private final Config npcConfig;
    // The only map we need: stores the configured location for each vendor ID.
    private final Map<String, Location> npcLocations = new ConcurrentHashMap<>();

    public NPCManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.npcConfig = new Config(new File(plugin.getDataFolder(), "npcs.yml"), Config.YAML);
        loadNpcLocations();
    }

    private void loadNpcLocations() {
        npcLocations.clear();
        for (String vendorId : npcConfig.getKeys(false)) {
            String levelName = npcConfig.getString(vendorId + ".level");
            Level level = plugin.getServer().getLevelByName(levelName);
            if (level == null) {
                plugin.getLogger().warning("Level '" + levelName + "' for NPC '" + vendorId + "' not found. Skipping.");
                continue;
            }
            Location loc = new Location(
                npcConfig.getDouble(vendorId + ".x"),
                npcConfig.getDouble(vendorId + ".y"),
                npcConfig.getDouble(vendorId + ".z"),
                npcConfig.getDouble(vendorId + ".yaw"),
                npcConfig.getDouble(vendorId + ".pitch"),
                level
            );
            npcLocations.put(vendorId, loc);
        }
        plugin.getLogger().info("&aLoaded " + npcLocations.size() + " persistent NPC locations.");
    }

    public void onChunkLoad(IChunk chunk) {
        for (Map.Entry<String, Location> entry : npcLocations.entrySet()) {
            Location loc = entry.getValue();
            boolean isNpcInChunk = (loc.getFloorX() >> 4) == chunk.getX() && (loc.getFloorZ() >> 4) == chunk.getZ() && Objects.equals(loc.getLevel().getName(), chunk.getProvider().getLevel().getName());

            if (isNpcInChunk) {
                // To prevent duplicates, despawn any existing instance before spawning a new one.
                // This is a safeguard against corrupted state from server crashes.
                despawnNpc(entry.getKey(), chunk.getProvider().getLevel());
                spawnNpc(entry.getKey(), loc);
            }
        }
    }

    public void onChunkUnload(IChunk chunk) {
        // Simple and direct: close any VendorNPC entities within the unloading chunk.
        for (Entity entity : chunk.getEntities().values()) {
            if (entity instanceof VendorNPC) {
                entity.close();
            }
        }
    }

    private void spawnNpc(String vendorId, Location location) {
        Vendor vendor = plugin.getVendorManager().getVendor(vendorId);
        if (vendor == null) return;

        CompoundTag nbt = Entity.getDefaultNBT(location);
        VendorNPC npc = new VendorNPC(location.getChunk(), nbt);

        Skin skin = new Skin();
        try {
            File skinFile = new File(plugin.getDataFolder(), "skins/" + vendor.getSkin() + ".png");
            if (skinFile.exists()) {
                BufferedImage skinImage = ImageIO.read(skinFile);
                if (skinImage != null) {
                    skin.setSkinData(skinImage);
                    skin.setSkinId(vendor.getSkin());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().error("Error loading skin for vendor '" + vendorId + "'.", e);
        }

        npc.setSkin(skin);
        npc.setNameTag(TextFormat.colorize(vendor.getName()));
        npc.setNameTagVisible(true);
        npc.setNameTagAlwaysVisible(true);
        npc.spawnToAll();
    }

    public void createAndSaveNPC(Player player, String vendorId) {
        Vendor vendor = plugin.getVendorManager().getVendor(vendorId);
        if (vendor == null) {
            player.sendMessage(TextFormat.colorize('&', "&cVendor ID '" + vendorId + "' not found in vendors.yml."));
            return;
        }

        // Despawn any old instance of this vendor ID, wherever it might be.
        despawnNpc(vendorId, null);

        Location location = player.getLocation();
        npcConfig.set(vendorId + ".level", location.getLevel().getName());
        npcConfig.set(vendorId + ".x", location.getX());
        npcConfig.set(vendorId + ".y", location.getY());
        npcConfig.set(vendorId + ".z", location.getZ());
        npcConfig.set(vendorId + ".yaw", location.getYaw());
        npcConfig.set(vendorId + ".pitch", location.getPitch());
        npcConfig.save();

        npcLocations.put(vendorId, location); // Update in-memory location
        spawnNpc(vendorId, location);
        player.sendMessage(TextFormat.colorize('&', "&aSpawned and saved vendor '" + vendor.getName() + "' at your location."));
    }

    private void despawnNpc(String vendorId, Level level) {
        // Find and close any entity whose nametag matches the vendor's name.
        // This is more robust than tracking entity IDs.
        Vendor vendor = plugin.getVendorManager().getVendor(vendorId);
        if (vendor == null) return;

        Level searchLevel = (level != null) ? level : plugin.getServer().getDefaultLevel();
        for (Entity entity : searchLevel.getEntities()) {
            if (entity instanceof VendorNPC && entity.getNameTag().equals(vendor.getName())) {
                entity.close();
            }
        }
    }

    public Vendor getVendorFromEntity(Entity entity) {
        if (!(entity instanceof VendorNPC)) return null;
        String nameTag = entity.getNameTag();
        for (Vendor vendor : plugin.getVendorManager().getVendors().values()) {
            if (vendor.getName().equals(nameTag)) {
                return vendor;
            }
        }
        return null;
    }
}
