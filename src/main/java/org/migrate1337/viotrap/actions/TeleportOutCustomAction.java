package org.migrate1337.viotrap.actions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.migrate1337.viotrap.VioTrap;

public class TeleportOutCustomAction implements CustomAction {
    private final String target;
    private final int blocks;
    private final int minHeight;

    public TeleportOutCustomAction(String target, int blocks, int minHeight) {
        this.target = target.toLowerCase();
        this.blocks = blocks;
        this.minHeight = minHeight;
    }

    @Override
    public void execute(Player player, Player[] opponents, VioTrap plugin) {
        switch (target) {
            case "p":
            case "player":
                if (player != null && player.isOnline()) {
                    teleportPlayer(player, player.getLocation(), plugin);
                }
                break;
            case "o":
                for (Player opponent : opponents) {
                    if (opponent != null && opponent.isOnline()) {
                        teleportPlayer(opponent, opponent.getLocation(), plugin);
                    }
                }
                break;
            case "rp":
                Player randomPlayer = CustomActionFactory.getRandomPlayer(player, opponents);
                if (randomPlayer != null && randomPlayer.isOnline()) {
                    teleportPlayer(randomPlayer, randomPlayer.getLocation(), plugin);
                } else {
                    plugin.getLogger().warning("Не удалось выбрать случайного игрока для телепортации.");
                }
                break;
            default:
                plugin.getLogger().warning("Некорректный таргет в TeleportOutCustomAction: " + target);
                break;
        }
    }

    private void teleportPlayer(Player player, Location trapLocation, VioTrap plugin) {
        Location currentLocation = player.getLocation();
        World world = currentLocation.getWorld();
        if (world == null) {
            plugin.getLogger().warning("Мир не найден для игрока " + player.getName());
            return;
        }

        // Целевая высота (y + blocks)
        double targetY = currentLocation.getY() + blocks;
        Location targetLocation = new Location(world, currentLocation.getX(), targetY, currentLocation.getZ(), currentLocation.getYaw(), currentLocation.getPitch());

        if (isSafeLocation(targetLocation)) {
            plugin.getLogger().info("Телепортация " + player.getName() + " на целевую высоту: y=" + targetY);
            player.teleport(targetLocation);
            player.playSound(targetLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            return;
        }

        Location safeLocation = findSafeLocationAbove(currentLocation, plugin);
        if (safeLocation != null) {
            plugin.getLogger().info("Телепортация " + player.getName() + " на безопасную высоту: y=" + safeLocation.getY());
            player.teleport(safeLocation);
            player.playSound(safeLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        } else {
            plugin.getLogger().warning("Не удалось найти безопасное место для телепортации игрока " + player.getName() + " на высоте y + " + minHeight + " или выше");
            player.sendMessage("§cНе удалось телепортироваться: нет безопасного места!");
        }
    }

    private boolean isSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        Block feetBlock = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        Block headBlock = world.getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ());

        return feetBlock.getType() == Material.AIR && headBlock.getType() == Material.AIR &&
                !isDangerousBlock(feetBlock) && !isDangerousBlock(headBlock);
    }

    private boolean isDangerousBlock(Block block) {
        Material type = block.getType();
        return type == Material.LAVA || type == Material.FIRE || type == Material.MAGMA_BLOCK;
    }

    private Location findSafeLocationAbove(Location startLocation, VioTrap plugin) {
        World world = startLocation.getWorld();
        if (world == null) return null;

        int maxHeight = Math.min(world.getMaxHeight(), (int) startLocation.getY() + 100);
        int startY = (int) Math.max(startLocation.getY() + minHeight, startLocation.getY() + 1);

        for (int y = startY; y <= maxHeight - 1; y++) {
            Location checkLocation = new Location(world, startLocation.getX(), y, startLocation.getZ(), startLocation.getYaw(), startLocation.getPitch());
            if (isSafeLocation(checkLocation)) {
                return checkLocation;
            }
        }

        return null;
    }
}