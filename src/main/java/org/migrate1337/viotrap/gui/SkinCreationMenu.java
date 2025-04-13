package org.migrate1337.viotrap.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkinCreationMenu implements Listener {
    private final VioTrap plugin;
    private final Map<Player, String> currentSubMenu = new HashMap<>();
    private final Map<Player, String> editingActionKey = new HashMap<>();

    public SkinCreationMenu(VioTrap plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 18, "Создание скина для трапки");
        currentSubMenu.remove(player);
        updateMainMenuItems(inventory);
        player.openInventory(inventory);
    }

    private void updateMainMenu(Player player) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (inventory == null || !player.getOpenInventory().getTitle().equals("Создание скина для трапки")) {
            openMenu(player);
        } else {
            updateMainMenuItems(inventory);
        }
    }

    private void updateMainMenuItems(Inventory inventory) {
        inventory.setItem(0, createMenuItem(Material.PAPER, "Название скина", "Текущее: " + plugin.getTempSkinData().getOrDefault("name", "Не задано")));
        inventory.setItem(1, createMenuItem(Material.NAME_TAG, "Имя предмета", "Текущее: " + plugin.getTempSkinData().getOrDefault("display_name", "Не задано")));
        inventory.setItem(2, createMenuItem(Material.CHEST, "Схематика", "Текущая: " + plugin.getTempSkinData().getOrDefault("schem", "Не задано")));
        inventory.setItem(4, createMenuItem(Material.BOOK, "Описание для трапки", "Текущее: " + plugin.getTempSkinData().getOrDefault("desc_for_trap", "Не задано")));
        inventory.setItem(5, createMenuItem(Material.NOTE_BLOCK, "Тип звука", "Текущий: " + plugin.getTempSkinData().getOrDefault("sound.type", "Не задано")));
        inventory.setItem(6, createMenuItem(Material.NOTE_BLOCK, "Тип звука (завершение)", "Текущий: " + plugin.getTempSkinData().getOrDefault("sound.type-ended", "Не задано")));
        inventory.setItem(7, createMenuItem(Material.SPIDER_EYE, "Эффект для противников", "Текущий: " + plugin.getTempSkinData().getOrDefault("opponent_effect", "Не задано")));
        inventory.setItem(8, createMenuItem(Material.GOLDEN_APPLE, "Эффект для игрока", "Текущий: " + plugin.getTempSkinData().getOrDefault("player_effect", "Не задано")));
        inventory.setItem(9, createMenuItem(Material.COMMAND_BLOCK, "Действия", "Настроить действия скина (" + getActionsCount() + ")"));
        inventory.setItem(17, createMenuItem(Material.GREEN_WOOL, "Сохранить", "Сохранить новый скин"));
    }

    private int getActionsCount() {
        String actionsStr = plugin.getTempSkinData().getOrDefault("actions", "");
        if (actionsStr.isEmpty()) return 0;
        return actionsStr.split("\\|").length;
    }

    private void openActionsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, "Действия скина");
        currentSubMenu.put(player, "actions");
        updateActionsMenuItems(inventory, player);
        player.openInventory(inventory);
    }

    private void updateActionsMenuItems(Inventory inventory, Player player) {
        inventory.clear();
        Map<String, String> actions = parseActions(plugin.getTempSkinData().getOrDefault("actions", ""));

        int slot = 0;
        for (Map.Entry<String, String> actionEntry : actions.entrySet()) {
            String actionKey = actionEntry.getKey();
            String actionData = actionEntry.getValue();
            String[] parts = actionData.split(";", 2);
            String type = parts[0];
            String data = parts.length > 1 ? parts[1] : "";
            String display = switch (type.toLowerCase()) {
                case "effect" -> "Эффект: " + data;
                case "command" -> "Команда: " + data;
                case "teleportout" -> "Телепортация: " + data;
                case "particlehitbox" -> "Частицы: " + data;
                default -> "Неизвестное действие";
            };
            inventory.setItem(slot++, createMenuItem(Material.PAPER, "Действие: " + actionKey, display));
        }

        inventory.setItem(25, createMenuItem(Material.EMERALD, "Добавить действие", "Добавить новое действие"));
        inventory.setItem(26, createMenuItem(Material.BARRIER, "Назад", "Вернуться в главное меню"));
    }

    private void openActionTypeMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 9, "Выбор типа действия");
        currentSubMenu.put(player, "action_type");
        inventory.setItem(0, createMenuItem(Material.POTION, "Эффект", "Добавить эффект (например, SPEED, POISON)"));
        inventory.setItem(1, createMenuItem(Material.COMMAND_BLOCK, "Команда", "Добавить команду (например, gamemode)"));
        inventory.setItem(2, createMenuItem(Material.ENDER_PEARL, "Телепортация", "Добавить телепортацию вверх"));
        inventory.setItem(3, createMenuItem(Material.FIREWORK_ROCKET, "Частицы хитбокса", "Добавить эффект частиц вокруг хитбокса"));
        inventory.setItem(8, createMenuItem(Material.BARRIER, "Назад", "Вернуться к действиям"));
        player.openInventory(inventory);
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
        String title = event.getView().getTitle();
        if (!title.equals("Создание скина для трапки") && !title.equals("Действия скина") && !title.equals("Выбор типа действия")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String itemName = clickedItem.getItemMeta().getDisplayName();

        if (title.equals("Создание скина для трапки")) {
            switch (itemName) {
                case "Название скина" -> handleInput(player, "name", "Введите название скина в чат:");
                case "Имя предмета" -> handleInput(player, "display_name", "Введите имя предмета (как оно будет отображаться у игрока):");
                case "Схематика" -> handleInput(player, "schem", "Введите название схематики в чат:");
                case "Описание для трапки" -> handleInput(player, "desc_for_trap", "Введите описание для трапки в чат (поддерживается кодировка &):");
                case "Тип звука" -> handleInput(player, "sound.type", "Введите тип звука (например, ENTITY_WITHER_AMBIENT):");
                case "Тип звука (завершение)" -> handleInput(player, "sound.type-ended", "Введите тип звука при завершении (например, ENTITY_WITHER_AMBIENT):");
                case "Действия" -> openActionsMenu(player);
                case "Сохранить" -> saveSkin(player);
            }
        } else if (title.equals("Действия скина")) {
            if (itemName.equals("Добавить действие")) {
                openActionTypeMenu(player);
            } else if (itemName.equals("Назад")) {
                updateMainMenu(player);
            } else if (itemName.startsWith("Действие: ")) {
                String actionKey = itemName.replace("Действие: ", "");
                editingActionKey.put(player, actionKey);
                handleActionEdit(player, actionKey);
            }
        } else if (title.equals("Выбор типа действия")) {
            switch (itemName) {
                case "Эффект" -> handleInput(player, "action_effect", "Введите эффект (формат: <p/o/rp> <effect> <amplifier> <duration>, например: rp SPEED 1 10):");
                case "Команда" -> handleInput(player, "action_command", "Введите команду (формат: <command> <p/o/rp>, например: gamemode creative rp):");
                case "Телепортация" -> handleInput(player, "action_teleportout", "Введите телепортацию (формат: <p/o/rp> <blocks> up, например: rp 5 up):");
                case "Частицы хитбокса" -> handleInput(player, "action_particlehitbox", "Введите частицы (формат: <p/o/rp> <particle> <duration>, например: rp REDSTONE 5):");
                case "Назад" -> openActionsMenu(player);
            }
        }
    }

    private void handleInput(Player player, String key, String prompt) {
        player.closeInventory();
        player.sendMessage(prompt);
        plugin.getChatInputHandler().waitForInput(player, input -> {
            if (key.equals("sound.type") || key.equals("sound.type-ended")) {
                try {
                    Sound.valueOf(input);
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cУказан недопустимый тип звука. Попробуйте снова.");
                    return;
                }
            }
            if (key.equals("desc_for_trap") || key.equals("display_name")) {
                input = org.bukkit.ChatColor.translateAlternateColorCodes('&', input);
            }

            if (key.startsWith("action_")) {
                handleActionInput(player, key, input);
            } else {
                plugin.getTempSkinData().put(key, input);
                player.sendMessage("§a" + key + " установлен: " + input);
                Bukkit.getScheduler().runTask(plugin, () -> updateMainMenu(player));
            }
        });
    }

    private Map<String, String> parseActions(String actionsStr) {
        Map<String, String> actions = new HashMap<>();
        if (actionsStr.isEmpty()) return actions;
        String[] actionEntries = actionsStr.split("\\|");
        for (String entry : actionEntries) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                actions.put(parts[0], parts[1]);
            }
        }
        return actions;
    }

    private String serializeActions(Map<String, String> actions) {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : actions.entrySet()) {
            entries.add(entry.getKey() + ":" + entry.getValue());
        }
        return String.join("|", entries);
    }

    private void handleActionInput(Player player, String key, String input) {
        Map<String, String> actions = parseActions(plugin.getTempSkinData().getOrDefault("actions", ""));

        String actionType = key.replace("action_", "");
        String actionKey = editingActionKey.getOrDefault(player, "action" + (actions.size() + 1));

        String actionData;

        switch (actionType) {
            case "effect":
                String[] effectParts = input.split(" ");
                if (effectParts.length == 4) {
                    String target = effectParts[0];
                    try {
                        Integer.parseInt(effectParts[2]); // amplifier
                        Integer.parseInt(effectParts[3]); // duration
                        if (!isValidTarget(target)) {
                            player.sendMessage("§cНекорректный таргет эффекта. Ожидается: p, o или rp");
                            return;
                        }
                        actionData = "effect;" + input;
                        actions.put(actionKey, actionData);
                        player.sendMessage("§aДействие '" + actionKey + "' (эффект) добавлено/обновлено.");
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cНекорректный формат чисел в эффекте. Ожидается: <p/o/rp> <effect> <amplifier> <duration>");
                        return;
                    }
                } else {
                    player.sendMessage("§cНекорректный формат эффекта. Ожидается: <p/o/rp> <effect> <amplifier> <duration>");
                    return;
                }
                break;
            case "command":
                String[] commandParts = input.split(" ");
                if (commandParts.length >= 2) {
                    String target = commandParts[commandParts.length - 1];
                    if (!isValidTarget(target)) {
                        player.sendMessage("§cНекорректный таргет команды. Ожидается: p, o или rp");
                        return;
                    }
                    actionData = "command;" + input;
                    actions.put(actionKey, actionData);
                    player.sendMessage("§aДействие '" + actionKey + "' (команда) добавлено/обновлено.");
                } else {
                    player.sendMessage("§cНекорректный формат команды. Ожидается: <command> <p/o/rp>");
                    return;
                }
                break;
            case "teleportout":
                String[] teleportParts = input.split(" ");
                if (teleportParts.length == 3 && teleportParts[2].equalsIgnoreCase("up")) {
                    String target = teleportParts[0];
                    try {
                        Integer.parseInt(teleportParts[1]); // blocks
                        if (!isValidTarget(target)) {
                            player.sendMessage("§cНекорректный таргет телепортации. Ожидается: p, o или rp");
                            return;
                        }
                        actionData = "teleportout;" + input + ";10";
                        actions.put(actionKey, actionData);
                        player.sendMessage("§aДействие '" + actionKey + "' (телепортация) добавлено/обновлено.");
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cНекорректный формат числа блоков. Ожидается: <p/o/rp> <blocks> up");
                        return;
                    }
                } else {
                    player.sendMessage("§cНекорректный формат телепортации. Ожидается: <p/o/rp> <blocks> up");
                    return;
                }
                break;
            case "particlehitbox":
                String[] particleParts = input.split(" ");
                if (particleParts.length == 3) {
                    String target = particleParts[0];
                    try {
                        Particle.valueOf(particleParts[1].toUpperCase());
                        Integer.parseInt(particleParts[2]); // duration
                        if (!isValidTarget(target)) {
                            player.sendMessage("§cНекорректный таргет частиц. Ожидается: p, o или rp");
                            return;
                        }
                        actionData = "particlehitbox;" + input + ";4";
                        actions.put(actionKey, actionData);
                        player.sendMessage("§aДействие '" + actionKey + "' (частицы) добавлено/обновлено.");
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cНекорректный тип частиц: " + particleParts[1]);
                        return;
                    }
                } else {
                    player.sendMessage("§cНекорректный формат частиц. Ожидается: <p/o/rp> <particle> <duration>");
                    return;
                }
                break;
            default:
                player.sendMessage("§cНеизвестный тип действия: " + actionType);
                return;
        }

        plugin.getTempSkinData().put("actions", serializeActions(actions));
        editingActionKey.remove(player);
        Bukkit.getScheduler().runTask(plugin, () -> openActionsMenu(player));
    }

    private boolean isValidTarget(String target) {
        return target.equalsIgnoreCase("p") || target.equalsIgnoreCase("o") || target.equalsIgnoreCase("rp");
    }

    private void handleActionEdit(Player player, String actionKey) {
        Map<String, String> actions = parseActions(plugin.getTempSkinData().getOrDefault("actions", ""));
        if (actions.containsKey(actionKey)) {
            String actionData = actions.get(actionKey);
            String[] parts = actionData.split(";", 2);
            String type = parts[0];
            String data = parts.length > 1 ? parts[1].split(";", 2)[0] : "";
            switch (type.toLowerCase()) {
                case "effect":
                    handleInput(player, "action_effect", "Введите новый эффект (формат: <p/o/rp> <effect> <amplifier> <duration>, текущий: " + data + "):");
                    break;
                case "command":
                    handleInput(player, "action_command", "Введите новую команду (формат: <command> <p/o/rp>, текущая: " + data + "):");
                    break;
                case "teleportout":
                    handleInput(player, "action_teleportout", "Введите новую телепортацию (формат: <p/o/rp> <blocks> up, текущая: " + data + "):");
                    break;
                case "particlehitbox":
                    handleInput(player, "action_particlehitbox", "Введите новые частицы (формат: <p/o/rp> <particle> <duration>, текущие: " + data + "):");
                    break;
                default:
                    player.sendMessage("§cНеизвестный тип действия: " + type);
                    openActionsMenu(player);
            }
        }
    }

    private void saveSkin(Player player) {
        String skinName = plugin.getTempSkinData().getOrDefault("name", null);
        String displayName = plugin.getTempSkinData().getOrDefault("display_name", null);
        String schematic = plugin.getTempSkinData().getOrDefault("schem", null);
        String description = plugin.getTempSkinData().getOrDefault("desc_for_trap", null);
        String sound = plugin.getTempSkinData().getOrDefault("sound.type", null);
        String soundEnded = plugin.getTempSkinData().getOrDefault("sound.type-ended", "ENTITY_WITHER_AMBIENT");

        if (skinName == null || displayName == null || schematic == null || description == null || sound == null) {
            player.sendMessage("§cПожалуйста, заполните все обязательные поля!");
            return;
        }

        plugin.getConfig().set("skins." + skinName + ".name", displayName);
        plugin.getConfig().set("skins." + skinName + ".schem", schematic);
        plugin.getConfig().set("skins." + skinName + ".desc_for_trap", description);

        plugin.getConfig().set("skins." + skinName + ".sound.type", sound);
        plugin.getConfig().set("skins." + skinName + ".sound.volume", 1.0f);
        plugin.getConfig().set("skins." + skinName + ".sound.pitch", 1.0f);

        plugin.getConfig().set("skins." + skinName + ".sound.type-ended", soundEnded);
        plugin.getConfig().set("skins." + skinName + ".sound.volume-ended", 1.0f);
        plugin.getConfig().set("skins." + skinName + ".sound.pitch-ended", 1.0f);


        Map<String, String> actions = parseActions(plugin.getTempSkinData().getOrDefault("actions", ""));
        for (Map.Entry<String, String> actionEntry : actions.entrySet()) {
            String actionKey = actionEntry.getKey();
            String actionData = actionEntry.getValue();
            String[] parts = actionData.split(";");
            String type = parts[0];
            plugin.getConfig().set("skins." + skinName + ".actions." + actionKey + ".type", type);
            if (type.equals("effect")) {
                plugin.getConfig().set("skins." + skinName + ".actions." + actionKey + ".effect", parts[1]);
            } else if (type.equals("command")) {
                plugin.getConfig().set("skins." + skinName + ".actions." + actionKey + ".command", parts[1]);
            } else if (type.equals("teleportout")) {
                plugin.getConfig().set("skins." + skinName + ".actions." + actionKey + ".teleport", parts[1]);
                plugin.getConfig().set("skins." + skinName + ".actions." + actionKey + ".min-height", Integer.parseInt(parts[2]));
            } else if (type.equals("particlehitbox")) {
                plugin.getConfig().set("skins." + skinName + ".actions." + actionKey + ".particle", parts[1]);
                plugin.getConfig().set("skins." + skinName + ".actions." + actionKey + ".update-interval", Integer.parseInt(parts[2]));
            }
        }

        plugin.saveConfig();
        plugin.getTempSkinData().clear();
        player.sendMessage("§aСкин '" + skinName + "' успешно сохранён!");
        player.closeInventory();
    }
}