package org.migrate1337.viotrap.items;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.migrate1337.viotrap.VioTrap;

import java.util.UUID;

public class TrapItem {

    private static final NamespacedKey TRAP_ITEM_KEY = new NamespacedKey(VioTrap.getPlugin(VioTrap.class), "trap_item_id");

    private static String generateUniqueId() {
        return UUID.randomUUID().toString();
    }

    public static ItemStack getTrapItem(int amount) {
        ItemStack item = new ItemStack(Material.getMaterial(VioTrap.getPlugin().getTrapType()), amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(VioTrap.getPlugin().getTrapDisplayName());
            meta.setLore(java.util.Collections.singletonList(VioTrap.getPlugin().getTrapDescription()));

            meta.getPersistentDataContainer().set(
                    TRAP_ITEM_KEY,
                    PersistentDataType.STRING,
                    generateUniqueId()
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    public static String getUniqueId(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(TRAP_ITEM_KEY, PersistentDataType.STRING)) {
            return item.getItemMeta().getPersistentDataContainer().get(TRAP_ITEM_KEY, PersistentDataType.STRING);
        }
        return null;
    }
}
