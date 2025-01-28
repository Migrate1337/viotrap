package org.migrate1337.viotrap.utils;

import me.NoChance.PvPManager.PvPlayer;
import me.NoChance.PvPManager.PvPManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public class PVPManagerHandle {
    private final PvPManager pvpManager;
    private final boolean isPvPManagerEnabled;

    public PVPManagerHandle() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin pvpManagerPlugin = pluginManager.getPlugin("PvPManager");

        if (pvpManagerPlugin instanceof PvPManager) {
            this.pvpManager = (PvPManager) pvpManagerPlugin;
            this.isPvPManagerEnabled = true;
        } else {
            this.pvpManager = null;
            this.isPvPManagerEnabled = false;
        }
    }

    public boolean isPvPManagerEnabled() {
        return isPvPManagerEnabled;
    }

    public void tagPlayerForPvP(Player player) {
        if (isPvPManagerEnabled && pvpManager != null) {
            PvPlayer pvPlayer = pvpManager.getPlayerHandler().get(player);
            if (pvPlayer != null) {
                if (!pvPlayer.isInCombat()) {
                    pvPlayer.setTagged(true, pvPlayer);  // Помечаем игрока для PvP
                }
            }
        }
    }
}
