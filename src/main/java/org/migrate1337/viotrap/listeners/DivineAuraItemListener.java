package org.migrate1337.viotrap.listeners;

import com.github.sirblobman.combatlogx.api.ICombatLogX;
import com.github.sirblobman.combatlogx.api.manager.ICombatManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.DivineAuraItem;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        if (isInBannedRegion(location, location.getWorld().getName()) || hasBannedRegionFlags(location, location.getWorld().getName())) {
            player.sendMessage("§cВы не можете использовать данный предмет в этом регионе!");
            return;
        }
        item.setAmount(item.getAmount() - 1);
        player.sendMessage(plugin.getConfig().getString("divine_aura.messages.success_used"));
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
                    player.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
                }
            }
        }

        player.sendMessage("§aВы сняли с себя негативные эффекты и получили благословение!");
    }
    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (plugin.getConfig().getBoolean("divine_aura.disabled_all_regions", false)) {
            return regionManager != null && regionManager.getApplicableRegions(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                    .getRegions().stream().anyMatch(region -> !"__default__".equals(region.getId()));
        }

        if (regionManager == null) {
            return false;
        }

        java.util.List<String> bannedRegions = plugin.getConfig().getStringList("divine_aura.banned_regions");
        BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        return regionManager.getApplicableRegions(vector).getRegions()
                .stream()
                .anyMatch(region -> bannedRegions.contains(region.getId()));
    }
    private boolean hasBannedRegionFlags(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));

        if (regionManager == null) {
            return false;
        }

        ConfigurationSection bannedFlagsSection = plugin.getConfig().getConfigurationSection("divine_aura.banned_region_flags");
        if (bannedFlagsSection == null) {
            return false;
        }

        BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        Set<ProtectedRegion> regions = regionManager.getApplicableRegions(vector).getRegions();


        for (ProtectedRegion region : regions) {

            for (String flagName : bannedFlagsSection.getKeys(false)) {
                StateFlag flag = (StateFlag) Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
                if (flag == null) {
                    continue;
                }

                if (region.getFlag(flag) != null) {
                    return true;
                }
            }
        }
        return false;
    }
}
