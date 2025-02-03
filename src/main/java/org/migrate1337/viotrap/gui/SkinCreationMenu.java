package org.migrate1337.viotrap.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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

    public void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 9, "Создание скина для трапки");
        updateItems(inventory);
        player.openInventory(inventory);
    }

    private void updateMenu(Player player) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (inventory == null || !player.getOpenInventory().getType().equals(Bukkit.createInventory(null, 9).getType())) {
            openMenu(player); // Если меню закрыто, открываем заново
        } else {
            updateItems(inventory); // Если открыто, просто обновляем предметы
        }
    }

    private void updateItems(Inventory inventory) {
        inventory.setItem(0, createMenuItem(Material.PAPER, "Название скина", "Текущее: " + plugin.getTempSkinData().getOrDefault("name", "Не задано")));
        inventory.setItem(2, createMenuItem(Material.CHEST, "Схематика", "Текущая: " + plugin.getTempSkinData().getOrDefault("schem", "Не задано")));
        inventory.setItem(4, createMenuItem(Material.BOOK, "Описание для трапки", "Текущее: " + plugin.getTempSkinData().getOrDefault("desc_for_trap", "Не задано")));
        inventory.setItem(5, createMenuItem(Material.NOTE_BLOCK, "Тип звука", "Текущий: " + plugin.getTempSkinData().getOrDefault("sound.type", "Не задано")));
        inventory.setItem(6, createMenuItem(Material.SPIDER_EYE, "Эффект для противников", "Текущий: " + plugin.getTempSkinData().getOrDefault("opponent_effect", "Не задано")));
        inventory.setItem(7, createMenuItem(Material.GOLDEN_APPLE, "Эффект для игрока", "Текущий: " + plugin.getTempSkinData().getOrDefault("player_effect", "Не задано")));
        inventory.setItem(8, createMenuItem(Material.GREEN_WOOL, "Сохранить", "Сохранить новый скин"));
    }

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

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("Создание скина для трапки")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String itemName = clickedItem.getItemMeta().getDisplayName();

        switch (itemName) {
            case "Название скина" -> handleInput(player, "name", "Введите название скина в чат:");
            case "Схематика" -> handleInput(player, "schem", "Введите название схематики в чат:");
            case "Описание для трапки" -> handleInput(player, "desc_for_trap", "Введите описание для трапки в чат (поддерживается кодировка & и #):");
            case "Тип звука" -> handleInput(player, "sound.type", "Введите тип звука (например, ENTITY_WITHER_AMBIENT):");
            case "Эффект для противников" -> handleEffectInput(player, "opponent_effect", "Введите эффект для противников (например, NAUSEA):");
            case "Эффект для игрока" -> handleEffectInput(player, "player_effect", "Введите эффект для игрока (например, REGENERATION):");
            case "Сохранить" -> saveSkin(player);
        }
    }

    private void handleInput(Player player, String key, String prompt) {
        player.closeInventory();
        player.sendMessage(prompt);
        plugin.getChatInputHandler().waitForInput(player, input -> {
            if (key.equals("sound.type")) {
                try {
                    Sound.valueOf(input);
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cУказан недопустимый тип звука. Попробуйте снова.");
                    return;
                }
            }
            if (key.equals("desc_for_trap")) {
                input = org.bukkit.ChatColor.translateAlternateColorCodes('&', input);
            }

            plugin.getTempSkinData().put(key, input);
            player.sendMessage("§a" + key + " установлен: " + input);
            Bukkit.getScheduler().runTask(plugin, () -> updateMenu(player));
        });
    }

    private void handleEffectInput(Player player, String key, String message) {
        player.closeInventory();
        player.sendMessage(message);
        plugin.getChatInputHandler().waitForInput(player, input -> {
            plugin.getTempSkinData().put(key, input.toUpperCase());
            player.sendMessage("§a" + key + " установлен: " + input);
            Bukkit.getScheduler().runTask(plugin, () -> updateMenu(player));
        });
    }

    private void saveSkin(Player player) {
        String skinName = plugin.getTempSkinData().get("name");
        String schematic = plugin.getTempSkinData().get("schem");
        String description = plugin.getTempSkinData().get("desc_for_trap");
        String sound = plugin.getTempSkinData().get("sound.type");
        String opponentEffect = plugin.getTempSkinData().get("opponent_effect");
        String playerEffect = plugin.getTempSkinData().get("player_effect");

        if (skinName == null || schematic == null || description == null || sound == null) {
            player.sendMessage("§cПожалуйста, заполните все поля!");
            return;
        }

        plugin.getConfig().set("skins." + skinName + ".schem", schematic);
        plugin.getConfig().set("skins." + skinName + ".desc_for_trap", description);
        plugin.getConfig().set("skins." + skinName + ".sound.type", sound);
        plugin.getConfig().set("skins." + skinName + ".sound.volume", 1.0f);
        plugin.getConfig().set("skins." + skinName + ".sound.pitch", 1.0f);
        plugin.getConfig().set("skins." + skinName + ".effects.player." + playerEffect + ".amplifier", 1);
        plugin.getConfig().set("skins." + skinName + ".effects.player." + playerEffect + ".duration", 10);
        plugin.getConfig().set("skins." + skinName + ".effects.opponents." + opponentEffect + ".amplifier", 1);
        plugin.getConfig().set("skins." + skinName + ".effects.opponents." + opponentEffect + ".duration", 10);
        plugin.saveConfig();

        player.sendMessage("§aСкин '" + skinName + "' успешно сохранён!");
        player.closeInventory();
    }
}
