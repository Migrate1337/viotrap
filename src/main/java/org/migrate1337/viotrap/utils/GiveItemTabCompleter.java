package org.migrate1337.viotrap.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.migrate1337.viotrap.VioTrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GiveItemTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("give");
        } else if (args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        } else if (args.length == 3) {
            suggestions.add("Трапка");
            suggestions.add("Пласт");
            suggestions.add("Явная_пыль");
            suggestions.add("Дезориентация");
            suggestions.add("Божья_аура");
        } else if (args.length == 4 && args[2].equalsIgnoreCase("Трапка")) {
            List<String> availableSkins = VioTrap.getPlugin().getSkinNames();
            suggestions.addAll(availableSkins);
        } else if ((args.length == 4 && !args[2].equalsIgnoreCase("Трапка")) || args.length == 5) {
            suggestions.add("1");
            suggestions.add("4");
            suggestions.add("16");
            suggestions.add("64");
        }

        return StringUtil.copyPartialMatches(args[args.length - 1], suggestions, new ArrayList<>());
    }

}
