package org.migrate1337.viotrap.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
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
import org.migrate1337.viotrap.items.DivineAuraItem;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DivineAuraItemListener implements Listener {
    private final VioTrap plugin;

    public DivineAuraItemListener(VioTrap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseDivineAuraItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.isSimilar(DivineAuraItem.getDivineAuraItem(item.getAmount(), plugin))) return;
        if (!event.getAction().toString().contains("RIGHT_CLICK")) return;

        if (player.hasCooldown(item.getType())) {
            player.sendMessage("§cПодождите перед использованием снова!");
            return;
        }
        Location location = player.getLocation();
        String worldName = location.getWorld().getName();

        if (isInBannedRegion(location, worldName)) {
            player.sendMessage("§cВы не можете установить пласт в этом регионе!");
            return;
        }
        item.setAmount(item.getAmount() - 1);

        int cooldownSeconds = plugin.getDivineAuraItemCooldown();
        player.setCooldown(item.getType(), cooldownSeconds * 20);

        Location playerLocation = player.getLocation();
        String particleType = plugin.getDivineAuraItemParticleType();
        String soundType = plugin.getDivineAuraItemSoundType();
        float volume = plugin.getDivineAuraItemSoundVolume();
        float pitch = plugin.getDivineAuraItemSoundPitch();

        player.getWorld().spawnParticle(Particle.valueOf(particleType), playerLocation, 50, 0.5, 1, 0.5, 0.05);
        player.getWorld().playSound(playerLocation, Sound.valueOf(soundType), volume, pitch);

        List<String> negativeEffects = plugin.getConfig().getStringList("divine_aura.negative_effects");
        for (String effect : negativeEffects) {
            PotionEffectType effectType = PotionEffectType.getByName(effect);
            if (effectType != null) {
                player.removePotionEffect(effectType);
            }
        }

        List<Map<?, ?>> positiveEffects = plugin.getConfig().getMapList("divine_aura.positive_effects");
        for (Map<?, ?> effect : positiveEffects) {
            for (Map.Entry<?, ?> entry : effect.entrySet()) {
                String effectName = (String) entry.getKey();
                Map<?, ?> effectDetails = (Map<?, ?>) entry.getValue();
                PotionEffectType effectType = PotionEffectType.getByName(effectName);
                if (effectType != null) {
                    int duration = (int) effectDetails.get("duration") * 20;
                    int amplifier = (int) effectDetails.get("amplifier");
                    player.addPotionEffect(new PotionEffect(effectType, duration, amplifier)); // Накладываем позитивный эффект
                }
            }
        }

        player.sendMessage("§aВы сняли с себя негативные эффекты и получили благословение!");
    }
    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));

        if (regionManager == null) {
            return false;
        }

        java.util.List<String> bannedRegions = plugin.getConfig().getStringList("divine_aura.banned_regions");

        com.sk89q.worldedit.math.BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return regionManager.getApplicableRegions(vector).getRegions()
                .stream()
                .anyMatch(region -> bannedRegions.contains(region.getId()));
    }
}
