package org.migrate1337.viotrap.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;

import java.util.Collections;

public class SkinCreationMenu implements Listener {
    private final VioTrap plugin;

    public SkinCreationMenu(VioTrap plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Открывает меню создания скина для игрока.
     *
     * @param player Игрок, которому открывается меню.
     */
    public void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 9, "Создание скина для трапки");

        // Название скина
        String skinName = (String) plugin.getTempSkinData().getOrDefault("name", "Не задано");
        ItemStack nameItem = createMenuItem(Material.PAPER, "Название скина", "Текущее: " + skinName);
        inventory.setItem(2, nameItem);

        // Схематика
        String schematic = (String) plugin.getTempSkinData().getOrDefault("schem", "Не задано");
        ItemStack schematicItem = createMenuItem(Material.CHEST, "Схематика", "Текущая: " + schematic);
        inventory.setItem(4, schematicItem);

        // Описание для трапки
        String desc = (String) plugin.getTempSkinData().getOrDefault("desc_for_trap", "Не задано");
        ItemStack descItem = createMenuItem(Material.BOOK, "Описание для трапки", "Текущее: " + desc);
        inventory.setItem(6, descItem);

        // Сохранить скин
        ItemStack saveItem = createMenuItem(Material.GREEN_WOOL, "Сохранить", "Сохранить новый скин");
        inventory.setItem(8, saveItem);

        player.openInventory(inventory);
    }



    /**
     * Создает элемент меню с заданным материалом, именем и описанием.
     *
     * @param material Материал элемента.
     * @param name     Имя элемента.
     * @param lore     Описание элемента.
     * @return Элемент меню.
     */
    private ItemStack createMenuItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Collections.singletonList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Обработчик кликов в меню создания скина.
     *
     * @param event Событие клика.
     */
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("Создание скина для трапки")) return;

        event.setCancelled(true); // Запрещаем забирать предметы

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String itemName = clickedItem.getItemMeta().getDisplayName();

        switch (itemName) {
            case "Название скина" -> {
                player.closeInventory();
                player.sendMessage("Введите название скина в чат:");
                plugin.getChatInputHandler().waitForInput(player, input -> {
                    plugin.getTempSkinData().put("name", input);
                    player.sendMessage("§aНазвание скина установлено: " + input);

                    Bukkit.getScheduler().runTask(plugin, () -> openMenu(player));
                });
            }
            case "Схематика" -> {
                player.closeInventory();
                player.sendMessage("Введите название схематики в чат:");
                plugin.getChatInputHandler().waitForInput(player, input -> {
                    plugin.getTempSkinData().put("schem", input);
                    player.sendMessage("§aСхематика установлена: " + input);

                    Bukkit.getScheduler().runTask(plugin, () -> openMenu(player));
                });
            }
            case "Описание для трапки" -> {
                player.closeInventory();
                player.sendMessage("Введите описание для трапки в чат (поддерживается кодировка & и #):");
                plugin.getChatInputHandler().waitForInput(player, input -> {
                    String coloredDesc = org.bukkit.ChatColor.translateAlternateColorCodes('&', input);
                    plugin.getTempSkinData().put("desc_for_trap", coloredDesc);
                    player.sendMessage("§aОписание для трапки установлено: " + coloredDesc);

                    // Открываем меню заново с обновлённым значением
                    Bukkit.getScheduler().runTask(plugin, () -> openMenu(player));
                });
            }

            case "Сохранить" -> {
                String skinName = (String) plugin.getTempSkinData().get("name");
                String schematic = (String) plugin.getTempSkinData().get("schem");
                String description = (String) plugin.getTempSkinData().get("desc_for_trap");

                if (skinName == null || schematic == null || description == null) {
                    player.sendMessage("§cПожалуйста, заполните все поля!");
                    return;
                }

                plugin.getConfig().set("skins." + skinName + ".schem", schematic);
                plugin.getConfig().set("skins." + skinName + ".desc_for_trap", description);
                plugin.saveConfig();

                player.sendMessage("§aСкин '" + skinName + "' успешно сохранён!");
                player.closeInventory();
            }
        }
    }
}
