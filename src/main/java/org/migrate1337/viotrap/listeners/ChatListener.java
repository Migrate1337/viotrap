package org.migrate1337.viotrap.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.migrate1337.viotrap.VioTrap;

public class ChatListener implements Listener {
    private final VioTrap plugin;

    public ChatListener(VioTrap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (plugin.getChatInputHandler().handleChatInput(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true); // Останавливаем дальнейшую обработку сообщения
        }
    }
}
