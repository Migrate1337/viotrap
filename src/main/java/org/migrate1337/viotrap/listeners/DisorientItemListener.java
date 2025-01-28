package org.migrate1337.viotrap.listeners;

import com.github.sirblobman.combatlogx.api.object.TagReason;
import com.github.sirblobman.combatlogx.api.object.TagType;
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
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.PVPManagerHandle;
import org.migrate1337.viotrap.items.DisorientItem;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

public class DisorientItemListener implements Listener {
    private final VioTrap plugin;
    private final CombatLogXHandler combatLogXHandler;
    private final PVPManagerHandle pvpManagerHandler;

    public DisorientItemListener(VioTrap plugin) {
        this.plugin = plugin;
        this.combatLogXHandler = new CombatLogXHandler();
        this.pvpManagerHandler = new PVPManagerHandle();
    }

    @EventHandler
    public void onPlayerUseDisorientItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.isSimilar(DisorientItem.getDisorientItem(item.getAmount()))) return;
        if (!event.getAction().toString().contains("RIGHT_CLICK")) return;

        if (player.hasCooldown(item.getType())) {
            player.sendMessage("§cПодождите перед использованием снова!");
            return;
        }

        Location location = player.getLocation();
        String worldName = location.getWorld().getName();

        if (isInBannedRegion(location, worldName)) {
            player.sendMessage("§cВы не можете использовать данный предмет в этом регионе!");
            return;
        }

        int radius = plugin.getDisorientItemRadius();
        Location playerLocation = player.getLocation();

        boolean foundOpponent = false;

        for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (nearbyPlayer.equals(player)) continue;

            if (nearbyPlayer.getLocation().distance(playerLocation) <= radius) {
                foundOpponent = true;

                if (combatLogXHandler.isCombatLogXEnabled()) {
                    combatLogXHandler.tagPlayer(nearbyPlayer, TagType.DAMAGE, TagReason.ATTACKED);
                    nearbyPlayer.sendMessage(plugin.getConfig().getString("disorient_item.messages.pvp-enabled-for-player"));
                }

                if (pvpManagerHandler.isPvPManagerEnabled()) {
                    pvpManagerHandler.tagPlayerForPvP(nearbyPlayer);
                    nearbyPlayer.sendMessage(plugin.getConfig().getString("disorient_item.messages.pvp-enabled-for-player"));
                }

                List<Map<?, ?>> negativeEffects = plugin.getConfig().getMapList("disorient_item.negative_effects");
                for (Map<?, ?> effect : negativeEffects) {
                    for (Map.Entry<?, ?> entry : effect.entrySet()) {
                        String effectName = (String) entry.getKey();
                        Map<?, ?> effectDetails = (Map<?, ?>) entry.getValue();
                        PotionEffectType effectType = PotionEffectType.getByName(effectName);
                        if (effectType != null) {
                            int duration = (int) effectDetails.get("duration") * 20;
                            int amplifier = (int) effectDetails.get("amplifier");
                            nearbyPlayer.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
                        }
                    }
                }
            }
        }

        if (combatLogXHandler.isCombatLogXEnabled()) {
            if (foundOpponent) {
                combatLogXHandler.tagPlayer(player, TagType.DAMAGE, TagReason.UNKNOWN);
                player.sendMessage(plugin.getConfig().getString("disorient_item.messages.pvp-enabled-by-player"));
            }
        }
        if (pvpManagerHandler.isPvPManagerEnabled()) {
            if (foundOpponent) {
                pvpManagerHandler.tagPlayerForPvP(player);
                player.sendMessage(plugin.getConfig().getString("disorient_item.messages.pvp-enabled-by-player"));
            }
        }
        item.setAmount(item.getAmount() - 1);


        int cooldownSeconds = plugin.getDisorientItemCooldown();
        player.setCooldown(item.getType(), cooldownSeconds * 20);

        String soundType = plugin.getDisorientItemSoundType();
        float volume = plugin.getDisorientItemSoundVolume();
        float pitch = plugin.getDisorientItemSoundPitch();
        player.playSound(playerLocation, Sound.valueOf(soundType), volume, pitch);

        showParticleCircle(playerLocation, radius, Particle.valueOf(VioTrap.getPlugin().getDisorientItemParticleType()));
    }

    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));

        if (regionManager == null) {
            return false;
        }

        List<String> bannedRegions = plugin.getConfig().getStringList("disorient_item.banned_regions");

        com.sk89q.worldedit.math.BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return regionManager.getApplicableRegions(vector).getRegions()
                .stream()
                .anyMatch(region -> bannedRegions.contains(region.getId()));
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
