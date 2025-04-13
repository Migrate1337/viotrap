package org.migrate1337.viotrap.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;

public class CommandCustomAction implements CustomAction {
    private final String target;
    private final String command;

    public CommandCustomAction(String target, String command) {
        this.target = target.toLowerCase();
        this.command = command;
    }

    @Override
    public void execute(Player player, Player[] opponents, VioTrap plugin) {
        switch (target) {
            case "p":
            case "player":
                executeCommand(player);
                break;
            case "o":
                for (Player opponent : opponents) {
                    executeCommand(opponent);
                }
                break;
            case "rp":
                Player randomPlayer = CustomActionFactory.getRandomPlayer(player, opponents);
                executeCommand(randomPlayer);
                break;
            default:
                plugin.getLogger().warning("Некорректный таргет в CommandCustomAction: " + target);
        }
    }

    private void executeCommand(Player player) {
        if (player != null && player.isOnline()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }
}