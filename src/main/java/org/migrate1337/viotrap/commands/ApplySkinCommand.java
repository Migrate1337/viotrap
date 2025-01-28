package org.migrate1337.viotrap.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.TrapItem;

public class ApplySkinCommand implements CommandExecutor {

    private final VioTrap plugin;

    public ApplySkinCommand(VioTrap plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(!sender.hasPermission("viotrap.applyskin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на использование данной команды!");
            return false;
        }

        if (args.length < 1 || args.length > 2) {
            sender.sendMessage("Использование: /applyskin <название_скина> [игрок]");
            return false;
        }

        String skinName = args[0];

        if (!VioTrap.getPlugin().getConfig().contains("skins." + skinName)) {
            sender.sendMessage("§cСкин с названием '" + skinName + "' не найден.");
            return false;
        }

        Player targetPlayer;
        if (args.length == 2) {
            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sender.sendMessage("§cИгрок '" + args[1] + "' не найден или не в сети.");
                return false;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cЭту команду может использовать только игрок.");
                return false;
            }
            targetPlayer = (Player) sender;
        }

        ItemStack[] inventoryContents = targetPlayer.getInventory().getContents();
        int slotWithTrap = -1;

        for (int i = 0; i < inventoryContents.length; i++) {
            ItemStack item = inventoryContents[i];
            if (item != null && TrapItem.isTrapItem(item)) {
                String currentSkin = TrapItem.getSkin(item);
                if (currentSkin == null) {
                    slotWithTrap = i;
                    break;
                }
            }
        }

        if (slotWithTrap == -1) {
            sender.sendMessage("§cУ " + targetPlayer.getName() + " нет трапок без скинов.");
            return false;
        }

        ItemStack trapToSkin = inventoryContents[slotWithTrap];
        ItemStack singleTrap = trapToSkin.clone();
        singleTrap.setAmount(1);

        ItemMeta meta = singleTrap.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(TrapItem.getSkinKey(), PersistentDataType.STRING, skinName);

            String newDescription = VioTrap.getPlugin().getConfig().getString("skins." + skinName + ".desc_for_trap");
            if (newDescription != null) {
                meta.setLore(java.util.Collections.singletonList(newDescription));
            }

            singleTrap.setItemMeta(meta);
        }

        trapToSkin.setAmount(trapToSkin.getAmount() - 1);

        targetPlayer.getInventory().addItem(singleTrap);

        sender.sendMessage("§aСкин '" + skinName + "' успешно применён к одной из трапок игрока " + targetPlayer.getName() + ".");
        if (!targetPlayer.equals(sender)) {
            targetPlayer.sendMessage("§aНа одной из ваших трапок был применён скин '" + skinName + "'.");
        }

        return true;
    }
}
