package org.migrate1337.viotrap.utils;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ChatInputHandler {
    private final Map<Player, Consumer<String>> inputMap = new HashMap<>();

    public void waitForInput(Player player, Consumer<String> callback) {
        inputMap.put(player, callback);
    }

    public boolean handleChatInput(Player player, String message) {
        if (inputMap.containsKey(player)) {
            Consumer<String> callback = inputMap.remove(player);
            callback.accept(message);
            return true;
        }
        return false;
    }
}
