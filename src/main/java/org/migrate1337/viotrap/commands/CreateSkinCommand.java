package org.migrate1337.viotrap.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.gui.SkinCreationMenu;

public class CreateSkinCommand implements CommandExecutor {
    private final VioTrap plugin;

    public CreateSkinCommand(VioTrap plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЭту команду можно использовать только игроку.");
            return true;
        }

        Player player = (Player) sender;
        SkinCreationMenu menu = new SkinCreationMenu(plugin);
        menu.openMenu(player);
        return true;
    }
}
