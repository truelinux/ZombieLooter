package com.zombielooter.kitpvp;

import cn.nukkit.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a kit loadout that can be previewed and applied to a player.
 */
public class KitDefinition {
    private final String id;
    private final String displayName;
    private final Item icon;
    private final List<Item> contents = new ArrayList<>();
    private final Item[] armor;

    public KitDefinition(String id, String displayName, Item icon, List<Item> contents, Item[] armor) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        if (contents != null) {
            this.contents.addAll(contents);
        }
        this.armor = armor;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Item getIcon() {
        return icon;
    }

    public List<Item> getContents() {
        return contents;
    }

    public Item[] getArmor() {
        return armor;
    }
}
