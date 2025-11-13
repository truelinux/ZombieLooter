package com.zombielooter.npc;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityIntelligentHuman;
import cn.nukkit.entity.custom.CustomEntity;
import cn.nukkit.entity.custom.CustomEntityDefinition;
import cn.nukkit.level.format.IChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.registry.EntityRegistry;
import org.jetbrains.annotations.NotNull;

public class VendorNPC extends EntityIntelligentHuman implements CustomEntity {

    public static CustomEntityDefinition DEF =  CustomEntityDefinition.simpleBuilder("zombielooter:vendor_npc").build();
    public static String id =  "zombielooter:vendor_npc";

    public VendorNPC(IChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "zombielooter:vendor_npc"; // Must be unique
    }
}
