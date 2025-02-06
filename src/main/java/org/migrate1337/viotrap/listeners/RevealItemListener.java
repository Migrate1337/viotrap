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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.RevealItem;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.PVPManagerHandle;

import java.util.Set;

public class RevealItemListener implements Listener {
    private final VioTrap plugin;
    private final CombatLogXHandler combatLogXHandler;
    private final PVPManagerHandle pvpManagerHandler;
    public RevealItemListener(VioTrap plugin) {
        this.plugin = plugin;
        this.combatLogXHandler = new CombatLogXHandler();
        this.pvpManagerHandler = new PVPManagerHandle();
    }

    @EventHandler
    public void onPlayerUseRevealItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.isSimilar(RevealItem.getRevealItem(item.getAmount()))) return;
        if (!event.getAction().toString().contains("RIGHT_CLICK")) return;
        if (player.hasCooldown(item.getType())) {
            player.sendMessage("§cПодождите перед использованием снова!");
            return;
        }

        item.setAmount(item.getAmount() - 1);
        Location location = player.getLocation();
        String worldName = location.getWorld().getName();

        if (isInBannedRegion(location, location.getWorld().getName()) || hasBannedRegionFlags(location, location.getWorld().getName())) {
            player.sendMessage("§cВы не можете установить трапку в этом месте!");
            return;
        }
        int cooldownSeconds = plugin.getRevealItemCooldown();
        int durationSeconds = plugin.getRevealItemGlowDuration();
        player.setCooldown(item.getType(), cooldownSeconds * 20);

        int radius = plugin.getRevealItemRadius();
        Location playerLocation = player.getLocation();
        boolean foundOpponent = false;

        for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (nearbyPlayer.equals(player)) continue;

            if (nearbyPlayer.getLocation().distance(playerLocation) <= radius) {
                foundOpponent = true;
                if (combatLogXHandler.isCombatLogXEnabled()) {
                    nearbyPlayer.sendMessage(plugin.getConfig().getString("reveal_item.messages.pvp-enabled-for-player"));
                    combatLogXHandler.tagPlayer(nearbyPlayer, TagType.DAMAGE, TagReason.ATTACKED);
                }
                if (pvpManagerHandler.isPvPManagerEnabled()) {
                    pvpManagerHandler.tagPlayerForPvP(nearbyPlayer);
                    nearbyPlayer.sendMessage(plugin.getConfig().getString("reveal_item.messages.pvp-enabled-for-player"));
                }
                boolean wasInvisible = nearbyPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY);
                int remainingInvisibilityTime = 0;
                if (wasInvisible) {
                    PotionEffect invisibilityEffect = nearbyPlayer.getPotionEffect(PotionEffectType.INVISIBILITY);
                    remainingInvisibilityTime = invisibilityEffect.getDuration();
                    nearbyPlayer.removePotionEffect(PotionEffectType.INVISIBILITY);
                }
                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationSeconds * 20, 0));

                if (wasInvisible) {
                    int finalRemainingInvisibilityTime = remainingInvisibilityTime - durationSeconds * 20;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, finalRemainingInvisibilityTime, 0, false, false));
                        }
                    }.runTaskLater(plugin, durationSeconds * 20L);
                }
            }
        }

        if (combatLogXHandler.isCombatLogXEnabled()) {
            if (foundOpponent) {
                combatLogXHandler.tagPlayer(player, TagType.DAMAGE, TagReason.UNKNOWN);
                player.sendMessage(plugin.getConfig().getString("reveal_item.messages.pvp-enabled-by-player"));
            }
        }
        if (pvpManagerHandler.isPvPManagerEnabled()) {
            if (foundOpponent) {
                pvpManagerHandler.tagPlayerForPvP(player);
                player.sendMessage(plugin.getConfig().getString("reveal_item.messages.pvp-enabled-by-player"));
            }
        }
        String soundType = plugin.getRevealItemSoundType();
        float volume = plugin.getRevealItemSoundVolume();
        float pitch = plugin.getRevealItemSoundPitch();
        player.playSound(playerLocation, Sound.valueOf(soundType), volume, pitch);


        showParticleCircle(playerLocation, radius, Particle.valueOf(VioTrap.getPlugin().getRevealItemParticleType()));
    }

    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));

        if (regionManager == null) {
            return false;
        }

        java.util.List<String> bannedRegions = plugin.getConfig().getStringList("reveal_item.banned_regions");

        com.sk89q.worldedit.math.BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
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

        ConfigurationSection bannedFlagsSection = plugin.getConfig().getConfigurationSection("reveal_item.banned_region_flags");
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
