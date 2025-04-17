package org.migrate1337.viotrap.utils;
import net.md_5.bungee.api.ChatColor;

public class ColorUtil {

    public static String format(String message) {
        // Поддержка hex кодов
        message = translateHexColorCodes(message);
        // Поддержка стандартных &a, &b и т.п.
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String translateHexColorCodes(String message) {
        StringBuilder builder = new StringBuilder();
        char[] chars = message.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 7 < chars.length && chars[i + 1] == '#' &&
                    isHex(chars[i + 2]) && isHex(chars[i + 3]) &&
                    isHex(chars[i + 4]) && isHex(chars[i + 5]) &&
                    isHex(chars[i + 6]) && isHex(chars[i + 7])) {

                String hexCode = "#" + message.substring(i + 2, i + 8);
                builder.append(ChatColor.of(hexCode));
                i += 7; // пропустить &#rrggbb
            } else {
                builder.append(chars[i]);
            }
        }

        return builder.toString();
    }

    private static boolean isHex(char c) {
        return "0123456789abcdefABCDEF".indexOf(c) != -1;
    }
}

