package org.migrate1337.viotrap.listeners;

import com.github.sirblobman.combatlogx.api.ICombatLogX;
import com.github.sirblobman.combatlogx.api.manager.ICombatManager;
import com.github.sirblobman.combatlogx.api.object.TagReason;
import com.github.sirblobman.combatlogx.api.object.TagType;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.migrate1337.viotrap.VioTrap;
import org.migrate1337.viotrap.items.TrapItem;
import org.migrate1337.viotrap.utils.CombatLogXHandler;
import org.migrate1337.viotrap.utils.PVPManagerHandle;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrapItemListener implements Listener {
    private final VioTrap plugin;
    private final Map<UUID, Map<Location, Material>> playerReplacedBlocks = new HashMap<>();
    private final Map<UUID, Long> lastUseTime = new HashMap<>();
    private final Map<String, ProtectedCuboidRegion> activeTraps = new HashMap<>();
    private final CombatLogXHandler combatLogXHandler;
    private final PVPManagerHandle pvpManagerHandler;
    public TrapItemListener(VioTrap plugin) {
        this.plugin = plugin;
        this.combatLogXHandler = new CombatLogXHandler();
        this.pvpManagerHandler = new PVPManagerHandle();
    }

    @EventHandler
    public void onPlayerUseTrap(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !TrapItem.isTrapItem(item)) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (player.hasCooldown(item.getType())) {
            player.sendMessage(plugin.getTrapMessageCooldown());
            return;
        }

        Location location = player.getLocation();
        String skin = TrapItem.getSkin(item);
        String schematic = plugin.getSkinSchematic(skin);
        if (!plugin.getConfig().contains("skins." + skin)) {
            player.sendMessage("§cСкин не найден в конфигурации.");
            return;
        }

        if (isInBannedRegion(location, location.getWorld().getName())) {
            player.sendMessage("§cВы не можете установить трапку в этом регионе!");
            return;
        }

        if (isRegionNearby(location, location.getWorld().getName())) {
            player.sendMessage(plugin.getTrapMessageNearby());
            return;
        }


        player.setCooldown(item.getType(), plugin.getTrapCooldown() * 20);
        player.playSound(location, Sound.valueOf(plugin.getTrapSoundType()), plugin.getTrapSoundVolume(), plugin.getTrapSoundPitch());



        // Логика создания региона и установки схематики
        try {
            File schematicFile = new File("plugins/WorldEdit/schematics/" + schematic);
            try (ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile))) {
                Clipboard clipboard = reader.read();
                com.sk89q.worldedit.math.BlockVector3 min = clipboard.getRegion().getMinimumPoint();
                com.sk89q.worldedit.math.BlockVector3 max = clipboard.getRegion().getMaximumPoint();

                double sizeX = max.getBlockX() - min.getBlockX() + 1;
                double sizeY = max.getBlockY() - min.getBlockY() + 1;
                double sizeZ = max.getBlockZ() - min.getBlockZ() + 1;
                // Применяем эффекты для игрока
                applyEffects(player, "skins." + skin + ".effects.player");

                // Применяем эффекты для противников
                location.getWorld().getNearbyEntities(location, sizeX - 3, sizeY, sizeZ - 3, entity -> entity instanceof Player && !entity.equals(player))
                        .forEach(entity -> {
                            if (entity instanceof Player opponent) {
                                applyEffects(opponent, "skins." + skin + ".effects.opponents");
                            }
                        });
                createTrapRegion(player, location);
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
                    BlockVector3 pastePosition = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    saveReplacedBlocks(player.getUniqueId(), location, clipboard);

                    ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, clipboard.getRegion(), clipboard.getOrigin(), editSession, pastePosition);
                    Operations.complete(copy);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        restoreBlocks(player.getUniqueId());
                        removeTrapRegion(player.getName() + "_trap", location);
                    }, plugin.getTrapDuration() * 20);
                }

                item.setAmount(item.getAmount() - 1);
            }
        } catch (Exception e) {
            player.sendMessage(plugin.getTrapMessageFailed());
            e.printStackTrace();
        }
    }
    private void applyEffects(Player player, String configPath) {
        if (!plugin.getConfig().contains(configPath)) {
            return;
        }

        plugin.getConfig().getConfigurationSection(configPath).getKeys(false).forEach(effectName -> {
            try {
                int duration = plugin.getTrapDuration() * 20;
                int amplifier = plugin.getConfig().getInt(configPath + "." + effectName + ".amplifier");
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName(effectName), duration, amplifier));
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка применения эффекта: " + effectName);
            }
        });
    }


    private boolean isInBannedRegion(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));

        if (regionManager == null) {
            return false;
        }

        java.util.List<String> bannedRegions = plugin.getConfig().getStringList("trap.banned_regions");

        com.sk89q.worldedit.math.BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return regionManager.getApplicableRegions(vector).getRegions()
                .stream()
                .anyMatch(region -> bannedRegions.contains(region.getId()));
    }


    private boolean isRegionNearby(Location location, String worldName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(Bukkit.getWorld(worldName)));

        if (regionManager != null) {
            com.sk89q.worldedit.math.BlockVector3 min = BlockVector3.at(location.getBlockX() - 3, location.getBlockY() - 3, location.getBlockZ() - 3);
            com.sk89q.worldedit.math.BlockVector3 max = BlockVector3.at(location.getBlockX() + 3, location.getBlockY() + 3, location.getBlockZ() + 3);

            ProtectedCuboidRegion checkRegion = new ProtectedCuboidRegion("checkRegion", min, max);

            return regionManager.getApplicableRegions(checkRegion).getRegions().stream()
                    .anyMatch(region -> region.getId().endsWith("_trap") || region.getId().startsWith("plate_"));
        }
        return false;
    }


    private void saveReplacedBlocks(UUID playerId, Location startLocation, Clipboard clipboard) {
        Map<Location, Material> replacedBlocks = new HashMap<>();
        com.sk89q.worldedit.math.BlockVector3 min = clipboard.getRegion().getMinimumPoint();
        com.sk89q.worldedit.math.BlockVector3 max = clipboard.getRegion().getMaximumPoint();

        int offsetX = -2;
        int offsetY = -1;
        int offsetZ = -2;

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Location blockLocation = startLocation.clone().add(
                            x - min.getBlockX() + offsetX,
                            y - min.getBlockY() + offsetY,
                            z - min.getBlockZ() + offsetZ
                    );
                    replacedBlocks.put(blockLocation, blockLocation.getBlock().getType());
                }
            }
        }

        playerReplacedBlocks.put(playerId, replacedBlocks);
    }

    private void restoreBlocks(UUID playerId) {
        Map<Location, Material> replacedBlocks = playerReplacedBlocks.get(playerId);
        if (replacedBlocks != null) {
            for (Map.Entry<Location, Material> entry : replacedBlocks.entrySet()) {
                Location location = entry.getKey();
                Material originalMaterial = entry.getValue();
                location.getBlock().setType(originalMaterial);
            }
            playerReplacedBlocks.remove(playerId);
        }
    }

    public void createTrapRegion(Player player, Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));

        if (regionManager == null) {
            return;
        }

        com.sk89q.worldedit.math.BlockVector3 min = BlockVector3.at(location.getBlockX() - 2, location.getBlockY() - 2, location.getBlockZ() - 2);
        com.sk89q.worldedit.math.BlockVector3 max = BlockVector3.at(location.getBlockX() + 2, location.getBlockY() + 3, location.getBlockZ() + 2);

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(player.getName() + "_trap", min, max);

        regionManager.addRegion(region);
        activeTraps.put(player.getName() + "_trap", region);
    }

    public void removeTrapRegion(String regionName, Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));

        if (regionManager != null) {
            regionManager.removeRegion(regionName);
            activeTraps.remove(regionName);
        }
    }
}
