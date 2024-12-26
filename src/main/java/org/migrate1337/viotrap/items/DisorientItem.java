package org.migrate1337.viotrap.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;

import java.util.Arrays;
import java.util.List;

public class DisorientItem {
    public static ItemStack getDisorientItem(int amount) {
        ItemStack item = new ItemStack(Material.valueOf(VioTrap.getPlugin().getDisorientItemType()), amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(VioTrap.getPlugin().getDisorientItemName());
            List<String> itemDescription = VioTrap.getPlugin().getDisorientItemDescription();
            meta.setLore(itemDescription);
            item.setItemMeta(meta);
        }

        return item;
    }
}
