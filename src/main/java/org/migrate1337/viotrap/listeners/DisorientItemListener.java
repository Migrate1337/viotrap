package org.migrate1337.viotrap.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.DisorientItem;

public class DisorientItemListener implements Listener {
    private final VioTrap plugin;

    public DisorientItemListener(VioTrap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseDisorientItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.isSimilar(DisorientItem.getDisorientItem(item.getAmount()))) return;

        if (player.hasCooldown(item.getType())) {
            player.sendMessage("§cПодождите перед использованием снова!");
            return;
        }

        item.setAmount(item.getAmount() - 1);

        int cooldownSeconds = plugin.getDisorientItemCooldown();
        int durationSeconds = plugin.getDisorientItemEffectDuration();
        player.setCooldown(item.getType(), cooldownSeconds * 20);

        int radius = plugin.getDisorientItemRadius();
        Location playerLocation = player.getLocation();

        for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (nearbyPlayer.equals(player)) continue;

            if (nearbyPlayer.getLocation().distance(playerLocation) <= radius) {
                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationSeconds * 20, 0));
                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, durationSeconds * 20, 1));
                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, durationSeconds * 20, 0));
            }
        }

        // Воспроизведение звука
        String soundType = plugin.getDisorientItemSoundType();
        float volume = plugin.getDisorientItemSoundVolume();
        float pitch = plugin.getDisorientItemSoundPitch();
        player.playSound(playerLocation, Sound.valueOf(soundType), volume, pitch);

        player.sendMessage("§cИгроки в радиусе получили негативные эффекты!");

        showParticleCircle(playerLocation, radius, Particle.valueOf(VioTrap.getPlugin().getDisorientItemParticleType()));
    }

    private void showParticleCircle(Location center, double radius, Particle particle) {
        int points = 100;
        double increment = (2 * Math.PI) / points;

        for (int i = 0; i < points; i++) {
            double angle = i * increment;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            Location particleLocation = new Location(center.getWorld(), x, center.getY(), z);
            center.getWorld().spawnParticle(particle, particleLocation, 1, 0, 0, 0, 0);
        }
    }
}
