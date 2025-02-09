package org.migrate1337.viotrap.listeners;

import com.github.sirblobman.combatlogx.api.object.TagReason;
import com.github.sirblobman.combatlogx.api.object.TagType;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.PVPManagerHandle;
import org.migrate1337.viotrap.items.FirestormItem;

import java.util.List;
import java.util.Set;

public class FirestormItemListener implements Listener {
    private final VioTrap plugin;
    private final CombatLogXHandler combatLogXHandler;
    private final PVPManagerHandle pvpManagerHandler;

    public FirestormItemListener(VioTrap plugin) {
        this.plugin = plugin;
        this.combatLogXHandler = new CombatLogXHandler();
        this.pvpManagerHandler = new PVPManagerHandle();
    }

    @EventHandler
    public void onPlayerUseFirestormItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.isSimilar(FirestormItem.getFirestormItem(item.getAmount(), plugin))) return;
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

        int radius = plugin.getFirestormItemRadius();
        Location playerLocation = player.getLocation();

        boolean foundOpponent = false;

        for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (nearbyPlayer.equals(player)) continue;

            if (nearbyPlayer.getLocation().distance(playerLocation) <= radius) {
                foundOpponent = true;

                if (combatLogXHandler.isCombatLogXEnabled()) {
                    combatLogXHandler.tagPlayer(nearbyPlayer, TagType.DAMAGE, TagReason.ATTACKED);
                    nearbyPlayer.sendMessage(plugin.getConfig().getString("firestorm_item.messages.pvp-enabled-for-player"));
                }

                if (pvpManagerHandler.isPvPManagerEnabled()) {
                    pvpManagerHandler.tagPlayerForPvP(nearbyPlayer);
                    nearbyPlayer.sendMessage(plugin.getConfig().getString("firestorm_item.messages.pvp-enabled-for-player"));
                }

                nearbyPlayer.setFireTicks(plugin.getFirestormItemFireDuration() * 20);
            }
        }

        if (combatLogXHandler.isCombatLogXEnabled()) {
            if (foundOpponent) {
                combatLogXHandler.tagPlayer(player, TagType.DAMAGE, TagReason.UNKNOWN);
                player.sendMessage(plugin.getConfig().getString("firestorm_item.messages.pvp-enabled-by-player"));
            }
        }
        if (pvpManagerHandler.isPvPManagerEnabled()) {
            if (foundOpponent) {
                pvpManagerHandler.tagPlayerForPvP(player);
                player.sendMessage(plugin.getConfig().getString("firestorm_item.messages.pvp-enabled-by-player"));
            }
        }
        item.setAmount(item.getAmount() - 1);

        int cooldownSeconds = plugin.getFirestormItemCooldown();
        player.setCooldown(item.getType(), cooldownSeconds * 20);

        String soundType = plugin.getFirestormItemSoundType();
        float volume = plugin.getFirestormItemSoundVolume();
        float pitch = plugin.getFirestormItemSoundPitch();
        player.playSound(playerLocation, Sound.valueOf(soundType), volume, pitch);

        showParticleCircle(playerLocation, radius, Particle.FLAME);
    }

    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));
        if (plugin.getConfig().getBoolean("firestorm_item.disabled_all_regions", false)) {
            return regionManager != null && regionManager.getApplicableRegions(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                    .getRegions().stream().anyMatch(region -> !"__default__".equals(region.getId()));
        }

        if (regionManager == null) {
            return false;
        }

        java.util.List<String> bannedRegions = plugin.getConfig().getStringList("firestorm_item.banned_regions");
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

        ConfigurationSection bannedFlagsSection = plugin.getConfig().getConfigurationSection("firestorm_item.banned_region_flags");
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
