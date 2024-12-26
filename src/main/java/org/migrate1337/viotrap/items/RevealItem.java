package org.migrate1337.viotrap.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;

import java.util.Collections;

public class RevealItem {

    public static ItemStack getRevealItem(int amount) {
        ItemStack item = new ItemStack(Material.valueOf(VioTrap.getPlugin().getRevealItemType()), amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(VioTrap.getPlugin().getRevealItemDisplayName());
            meta.setLore(VioTrap.getPlugin().getRevealItemDescription());
            item.setItemMeta(meta);
        }
        return item;
    }
}