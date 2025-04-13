package org.migrate1337.viotrap.actions;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.migrate1337.viotrap.VioTrap;

public class ParticleHitboxCustomAction implements CustomAction {
    private final String target;
    private final String particleType;
    private final int duration;
    private final int updateInterval;

    public ParticleHitboxCustomAction(String target, String particleType, int duration, int updateInterval) {
        this.target = target.toLowerCase();
        this.particleType = particleType.toUpperCase();
        this.duration = duration;
        this.updateInterval = updateInterval;
    }

    @Override
    public void execute(Player player, Player[] opponents, VioTrap plugin) {
        switch (target) {
            case "p":
            case "player":
                spawnHitboxParticles(player, plugin);
                break;
            case "o":
                for (Player opponent : opponents) {
                    spawnHitboxParticles(opponent, plugin);
                }
                break;
            case "rp":
                Player randomPlayer = CustomActionFactory.getRandomPlayer(player, opponents);
                spawnHitboxParticles(randomPlayer, plugin);
                break;
            default:
                plugin.getLogger().warning("Некорректный таргет в ParticleHitboxCustomAction: " + target);
        }
    }

    private void spawnHitboxParticles(Player player, VioTrap plugin) {
        if (player == null || !player.isOnline()) {
            return;
        }

        Particle particle;
        try {
            particle = Particle.valueOf(particleType);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Некорректный тип частиц в ParticleHitboxCustomAction: " + particleType);
            return;
        }

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = duration * 20;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation();
                double x = loc.getX();
                double y = loc.getY();
                double z = loc.getZ();

                double width = 0.3;
                double height = 1.8;
                double depth = 0.3;

                double[][] corners = {
                        {x - width, y, z - depth},
                        {x + width, y, z - depth},
                        {x - width, y, z + depth},
                        {x + width, y, z + depth},
                        {x - width, y + height, z - depth},
                        {x + width, y + height, z - depth},
                        {x - width, y + height, z + depth},
                        {x + width, y + height, z + depth}
                };

                for (double[] corner : corners) {
                    player.getWorld().spawnParticle(particle, corner[0], corner[1], corner[2], 1, 0, 0, 0, 0);
                }

                spawnLineParticles(player, corners[0], corners[1], particle, 5);
                spawnLineParticles(player, corners[2], corners[3], particle, 5);
                spawnLineParticles(player, corners[0], corners[2], particle, 5);
                spawnLineParticles(player, corners[1], corners[3], particle, 5);
                spawnLineParticles(player, corners[4], corners[5], particle, 5);
                spawnLineParticles(player, corners[6], corners[7], particle, 5);
                spawnLineParticles(player, corners[4], corners[6], particle, 5);
                spawnLineParticles(player, corners[5], corners[7], particle, 5);
                spawnLineParticles(player, corners[0], corners[4], particle, 8);
                spawnLineParticles(player, corners[1], corners[5], particle, 8);
                spawnLineParticles(player, corners[2], corners[6], particle, 8);
                spawnLineParticles(player, corners[3], corners[7], particle, 8);

                ticks += updateInterval;
            }
        }.runTaskTimer(plugin, 0L, updateInterval);
    }

    private void spawnLineParticles(Player player, double[] start, double[] end, Particle particle, int count) {
        double dx = (end[0] - start[0]) / (count - 1);
        double dy = (end[1] - start[1]) / (count - 1);
        double dz = (end[2] - start[2]) / (count - 1);

        for (int i = 0; i < count; i++) {
            double x = start[0] + dx * i;
            double y = start[1] + dy * i;
            double z = start[2] + dz * i;
            player.getWorld().spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }
}