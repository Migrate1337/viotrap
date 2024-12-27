package org.migrate1337.viotrap.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;

import java.util.List;

public class DivineAuraItem {
    public static ItemStack getDivineAuraItem(int amount, VioTrap plugin) {
        Material material = plugin.getDivineAuraItemMaterial();
        String itemName = plugin.getDivineAuraItemName();
        List<String> itemDescription = plugin.getDivineAuraItemDescription();

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + itemName);
            meta.setLore(itemDescription);
            item.setItemMeta(meta);
        }

        return item;
    }
}
