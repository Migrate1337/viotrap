package org.migrate1337.viotrap.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.utils.ColorUtil;

import java.util.*;

public class SkinCreationMenu implements Listener {
    private final VioTrap plugin;
    private final Map<Player, String> currentSubMenu = new HashMap<>();
    private final Map<Player, String> editingActionKey = new HashMap<>();
    private final Map<Player, List<String>> pendingDescriptionLines = new HashMap<>();

    public SkinCreationMenu(VioTrap plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "Создание скина для трапки");
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
        inventory.setItem(0, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(1, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(2, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(3, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(4, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(5, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(6, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(7, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(8, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(9, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(52, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(51, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(50, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(49, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(48, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(47, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(46, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(45, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(18, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(27, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(36, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(45, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(17, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(26, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(35, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(44, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(53, createMenuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(19, createMenuItem(Material.PAPER,
                ColorUtil.format("&#CCCAF0Название скина"),
                ColorUtil.format("&#AEC1F2 • Текущее: ") + plugin.getTempSkinData().getOrDefault("name", "Не задано ")));

        inventory.setItem(20, createMenuItem(Material.NAME_TAG,
                ColorUtil.format("&#F7E3CCИмя предмета"),
                ColorUtil.format("&#AEC1F2 • Текущее: ") + plugin.getTempSkinData().getOrDefault("display_name", "Не задано ")));

        inventory.setItem(21, createMenuItem(Material.CHEST,
                ColorUtil.format("&#EAD7A2Схематика"),
                ColorUtil.format("&#AEC1F2 • Текущая: ") + plugin.getTempSkinData().getOrDefault("schem", "Не задано ")));

        List<String> descLines = getDescriptionLines();
        String[] descLore = descLines.isEmpty() ? new String[]{ColorUtil.format("&#AEC1F2 • Не задано")} :
                descLines.stream().map(line -> ColorUtil.format("&#AEC1F2 • ") + line).toArray(String[]::new);
        inventory.setItem(22, createMenuItem(Material.BOOK,
                ColorUtil.format("&#C89691Описание для трапки"),
                descLore));

        inventory.setItem(23, createMenuItem(Material.NOTE_BLOCK,
                ColorUtil.format("&#9ABDADТип звука"),
                ColorUtil.format("&#AEC1F2 • Текущий: ") + plugin.getTempSkinData().getOrDefault("sound.type", "Не задано ")));

        inventory.setItem(24, createMenuItem(Material.NOTE_BLOCK,
                ColorUtil.format("&#E6E6C2Тип звука (завершение)"),
                ColorUtil.format("&#AEC1F2 • Текущий: ") + plugin.getTempSkinData().getOrDefault("sound.type-ended", "Не задано ")));

        inventory.setItem(25, createMenuItem(Material.COMMAND_BLOCK,
                ColorUtil.format("&#ADD8E6Действия"),
                ColorUtil.format("&#AEC1F2 • Настроить действия скина (" + getActionsCount() + ") ")));

        inventory.setItem(31, createMenuItem(Material.GREEN_WOOL,
                ColorUtil.format("&#90EE90Сохранить"),
                ColorUtil.format("&#AEC1F2 • Сохранить новый скин ")));
    }

    private List<String> getDescriptionLines() {
        String descData = plugin.getTempSkinData().getOrDefault("desc_for_trap", "");
        if (descData.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(descData.split("\\|")));
    }

    private int getActionsCount() {
        String actionsStr = plugin.getTempSkinData().getOrDefault("actions", "");
        if (actionsStr.isEmpty()) return 0;
        return actionsStr.split("\\|").length;
    }

    private void openDescriptionMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, "Редактирование описания");
        currentSubMenu.put(player, "description");
        updateDescriptionMenuItems(inventory);
        player.openInventory(inventory);
    }

    private void updateDescriptionMenuItems(Inventory inventory) {
        inventory.clear();
        List<String> descLines = getDescriptionLines();
        for (int i = 0; i < descLines.size() && i < 20; i++) {
            String line = descLines.get(i);
            inventory.setItem(i, createMenuItem(Material.PAPER,
                    ColorUtil.format("&#FFFFFFСтрока " + (i + 1)),
                    ColorUtil.format("&#AEC1F2 • ") + line, "",
                    ColorUtil.format("&#55FF55СКМ для изменения"),
                    ColorUtil.format("&#FF5555ПКМ для удаления")));
        }
        inventory.setItem(25, createMenuItem(Material.EMERALD, ColorUtil.format("&#90EE90Добавить строку"),
                ColorUtil.format("&#AEC1F2 • Добавить новую строку описания")));
        inventory.setItem(26, createMenuItem(Material.BARRIER, ColorUtil.format("&#FF5555Назад"),
                ColorUtil.format("&#AEC1F2 • Вернуться в главное меню")));
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
            inventory.setItem(slot++, createMenuItem(Material.PAPER,
                    ColorUtil.format("&#FFFFFFДействие: " + actionKey),
                    ColorUtil.format("&#AEC1F2 • ") + display,
                    ColorUtil.format("&#55FF55ЛКМ для изменения"),
                    ColorUtil.format("&#FF5555ПКМ для удаления")));
        }
        inventory.setItem(25, createMenuItem(Material.EMERALD,
                ColorUtil.format("&#90EE90Добавить действие"),
                ColorUtil.format("&#AEC1F2 • Добавить новое действие")));
        inventory.setItem(26, createMenuItem(Material.BARRIER,
                ColorUtil.format("&#EB2D3AНазад"),
                ColorUtil.format("&#AEC1F2 • Вернуться в главное меню")));
    }

    private void openActionTypeMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 9, "Выбор типа действия");
        currentSubMenu.put(player, "action_type");
        inventory.setItem(0, createMenuItem(Material.POTION,
                ColorUtil.format("&#EED7A1Эффект"),
                ColorUtil.format("&#AEC1F2 • Добавить эффект (например, SPEED, POISON)")));
        inventory.setItem(1, createMenuItem(Material.COMMAND_BLOCK,
                ColorUtil.format("&#CD8B62Команда"),
                ColorUtil.format("&#AEC1F2 • Добавить команду (например, gamemode)")));
        inventory.setItem(2, createMenuItem(Material.ENDER_PEARL,
                ColorUtil.format("&#475C6CТелепортация"),
                ColorUtil.format("&#AEC1F2 • Добавить телепортацию вверх")));
        inventory.setItem(3, createMenuItem(Material.FIREWORK_ROCKET,
                ColorUtil.format("&#50B8E7Частицы хитбокса"),
                ColorUtil.format("&#AEC1F2 • Добавить эффект частиц вокруг хитбокса")));
        inventory.setItem(8, createMenuItem(Material.BARRIER,
                ColorUtil.format("&#EB2D3AНазад"),
                ColorUtil.format("&#AEC1F2 • Вернуться к действиям")));
        player.openInventory(inventory);
    }

    private ItemStack createMenuItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            Collections.addAll(lore, loreLines);
            lore.add(" ");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.equals("Создание скина для трапки") && !title.equals("Действия скина") &&
                !title.equals("Выбор типа действия") && !title.equals("Редактирование описания")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String itemName = org.bukkit.ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (title.equals("Создание скина для трапки")) {
            if (itemName.equals("Описание для трапки")) {
                openDescriptionMenu(player);
            } else {
                switch (itemName) {
                    case "Название скина" -> handleInput(player, "name", "Введите название скина в чат:");
                    case "Имя предмета" -> handleInput(player, "display_name", "Введите имя предмета (как оно будет отображаться у игрока):");
                    case "Схематика" -> handleInput(player, "schem", "Введите название схематики в чат:");
                    case "Тип звука" -> handleInput(player, "sound.type", "Введите тип звука (например, ENTITY_WITHER_AMBIENT):");
                    case "Тип звука (завершение)" -> handleInput(player, "sound.type-ended", "Введите тип звука при завершении (например, ENTITY_WITHER_AMBIENT):");
                    case "Действия" -> openActionsMenu(player);
                    case "Сохранить" -> saveSkin(player);
                }
            }
        } else if (title.equals("Редактирование описания")) {
            if (itemName.equals("Добавить строку")) {
                handleDescriptionInput(player);
            } else if (itemName.equals("Назад")) {
                updateMainMenu(player);
            } else if (itemName.startsWith("Строка ")) {
                int index = Integer.parseInt(itemName.replace("Строка ", "")) - 1;
                if (event.getClick() == ClickType.RIGHT) {
                    removeDescriptionLine(player, index);
                } else if (event.getClick() == ClickType.MIDDLE) {
                    editDescriptionLine(player, index);
                }
            }
        } else if (title.equals("Действия скина")) {
            if (itemName.equals("Добавить действие")) {
                openActionTypeMenu(player);
            } else if (itemName.equals("Назад")) {
                updateMainMenu(player);
            } else if (itemName.startsWith("Действие: ")) {
                String actionKey = itemName.replace("Действие: ", "");
                if (event.getClick() == ClickType.RIGHT) {
                    removeAction(player, actionKey);
                } else if (event.getClick() == ClickType.LEFT) {
                    editingActionKey.put(player, actionKey);
                    handleActionEdit(player, actionKey);
                }
            }
        } else if (title.equals("Выбор типа действия")) {
            switch (itemName) {
                case "Эффект" -> handleInput(player, "action_effect", "Введите эффект (формат: <p/o/rp> <effect> <amplifier> <duration>, например: rp SPEED 1 10):");
                case "Команда" -> handleInput(player, "action_command", "Введите команду (формат: <command> <p/o/rp>, например: gamemode creative rp):");
                case "Телепортация" -> handleInput(player, "action_teleportout", "Введите телепортацию (формат: <p/o/rp> <blocks> up, например: rp 5 up):");
                case "Частицы хитбокса" -> handleInput(player, "action_particlehitbox", "Введите частицы (формат: <p/o/rp> <particle> <duration>, например: rp CRIT 5):");
                case "Назад" -> openActionsMenu(player);
            }
        }
    }

    private void handleInput(Player player, String key, String prompt) {
        player.closeInventory();
        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#FB654E" + prompt));
        plugin.getChatInputHandler().waitForInput(player, input -> {
            if (key.equals("sound.type") || key.equals("sound.type-ended")) {
                try {
                    Sound.valueOf(input);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AУказан недопустимый тип звука. Попробуйте снова."));
                    return;
                }
            }
            if (key.equals("display_name")) {
                input = org.bukkit.ChatColor.translateAlternateColorCodes('&', input);
            }

            if (key.startsWith("action_")) {
                handleActionInput(player, key, input);
            } else {
                plugin.getTempSkinData().put(key, input);
                player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6" + key + " установлен: " + input));
                Bukkit.getScheduler().runTask(plugin, () -> updateMainMenu(player));
            }
        });
    }

    private void handleDescriptionInput(Player player) {
        player.closeInventory();
        pendingDescriptionLines.put(player, new ArrayList<>());
        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#FB654EВведите строку описания (поддерживается кодировка &). Введите 'done' для завершения:"));
        collectDescriptionLine(player);
    }

    private void collectDescriptionLine(Player player) {
        plugin.getChatInputHandler().waitForInput(player, input -> {
            if (input.equalsIgnoreCase("done")) {
                List<String> lines = pendingDescriptionLines.getOrDefault(player, new ArrayList<>());
                if (lines.isEmpty()) {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AОписание не может быть пустым. Добавьте хотя бы одну строку."));
                    Bukkit.getScheduler().runTask(plugin, () -> openDescriptionMenu(player));
                } else {
                    List<String> existingLines = getDescriptionLines();
                    existingLines.addAll(lines);
                    String serialized = String.join("|", existingLines);
                    plugin.getTempSkinData().put("desc_for_trap", serialized);
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Добавлено " + lines.size() + " строк описания"));
                    pendingDescriptionLines.remove(player);
                    Bukkit.getScheduler().runTask(plugin, () -> openDescriptionMenu(player));
                }
            } else {
                List<String> lines = pendingDescriptionLines.getOrDefault(player, new ArrayList<>());
                String coloredInput = org.bukkit.ChatColor.translateAlternateColorCodes('&', input);
                lines.add(coloredInput);
                pendingDescriptionLines.put(player, lines);
                player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Строка добавлена. Введите следующую строку или 'done' для завершения:"));
                collectDescriptionLine(player);
            }
        });
    }

    private void editDescriptionLine(Player player, int index) {
        List<String> descLines = getDescriptionLines();
        if (index < 0 || index >= descLines.size()) {
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный индекс строки."));
            Bukkit.getScheduler().runTask(plugin, () -> openDescriptionMenu(player));
            return;
        }
        String oldLine = descLines.get(index);
        player.closeInventory();
        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#FB654EВведите новую строку для замены (текущая: " + oldLine + "):"));
        plugin.getChatInputHandler().waitForInput(player, input -> {
            String coloredInput = org.bukkit.ChatColor.translateAlternateColorCodes('&', input);
            descLines.set(index, coloredInput);
            String serialized = String.join("|", descLines);
            plugin.getTempSkinData().put("desc_for_trap", serialized);
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Строка " + (index + 1) + " изменена на: " + coloredInput));
            Bukkit.getScheduler().runTask(plugin, () -> openDescriptionMenu(player));
        });
    }

    private void removeDescriptionLine(Player player, int index) {
        List<String> descLines = getDescriptionLines();
        if (index >= 0 && index < descLines.size()) {
            String removedLine = descLines.remove(index);
            String serialized = descLines.isEmpty() ? "" : String.join("|", descLines);
            plugin.getTempSkinData().put("desc_for_trap", serialized);
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Строка удалена: " + removedLine));
        } else {
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный индекс строки."));
        }
        Bukkit.getScheduler().runTask(plugin, () -> openDescriptionMenu(player));
    }

    private void removeAction(Player player, String actionKey) {
        Map<String, String> actions = parseActions(plugin.getTempSkinData().getOrDefault("actions", ""));
        if (actions.containsKey(actionKey)) {
            String removedAction = actions.remove(actionKey);
            String serialized = actions.isEmpty() ? "" : serializeActions(actions);
            plugin.getTempSkinData().put("actions", serialized);
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Действие '" + actionKey + "' удалено."));
        } else {
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный ключ действия."));
        }
        Bukkit.getScheduler().runTask(plugin, () -> openActionsMenu(player));
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
                            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный таргет эффекта. Ожидается: p, o или rp"));
                            return;
                        }
                        actionData = "effect;" + input;
                        actions.put(actionKey, actionData);
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Действие '" + actionKey + "' (эффект) добавлено/обновлено."));
                    } catch (NumberFormatException e) {
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный формат чисел в эффекте. Ожидается: <p/o/rp> <effect> <amplifier> <duration>"));
                        return;
                    }
                } else {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный формат эффекта. Ожидается: <p/o/rp> <effect> <amplifier> <duration>"));
                    return;
                }
                break;
            case "command":
                String[] commandParts = input.split(" ");
                if (commandParts.length >= 2) {
                    String target = commandParts[commandParts.length - 1];
                    if (!isValidTarget(target)) {
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный таргет команды. Ожидается: p, o или rp"));
                        return;
                    }
                    actionData = "command;" + input;
                    actions.put(actionKey, actionData);
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Действие '" + actionKey + "' (команда) добавлено/обновлено."));
                } else {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный формат команды. Ожидается: <command> <p/o/rp>"));
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
                            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный таргет телепортации. Ожидается: p, o или rp"));
                            return;
                        }
                        actionData = "teleportout;" + input + ";10";
                        actions.put(actionKey, actionData);
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Действие '" + actionKey + "' (телепортация) добавлено/обновлено."));
                    } catch (NumberFormatException e) {
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный формат числа блоков. Ожидается: <p/o/rp> <blocks> up"));
                        return;
                    }
                } else {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный формат телепортации. Ожидается: <p/o/rp> <blocks> up"));
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
                            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный таргет частиц. Ожидается: p, o или rp"));
                            return;
                        }
                        actionData = "particlehitbox;" + input + ";4";
                        actions.put(actionKey, actionData);
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Действие '" + actionKey + "' (частицы) добавлено/обновлено."));
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный тип частиц: " + particleParts[1]));
                        return;
                    }
                } else {
                    player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНекорректный формат частиц. Ожидается: <p/o/rp> <particle> <duration>"));
                    return;
                }
                break;
            default:
                player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНеизвестный тип действия: " + actionType));
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
                    player.sendMessage("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AНеизвестный тип действия: " + type);
                    openActionsMenu(player);
            }
        }
    }

    private void saveSkin(Player player) {
        String skinName = plugin.getTempSkinData().getOrDefault("name", null);
        String displayName = plugin.getTempSkinData().getOrDefault("display_name", null);
        String schematic = plugin.getTempSkinData().getOrDefault("schem", null);
        String sound = plugin.getTempSkinData().getOrDefault("sound.type", null);
        String soundEnded = plugin.getTempSkinData().getOrDefault("sound.type-ended", "ENTITY_WITHER_AMBIENT");
        List<String> descriptionLines = getDescriptionLines();

        if (skinName == null || displayName == null || schematic == null || descriptionLines.isEmpty() || sound == null) {
            player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#EB2D3AПожалуйста, заполните все обязательные поля!"));
            return;
        }

        plugin.getConfig().set("skins." + skinName + ".name", displayName);
        plugin.getConfig().set("skins." + skinName + ".schem", schematic);
        plugin.getConfig().set("skins." + skinName + ".desc_for_trap", descriptionLines);

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
        player.sendMessage(ColorUtil.format("&#90EE90[&#89E989V&#83E583T&#7CE07C] &#ADD8E6Скин '" + skinName + "' успешно сохранён!"));
        player.closeInventory();
    }
}