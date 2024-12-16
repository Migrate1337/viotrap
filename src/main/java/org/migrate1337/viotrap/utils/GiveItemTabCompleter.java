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

        // Автодополнение для первого аргумента (give)
        if (args.length == 1) {
            suggestions.add("give");
        }
        // Автодополнение для второго аргумента (игрок)
        else if (args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        }
        // Автодополнение для третьего аргумента (предмет)
        else if (args.length == 3) {
            suggestions.add("Пласт");  // Для GivePlateCommand
            suggestions.add("Трапка"); // Для GiveTrapCommand
        }
        // Автодополнение для четвертого аргумента (количество)
        else if (args.length == 4) {
            suggestions.add("1");
            suggestions.add("4");
            suggestions.add("16");
            suggestions.add("64");
        }

        // Фильтруем предложения на основе ввода
        List<String> filteredSuggestions = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (StringUtil.startsWithIgnoreCase(suggestion, args[args.length - 1])) {
                filteredSuggestions.add(suggestion);
            }
        }

        return filteredSuggestions;
    }
}
