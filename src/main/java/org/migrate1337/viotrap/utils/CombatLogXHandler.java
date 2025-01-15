package org.migrate1337.viotrap.utils;

import com.github.sirblobman.combatlogx.api.ICombatLogX;
import com.github.sirblobman.combatlogx.api.manager.ICombatManager;
import com.github.sirblobman.combatlogx.api.object.TagReason;
import com.github.sirblobman.combatlogx.api.object.TagType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public class CombatLogXHandler {
    private final ICombatManager combatManager;
    private final boolean isCombatLogXEnabled;

    public CombatLogXHandler() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin combatLogXPlugin = pluginManager.getPlugin("CombatLogX");

        if (combatLogXPlugin instanceof ICombatLogX) {
            ICombatLogX combatLogX = (ICombatLogX) combatLogXPlugin;
            this.combatManager = combatLogX.getCombatManager();
            this.isCombatLogXEnabled = true;
        } else {
            this.combatManager = null;
            this.isCombatLogXEnabled = false;
        }
    }

    public boolean isCombatLogXEnabled() {
        return isCombatLogXEnabled;
    }

    public void tagPlayer(Player player, TagType tagType, TagReason tagReason) {
        if (isCombatLogXEnabled && combatManager != null) {
            combatManager.tag(player, null, tagType, tagReason);
        }
    }
}
