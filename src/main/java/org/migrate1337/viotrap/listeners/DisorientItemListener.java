package org.migrate1337.viotrap.listeners;

import com.github.sirblobman.combatlogx.api.ICombatLogX;
import com.github.sirblobman.combatlogx.api.manager.ICombatManager;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.DisorientItem;

import java.util.List;
import java.util.Map;

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
        if (!event.getAction().toString().contains("RIGHT_CLICK")) return;
        ICombatLogX combatLogX = getAPI();
        ICombatManager combatManager = combatLogX.getCombatManager();
        combatManager.tag(player, null, TagType.DAMAGE, TagReason.UNKNOWN);
        player.sendMessage(plugin.getConfig().getString("disorient_item.messages.pvp-enabled-by-player"));
        if (player.hasCooldown(item.getType())) {
            player.sendMessage("§cПодождите перед использованием снова!");
            return;
        }
        Location location = player.getLocation();
        String worldName = location.getWorld().getName();

        // Проверяем, находится ли игрок в запрещённом регионе
        if (isInBannedRegion(location, worldName)) {
            player.sendMessage("§cВы не можете использовать данный предмет в этом регионе!");
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

                // Негативные эффекты
                combatManager.tag(nearbyPlayer, null, TagType.DAMAGE, TagReason.ATTACKED);
                List<Map<?, ?>> positiveEffects = plugin.getConfig().getMapList("disorient_item.negative_effects");
                for (Map<?, ?> effect : positiveEffects) {
                    for (Map.Entry<?, ?> entry : effect.entrySet()) {
                        String effectName = (String) entry.getKey();
                        Map<?, ?> effectDetails = (Map<?, ?>) entry.getValue();
                        PotionEffectType effectType = PotionEffectType.getByName(effectName);
                        if (effectType != null) {
                            int duration = (int) effectDetails.get("duration") * 20;
                            int amplifier = (int) effectDetails.get("amplifier");
                            nearbyPlayer.addPotionEffect(new PotionEffect(effectType, duration, amplifier)); // Накладываем позитивный эффект
                        }
                    }
                }
            }
        }

        // Воспроизведение звука
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

        // Получаем список запрещённых регионов из конфига
        java.util.List<String> bannedRegions = plugin.getConfig().getStringList("disorent_item.banned_regions");

        // Получаем все регионы, содержащие точку установки
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

    public ICombatLogX getAPI() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin plugin = pluginManager.getPlugin("CombatLogX");
        return (ICombatLogX) plugin;
    }
}
