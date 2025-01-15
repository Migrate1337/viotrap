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

        if (args.length < 4 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage("Использование: /viotrap give <игрок> <предмет> [<скин>] <количество>");
            return false;
        }

        String targetPlayerName = args[1];
        String itemName = args[2];
        String skinName = args.length == 5 ? args[3] : null; // Скин, если он указан
        int amountIndex = args.length == 5 ? 4 : 3; // Индекс количества в зависимости от наличия скина
        int amount;

        try {
            amount = Integer.parseInt(args[amountIndex]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Количество должно быть числом.");
            return false;
        }

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("Игрок не найден или не в сети.");
            return false;
        }

        ItemStack itemStack = createItemByName(itemName, skinName, amount);
        if (itemStack == null) {
            sender.sendMessage("Предмет с таким именем не найден: " + itemName);
            return false;
        }

        targetPlayer.getInventory().addItem(itemStack);
        sender.sendMessage("Игроку " + targetPlayerName + " выдано " + amount + "x " + itemName +
                (skinName != null ? " со скином " + skinName : "") + ".");
        return true;
    }

    private ItemStack createItemByName(String itemName, String skinName, int amount) {
        switch (itemName.toLowerCase()) {
            case "трапка":
                return skinName != null ? TrapItem.getTrapItem(amount, skinName) : TrapItem.getTrapItem(amount, null);
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
