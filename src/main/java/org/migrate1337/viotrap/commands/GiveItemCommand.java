package org.migrate1337.viotrap.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.*;

public class GiveItemCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length != 4 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage("Использование: /viotrap give <игрок> <предмет> <количество>");
            return false;
        }

        String targetPlayerName = args[1];
        String itemName = args[2];
        int amount;

        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Количество должно быть числом.");
            return false;
        }

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("Игрок не найден или не в сети.");
            return false;
        }

        ItemStack itemStack = createItemByName(itemName, amount);
        if (itemStack == null) {
            sender.sendMessage("Предмет с таким именем не найден: " + itemName);
            return false;
        }

        targetPlayer.getInventory().addItem(itemStack);
        sender.sendMessage("Игроку " + targetPlayerName + " выдано " + amount + "x " + itemName + ".");
        return true;
    }

    private ItemStack createItemByName(String itemName, int amount) {
        switch (itemName.toLowerCase()) {
            case "трапка":
                return TrapItem.getTrapItem(amount);
            case "пласт":
                return PlateItem.getPlateItem(amount);
            case "явная_пыль":
                return RevealItem.getRevealItem(amount);
            case "дезориентация":
                return DisorientItem.getDisorientItem(amount);
            case "божья_аура":
                return DivineAuraItem.getDivineAuraItem(amount, VioTrap.getPlugin());
            default:
                return null;
        }
    }
}
