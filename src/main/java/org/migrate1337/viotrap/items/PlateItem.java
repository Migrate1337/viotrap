package org.migrate1337.viotrap.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.migrate1337.viotrap.VioTrap;

public class PlateItem {

    private static final NamespacedKey PLATE_ITEM_KEY = new NamespacedKey(VioTrap.getPlugin(VioTrap.class), "plate_item_id");

    private static final String STATIC_ITEM_ID = "static_plate_item_id";

    public static ItemStack getPlateItem(int amount) {
        ItemStack item = new ItemStack(Material.valueOf(VioTrap.getPlugin().getPlateType()), amount);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(java.util.Collections.singletonList(VioTrap.getPlugin().getPlateDescription()));
        if (meta != null) {
            meta.setDisplayName(VioTrap.getPlugin().getPlateDisplayName());

            meta.getPersistentDataContainer().set(
                    PLATE_ITEM_KEY,
                    PersistentDataType.STRING,
                    STATIC_ITEM_ID
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    public static String getUniqueId(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(PLATE_ITEM_KEY, PersistentDataType.STRING)) {
            return item.getItemMeta().getPersistentDataContainer().get(PLATE_ITEM_KEY, PersistentDataType.STRING);
        }
        return null;
    }
}
