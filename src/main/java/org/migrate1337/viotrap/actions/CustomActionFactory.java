package org.migrate1337.viotrap.actions;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Random;

public class CustomActionFactory {
    private static final Random random = new Random();

    public static List<CustomAction> loadActions(String skinName, VioTrap plugin) {
        List<CustomAction> actions = new ArrayList<>();
        ConfigurationSection actionsSection = plugin.getConfig().getConfigurationSection("skins." + skinName + ".actions");

        if (actionsSection == null) {
            return actions;
        }

        for (String actionKey : actionsSection.getKeys(false)) {
            ConfigurationSection actionConfig = actionsSection.getConfigurationSection(actionKey);
            if (actionConfig == null) continue;

            String type = actionConfig.getString("type");
            if (type == null) continue;

            switch (type.toLowerCase()) {
                case "effect":
                    String effectData = actionConfig.getString("effect");
                    if (effectData != null) {
                        String[] parts = effectData.split(" ");
                        if (parts.length == 4) {
                            String target = parts[0];
                            String effectName = parts[1];
                            try {
                                int amplifier = Integer.parseInt(parts[2]);
                                int duration = Integer.parseInt(parts[3]);
                                if (!isValidTarget(target)) {
                                    plugin.getLogger().warning("Некорректный таргет для эффекта: " + target);
                                    continue;
                                }
                                actions.add(new EffectCustomAction(target, effectName, amplifier, duration));
                            } catch (NumberFormatException e) {
                                plugin.getLogger().warning("Некорректный формат чисел в эффекте: " + effectData);
                            }
                        }
                    }
                    break;
                case "command":
                    String commandData = actionConfig.getString("command");
                    if (commandData != null) {
                        String[] parts = commandData.split(" ");
                        if (parts.length >= 2) {
                            String target = parts[parts.length - 1];
                            String command = String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1));
                            if (!isValidTarget(target)) {
                                plugin.getLogger().warning("Некорректный таргет для команды: " + target);
                                continue;
                            }
                            actions.add(new CommandCustomAction(target, command));
                        } else {
                            plugin.getLogger().warning("Некорректный формат команды: " + commandData);
                        }
                    }
                    break;
                case "teleportout":
                    String teleportData = actionConfig.getString("teleport");
                    if (teleportData != null) {
                        String[] parts = teleportData.split(" ");
                        if (parts.length == 3) {
                            String target = parts[0];
                            try {
                                int blocks = Integer.parseInt(parts[1]);
                                String location = parts[2];
                                int minHeight = actionConfig.getInt("min-height", 10);
                                if (!isValidTarget(target)) {
                                    plugin.getLogger().warning("Некорректный таргет для телепортации: " + target);
                                    continue;
                                }
                                if (!location.equalsIgnoreCase("up")) {
                                    plugin.getLogger().warning("Некорректное местоположение для телепортации: " + location);
                                    continue;
                                }
                                if (blocks <= 0) {
                                    plugin.getLogger().warning("Количество блоков для телепортации должно быть положительным: " + blocks);
                                    continue;
                                }
                                if (minHeight < 0) {
                                    plugin.getLogger().warning("Минимальная высота телепортации не может быть отрицательной: " + minHeight);
                                    continue;
                                }
                                actions.add(new TeleportOutCustomAction(target, blocks, minHeight));
                            } catch (NumberFormatException e) {
                                plugin.getLogger().warning("Некорректный формат числа блоков в телепортации: " + teleportData);
                            }
                        } else {
                            plugin.getLogger().warning("Некорректный формат данных телепортации: " + teleportData);
                        }
                    }
                    break;
                case "particlehitbox":
                    String particleData = actionConfig.getString("particle");
                    if (particleData != null) {
                        String[] parts = particleData.split(" ");
                        if (parts.length == 3) {
                            String target = parts[0];
                            String particleType = parts[1];
                            try {
                                int duration = Integer.parseInt(parts[2]);
                                int updateInterval = actionConfig.getInt("update-interval", 4);
                                if (!isValidTarget(target)) {
                                    plugin.getLogger().warning("Некорректный таргет для частиц: " + target);
                                    continue;
                                }
                                if (duration <= 0) {
                                    plugin.getLogger().warning("Длительность частиц должна быть положительной: " + duration);
                                    continue;
                                }
                                if (updateInterval < 1) {
                                    plugin.getLogger().warning("Интервал обновления частиц должен быть положительным: " + updateInterval);
                                    continue;
                                }
                                try {
                                    Particle.valueOf(particleType.toUpperCase());
                                    actions.add(new ParticleHitboxCustomAction(target, particleType, duration, updateInterval));
                                } catch (IllegalArgumentException e) {
                                    plugin.getLogger().warning("Некорректный тип частиц: " + particleType);
                                    continue;
                                }
                            } catch (NumberFormatException e) {
                                plugin.getLogger().warning("Некорректный формат длительности в частицах: " + particleData);
                            }
                        } else {
                            plugin.getLogger().warning("Некорректный формат данных частиц: " + particleData);
                        }
                    }
                    break;
                default:
                    plugin.getLogger().warning("Неизвестный тип действия: " + type);
                    break;
            }
        }

        return actions;
    }

    private static boolean isValidTarget(String target) {
        return target.equalsIgnoreCase("p") || target.equalsIgnoreCase("player") ||
                target.equalsIgnoreCase("o") || target.equalsIgnoreCase("rp");
    }

    // Утилита для выбора случайного игрока
    public static Player getRandomPlayer(Player player, Player[] opponents) {
        List<Player> candidates = new ArrayList<>();
        if (player != null && player.isOnline()) {
            candidates.add(player);
        }
        for (Player opponent : opponents) {
            if (opponent != null && opponent.isOnline()) {
                candidates.add(opponent);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }
}