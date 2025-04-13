package org.migrate1337.viotrap.actions;

import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;

public interface CustomAction {
    void execute(Player player, Player[] opponents, VioTrap plugin);
}