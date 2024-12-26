package org.migrate1337.viotrap.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

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
        } else if (args.length == 4) {
            suggestions.add("1");
            suggestions.add("4");
            suggestions.add("16");
            suggestions.add("64");
        }

        return StringUtil.copyPartialMatches(args[args.length - 1], suggestions, new ArrayList<>());
    }
}
