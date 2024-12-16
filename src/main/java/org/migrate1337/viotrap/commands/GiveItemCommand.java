package org.migrate1337.viotrap.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.migrate1337.viotrap.items.TrapItem;
import org.migrate1337.viotrap.items.PlateItem;

public class GiveItemCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может выполнить только игрок!");
            return false;
        }

        Player player = (Player) sender;

        // Проверка на правильное количество аргументов
        if (args.length != 4 || !args[0].equalsIgnoreCase("give")) {
            return false;
        }

        String targetPlayerName = args[1];
        String itemName = args[2];
        int amount;

        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            return false;
        }

        // Находим игрока, которому выдадим предмет
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("Игрок не найден или не в сети.");
            return false;
        }

        // Создаём предмет на основе имени
        ItemStack itemStack = null;

        if ("Трапка".equalsIgnoreCase(itemName)) {
            itemStack = TrapItem.getTrapItem(amount);
        } else if ("Пласт".equalsIgnoreCase(itemName)) {
            itemStack = PlateItem.getPlateItem(amount);
        }

        // Проверка, что предмет был создан
        if (itemStack == null) {
            sender.sendMessage("Предмет с таким именем не найден.");
            return false;
        }

        // Проверка имени предмета с использованием метаданных
        if (!isItemNameValid(itemStack, itemName)) {
            sender.sendMessage("Предмет с таким именем не найден.");
            return false;
        }

        // Выдаем предмет целевому игроку
        targetPlayer.getInventory().addItem(itemStack);
        sender.sendMessage("Игроку " + targetPlayerName + " выдано " + itemName + " " + amount + "шт.");
        return true;
    }

    // Метод для проверки имени предмета через ItemMeta
    private boolean isItemNameValid(ItemStack item, String itemName) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        // Извлекаем метаданные
        String displayName = item.getItemMeta().getDisplayName();

        // Проверяем, что название предмета соответствует ожидаемому
        if ("Трапка".equalsIgnoreCase(itemName)) {
            return displayName.equals("§6Трапка");
        } else if ("Пласт".equalsIgnoreCase(itemName)) {
            return displayName.equals("§6Пласт");
        }

        return false;
    }
}
