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
import org.migrate1337.viotrap.items.RevealItem;

public class RevealItemListener implements Listener {
    private final VioTrap plugin;

    public RevealItemListener(VioTrap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseRevealItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.isSimilar(RevealItem.getRevealItem(item.getAmount()))) return;

        if (player.hasCooldown(item.getType())) {
            player.sendMessage("§cПодождите перед использованием снова!");
            return;
        }

        item.setAmount(item.getAmount() - 1);

        int cooldownSeconds = plugin.getRevealItemCooldown();
        int durationSeconds = plugin.getRevealItemGlowDuration();
        player.setCooldown(item.getType(), cooldownSeconds * 20);

        int radius = plugin.getRevealItemRadius();
        Location playerLocation = player.getLocation();

        for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (nearbyPlayer.equals(player)) continue;

            if (nearbyPlayer.getLocation().distance(playerLocation) <= radius) {
                if (nearbyPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    nearbyPlayer.removePotionEffect(PotionEffectType.INVISIBILITY);
                }
                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationSeconds * 20, 0));
            }
        }

        String soundType = plugin.getRevealItemSoundType();
        float volume = plugin.getRevealItemSoundVolume();
        float pitch = plugin.getRevealItemSoundPitch();
        player.playSound(playerLocation, Sound.valueOf(soundType), volume, pitch);

        player.sendMessage("§aЭффекты сняты с игроков!");

        showParticleCircle(playerLocation, radius, Particle.valueOf(VioTrap.getPlugin().getRevealItemParticleType()));
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
