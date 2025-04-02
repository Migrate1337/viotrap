package org.migrate1337.viotrap.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.migrate1337.viotrap.VioTrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrapItem {
    private static final NamespacedKey TRAP_ITEM_KEY = new NamespacedKey(VioTrap.getPlugin(VioTrap.class), "trap_item_id");
    private static final NamespacedKey SKIN_KEY = new NamespacedKey(VioTrap.getPlugin(VioTrap.class), "trap_skin");
    private static final String DEFAULT_TRAP_ID = "default_trap"; // Общий ID для всех предметов

    public static ItemStack getTrapItem(int amount, String skin) {
        ItemStack item = new ItemStack(Material.getMaterial(VioTrap.getPlugin().getTrapType()), amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(VioTrap.getPlugin().getTrapDisplayName());
            List<String> lore = VioTrap.getPlugin().getTrapDescription();
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(TRAP_ITEM_KEY, PersistentDataType.STRING, DEFAULT_TRAP_ID);

            if (skin != null) {
                meta.getPersistentDataContainer().set(SKIN_KEY, PersistentDataType.STRING, skin);
                meta.setDisplayName(VioTrap.getPlugin().getSkinDisplayName(skin));
                List<String> desc = VioTrap.getPlugin().getSkinDescription(skin);
                if (desc != null) {
                    meta.setLore(desc);
                }
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static String getSkin(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(SKIN_KEY, PersistentDataType.STRING)) {
            return item.getItemMeta().getPersistentDataContainer().get(SKIN_KEY, PersistentDataType.STRING);
        }
        return null;
    }

    public static boolean isTrapItem(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(TRAP_ITEM_KEY, PersistentDataType.STRING)) {
            String trapId = item.getItemMeta().getPersistentDataContainer().get(TRAP_ITEM_KEY, PersistentDataType.STRING);
            return DEFAULT_TRAP_ID.equals(trapId);
        }
        return false;
    }
    public static NamespacedKey getSkinKey() {
        return SKIN_KEY;
    }
}
